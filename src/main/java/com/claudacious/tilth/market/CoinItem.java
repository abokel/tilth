package com.claudacious.tilth.market;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A coin of Tilth's money (see {@link Coins}); its tooltip says what it and the stack are worth. */
public class CoinItem extends Item {
    private final int pennies;

    public CoinItem(Properties properties, int pennies) {
        super(properties);
        this.pennies = pennies;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(pennies == 1 ? "Worth 1 penny" : "Worth " + pennies + " pennies").withStyle(ChatFormatting.GRAY));
        if (stack.getCount() > 1) {
            tooltip.add(Component.literal("Stack: " + Coins.describe(pennies * stack.getCount())).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal("Nine make the next coin up").withStyle(ChatFormatting.DARK_GRAY));
    }
}
