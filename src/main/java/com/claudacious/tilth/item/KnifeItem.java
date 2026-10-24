package com.claudacious.tilth.item;

import com.claudacious.tilth.block.CropBlockEntity;
import com.claudacious.tilth.registry.TilthItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** The kitchen knife: a crafting tool that also cuts plant samples from crops for the microscope. */
public class KnifeItem extends CraftingToolItem {
    public KnifeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof CropBlockEntity crop) || player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel() instanceof ServerLevel server) {
            ItemStack sample = PlantSampleItem.take(server, crop, TilthItems.PLANT_SAMPLE.get());
            if (!player.getInventory().add(sample)) {
                player.drop(sample, false);
            }
            server.playSound(null, context.getClickedPos(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.6f, 1.4f);
            player.displayClientMessage(Component.literal("Cut a sample of " + crop.species().name.toLowerCase() + ". Look at it under a microscope.")
                    .withStyle(ChatFormatting.GREEN), true);
            context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click a crop to cut a plant sample").withStyle(ChatFormatting.GRAY));
    }
}
