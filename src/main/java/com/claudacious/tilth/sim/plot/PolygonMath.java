package com.claudacious.tilth.sim.plot;

import java.util.List;

/**
 * Geometry of field plots drawn as polygons on the block grid. Vertices are block coordinates
 * (x, z). A block belongs to the plot when its centre lies inside or on the boundary, so a plot
 * whose corners are the four corner blocks of a 10x10 field covers exactly 100 blocks.
 */
public final class PolygonMath {
    public static final double SQ_M_PER_ACRE = 4046.8564224;
    public static final double SQ_M_PER_HECTARE = 10000.0;
    public static final double FEET_PER_METRE = 3.280839895;

    private PolygonMath() {
    }

    public record Vertex(int x, int z) {
    }

    /** Twice the signed shoelace area of the polygon through the vertices. */
    public static long doubleSignedArea(List<Vertex> v) {
        long sum = 0;
        int n = v.size();
        for (int i = 0; i < n; i++) {
            Vertex a = v.get(i);
            Vertex b = v.get((i + 1) % n);
            sum += (long) a.x() * b.z() - (long) b.x() * a.z();
        }
        return sum;
    }

    /** Lattice points on the boundary: sum over edges of gcd(|dx|, |dz|). */
    public static long boundaryPoints(List<Vertex> v) {
        long sum = 0;
        int n = v.size();
        for (int i = 0; i < n; i++) {
            Vertex a = v.get(i);
            Vertex b = v.get((i + 1) % n);
            sum += gcd(Math.abs(b.x() - a.x()), Math.abs(b.z() - a.z()));
        }
        return sum;
    }

    /**
     * Number of blocks covered (interior + boundary lattice points), from Pick's theorem:
     * I + B = A + B/2 + 1.
     */
    public static long blockCount(List<Vertex> v) {
        if (v.size() < 3) {
            return v.isEmpty() ? 0 : boundaryPoints(v) + (v.size() == 1 ? 1 : 1);
        }
        long a2 = Math.abs(doubleSignedArea(v));
        long b = boundaryPoints(v);
        return (a2 + b) / 2 + 1;
    }

    public static double areaSquareMetres(List<Vertex> v, double metresPerBlock) {
        return blockCount(v) * metresPerBlock * metresPerBlock;
    }

    public static double acres(double squareMetres) {
        return squareMetres / SQ_M_PER_ACRE;
    }

    public static double hectares(double squareMetres) {
        return squareMetres / SQ_M_PER_HECTARE;
    }

    /** Perimeter through the vertices (m). */
    public static double perimeter(List<Vertex> v, double metresPerBlock) {
        double p = 0;
        int n = v.size();
        for (int i = 0; i < n; i++) {
            Vertex a = v.get(i);
            Vertex b = v.get((i + 1) % n);
            p += Math.hypot(b.x() - a.x(), b.z() - a.z());
        }
        return p * metresPerBlock;
    }

    /** True if the block at (x, z) is inside the polygon or on its boundary. */
    public static boolean contains(List<Vertex> v, int x, int z) {
        int n = v.size();
        if (n < 3) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (onSegment(v.get(i), v.get((i + 1) % n), x, z)) {
                return true;
            }
        }
        boolean inside = false;
        double px = x;
        double pz = z;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Vertex a = v.get(i);
            Vertex b = v.get(j);
            if ((a.z() > pz) != (b.z() > pz)) {
                double xCross = a.x() + (pz - a.z()) * (double) (b.x() - a.x()) / (b.z() - a.z());
                if (px < xCross) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    private static boolean onSegment(Vertex a, Vertex b, int x, int z) {
        long cross = (long) (b.x() - a.x()) * (z - a.z()) - (long) (b.z() - a.z()) * (x - a.x());
        if (cross != 0) {
            return false;
        }
        return x >= Math.min(a.x(), b.x()) && x <= Math.max(a.x(), b.x())
                && z >= Math.min(a.z(), b.z()) && z <= Math.max(a.z(), b.z());
    }

    /** True if any two non-adjacent edges cross (a bow-tie polygon has no well-defined area). */
    public static boolean selfIntersects(List<Vertex> v) {
        int n = v.size();
        if (n < 4) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            Vertex a1 = v.get(i);
            Vertex a2 = v.get((i + 1) % n);
            for (int j = i + 1; j < n; j++) {
                if (j == i || (j + 1) % n == i || (i + 1) % n == j) {
                    continue;
                }
                Vertex b1 = v.get(j);
                Vertex b2 = v.get((j + 1) % n);
                if (segmentsIntersect(a1, a2, b1, b2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean segmentsIntersect(Vertex p1, Vertex p2, Vertex p3, Vertex p4) {
        long d1 = orient(p3, p4, p1);
        long d2 = orient(p3, p4, p2);
        long d3 = orient(p1, p2, p3);
        long d4 = orient(p1, p2, p4);
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }
        return (d1 == 0 && onSegment(p3, p4, p1.x(), p1.z())) || (d2 == 0 && onSegment(p3, p4, p2.x(), p2.z()))
                || (d3 == 0 && onSegment(p1, p2, p3.x(), p3.z())) || (d4 == 0 && onSegment(p1, p2, p4.x(), p4.z()));
    }

    private static long orient(Vertex a, Vertex b, Vertex c) {
        return (long) (b.x() - a.x()) * (c.z() - a.z()) - (long) (b.z() - a.z()) * (c.x() - a.x());
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    /** Area formatted in acres with hectares and square metres, e.g. "1.012 ac (0.410 ha, 4,096 m²)". */
    public static String formatArea(double squareMetres) {
        return String.format("%.3f ac (%.3f ha, %,.0f m²)", acres(squareMetres), hectares(squareMetres), squareMetres);
    }
}
