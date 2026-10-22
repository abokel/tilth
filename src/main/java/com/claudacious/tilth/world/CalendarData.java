package com.claudacious.tilth.world;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/** Calendar offset (from sleeping and /tilth time) and the real days on which Minecraft weather rained. */
public final class CalendarData extends SavedData {
    public static final SavedData.Factory<CalendarData> FACTORY = new SavedData.Factory<>(CalendarData::new, CalendarData::load);
    public static final String NAME = "tilth_calendar";
    private static final int RAIN_MEMORY_DAYS = 800;

    long offsetTicks;
    private final LongSet rainDays = new LongOpenHashSet();

    public static CalendarData load(CompoundTag tag, HolderLookup.Provider registries) {
        CalendarData d = new CalendarData();
        d.offsetTicks = tag.getLong("offset");
        for (long day : tag.getLongArray("rain_days")) {
            d.rainDays.add(day);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("offset", offsetTicks);
        tag.putLongArray("rain_days", rainDays.toLongArray());
        return tag;
    }

    public void addOffset(long ticks) {
        offsetTicks += ticks;
        setDirty();
    }

    public boolean rained(long day) {
        return rainDays.contains(day);
    }

    public void markRain(long day) {
        if (rainDays.add(day)) {
            rainDays.removeIf(d -> d < day - RAIN_MEMORY_DAYS);
            setDirty();
        }
    }
}
