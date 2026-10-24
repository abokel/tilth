package com.claudacious.tilth.block;

import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import com.claudacious.tilth.item.SoilProbeItem;
import com.claudacious.tilth.item.KnifeItem;
import com.claudacious.tilth.item.MoistureMeterItem;
import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.sim.crop.Bbch;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.CropState;
import com.claudacious.tilth.sim.crop.Crops;
import com.claudacious.tilth.sim.crop.GrowthForm;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** A planted crop of one species with ten visual growth stages mapped onto the BBCH scale. */
public class TilthCropBlock extends BushBlock implements EntityBlock, BonemealableBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, CropSpecies.STAGES - 1);
    public static final EnumProperty<CropCondition> CONDITION = EnumProperty.create("condition", CropCondition.class);
    public static final MapCodec<TilthCropBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("species").forGetter(b -> b.species.id),
            propertiesCodec()
    ).apply(i, (s, p) -> new TilthCropBlock(Crops.byId(s), p)));
    private static final int[] HEIGHTS = {2, 3, 5, 7, 9, 11, 13, 14, 15, 16};

    private final CropSpecies species;
    private final VoxelShape[] shapes = new VoxelShape[CropSpecies.STAGES];

    public TilthCropBlock(CropSpecies species, Properties properties) {
        super(properties);
        this.species = species;
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(CONDITION, CropCondition.HEALTHY));
        boolean low = species.form == GrowthForm.ROOT || species.form == GrowthForm.BULB || species.form == GrowthForm.HEAD_VEGETABLE;
        for (int i = 0; i < shapes.length; i++) {
            double h = HEIGHTS[i];
            if (low) {
                h = Math.min(h, 10);
            }
            if (species.isTall(i)) {
                h = 16 + (i - species.tallFromStage + 1) * 3.0;
            }
            shapes[i] = box(1, 0, 1, 15, Math.min(30, h), 15);
        }
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    public CropSpecies species() {
        return species;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(STAGE, CONDITION);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(STAGE)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof TilthFarmlandBlock;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CropBlockEntity(pos, state);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos.below()) instanceof FarmlandBlockEntity farmland) {
            farmland.advance(level);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Bring the soil up to today before the seed goes in, so the crop starts its life now.
        if (level instanceof ServerLevel server && !oldState.is(this) && level.getBlockEntity(pos.below()) instanceof FarmlandBlockEntity farmland) {
            farmland.advance(server);
        }
    }

    /** The knife, soil probe and moisture meter work on the crop itself rather than opening the crop report. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof KnifeItem || stack.getItem() instanceof SoilProbeItem || stack.getItem() instanceof MoistureMeterItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof CropBlockEntity crop)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos.below()) instanceof FarmlandBlockEntity farmland) {
            farmland.advance(server);
        }
        if (!(level.getBlockEntity(pos) instanceof CropBlockEntity current)) {
            return InteractionResult.SUCCESS;
        }
        CropState cs = current.cropState();
        if (species.isHarvestable(cs)) {
            current.harvest(server, player);
        } else {
            player.displayClientMessage(status(species, cs), true);
        }
        return InteractionResult.SUCCESS;
    }

    public static Component status(CropSpecies species, CropState cs) {
        int stage = species.visualStage(cs);
        String text;
        if (!cs.germinated) {
            text = species.name + ": sown, waiting to germinate (" + Math.round(100 * cs.germinationGdd / species.germinationGdd) + "%)";
        } else {
            int pct = (int) Math.round(100 * Math.min(1.0, cs.dev));
            text = String.format("%s: %s | %d%% to maturity | limiting: %s", species.name, Bbch.describe(species.bbchFor(stage)), pct, cs.limitingFactor);
        }
        return Component.literal(text).withStyle(cs.lastGrowthFactor > 0.7 ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof CropBlockEntity crop) {
            if (level.getBlockEntity(pos.below()) instanceof FarmlandBlockEntity farmland) {
                farmland.advance(server);
            }
            crop.prepareBreak(server);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof CropBlockEntity crop) {
            return crop.breakDrops(params.getLevel());
        }
        return List.of();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(species.seedItem)));
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return TilthConfig.BONE_MEAL_GROWS_CROPS.getAsBoolean() && state.getValue(STAGE) < species.harvestStage;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof CropBlockEntity crop) {
            crop.boost(level, 0.05);
        }
    }
}
