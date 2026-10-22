package com.claudacious.tilth.plot;

import com.claudacious.tilth.sim.plot.PolygonMath;
import com.claudacious.tilth.world.TilthCalendar;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** All plots in one dimension. */
public final class PlotData extends SavedData {
    public static final SavedData.Factory<PlotData> FACTORY = new SavedData.Factory<>(PlotData::new, PlotData::load);
    private final List<Plot> plots = new ArrayList<>();

    public static PlotData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "tilth_plots");
    }

    public static PlotData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlotData d = new PlotData();
        ListTag list = tag.getList("plots", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            d.plots.add(Plot.load(list.getCompound(i)));
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

    public List<Plot> all() {
        return List.copyOf(plots);
    }

    public List<Plot> ownedBy(UUID owner) {
        return plots.stream().filter(p -> p.owner().equals(owner)).toList();
    }

    @Nullable
    public Plot at(int x, int z) {
        for (Plot p : plots) {
            if (p.contains(x, z)) {
                return p;
            }
        }
        return null;
    }

    @Nullable
    public Plot byName(String name) {
        for (Plot p : plots) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
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
        int suffix = 2;
        while (byName(unique) != null) {
            unique = name + " " + suffix++;
        }
        Plot p = new Plot(UUID.randomUUID(), unique, owner.getUUID(), owner.getGameProfile().getName(), vertices, new HashSet<>(),
                TilthCalendar.today(level));
        plots.add(p);
        setDirty();
        return p;
    }

    public boolean remove(Plot plot) {
        boolean removed = plots.remove(plot);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void changed() {
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
