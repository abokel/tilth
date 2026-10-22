package com.claudacious.tilth.sim;

/**
 * Deterministic hash-based noise. Every random-looking quantity in the simulation (weather, soil
 * variation, parent material patches) is derived from the world seed through these functions, so a
 * chunk that is unloaded for a month and then caught up gets exactly the weather it would have had.
 */
public final class Noise {
    private Noise() {
    }

    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static long hash(long seed, long a) {
        return mix(seed ^ mix(a + 0x9E3779B97F4A7C15L));
    }

    public static long hash(long seed, long a, long b) {
        return mix(hash(seed, a) ^ mix(b * 0x632BE59BD9B4E019L + 0x5851F42D4C957F2DL));
    }

    public static long hash(long seed, long a, long b, long c) {
        return mix(hash(seed, a, b) ^ mix(c * 0xD6E8FEB86659FD93L + 0x2545F4914F6CDD1DL));
    }

    /** Uniform double in [0, 1). */
    public static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }

    public static double unit(long seed, long a) {
        return unit(hash(seed, a));
    }

    public static double unit(long seed, long a, long b) {
        return unit(hash(seed, a, b));
    }

    public static double unit(long seed, long a, long b, long c) {
        return unit(hash(seed, a, b, c));
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    /** Smooth 1D value noise in [-1, 1]. */
    public static double value(long seed, double x) {
        long x0 = (long) Math.floor(x);
        double t = smooth(x - x0);
        double a = unit(seed, x0) * 2 - 1;
        double b = unit(seed, x0 + 1) * 2 - 1;
        return a + (b - a) * t;
    }

    /** Smooth 2D value noise in [-1, 1]. */
    public static double value(long seed, double x, double z) {
        long x0 = (long) Math.floor(x);
        long z0 = (long) Math.floor(z);
        double tx = smooth(x - x0);
        double tz = smooth(z - z0);
        double a = unit(seed, x0, z0) * 2 - 1;
        double b = unit(seed, x0 + 1, z0) * 2 - 1;
        double c = unit(seed, x0, z0 + 1) * 2 - 1;
        double d = unit(seed, x0 + 1, z0 + 1) * 2 - 1;
        double ab = a + (b - a) * tx;
        double cd = c + (d - c) * tx;
        return ab + (cd - ab) * tz;
    }

    /** Fractal sum of 2D value noise, normalised to roughly [-1, 1]. */
    public static double fbm(long seed, double x, double z, int octaves) {
        double sum = 0;
        double amp = 1;
        double norm = 0;
        double freq = 1;
        for (int i = 0; i < octaves; i++) {
            sum += amp * value(seed + i * 0x9E3779B97F4A7C15L, x * freq, z * freq);
            norm += amp;
            amp *= 0.5;
            freq *= 2.0;
        }
        return sum / norm;
    }

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
