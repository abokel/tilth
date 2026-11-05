package com.claudacious.tilth.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A wooden screw press for cheese: pour in milk, wait three days, take out graded cheese. */
public class CheesePressBlock extends BaseEntityBlock {
    public static final MapCodec<CheesePressBlock> CODEC = simpleCodec(CheesePressBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public CheesePressBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
        return new CheesePressBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(Items.MILK_BUCKET) || !(level.getBlockEntity(pos) instanceof CheesePressBlockEntity press)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (press.addMilk(stack)) {
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
            }
            player.displayClientMessage(Component.literal(press.describe()).withStyle(ChatFormatting.GRAY), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CheesePressBlockEntity press) {
            ItemStack cheese = press.take();
            if (cheese.isEmpty()) {
                player.displayClientMessage(Component.literal(press.describe()).withStyle(ChatFormatting.GRAY), true);
            } else {
                level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
                if (!player.getInventory().add(cheese)) {
                    player.drop(cheese, false);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
