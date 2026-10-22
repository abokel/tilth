package com.claudacious.tilth.sim.crop;

/** Botanical families; rotation problems (shared pests and diseases) run along family lines. */
public enum CropFamily {
    POACEAE("Grasses (Poaceae)"),
    FABACEAE("Legumes (Fabaceae)"),
    SOLANACEAE("Nightshades (Solanaceae)"),
    BRASSICACEAE("Brassicas (Brassicaceae)"),
    AMARANTHACEAE("Beets (Amaranthaceae)"),
    AMARYLLIDACEAE("Alliums (Amaryllidaceae)"),
    APIACEAE("Umbellifers (Apiaceae)"),
    ASTERACEAE("Composites (Asteraceae)"),
    LINACEAE("Flax family (Linaceae)"),
    MALVACEAE("Mallows (Malvaceae)");

    private final String displayName;

    CropFamily(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
