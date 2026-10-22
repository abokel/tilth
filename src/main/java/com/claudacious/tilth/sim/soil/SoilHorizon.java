package com.claudacious.tilth.sim.soil;

/** Master soil horizons represented by blocks. */
public enum SoilHorizon {
    /** A horizon: dark, humus-rich, biologically active. */
    TOPSOIL("topsoil"),
    /** B horizon: accumulates clay, iron and carbonates washed down from above. */
    SUBSOIL("subsoil"),
    /** Bg horizon: grey-blue, reduced iron from permanent waterlogging. */
    GLEY("gley");

    private final String id;

    SoilHorizon(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
