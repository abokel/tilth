package com.claudacious.tilth.plot;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.block.TilthCropBlock;
import com.claudacious.tilth.item.SurveyorsChainItem;
import com.claudacious.tilth.sim.plot.PolygonMath;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

import java.util.List;

/** Shows plot boundaries to players holding a surveyor's chain, and enforces optional protection. */
public final class PlotEvents {
    private static final DustParticleOptions STAKED = new DustParticleOptions(new Vector3f(0.2f, 0.9f, 1.0f), 1.0f);
    private static final DustParticleOptions BOUNDARY = new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.2f), 1.0f);
    private static final DustParticleOptions CORNER = new DustParticleOptions(new Vector3f(1.0f, 0.3f, 0.2f), 1.6f);
    private static final int VIEW_RANGE = 48;
    private static final int PARTICLE_BUDGET = 600;

    private PlotEvents() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        ItemStack chain = player.getMainHandItem().getItem() instanceof SurveyorsChainItem ? player.getMainHandItem()
                : (player.getOffhandItem().getItem() instanceof SurveyorsChainItem ? player.getOffhandItem() : null);
        if (chain == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        int[] budget = {PARTICLE_BUDGET};
        List<BlockPos> stakes = SurveyorsChainItem.points(chain);
        List<PolygonMath.Vertex> staked = SurveyorsChainItem.vertices(stakes);
        drawPolygon(level, player, staked, STAKED, stakes.size() >= 3, budget);
        for (Plot plot : PlotData.get(level).all()) {
            if (plot.maxX() < player.getX() - VIEW_RANGE || plot.minX() > player.getX() + VIEW_RANGE
                    || plot.maxZ() < player.getZ() - VIEW_RANGE || plot.minZ() > player.getZ() + VIEW_RANGE) {
                continue;
            }
            drawPolygon(level, player, plot.vertices(), BOUNDARY, true, budget);
        }
    }

    private static void drawPolygon(ServerLevel level, ServerPlayer player, List<PolygonMath.Vertex> v, DustParticleOptions dust,
                                    boolean closed, int[] budget) {
        int n = v.size();
        for (int i = 0; i < n; i++) {
            PolygonMath.Vertex a = v.get(i);
            spawn(level, player, a.x() + 0.5, a.z() + 0.5, CORNER, budget);
            if (i == n - 1 && !closed) {
                break;
            }
            PolygonMath.Vertex b = v.get((i + 1) % n);
            double len = Math.hypot(b.x() - a.x(), b.z() - a.z());
            int steps = (int) Math.ceil(len);
            for (int s = 1; s < steps; s++) {
                double t = s / (double) steps;
                double x = a.x() + 0.5 + (b.x() - a.x()) * t;
                double z = a.z() + 0.5 + (b.z() - a.z()) * t;
                if (Math.abs(x - player.getX()) > VIEW_RANGE || Math.abs(z - player.getZ()) > VIEW_RANGE) {
                    continue;
                }
                spawn(level, player, x, z, dust, budget);
            }
        }
    }

    private static void spawn(ServerLevel level, ServerPlayer player, double x, double z, DustParticleOptions dust, int[] budget) {
        if (budget[0]-- <= 0) {
            return;
        }
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        if (!level.hasChunkAt(bx, bz)) {
            return;
        }
        double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) + 0.3;
        level.sendParticles(player, dust, true, x, y, z, 1, 0, 0, 0, 0);
    }

    // ------------------------------------------------------------------ protection

    private static boolean denied(Player player, ServerLevel level, BlockPos pos) {
        if (!TilthConfig.PLOT_PROTECTION.getAsBoolean() || player.hasPermissions(2)) {
            return false;
        }
        Plot plot = PlotData.get(level).at(pos.getX(), pos.getZ());
        if (plot == null || plot.mayModify(player.getUUID())) {
            return false;
        }
        player.displayClientMessage(Component.literal("This land belongs to plot \"" + plot.name() + "\" (" + plot.ownerName() + ")")
                .withStyle(ChatFormatting.RED), true);
        return true;
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && denied(event.getPlayer(), level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Player player && denied(player, level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && level.getBlockState(event.getPos()).getBlock() instanceof TilthCropBlock
                && denied(event.getEntity(), level, event.getPos())) {
            event.setCanceled(true);
        }
    }
}
