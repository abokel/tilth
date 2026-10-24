package com.claudacious.tilth.plot;

import com.claudacious.tilth.TilthConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Optional rule that land may only be farmed inside a surveyed plot. */
public final class PlotRules {
    private PlotRules() {
    }

    /**
     * Whether ground may be tilled at a position. With {@code plots.farmOnlyInPlots} on, it must lie inside a
     * plot, and a player must be its owner or a member. Creative players are exempt.
     */
    public static boolean mayFarm(@Nullable Player player, ServerLevel level, BlockPos pos) {
        if (!TilthConfig.FARM_ONLY_IN_PLOTS.getAsBoolean() || (player != null && player.getAbilities().instabuild)) {
            return true;
        }
        Plot plot = PlotData.get(level).at(pos.getX(), pos.getZ());
        if (plot != null && (player == null || plot.mayModify(player.getUUID()))) {
            return true;
        }
        if (player != null) {
            player.displayClientMessage((plot == null
                    ? Component.literal("You can only farm inside a surveyed plot. Stake one out with the Surveyor's Chain.")
                    : Component.literal("This land is plot \"" + plot.name() + "\" (" + plot.ownerName() + ")")).withStyle(ChatFormatting.RED), true);
        }
        return false;
    }
}
