package com.claudacious.tilth.item;

import com.claudacious.tilth.client.ClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A field microscope: shows the organisms on a plant sample held in the other hand, magnified. */
public class MicroscopeItem extends Item {
    public MicroscopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack microscope = player.getItemInHand(hand);
        ItemStack sample = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(sample.getItem() instanceof PlantSampleItem)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Hold a plant sample (cut one from a crop with a knife) in your other hand.")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(microscope);
        }
        if (level.isClientSide) {
            ClientAccess.openMicroscope(PlantSampleItem.data(sample));
        }
        return InteractionResultHolder.sidedSuccess(microscope, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use with a plant sample in your other hand").withStyle(ChatFormatting.GRAY));
    }
}
