package com.claudacious.tilth.sim.soil;

/** USDA soil texture triangle classification (NRCS rules). */
public final class TextureTriangle {
    private TextureTriangle() {
    }

    /**
     * @param sandPct sand percentage (0.05-2 mm)
     * @param clayPct clay percentage (&lt; 0.002 mm); silt is the remainder
     */
    public static SoilTexture classify(double sandPct, double clayPct) {
        double sand = Math.max(0, Math.min(100, sandPct));
        double clay = Math.max(0, Math.min(100 - sand, clayPct));
        double silt = 100 - sand - clay;

        if (silt + 1.5 * clay < 15) {
            return SoilTexture.SAND;
        }
        if (silt + 2 * clay < 30) {
            return SoilTexture.LOAMY_SAND;
        }
        if ((clay >= 7 && clay < 20 && sand > 52) || (clay < 7 && silt < 50)) {
            return SoilTexture.SANDY_LOAM;
        }
        if (clay >= 7 && clay < 27 && silt >= 28 && silt < 50 && sand <= 52) {
            return SoilTexture.LOAM;
        }
        if ((silt >= 50 && clay >= 12 && clay < 27) || (silt >= 50 && silt < 80 && clay < 12)) {
            return SoilTexture.SILT_LOAM;
        }
        if (silt >= 80 && clay < 12) {
            return SoilTexture.SILT;
        }
        if (clay >= 20 && clay < 35 && silt < 28 && sand > 45) {
            return SoilTexture.SANDY_CLAY_LOAM;
        }
        if (clay >= 27 && clay < 40 && sand > 20 && sand <= 45) {
            return SoilTexture.CLAY_LOAM;
        }
        if (clay >= 27 && clay < 40 && sand <= 20) {
            return SoilTexture.SILTY_CLAY_LOAM;
        }
        if (clay >= 35 && sand > 45) {
            return SoilTexture.SANDY_CLAY;
        }
        if (clay >= 40 && silt >= 40) {
            return SoilTexture.SILTY_CLAY;
        }
        if (clay >= 40) {
            return SoilTexture.CLAY;
        }
        // Thin slivers between class boundaries.
        return clay >= 27 ? SoilTexture.CLAY_LOAM : SoilTexture.LOAM;
    }
}
