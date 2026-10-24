package com.claudacious.tilth.item;

import com.claudacious.tilth.command.Reports;
import com.claudacious.tilth.registry.TilthItems;
import net.minecraft.ChatFormatting;
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

import java.util.List;

/**
 * A soil auger and field kit. Right-click for a quick field test (feel, moisture, pH strip, compaction);
 * sneak and right-click to take a soil sample for a Soil Lab, which measures the nutrients.
 */
public class SoilProbeItem extends Item {
    public SoilProbeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(context.getLevel() instanceof ServerLevel server) || player == null) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (player.isSecondaryUseActive()) {
            if (Reports.sampleAt(server, context.getClickedPos()) == null && Reports.sampleAt(server, context.getClickedPos().below()) == null) {
                player.displayClientMessage(Component.literal("There is no soil here to sample.").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.FAIL;
            }
            ItemStack sample = SoilSampleItem.take(server, context.getClickedPos(), TilthItems.SOIL_SAMPLE.get());
            if (!player.getInventory().add(sample)) {
                player.drop(sample, false);
            }
            server.playSound(null, context.getClickedPos(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 0.7f, 1.3f);
            player.displayClientMessage(Component.literal("Took a soil sample. Analyse it in a Soil Lab.").withStyle(ChatFormatting.GREEN), true);
        } else {
            Reports.fieldTest(server, context.getClickedPos()).forEach(player::sendSystemMessage);
        }
        context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click soil or a crop: field test").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak + right-click: take a sample for a Soil Lab").withStyle(ChatFormatting.GRAY));
    }
}
