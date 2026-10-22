package com.claudacious.tilth.item;

import com.claudacious.tilth.command.Reports;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** A soil auger and field test kit: right-click soil, farmland or a crop for a lab-style report. */
public class SoilProbeItem extends Item {
    public SoilProbeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel server && context.getPlayer() != null) {
            Reports.probe(server, context.getClickedPos()).forEach(line -> context.getPlayer().sendSystemMessage(line));
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click soil, farmland or crops for a soil test").withStyle(ChatFormatting.GRAY));
    }
}
