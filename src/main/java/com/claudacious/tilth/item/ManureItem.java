package com.claudacious.tilth.item;

import com.claudacious.tilth.block.FarmlandBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Shovelled dung. It is not spread as it is: mix four with a water bucket into slurry, or compost it. */
public class ManureItem extends Item {
    public ManureItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && !context.getLevel().isClientSide
                && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof FarmlandBlockEntity) {
            context.getPlayer().displayClientMessage(Component.literal("Mix manure with a water bucket into slurry to spread it.")
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Four manure and a water bucket make slurry; also composts").withStyle(ChatFormatting.GRAY));
    }
}
