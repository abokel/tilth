package com.claudacious.tilth.item;

import com.claudacious.tilth.entity.PlowEntity;
import com.claudacious.tilth.registry.TilthEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Places a moldboard plough in the world. */
public class PlowItem extends Item {
    public PlowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level instanceof ServerLevel server) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            PlowEntity plow = new PlowEntity(TilthEntities.PLOW.get(), server);
            plow.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getRotation(), 0);
            if (!server.noCollision(plow, plow.getBoundingBox())) {
                return InteractionResult.FAIL;
            }
            server.addFreshEntity(plow);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Three-bottom moldboard plough").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click: hitch to you, or to the horse you ride").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Sneak + right-click: lower/raise the shares").withStyle(ChatFormatting.DARK_GRAY));
    }
}
