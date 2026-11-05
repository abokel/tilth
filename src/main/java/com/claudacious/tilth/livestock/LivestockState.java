package com.claudacious.tilth.livestock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.claudacious.tilth.sim.genetics.LivestockGenome;

import java.util.LinkedHashMap;
import java.util.Map;

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
            Codec.LONG.optionalFieldOf("due", -1L).forGetter(s -> s.due),
            Codec.STRING.optionalFieldOf("genome", LivestockGenome.baseline().encode()).forGetter(s -> s.genome.encode()),
            Codec.STRING.optionalFieldOf("sire_genome", LivestockGenome.baseline().encode()).forGetter(s -> s.sireGenome.encode()),
            Codec.DOUBLE.optionalFieldOf("pasture_quality", 1.0).forGetter(s -> s.pastureQuality),
            Codec.LONG.optionalFieldOf("pasture_cell", Long.MIN_VALUE).forGetter(s -> s.pastureCell),
            Codec.LONG.optionalFieldOf("last_pasture_update", -1L).forGetter(s -> s.lastPastureUpdate),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("afflictions", Map.of()).forGetter(s -> s.afflictions),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("diagnosed", Map.of()).forGetter(s -> s.diagnosed),
            Codec.BOOL.optionalFieldOf("quarantine", false).forGetter(s -> s.quarantine),
            Codec.STRING.optionalFieldOf("breed", "").forGetter(s -> s.breed),
            Codec.STRING.optionalFieldOf("sire_breed", "").forGetter(s -> s.sireBreed)
    ).apply(i, LivestockState::new));

    public double condition;
    public long lastMilked;
    public long lastUpdate;
    public byte sex;
    /** Game time at which a pregnant female gives birth, or -1. */
    public long due;
    public LivestockGenome genome;
    public LivestockGenome sireGenome;
    public double pastureQuality;
    public long pastureCell;
    public long lastPastureUpdate;
    public Map<String, Double> afflictions;
    public Map<String, Long> diagnosed;
    public boolean quarantine;
    /** Breed id (see {@link Breed}), or empty before one is given. */
    public String breed;
    /** The father's breed while she carries his young. */
    public String sireBreed;

    public LivestockState() {
        this(0.7, -1000000L, -1L, UNKNOWN, -1L, LivestockGenome.baseline().encode(), LivestockGenome.baseline().encode(),
                1.0, Long.MIN_VALUE, -1L, Map.of(), Map.of(), false, "", "");
    }

    private LivestockState(double condition, long lastMilked, long lastUpdate, byte sex, long due, String genome, String sireGenome,
                           double pastureQuality, long pastureCell, long lastPastureUpdate, Map<String, Double> afflictions,
                           Map<String, Long> diagnosed, boolean quarantine, String breed, String sireBreed) {
        this.condition = condition;
        this.lastMilked = lastMilked;
        this.lastUpdate = lastUpdate;
        this.sex = sex;
        this.due = due;
        this.genome = LivestockGenome.decode(genome);
        this.sireGenome = LivestockGenome.decode(sireGenome);
        this.pastureQuality = pastureQuality;
        this.pastureCell = pastureCell;
        this.lastPastureUpdate = lastPastureUpdate;
        this.afflictions = new LinkedHashMap<>(afflictions);
        this.diagnosed = new LinkedHashMap<>(diagnosed);
        this.quarantine = quarantine;
        this.breed = breed;
        this.sireBreed = sireBreed;
        sanitize();
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

    public LivestockState sanitize() {
        if (!Double.isFinite(condition)) {
            condition = 0.7;
        }
        condition = Math.max(0, Math.min(1, condition));
        if (!Double.isFinite(pastureQuality)) {
            pastureQuality = 1.0;
        }
        pastureQuality = Math.max(0, Math.min(1, pastureQuality));
        if (genome == null) {
            genome = LivestockGenome.baseline();
        }
        if (breed == null) {
            breed = "";
        }
        if (sireBreed == null) {
            sireBreed = "";
        }
        if (sireGenome == null) {
            sireGenome = LivestockGenome.baseline();
        }
        if (afflictions == null) {
            afflictions = new LinkedHashMap<>();
        } else {
            afflictions.entrySet().removeIf(e -> e.getKey() == null || e.getKey().isBlank() || !Double.isFinite(e.getValue()) || e.getValue() <= 0.001);
            afflictions.replaceAll((k, v) -> Math.max(0.0, Math.min(1.0, v)));
        }
        if (diagnosed == null) {
            diagnosed = new LinkedHashMap<>();
        } else {
            diagnosed.entrySet().removeIf(e -> e.getKey() == null || e.getKey().isBlank());
        }
        return this;
    }

    public double diseaseBurden() {
        if (afflictions == null || afflictions.isEmpty()) {
            return 0.0;
        }
        double burden = 0.0;
        for (double severity : afflictions.values()) {
            burden += severity;
        }
        return Math.max(0.0, Math.min(1.0, burden / Math.max(1, afflictions.size())));
    }
}
