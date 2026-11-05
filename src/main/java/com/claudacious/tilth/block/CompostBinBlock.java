package com.claudacious.tilth.block;

import com.claudacious.tilth.registry.TilthBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

/** A slatted wooden compost bin: fill it with greens and browns, turn it with a shovel, dig out compost. */
public class CompostBinBlock extends BaseEntityBlock {
    public static final MapCodec<CompostBinBlock> CODEC = simpleCodec(CompostBinBlock::new);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 4);
    public static final BooleanProperty READY = BooleanProperty.create("ready");
    private static final VoxelShape SHAPE = Shapes.join(Block.box(0, 0, 0, 16, 14, 16), Block.box(2, 2, 2, 14, 14, 14),
            net.minecraft.world.phys.shapes.BooleanOp.ONLY_FIRST);

    public CompostBinBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILL, 0).setValue(READY, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL, READY);
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
        return new CompostBinBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TilthBlockEntities.COMPOST_BIN.get(), CompostBinBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CompostBinBlockEntity bin)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.canPerformAction(ItemAbilities.SHOVEL_DIG) && !bin.ready()) {
            if (!level.isClientSide) {
                boolean turned = bin.turn();
                level.playSound(null, pos, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
                if (turned && level instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.25, 0.1, 0.25, 0.01);
                }
                player.displayClientMessage(Component.literal(turned ? "Turned the heap: it breathes and heats up. " + bin.describe()
                        : bin.describe()).withStyle(ChatFormatting.GRAY), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (CompostBinBlockEntity.kind(stack) == 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            int moved = bin.add(stack);
            if (moved > 0) {
                level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            player.displayClientMessage(Component.literal(moved > 0 ? bin.describe() : bin.ready() ? "Dig out the compost first."
                    : "The bin is full.").withStyle(ChatFormatting.GRAY), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CompostBinBlockEntity bin) {
            ItemStack out = bin.takeCompost();
            if (out.isEmpty()) {
                player.displayClientMessage(Component.literal(bin.describe()).withStyle(ChatFormatting.GRAY), true);
            } else {
                level.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                if (!player.getInventory().add(out)) {
                    player.drop(out, false);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
