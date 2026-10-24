package com.claudacious.tilth.livestock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Body condition of a farm animal (0 = starving, 1 = in top condition), when it was last milked, its sex, and
 * when a pregnant female is due.
 */
public final class LivestockState {
    public static final byte UNKNOWN = 0;
    public static final byte FEMALE = 1;
    public static final byte MALE = 2;

    public static final Codec<LivestockState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("condition", 0.7).forGetter(s -> s.condition),
            Codec.LONG.optionalFieldOf("last_milked", -1000000L).forGetter(s -> s.lastMilked),
            Codec.LONG.optionalFieldOf("last_update", -1L).forGetter(s -> s.lastUpdate),
            Codec.BYTE.optionalFieldOf("sex", UNKNOWN).forGetter(s -> s.sex),
            Codec.LONG.optionalFieldOf("due", -1L).forGetter(s -> s.due)
    ).apply(i, LivestockState::new));

    public double condition;
    public long lastMilked;
    public long lastUpdate;
    public byte sex;
    /** Game time at which a pregnant female gives birth, or -1. */
    public long due;

    public LivestockState() {
        this(0.7, -1000000L, -1L, UNKNOWN, -1L);
    }

    private LivestockState(double condition, long lastMilked, long lastUpdate, byte sex, long due) {
        this.condition = condition;
        this.lastMilked = lastMilked;
        this.lastUpdate = lastUpdate;
        this.sex = sex;
        this.due = due;
    }

    public void add(double amount) {
        condition = Math.max(0, Math.min(1, condition + amount));
    }

    public boolean female() {
        return sex == FEMALE;
    }

    public boolean male() {
        return sex == MALE;
    }

    public boolean pregnant() {
        return due >= 0;
    }

    public String describe() {
        return condition >= 0.8 ? "in top condition" : condition >= 0.55 ? "in good condition" : condition >= 0.3 ? "thin" : "starving";
    }
}
