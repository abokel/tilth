package com.claudacious.tilth.block;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.sim.soil.SoilHorizon;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.sim.soil.Tillage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

/**
 * Tilled soil. The block entity holds the full soil state (nutrients, pH, organic matter, moisture,
 * compaction, residue, crop history) and runs the daily field simulation.
 */
public class TilthFarmlandBlock extends Block implements EntityBlock, SoilLike {
    public static final IntegerProperty MOISTURE = BlockStateProperties.MOISTURE;
    public static final MapCodec<TilthFarmlandBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("texture").forGetter(b -> b.texture.id()),
            propertiesCodec()
    ).apply(i, (t, p) -> new TilthFarmlandBlock(SoilTexture.byId(t), p)));
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

    private final SoilTexture texture;

    public TilthFarmlandBlock(SoilTexture texture, Properties properties) {
        super(properties);
        this.texture = texture;
        registerDefaultState(stateDefinition.any().setValue(MOISTURE, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public SoilTexture texture() {
        return texture;
    }

    @Override
    public SoilHorizon horizon() {
        return SoilHorizon.TOPSOIL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOISTURE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return !defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos())
                ? TilthBlocks.topsoil(texture) : super.getStateForPlacement(context);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        if (facing == Direction.UP && !state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, facing, facingState, level, pos, facingPos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return !above.isSolid() || above.getBlock() instanceof FenceGateBlock || above.getBlock() instanceof MovingPistonBlock;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            revertToSoil(level, pos, null);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof FarmlandBlockEntity be) {
            be.advance(level);
            int revertDays = TilthConfig.FARMLAND_REVERT_DAYS.getAsInt();
            if (revertDays > 0 && be.bareDays() > revertDays && level.getBlockState(pos.above()).isAir()) {
                revertToSoil(level, pos, null);
            }
        }
    }

    public void revertToSoil(Level level, BlockPos pos, @Nullable Entity entity) {
        if (level.getBlockEntity(pos) instanceof FarmlandBlockEntity be && be.isInitialized()) {
            SoilMemory.remember(level, pos, be.soil());
        }
        BlockState current = level.getBlockState(pos);
        level.setBlockAndUpdate(pos, Block.pushEntitiesUp(current, TilthBlocks.topsoil(texture), level, pos));
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!level.isClientSide && CommonHooks.onFarmlandTrample(level, pos, TilthBlocks.topsoil(texture), fallDistance, entity)) {
            if (TilthConfig.TRAMPLING_COMPACTS.getAsBoolean()) {
                if (level.getBlockEntity(pos) instanceof FarmlandBlockEntity be) {
                    be.compact(load(entity) * (1.0 + fallDistance));
                }
            } else {
                revertToSoil(level, pos, entity);
            }
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity && level.random.nextInt(40) == 0
                && level.getBlockEntity(pos) instanceof FarmlandBlockEntity be) {
            be.compact(load(entity) * 0.15);
        }
        super.stepOn(level, pos, state, entity);
    }

    private static double load(Entity entity) {
        if (entity instanceof AbstractHorse) {
            return 2.5;
        }
        return Math.max(0.2, entity.getBbWidth() * entity.getBbHeight());
    }

    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(MOISTURE) > 0;
    }

    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) {
        if (facing != Direction.UP) {
            return TriState.DEFAULT;
        }
        if (plant.getBlock() instanceof TilthCropBlock || plant.getBlock() instanceof DeadCropBlock
                || plant.getBlock() instanceof CropBlock || plant.getBlock() instanceof StemBlock) {
            return TriState.TRUE;
        }
        return TriState.DEFAULT;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FarmlandBlockEntity(pos, state);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(TilthBlocks.TOPSOIL.get(texture).get());
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FarmlandBlockEntity be && be.isInitialized()) {
            SoilMemory.remember(level, pos, be.soil());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Applies a tillage pass to existing farmland (used by the plough and ard). */
    public static void retill(ServerLevel level, BlockPos pos, Tillage.Implement implement, @Nullable Player player) {
        if (level.getBlockEntity(pos) instanceof FarmlandBlockEntity be) {
            be.retill(level, implement, player);
        }
    }
}
