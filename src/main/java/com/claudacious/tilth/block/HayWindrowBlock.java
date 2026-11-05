package com.claudacious.tilth.block;

import com.claudacious.tilth.registry.TilthItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;

/**
 * A windrow of cut grass drying in the field. Fresh grass wilts, cures and dries over about a game day of fair
 * weather; turning it with a hoe speeds it up. Rain on it once it has started to cure can spoil it into mouldy hay.
 * Dry windrows are gathered (broken) for dried hay: nine make a hay bale.
 */
public class HayWindrowBlock extends Block {
    public static final MapCodec<HayWindrowBlock> CODEC = simpleCodec(HayWindrowBlock::new);
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 4);
    /** 0 fresh, 1 wilted, 2 curing, 3 dry. */
    public static final IntegerProperty DRYNESS = IntegerProperty.create("dryness", 0, 3);
    public static final BooleanProperty SPOILED = BooleanProperty.create("spoiled");
    private static final VoxelShape[] SHAPES = {Block.box(0, 0, 0, 16, 2, 16), Block.box(0, 0, 0, 16, 3, 16),
            Block.box(0, 0, 0, 16, 4, 16), Block.box(0, 0, 0, 16, 5, 16)};

    public HayWindrowBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 1).setValue(DRYNESS, 0).setValue(SPOILED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS, DRYNESS, SPOILED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS) - 1];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(SPOILED) && state.getValue(DRYNESS) < 3;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        dry(state, level, pos, random, 0.2f);
    }

    private static void dry(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, float chance) {
        int dryness = state.getValue(DRYNESS);
        if (level.isRainingAt(pos.above())) {
            // Rain on curing hay washes it out and moulds it; fresh grass only gets wet again.
            if (dryness >= 1 && random.nextFloat() < 0.35f) {
                level.setBlock(pos, state.setValue(SPOILED, true), 3);
            } else if (dryness > 0 && random.nextFloat() < 0.5f) {
                level.setBlock(pos, state.setValue(DRYNESS, dryness - 1), 3);
            }
            return;
        }
        if (level.isDay() && random.nextFloat() < chance) {
            level.setBlock(pos, state.setValue(DRYNESS, dryness + 1), 3);
        }
    }

    /** Lay another armful on the windrow. */
    public static boolean addLayer(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof HayWindrowBlock && state.getValue(LAYERS) < 4 && !state.getValue(SPOILED)) {
            // Fresh grass mixed in wets the windrow again.
            level.setBlock(pos, state.setValue(LAYERS, state.getValue(LAYERS) + 1).setValue(DRYNESS, Math.min(1, state.getValue(DRYNESS))), 3);
            return true;
        }
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (stack.is(TilthItems.CUT_GRASS.get())) {
            if (!level.isClientSide && addLayer(level, pos)) {
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.canPerformAction(ItemAbilities.HOE_TILL)) {
            if (level instanceof ServerLevel server && !state.getValue(SPOILED) && state.getValue(DRYNESS) < 3) {
                // Turning (tedding) lets air through: a good chance to move a stage on.
                dry(state, server, pos, server.random, 0.5f);
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                level.playSound(null, pos, SoundEvents.GRASS_HIT, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            String stage = state.getValue(SPOILED) ? "Spoiled by rain: mouldy hay, only fit for compost."
                    : switch (state.getValue(DRYNESS)) {
                        case 0 -> "Fresh-cut grass. It needs a day of fair weather to make hay.";
                        case 1 -> "Wilted. Keep it out of the rain from here on.";
                        case 2 -> "Curing. Nearly hay; turn it with a hoe to finish it.";
                        default -> "Dry hay. Break it to gather it.";
                    };
            player.displayClientMessage(Component.literal(stage).withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
