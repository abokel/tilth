package com.claudacious.tilth.market;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A market stall, found in villages or built. Right-click with an empty hand for today's prices and news; right-click
 * with farm goods to sell the stack (sneak to sell one) for coins at the day's market price.
 */
public class MarketStallBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<MarketStallBlock> CODEC = simpleCodec(MarketStallBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public MarketStallBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (Commodity.quote(stack) == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player instanceof ServerPlayer sp) {
            Market market = Market.get(sp.server);
            int count = player.isSecondaryUseActive() ? 1 : stack.getCount();
            String name = stack.getHoverName().getString();
            double each = market.unitPrice(stack);
            int paid = market.sell(sp, stack, count);
            level.playSound(null, pos, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 0.8f, 1.0f);
            player.displayClientMessage(Component.literal(String.format("Sold %d %s at %.1f pennies each: paid %s.",
                    count, name, each * Coins.SHILLING, Coins.describe(paid))).withStyle(ChatFormatting.GREEN), false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Market.get(sp.server).board().forEach(line -> sp.sendSystemMessage(line));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
