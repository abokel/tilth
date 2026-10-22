package com.claudacious.tilth.command;

import com.claudacious.tilth.TilthConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Per-player action-bar readout of whatever soil, farmland or crop the player is looking at. */
public final class DebugHud {
    private static final Set<UUID> ENABLED = new HashSet<>();

    private DebugHud() {
    }

    public static boolean toggle(ServerPlayer player) {
        if (!ENABLED.remove(player.getUUID())) {
            ENABLED.add(player.getUUID());
            return true;
        }
        return false;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ENABLED.contains(player.getUUID())
                || player.tickCount % TilthConfig.HUD_INTERVAL.getAsInt() != 0) {
            return;
        }
        HitResult hit = player.pick(8.0, 0f, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            player.displayClientMessage(Reports.hudLine(player.serverLevel(), blockHit.getBlockPos()), true);
        } else {
            player.displayClientMessage(Reports.hudLine(player.serverLevel(), player.blockPosition()), true);
        }
    }
}
