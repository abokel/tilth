package com.claudacious.tilth.item;

import com.claudacious.tilth.block.CropBlockEntity;
import com.claudacious.tilth.block.DeadCropBlock;
import com.claudacious.tilth.block.TilthCropBlock;
import com.claudacious.tilth.command.Reports;
import com.claudacious.tilth.sim.SimSettings;
import com.claudacious.tilth.sim.soil.Hydraulics;
import com.claudacious.tilth.sim.soil.SoilState;
import com.claudacious.tilth.sim.soil.SoilTexture;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;

/**
 * A soil moisture meter (two probes pushed into the ground). Reads the volumetric water content of the root
 * zone, how much of the plant-available water is left, whether the soil is waterlogged, and how much water
 * would refill it to field capacity.
 */
public class MoistureMeterItem extends Item {
    public MoistureMeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(context.getLevel() instanceof ServerLevel level) || player == null) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        BlockPos pos = context.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();
        CropBlockEntity crop = level.getBlockEntity(pos) instanceof CropBlockEntity c ? c : null;
        BlockPos soilPos = Reports.soilUnder(level, pos);
        Map.Entry<SoilTexture, SoilState> sample = Reports.sampleAt(level, soilPos);
        if (sample == null) {
            player.displayClientMessage(Component.literal("The meter needs soil to read.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        SoilState s = sample.getValue();
        Hydraulics h = sample.getKey().hydraulics(s.organicMatter);
        double available = (s.moisture - h.wiltingPoint()) / Math.max(0.01, h.fieldCapacity() - h.wiltingPoint());
        double rootMm = SimSettings.rootZoneDepthMeters * 1000.0;
        player.sendSystemMessage(Reports.header("Soil moisture: " + sample.getKey().displayName()));
        player.sendSystemMessage(Reports.line("Water content", String.format("%.0f%% by volume (holds %.0f%% when full, plants wilt at %.0f%%)",
                100 * s.moisture, 100 * h.fieldCapacity(), 100 * h.wiltingPoint())));
        player.sendSystemMessage(Reports.line("Available to plants", String.format("%.0f%%", 100 * Math.max(0, Math.min(1, available)))));
        double threshold = crop != null ? 1.0 - crop.species().depletionFraction : 0.5;
        if (s.moisture >= h.saturation() - 0.02) {
            player.sendSystemMessage(Reports.note("Saturated: the pores are full of water and roots cannot breathe. Drainage needed."));
        } else if (available > 1.0) {
            player.sendSystemMessage(Reports.note("Wetter than field capacity: it will drain over the next day."));
        } else if (available < threshold) {
            double mm = (h.fieldCapacity() - s.moisture) * rootMm;
            player.sendSystemMessage(Reports.note(String.format("Dry enough to stress %s: about %.0f mm of water would refill the root zone.",
                    crop != null ? "this " + crop.species().name.toLowerCase() : "most crops", mm)));
        } else {
            player.sendSystemMessage(Reports.note("Moist: no need to water."));
        }
        player.sendSystemMessage(Reports.line("Water table", String.format("%.1f m down", s.waterTableDepth)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click soil, farmland or a crop for its moisture").withStyle(ChatFormatting.GRAY));
    }
}
