package com.claudacious.tilth.world;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/** Years of Serene Seasons completed (its own clock only counts within a year), and the days Minecraft weather rained. */
public final class CalendarData extends SavedData {
    public static final SavedData.Factory<CalendarData> FACTORY = new SavedData.Factory<>(CalendarData::new, CalendarData::load);
    public static final String NAME = "tilth_calendar";
    private static final int RAIN_MEMORY_DAYS = 800;

    private long years;
    private double lastFraction = -1;
    private final LongSet rainDays = new LongOpenHashSet();

    public static CalendarData load(CompoundTag tag, HolderLookup.Provider registries) {
        CalendarData d = new CalendarData();
        d.years = tag.getLong("years");
        d.lastFraction = tag.contains("fraction") ? tag.getDouble("fraction") : -1;
        for (long day : tag.getLongArray("rain_days")) {
            d.rainDays.add(day);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("years", years);
        tag.putDouble("fraction", lastFraction);
        tag.putLongArray("rain_days", rainDays.toLongArray());
        return tag;
    }

    public long years() {
        return years;
    }

    /** Notes where Serene Seasons is in its year; a jump from late winter back to spring is a new year. */
    public void observe(double fraction) {
        if (lastFraction >= 0 && fraction < lastFraction - 0.5) {
            years++;
        }
        if (Math.abs(fraction - lastFraction) > 1e-6) {
            lastFraction = fraction;
            setDirty();
        }
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
