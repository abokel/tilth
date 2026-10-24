package com.claudacious.tilth.block;

import com.claudacious.tilth.item.SoilSampleItem;
import com.claudacious.tilth.item.SoilSurveyItem;
import com.claudacious.tilth.registry.TilthBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A bench of glassware and reagents. Give it a soil sample (from the probe); after a short while it hands
 * back a written soil test report with nutrient levels and lime and fertiliser advice.
 */
public class SoilLabBlock extends BaseEntityBlock {
    public static final MapCodec<SoilLabBlock> CODEC = simpleCodec(SoilLabBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public SoilLabBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WORKING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WORKING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoilLabBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TilthBlockEntities.SOIL_LAB.get(), SoilLabBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof SoilSurveyItem) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof SoilLabBlockEntity lab) {
                if (!lab.hasResult()) {
                    player.displayClientMessage(Component.literal(lab.isEmpty() ? "Analyse a soil sample first, then file the result."
                            : String.format("Still analysing... %d%%", lab.progressPercent())).withStyle(ChatFormatting.GRAY), true);
                    return ItemInteractionResult.FAIL;
                }
                Component refused = SoilSurveyItem.file((net.minecraft.server.level.ServerLevel) level, stack, lab.result());
                if (refused != null) {
                    player.displayClientMessage(refused.copy().withStyle(ChatFormatting.RED), true);
                    return ItemInteractionResult.FAIL;
                }
                lab.takeReport();
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 0.8f);
                player.displayClientMessage(Component.literal("Result filed into the soil survey").withStyle(ChatFormatting.GREEN), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(stack.getItem() instanceof SoilSampleItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof SoilLabBlockEntity lab)) {
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            if (!lab.isEmpty()) {
                player.displayClientMessage(Component.literal("The lab is busy. Collect the last report first.").withStyle(ChatFormatting.RED), true);
                return ItemInteractionResult.FAIL;
            }
            lab.insert(stack.split(1));
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6f, 1.2f);
            player.displayClientMessage(Component.literal("Analysing the sample...").withStyle(ChatFormatting.YELLOW), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SoilLabBlockEntity lab)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack out = lab.takeReport();
            if (!out.isEmpty()) {
                if (!player.getInventory().add(out)) {
                    player.drop(out, false);
                }
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1f);
            } else {
                player.displayClientMessage(Component.literal(lab.isEmpty() ? "Use a soil sample on the lab to analyse it."
                        : String.format("Analysing... %d%%", lab.progressPercent())).withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SoilLabBlockEntity lab) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), lab.contents());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(WORKING) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.15,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
    }
}
