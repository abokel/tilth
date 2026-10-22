package com.claudacious.tilth.sim.soil;

/** Effects of primary and secondary tillage on soil structure, residue and nutrient cycling. */
public final class Tillage {
    private Tillage() {
    }

    public enum Implement {
        /** Inverts the furrow slice, buries residue and weeds, breaks compaction. */
        MOLDBOARD_PLOW("moldboard plow", 1.0, 0.3, 0.15, 30),
        /** Scratch plough: stirs the surface without inverting it. */
        ARD("ard", 0.4, 0.6, 0.05, 15),
        /** Hand hoe: shallow seedbed preparation of a single bed. */
        HOE("hoe", 0.3, 0.75, 0.04, 10);

        private final String displayName;
        private final double incorporation;
        private final double compactionMultiplier;
        private final double tilthGain;
        private final double flushDays;

        Implement(String displayName, double incorporation, double compactionMultiplier, double tilthGain, double flushDays) {
            this.displayName = displayName;
            this.incorporation = incorporation;
            this.compactionMultiplier = compactionMultiplier;
            this.tilthGain = tilthGain;
            this.flushDays = flushDays;
        }

        public String displayName() {
            return displayName;
        }
    }

    /**
     * Applies a tillage pass. Working clay that is wetter than field capacity smears and compacts
     * it instead, so the warning is returned for the player.
     *
     * @return a warning, or null if the pass went well
     */
    public static String apply(Implement implement, SoilState s, SoilTexture tex, Hydraulics h) {
        boolean wetClay = tex.clay() >= 35 && s.moisture > h.fieldCapacity() * 0.95;
        s.incorporatedFraction = Math.max(s.incorporatedFraction, implement.incorporation);
        s.tillageFlushDays = Math.max(s.tillageFlushDays, implement.flushDays);
        if (wetClay) {
            s.compaction = Math.min(1.0, s.compaction + 0.15 * implement.incorporation + 0.05);
            s.tilth = Math.max(0.0, s.tilth - 0.2);
            return "Working wet clay smeared the soil and left hard clods. Wait for it to dry.";
        }
        s.compaction = Math.max(0.05, s.compaction * implement.compactionMultiplier);
        s.tilth = Math.min(1.0, s.tilth + implement.tilthGain);
        return null;
    }

    /** Wheel and hoof traffic compact the soil, much more so when it is wet. */
    public static void traffic(SoilState s, Hydraulics h, double load) {
        double wet = s.moisture > h.fieldCapacity() ? 2.5 : 1.0;
        s.compaction = Math.min(1.0, s.compaction + 0.02 * load * wet);
    }
}
