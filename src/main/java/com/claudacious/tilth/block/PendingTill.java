package com.claudacious.tilth.block;

import com.claudacious.tilth.sim.soil.SoilHorizon;
import com.claudacious.tilth.sim.soil.Tillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Remembers which horizon and implement produced a block of farmland during the brief moment
 * between a tool deciding the new state and the farmland block entity being created.
 */
public final class PendingTill {
    public record Entry(SoilHorizon horizon, Tillage.Implement implement) {
    }

    private static final Map<GlobalPos, Entry> PENDING = new HashMap<>();

    private PendingTill() {
    }

    public static void mark(Level level, BlockPos pos, SoilHorizon horizon, Tillage.Implement implement) {
        if (level.isClientSide) {
            return;
        }
        if (PENDING.size() > 4096) {
            PENDING.clear();
        }
        PENDING.put(GlobalPos.of(level.dimension(), pos.immutable()), new Entry(horizon, implement));
    }

    public static Entry take(Level level, BlockPos pos) {
        Entry e = PENDING.remove(GlobalPos.of(level.dimension(), pos));
        return e != null ? e : new Entry(SoilHorizon.TOPSOIL, Tillage.Implement.HOE);
    }
}
