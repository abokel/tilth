package com.claudacious.tilth.item;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.plot.Plot;
import com.claudacious.tilth.plot.PlotData;
import com.claudacious.tilth.registry.TilthDataComponents;
import com.claudacious.tilth.sim.plot.PolygonMath;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Gunter's chain: 66 feet long, 10 square chains to the acre. Click the corner blocks of a field in
 * order to stake out a polygon of any shape, then sneak-use in the air to record it as a plot.
 */
public class SurveyorsChainItem extends Item {
    public SurveyorsChainItem(Properties properties) {
        super(properties);
    }

    public static List<BlockPos> points(ItemStack stack) {
        List<BlockPos> list = stack.get(TilthDataComponents.SURVEY_POINTS.get());
        return list == null ? List.of() : list;
    }

    public static List<PolygonMath.Vertex> vertices(List<BlockPos> points) {
        List<PolygonMath.Vertex> v = new ArrayList<>(points.size());
        for (BlockPos p : points) {
            v.add(new PolygonMath.Vertex(p.getX(), p.getZ()));
        }
        return v;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel) || player == null) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        ItemStack stack = context.getItemInHand();
        List<BlockPos> pts = new ArrayList<>(points(stack));
        if (player.isSecondaryUseActive()) {
            if (!pts.isEmpty()) {
                BlockPos removed = pts.remove(pts.size() - 1);
                stack.set(TilthDataComponents.SURVEY_POINTS.get(), List.copyOf(pts));
                player.displayClientMessage(Component.literal("Removed stake at " + removed.getX() + ", " + removed.getZ()
                        + " (" + pts.size() + " left)").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (pts.size() >= TilthConfig.MAX_PLOT_VERTICES.getAsInt()) {
            player.displayClientMessage(Component.literal("Too many corners for one plot.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        BlockPos pos = context.getClickedPos().immutable();
        if (!pts.isEmpty() && pts.get(pts.size() - 1).getX() == pos.getX() && pts.get(pts.size() - 1).getZ() == pos.getZ()) {
            return InteractionResult.SUCCESS;
        }
        pts.add(pos);
        stack.set(TilthDataComponents.SURVEY_POINTS.get(), List.copyOf(pts));
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8f, 1.2f);
        String msg = "Stake " + pts.size() + " at " + pos.getX() + ", " + pos.getZ();
        if (pts.size() >= 3) {
            double m2 = PolygonMath.areaSquareMetres(vertices(pts), TilthConfig.METRES_PER_BLOCK.getAsDouble());
            msg += " | enclosed: " + PolygonMath.formatArea(m2);
        }
        player.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResultHolder.success(stack);
        }
        if (!player.isSecondaryUseActive()) {
            PlotData data = PlotData.get(server);
            Plot here = data.at(player.getBlockX(), player.getBlockZ());
            player.displayClientMessage(Component.literal(here == null ? "Not inside a surveyed plot."
                    : "In plot \"" + here.name() + "\" (" + PolygonMath.formatArea(here.areaSquareMetres()) + ")").withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.success(stack);
        }
        List<BlockPos> pts = points(stack);
        if (pts.size() < 3) {
            player.displayClientMessage(Component.literal("Stake at least 3 corners first.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        List<PolygonMath.Vertex> verts = vertices(pts);
        if (PolygonMath.selfIntersects(verts)) {
            player.displayClientMessage(Component.literal("The boundary crosses itself. Re-stake the corners in order around the field.")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        PlotData data = PlotData.get(server);
        if (data.ownedBy(player.getUUID()).size() >= TilthConfig.MAX_PLOTS_PER_PLAYER.getAsInt()) {
            player.displayClientMessage(Component.literal("You own the maximum number of plots.").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        String name = stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().getString() : data.nextName();
        Plot plot = data.create(name, player, verts, server);
        stack.remove(TilthDataComponents.SURVEY_POINTS.get());
        level.playSound(null, player.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.displayClientMessage(Component.literal("Recorded plot \"" + plot.name() + "\": " + PolygonMath.formatArea(plot.areaSquareMetres())
                + ", perimeter " + String.format("%.0f m", plot.perimeterMetres())).withStyle(ChatFormatting.GREEN), false);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int n = points(stack).size();
        tooltip.add(Component.literal("Right-click blocks: stake corners in order (" + n + " placed)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak + right-click block: remove last stake").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Sneak + use in air: record plot | Use in air: which plot am I in?").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Rename in an anvil to name the next plot").withStyle(ChatFormatting.DARK_GRAY));
    }
}
