package com.claudacious.tilth.item;

import com.claudacious.tilth.block.Tilling;
import com.claudacious.tilth.sim.soil.Tillage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The ard (scratch plough): the oldest tillage implement, dragged by hand. It stirs a shallow strip
 * three blocks long in front of the user without inverting the soil, so it buries little residue and
 * breaks up less compaction than a moldboard plough.
 */
public class ArdItem extends Item {
    public static final int LENGTH = 3;

    public ArdItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos start = context.getClickedPos();
        if (context.getClickedFace() == Direction.DOWN || !Tilling.isTillable(level.getBlockState(start))) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (level instanceof ServerLevel server) {
            Direction dir = context.getHorizontalDirection();
            int tilled = 0;
            for (int i = 0; i < LENGTH; i++) {
                BlockPos ground = findGround(server, start.relative(dir, i));
                if (ground != null && Tilling.till(server, ground, Tillage.Implement.ARD, player)) {
                    tilled++;
                }
            }
            if (tilled > 0) {
                level.playSound(null, start, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 0.8f);
                if (player != null) {
                    context.getItemInHand().hurtAndBreak(tilled, player, LivingEntity.getSlotForHand(context.getHand()));
                    player.causeFoodExhaustion(0.3f * tilled);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static BlockPos findGround(ServerLevel level, BlockPos p) {
        for (int dy : new int[]{0, 1, -1}) {
            BlockPos q = p.above(dy);
            BlockState above = level.getBlockState(q.above());
            if (Tilling.isTillable(level.getBlockState(q)) && (above.isAir() || above.canBeReplaced())) {
                return q;
            }
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tills a 3-block strip ahead of you").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Shallow: little residue burial, partial loosening").withStyle(ChatFormatting.DARK_GRAY));
    }
}
