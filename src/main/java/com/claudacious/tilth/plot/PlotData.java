package com.claudacious.tilth.plot;

import com.claudacious.tilth.sim.plot.PolygonMath;
import com.claudacious.tilth.world.TilthCalendar;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * All plots in one dimension. Plots are indexed by the chunks their bounding boxes cover, so finding the plot
 * at a block (done on every till, break, place and right-click) only tests the few plots near it, and by
 * lower-case name.
 */
public final class PlotData extends SavedData {
    public static final SavedData.Factory<PlotData> FACTORY = new SavedData.Factory<>(PlotData::new, PlotData::load);
    private final List<Plot> plots = new ArrayList<>();
    private final Map<Long, List<Plot>> byChunk = new HashMap<>();
    private final Map<String, Plot> byName = new HashMap<>();

    public static PlotData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "tilth_plots");
    }

    public static PlotData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlotData d = new PlotData();
        ListTag list = tag.getList("plots", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            d.add(Plot.load(list.getCompound(i)));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Plot p : plots) {
            list.add(p.save());
        }
        tag.put("plots", list);
        return tag;
    }

    private void add(Plot p) {
        plots.add(p);
        byName.put(normalise(p.name()), p);
        forEachChunk(p, key -> byChunk.computeIfAbsent(key, k -> new ArrayList<>(2)).add(p));
    }

    private static void forEachChunk(Plot p, java.util.function.LongConsumer action) {
        for (int cx = p.minX() >> 4; cx <= p.maxX() >> 4; cx++) {
            for (int cz = p.minZ() >> 4; cz <= p.maxZ() >> 4; cz++) {
                action.accept(ChunkPos.asLong(cx, cz));
            }
        }
    }

    public List<Plot> all() {
        return List.copyOf(plots);
    }

    public List<Plot> ownedBy(UUID owner) {
        return plots.stream().filter(p -> p.owner().equals(owner)).toList();
    }

    @Nullable
    public Plot at(int x, int z) {
        List<Plot> near = byChunk.get(ChunkPos.asLong(x >> 4, z >> 4));
        if (near != null) {
            for (Plot p : near) {
                if (p.contains(x, z)) {
                    return p;
                }
            }
        }
        return null;
    }

    @Nullable
    public Plot byName(String name) {
        return byName.get(normalise(name));
    }

    public String nextName() {
        int n = plots.size() + 1;
        while (byName("Field " + n) != null) {
            n++;
        }
        return "Field " + n;
    }

    public Plot create(String name, Player owner, List<PolygonMath.Vertex> vertices, ServerLevel level) {
        String unique = name;
        for (int suffix = Math.max(2, plots.size()); byName(unique) != null; suffix++) {
            unique = name + " " + suffix;
        }
        Plot p = new Plot(UUID.randomUUID(), unique, owner.getUUID(), owner.getGameProfile().getName(), vertices, new HashSet<>(),
                TilthCalendar.today(level));
        add(p);
        setDirty();
        return p;
    }

    public boolean remove(Plot plot) {
        boolean removed = plots.remove(plot);
        if (removed) {
            byName.values().remove(plot);
            forEachChunk(plot, key -> {
                List<Plot> list = byChunk.get(key);
                if (list != null && list.remove(plot) && list.isEmpty()) {
                    byChunk.remove(key);
                }
            });
            setDirty();
        }
        return removed;
    }

    /** Call after renaming or editing a plot. */
    public void changed() {
        byName.clear();
        plots.forEach(p -> byName.put(normalise(p.name()), p));
        setDirty();
    }

    public List<String> names() {
        return plots.stream().map(Plot::name).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public static String quoteIfNeeded(String name) {
        return name.matches("[A-Za-z0-9_.+-]+") ? name : "\"" + name.replace("\"", "'") + "\"";
    }

    public static String normalise(String s) {
        return s.toLowerCase(Locale.ROOT).trim();
    }
}
