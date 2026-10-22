package com.claudacious.tilth.sim.crop;

import java.util.Map;

/**
 * The BBCH scale (Biologische Bundesanstalt, Bundessortenamt und CHemische Industrie), the
 * standard two-digit code for plant growth stages. The first digit is the principal stage.
 */
public final class Bbch {
    private static final String[] PRINCIPAL = {
            "Germination / sprouting",
            "Leaf development",
            "Formation of side shoots / tillering",
            "Stem elongation / crop cover",
            "Development of harvestable vegetative parts",
            "Inflorescence emergence / heading",
            "Flowering",
            "Development of fruit / grain fill",
            "Ripening",
            "Senescence"
    };

    private static final Map<Integer, String> DETAIL = Map.ofEntries(
            Map.entry(0, "Dry seed (sown)"),
            Map.entry(9, "Emergence"),
            Map.entry(12, "2 true leaves unfolded"),
            Map.entry(13, "3 leaves unfolded"),
            Map.entry(15, "5 leaves unfolded"),
            Map.entry(16, "6 leaves unfolded"),
            Map.entry(19, "9 or more leaves unfolded"),
            Map.entry(21, "First side shoot / tiller"),
            Map.entry(25, "5 side shoots"),
            Map.entry(31, "First node detectable"),
            Map.entry(33, "3 nodes detectable"),
            Map.entry(41, "Harvestable part begins to develop"),
            Map.entry(43, "30% of final size"),
            Map.entry(45, "50% of final size"),
            Map.entry(47, "70% of final size"),
            Map.entry(48, "80% of final size"),
            Map.entry(49, "Typical size and form reached"),
            Map.entry(51, "Inflorescence emerging"),
            Map.entry(55, "Half of inflorescence emerged"),
            Map.entry(60, "First flowers open"),
            Map.entry(65, "Full flowering"),
            Map.entry(69, "End of flowering"),
            Map.entry(71, "Fruit set"),
            Map.entry(75, "Medium milk / fruit half size"),
            Map.entry(81, "Beginning of ripening / colouring"),
            Map.entry(85, "Soft dough / most fruit coloured"),
            Map.entry(89, "Fully ripe"),
            Map.entry(91, "Foliage yellowing"),
            Map.entry(97, "Foliage dead, skin set")
    );

    private Bbch() {
    }

    public static String describe(int code) {
        String detail = DETAIL.get(code);
        String principal = PRINCIPAL[Math.max(0, Math.min(9, code / 10))];
        return String.format("BBCH %02d: %s", code, detail != null ? detail + " (" + principal + ")" : principal);
    }
}
