package com.claudacious.tilth.sim.catena;

/**
 * Terrain analysis for one column: slope gradient, local relief, plan/profile curvature and
 * landscape-scale relief, classified into a hillslope position.
 */
public final class CatenaAnalyzer {
    private static final int[][] RING = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {2, -2}, {-2, 2}, {-2, -2}};

    private CatenaAnalyzer() {
    }

    /** Terrain surface height (top of the ground, ignoring vegetation) at a block column. */
    @FunctionalInterface
    public interface HeightField {
        int height(int x, int z);
    }

    /**
     * @param height        surface height of the column
     * @param slopeDegrees  gradient over a 4-block baseline
     * @param localRelief   height above the mean of the surrounding 13x13 block window
     * @param curvature     mean height of the ring at distance 2 minus the centre; positive = hollow
     * @param macroRelief   height above the landscape mean within ~80 blocks
     */
    public record Result(int height, double slopeDegrees, double localRelief, double curvature, double macroRelief,
                         SlopePosition position) {
    }

    public static Result analyze(HeightField f, int x, int z, double macroRelief, boolean nearWater, int seaLevel) {
        int h = f.height(x, z);
        double gx = (f.height(x + 2, z) - f.height(x - 2, z)) / 4.0;
        double gz = (f.height(x, z + 2) - f.height(x, z - 2)) / 4.0;
        double slope = Math.toDegrees(Math.atan(Math.hypot(gx, gz)));

        double sum = 0;
        int n = 0;
        for (int dx = -6; dx <= 6; dx += 2) {
            for (int dz = -6; dz <= 6; dz += 2) {
                sum += f.height(x + dx, z + dz);
                n++;
            }
        }
        double localRelief = h - sum / n;

        double ring = 0;
        for (int[] o : RING) {
            ring += f.height(x + o[0], z + o[1]);
        }
        double curvature = ring / RING.length - h;

        SlopePosition pos = classify(slope, localRelief, curvature, macroRelief, nearWater, h, seaLevel);
        return new Result(h, slope, localRelief, curvature, macroRelief, pos);
    }

    public static SlopePosition classify(double slope, double localRelief, double curvature, double macroRelief,
                                         boolean nearWater, int height, int seaLevel) {
        if (nearWater && height <= seaLevel + 3 && slope < 6) {
            return SlopePosition.FLOODPLAIN;
        }
        if (curvature > 0.9 && localRelief < -1.2 && slope < 8) {
            return SlopePosition.DEPRESSION;
        }
        if (slope >= 22) {
            return SlopePosition.BACKSLOPE;
        }
        if (macroRelief > 7 && slope < 10 && curvature <= 0.3) {
            return SlopePosition.SUMMIT;
        }
        if ((macroRelief > 2.5 || localRelief > 1.0) && curvature < -0.4) {
            return SlopePosition.SHOULDER;
        }
        if (macroRelief < -4 && slope < 6) {
            return SlopePosition.TOESLOPE;
        }
        if ((macroRelief < 0 || localRelief < 0) && curvature > 0.25 && slope < 15) {
            return SlopePosition.FOOTSLOPE;
        }
        if (slope >= 10) {
            return SlopePosition.BACKSLOPE;
        }
        if (macroRelief < -1.5 && localRelief < 0) {
            return SlopePosition.TOESLOPE;
        }
        return SlopePosition.LEVEL;
    }
}
