package com.claudacious.tilth.sim.crop;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.claudacious.tilth.sim.crop.CropFamily.*;
import static com.claudacious.tilth.sim.crop.GrowthForm.*;
import static com.claudacious.tilth.sim.crop.YieldUnit.*;

/**
 * All crop species. Parameters come from the agronomic literature: cardinal temperatures and
 * thermal time from crop modelling studies, crop coefficients, rooting depths and depletion
 * fractions from FAO-56 tables 12 and 22, nutrient uptake from extension-service removal tables,
 * pH ranges and frost tolerances from standard field-crop references.
 */
public final class Crops {
    private static final Map<String, CropSpecies> BY_ID = new LinkedHashMap<>();

    // ---------------------------------------------------------------- Cereals (Poaceae)
    public static final CropSpecies COMMON_WHEAT = register(CropSpecies.builder("common_wheat", "Common Wheat", "Triticum aestivum", POACEAE, CEREAL)
            .temps(0, 22, 35, 26).gdd(100, 1500).frost(-12, -2).heat(32).ph(6.0, 7.5, 5.2, 8.5)
            .water(0.3, 1.15, 0.3, 1.5, 0.55, 0.25).uptake(105, 18, 80, 15, 15, 10)
            .yield(3.5, BUSHEL, 60).items("minecraft:wheat", 2, "minecraft:wheat_seeds", 2).residue(0.45, 0.6).build());
    public static final CropSpecies DURUM_WHEAT = register(CropSpecies.builder("durum_wheat", "Durum Wheat", "Triticum turgidum subsp. durum", POACEAE, CEREAL)
            .temps(0, 24, 36, 28).gdd(110, 1600).frost(-10, -2).heat(34).ph(6.0, 8.0, 5.5, 8.5)
            .water(0.3, 1.15, 0.3, 1.5, 0.6, 0.2).uptake(110, 18, 75, 14, 15, 10)
            .yield(3.0, BUSHEL, 60).items("tilth:durum_wheat", 2, "tilth:durum_wheat_seeds", 2).residue(0.4, 0.6).build());
    public static final CropSpecies BARLEY = register(CropSpecies.builder("barley", "Barley", "Hordeum vulgare", POACEAE, CEREAL)
            .temps(0, 20, 34, 26).gdd(100, 1250).frost(-10, -2).heat(32).ph(6.2, 8.0, 5.5, 8.8)
            .water(0.3, 1.15, 0.25, 1.3, 0.55, 0.15).uptake(95, 17, 85, 12, 15, 10)
            .yield(4.0, BUSHEL, 48).items("tilth:barley", 2, "tilth:barley_seeds", 2).residue(0.4, 0.7).build());
    public static final CropSpecies OATS = register(CropSpecies.builder("oats", "Oats", "Avena sativa", POACEAE, CEREAL)
            .temps(0, 20, 33, 25).gdd(100, 1350).frost(-7, -2).heat(30).ph(5.5, 7.0, 4.8, 8.0)
            .water(0.3, 1.15, 0.25, 1.2, 0.55, 0.3).uptake(85, 15, 90, 12, 12, 10)
            .yield(3.0, BUSHEL, 32).items("tilth:oats", 2, "tilth:oat_seeds", 2).residue(0.4, 0.7).build());
    public static final CropSpecies RYE = register(CropSpecies.builder("rye", "Rye", "Secale cereale", POACEAE, CEREAL)
            .temps(0, 18, 32, 25).gdd(100, 1400).frost(-20, -2).heat(31).ph(5.0, 7.0, 4.5, 8.0)
            .water(0.3, 1.15, 0.25, 1.5, 0.6, 0.25).uptake(80, 15, 75, 12, 12, 8)
            .yield(3.0, BUSHEL, 56).items("tilth:rye", 2, "tilth:rye_seeds", 2).residue(0.5, 0.5).build());
    public static final CropSpecies RICE = register(CropSpecies.builder("rice", "Rice", "Oryza sativa", POACEAE, CEREAL)
            .temps(10, 30, 42, 34).gdd(60, 1600).frost(2, 5).heat(35).ph(5.5, 7.0, 4.5, 8.0)
            .water(1.05, 1.2, 0.9, 0.5, 0.2, 1.0).uptake(100, 20, 110, 10, 15, 12)
            .yield(4.5, HUNDREDWEIGHT, 0).items("tilth:paddy_rice", 3, "tilth:paddy_rice", 0).residue(0.45, 0.7).flooded().build());
    public static final CropSpecies MAIZE = register(CropSpecies.builder("maize", "Maize", "Zea mays", POACEAE, TALL_GRASS)
            .temps(10, 30, 42, 30).gdd(60, 1350).frost(-1.5, -1).heat(35).ph(5.8, 7.0, 5.0, 8.0)
            .water(0.3, 1.2, 0.6, 1.5, 0.55, 0.2).uptake(190, 35, 170, 20, 30, 30)
            .yield(9.0, BUSHEL, 56).items("tilth:maize", 2, "tilth:maize_seeds", 2).residue(0.8, 0.8).tall(4).build());
    public static final CropSpecies SORGHUM = register(CropSpecies.builder("sorghum", "Sorghum", "Sorghum bicolor", POACEAE, TALL_GRASS)
            .temps(10, 31, 42, 33).gdd(60, 1300).frost(-1, 0).heat(38).ph(5.8, 7.5, 5.0, 8.5)
            .water(0.3, 1.05, 0.55, 1.5, 0.55, 0.35).uptake(120, 25, 120, 15, 20, 18)
            .yield(4.0, BUSHEL, 56).items("tilth:sorghum", 2, "tilth:sorghum_seeds", 2).residue(0.6, 0.8).tall(5).build());

    // ---------------------------------------------------------------- Legumes (Fabaceae)
    public static final CropSpecies SOYBEAN = register(CropSpecies.builder("soybean", "Soybean", "Glycine max", FABACEAE, LEGUME)
            .temps(10, 28, 40, 30).gdd(50, 1250).frost(-1.5, -1).heat(35).ph(6.0, 7.0, 5.2, 8.0)
            .water(0.4, 1.15, 0.5, 1.0, 0.5, 0.25).uptake(230, 25, 120, 20, 40, 25).fixation(0.6)
            .yield(3.0, BUSHEL, 60).items("tilth:soybeans", 3, "tilth:soybeans", 0).residue(0.3, 1.5).build());
    public static final CropSpecies PEA = register(CropSpecies.builder("pea", "Field Pea", "Pisum sativum", FABACEAE, LEGUME)
            .temps(4, 20, 32, 25).gdd(80, 1000).frost(-5, -2).heat(28).ph(6.0, 7.5, 5.5, 8.2)
            .water(0.5, 1.15, 0.3, 0.8, 0.35, 0.1).uptake(150, 15, 70, 12, 35, 12).fixation(0.7)
            .yield(3.0, BUSHEL, 60).items("tilth:peas", 3, "tilth:peas", 0).residue(0.25, 2.0).build());
    public static final CropSpecies CHICKPEA = register(CropSpecies.builder("chickpea", "Chickpea", "Cicer arietinum", FABACEAE, LEGUME)
            .temps(5, 25, 35, 30).gdd(80, 1400).frost(-4, -1.5).heat(33).ph(6.0, 8.0, 5.5, 8.8)
            .water(0.4, 1.0, 0.35, 0.8, 0.5, 0.05).uptake(110, 12, 60, 10, 25, 10).fixation(0.65)
            .yield(1.8, POUND, 0).items("tilth:chickpeas", 3, "tilth:chickpeas", 0).residue(0.2, 1.6).build());
    public static final CropSpecies LENTIL = register(CropSpecies.builder("lentil", "Lentil", "Lens culinaris", FABACEAE, LEGUME)
            .temps(5, 22, 33, 28).gdd(80, 1200).frost(-6, -2).heat(32).ph(6.0, 8.0, 5.5, 8.5)
            .water(0.4, 1.1, 0.3, 0.7, 0.5, 0.05).uptake(100, 12, 50, 10, 20, 8).fixation(0.7)
            .yield(1.5, POUND, 0).items("tilth:lentils", 3, "tilth:lentils", 0).residue(0.15, 1.8).build());
    public static final CropSpecies COMMON_BEAN = register(CropSpecies.builder("common_bean", "Common Bean", "Phaseolus vulgaris", FABACEAE, LEGUME)
            .temps(10, 25, 35, 30).gdd(50, 950).frost(-0.5, 0).heat(32).ph(6.0, 7.0, 5.3, 7.8)
            .water(0.4, 1.15, 0.35, 0.9, 0.45, 0.1).uptake(120, 15, 80, 12, 30, 12).fixation(0.4)
            .yield(2.0, HUNDREDWEIGHT, 0).items("tilth:beans", 3, "tilth:beans", 0).residue(0.2, 1.6).build());
    public static final CropSpecies PEANUT = register(CropSpecies.builder("peanut", "Peanut", "Arachis hypogaea", FABACEAE, LEGUME)
            .temps(10, 30, 40, 33).gdd(70, 1700).frost(-0.5, 0).heat(36).ph(5.8, 6.8, 5.0, 7.5)
            .water(0.4, 1.15, 0.6, 0.8, 0.5, 0.1).uptake(160, 15, 60, 15, 45, 15).fixation(0.65)
            .yield(3.5, POUND, 0).items("tilth:peanuts", 3, "tilth:peanuts", 0).residue(0.3, 1.8).build());
    public static final CropSpecies ALFALFA = register(CropSpecies.builder("alfalfa", "Alfalfa", "Medicago sativa", FABACEAE, FORAGE)
            .temps(5, 25, 37, 30).gdd(70, 550).frost(-20, -3).heat(38).ph(6.5, 7.5, 6.0, 8.5)
            .water(0.4, 0.95, 0.9, 1.5, 0.55, 0.05).uptake(75, 7, 55, 6, 35, 7).fixation(0.85)
            .yield(2.5, TON, 0).items("tilth:alfalfa", 3, "tilth:alfalfa_seeds", 1).residue(0.15, 3.0)
            .harvestAt(7).perennial(3, 8).build());
    public static final CropSpecies CLOVER = register(CropSpecies.builder("clover", "Red Clover", "Trifolium pratense", FABACEAE, FORAGE)
            .temps(4, 22, 33, 27).gdd(60, 600).frost(-15, -3).heat(32).ph(6.0, 7.0, 5.5, 7.8)
            .water(0.4, 0.9, 0.85, 0.8, 0.5, 0.4).uptake(60, 6, 50, 5, 25, 6).fixation(0.8)
            .yield(2.0, TON, 0).items("tilth:clover", 2, "tilth:clover_seeds", 1).residue(0.15, 3.0)
            .harvestAt(7).perennial(3, 6).build());

    // ---------------------------------------------------------------- Roots, tubers and bulbs
    public static final CropSpecies POTATO = register(CropSpecies.builder("potato", "Potato", "Solanum tuberosum", SOLANACEAE, TUBER)
            .temps(7, 18, 30, 22).gdd(150, 1100).frost(-1.5, -1.5).heat(29).ph(5.0, 6.5, 4.5, 7.5)
            .water(0.5, 1.15, 0.75, 0.5, 0.35, 0.1).uptake(150, 25, 220, 15, 10, 15)
            .yield(35, HUNDREDWEIGHT, 0).items("minecraft:potato", 4, "minecraft:potato", 0).residue(0.15, 2.0).build());
    public static final CropSpecies CARROT = register(CropSpecies.builder("carrot", "Carrot", "Daucus carota subsp. sativus", APIACEAE, ROOT)
            .temps(4, 18, 30, 24).gdd(160, 1250).frost(-5, -4).heat(30).ph(6.0, 6.8, 5.5, 7.5)
            .water(0.7, 1.05, 0.95, 0.6, 0.35, 0.15).uptake(110, 20, 200, 10, 40, 15)
            .yield(40, HUNDREDWEIGHT, 0).items("minecraft:carrot", 4, "minecraft:carrot", 0).residue(0.1, 2.5).build());
    public static final CropSpecies BEETROOT = register(CropSpecies.builder("beetroot", "Beetroot", "Beta vulgaris (table beet)", AMARANTHACEAE, ROOT)
            .temps(4, 20, 32, 26).gdd(110, 1000).frost(-3, -2).heat(32).ph(6.0, 7.5, 5.5, 8.0)
            .water(0.5, 1.05, 0.95, 0.6, 0.5, 0.2).uptake(110, 18, 160, 12, 25, 18)
            .yield(30, HUNDREDWEIGHT, 0).items("minecraft:beetroot", 2, "minecraft:beetroot_seeds", 2).residue(0.1, 2.5).build());
    /**
     * Starter crop. Deliberately unrealistic: it ripens in about two or three game days in any mild
     * weather, tolerates poor soil and light frost, but yields little and never grades above 2.
     */
    public static final CropSpecies RADISH = register(CropSpecies.builder("radish", "Radish", "Raphanus sativus", BRASSICACEAE, ROOT)
            .temps(2, 20, 34, 28).gdd(20, 130).frost(-6, -5).heat(34).ph(5.5, 7.5, 4.5, 8.5)
            .water(0.7, 0.9, 0.85, 0.3, 0.6, 0.35).uptake(25, 5, 35, 5, 8, 3)
            .yield(6, HUNDREDWEIGHT, 0).items("tilth:radish", 2, "tilth:radish_seeds", 2).residue(0.05, 2.5).maxGrade(2).build());
    public static final CropSpecies SUGAR_BEET = register(CropSpecies.builder("sugar_beet", "Sugar Beet", "Beta vulgaris (sugar beet)", AMARANTHACEAE, ROOT)
            .temps(3, 20, 33, 26).gdd(120, 2200).frost(-4, -3).heat(33).ph(6.5, 8.0, 6.0, 8.5)
            .water(0.35, 1.2, 0.7, 1.0, 0.55, 0.2).uptake(180, 30, 250, 20, 40, 30)
            .yield(60, TON, 0).items("tilth:sugar_beet", 2, "tilth:sugar_beet_seeds", 1).residue(0.3, 2.5).build());
    public static final CropSpecies ONION = register(CropSpecies.builder("onion", "Onion", "Allium cepa", AMARYLLIDACEAE, BULB)
            .temps(6, 20, 32, 26).gdd(110, 1500).frost(-6, -3).heat(32).ph(6.0, 7.0, 5.5, 7.8)
            .water(0.7, 1.05, 0.75, 0.4, 0.3, 0.15).uptake(120, 22, 120, 25, 25, 8)
            .yield(40, HUNDREDWEIGHT, 0).items("tilth:onion", 2, "tilth:onion_seeds", 1).residue(0.05, 2.0).build());
    public static final CropSpecies GARLIC = register(CropSpecies.builder("garlic", "Garlic", "Allium sativum", AMARYLLIDACEAE, BULB)
            .temps(0, 18, 30, 24).gdd(130, 1700).frost(-15, -4).heat(30).ph(6.0, 7.5, 5.5, 8.0)
            .water(0.7, 1.0, 0.7, 0.4, 0.3, 0.15).uptake(120, 22, 110, 30, 20, 8)
            .yield(10, HUNDREDWEIGHT, 0).items("tilth:garlic", 3, "tilth:garlic", 0).residue(0.05, 2.0).build());

    // ---------------------------------------------------------------- Vegetables
    public static final CropSpecies CABBAGE = register(CropSpecies.builder("cabbage", "Cabbage", "Brassica oleracea var. capitata", BRASSICACEAE, HEAD_VEGETABLE)
            .temps(4, 18, 30, 24).gdd(80, 1100).frost(-8, -4).heat(28).ph(6.0, 7.5, 5.5, 8.0)
            .water(0.7, 1.05, 0.95, 0.6, 0.45, 0.3).uptake(180, 28, 200, 30, 60, 15)
            .yield(50, TON, 0).items("tilth:cabbage", 1, "tilth:cabbage_seeds", 1).residue(0.25, 2.5).build());
    public static final CropSpecies LETTUCE = register(CropSpecies.builder("lettuce", "Lettuce", "Lactuca sativa", ASTERACEAE, HEAD_VEGETABLE)
            .temps(4, 18, 28, 24).gdd(50, 650).frost(-3, -2).heat(27).ph(6.0, 7.0, 5.5, 7.8)
            .water(0.7, 1.0, 0.95, 0.4, 0.3, 0.2).uptake(90, 15, 120, 8, 20, 6)
            .yield(35, HUNDREDWEIGHT, 0).items("tilth:lettuce", 1, "tilth:lettuce_seeds", 1).residue(0.1, 2.5).build());
    public static final CropSpecies TOMATO = register(CropSpecies.builder("tomato", "Tomato", "Solanum lycopersicum", SOLANACEAE, FRUITING)
            .temps(10, 25, 35, 30).gdd(70, 1250).frost(0, 0.5).heat(32).ph(6.0, 6.8, 5.5, 7.5)
            .water(0.6, 1.15, 0.8, 1.0, 0.4, 0.1).uptake(150, 25, 250, 25, 120, 25)
            .yield(60, TON, 0).items("tilth:tomato", 3, "tilth:tomato_seeds", 1).residue(0.2, 2.0)
            .perennial(7, 3).build());

    // ---------------------------------------------------------------- Oil and fibre crops
    public static final CropSpecies SUNFLOWER = register(CropSpecies.builder("sunflower", "Sunflower", "Helianthus annuus", ASTERACEAE, TALL_FLOWER)
            .temps(6, 25, 38, 30).gdd(80, 1500).frost(-3, -2).heat(36).ph(6.0, 7.5, 5.5, 8.5)
            .water(0.35, 1.1, 0.35, 1.2, 0.45, 0.15).uptake(110, 22, 150, 15, 40, 20)
            .yield(2.5, POUND, 0).items("tilth:sunflower_seeds", 4, "tilth:sunflower_seeds", 0).residue(0.5, 0.9).tall(4).build());
    public static final CropSpecies FLAX = register(CropSpecies.builder("flax", "Flax", "Linum usitatissimum", LINACEAE, FIBER)
            .temps(3, 20, 32, 26).gdd(90, 1150).frost(-4, -2).heat(30).ph(6.0, 7.0, 5.0, 8.0)
            .water(0.35, 1.1, 0.25, 0.8, 0.5, 0.2).uptake(70, 12, 60, 12, 15, 8)
            .yield(1.5, BUSHEL, 56).items("tilth:flax", 2, "tilth:flax_seeds", 2).residue(0.2, 0.8).build());
    public static final CropSpecies COTTON = register(CropSpecies.builder("cotton", "Cotton", "Gossypium hirsutum", MALVACEAE, FIBER)
            .temps(15.5, 30, 40, 32).gdd(40, 1150).frost(-0.5, 0).heat(38).ph(5.8, 8.0, 5.0, 8.5)
            .water(0.35, 1.15, 0.6, 1.3, 0.65, 0.15).uptake(130, 22, 120, 15, 30, 15)
            .yield(2.5, BALE, 0.38).items("tilth:cotton_boll", 2, "tilth:cotton_seeds", 1).residue(0.4, 1.0).build());

    private Crops() {
    }

    private static CropSpecies register(CropSpecies species) {
        BY_ID.put(species.id, species);
        return species;
    }

    public static List<CropSpecies> all() {
        return List.copyOf(BY_ID.values());
    }

    public static CropSpecies byId(String id) {
        return BY_ID.get(id);
    }

    public static Map<String, CropSpecies> map() {
        return Collections.unmodifiableMap(BY_ID);
    }
}
