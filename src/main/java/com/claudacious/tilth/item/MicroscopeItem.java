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

/**
 * A field microscope: shows the organisms on a plant sample held in the other hand, magnified. Sneak-use it on a block
 * to set it down as a bench microscope ({@link com.claudacious.tilth.block.MicroscopeBlock}).
 */
public class MicroscopeItem extends net.minecraft.world.item.BlockItem {
    public MicroscopeItem(net.minecraft.world.level.block.Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        // Only a sneaking player sets it down; otherwise it is used as a hand microscope, even when looking at a block.
        if (context.getPlayer() != null && !context.getPlayer().isSecondaryUseActive()) {
            return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
        }
        return super.useOn(context);
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
        tooltip.add(Component.literal("Sneak-use on a block to set it on a bench").withStyle(ChatFormatting.GRAY));
    }
}
