package com.claudacious.tilth.sim;

import com.claudacious.tilth.sim.plot.PolygonMath;
import com.claudacious.tilth.sim.plot.PolygonMath.Vertex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolygonMathTest {
    private static List<Vertex> rect(int w, int h) {
        return List.of(new Vertex(0, 0), new Vertex(w - 1, 0), new Vertex(w - 1, h - 1), new Vertex(0, h - 1));
    }

    @Test
    void cornerBlocksOfTenByTenFieldCoverHundredBlocks() {
        assertEquals(100, PolygonMath.blockCount(rect(10, 10)));
    }

    @Test
    void sixtyFourSquareIsAboutAnAcre() {
        double m2 = PolygonMath.areaSquareMetres(rect(64, 64), 1.0);
        assertEquals(4096, m2, 1e-9);
        assertEquals(1.012, PolygonMath.acres(m2), 0.001);
    }

    @Test
    void lShapedPlotCountsMatchBruteForce() {
        List<Vertex> l = List.of(new Vertex(0, 0), new Vertex(20, 0), new Vertex(20, 8), new Vertex(8, 8),
                new Vertex(8, 20), new Vertex(0, 20));
        long brute = 0;
        for (int x = -2; x <= 22; x++) {
            for (int z = -2; z <= 22; z++) {
                if (PolygonMath.contains(l, x, z)) {
                    brute++;
                }
            }
        }
        assertEquals(brute, PolygonMath.blockCount(l));
    }

    @Test
    void diagonalTriangleCountsMatchBruteForce() {
        List<Vertex> t = List.of(new Vertex(0, 0), new Vertex(17, 5), new Vertex(6, 23));
        long brute = 0;
        for (int x = -1; x <= 25; x++) {
            for (int z = -1; z <= 25; z++) {
                if (PolygonMath.contains(t, x, z)) {
                    brute++;
                }
            }
        }
        assertEquals(brute, PolygonMath.blockCount(t));
    }

    @Test
    void bowTieIsSelfIntersecting() {
        List<Vertex> bowTie = List.of(new Vertex(0, 0), new Vertex(10, 10), new Vertex(10, 0), new Vertex(0, 10));
        assertTrue(PolygonMath.selfIntersects(bowTie));
        assertFalse(PolygonMath.selfIntersects(rect(5, 5)));
    }
}
