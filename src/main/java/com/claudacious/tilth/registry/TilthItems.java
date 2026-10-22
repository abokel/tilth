package com.claudacious.tilth.registry;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.item.AmendmentItem;
import com.claudacious.tilth.item.ArdItem;
import com.claudacious.tilth.item.CraftingToolItem;
import com.claudacious.tilth.item.DrinkItem;
import com.claudacious.tilth.item.PlowItem;
import com.claudacious.tilth.item.SoilProbeItem;
import com.claudacious.tilth.item.SurveyorsChainItem;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.Crops;
import com.claudacious.tilth.sim.soil.Amendment;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TilthItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Tilth.MOD_ID);

    /** Raw crop produce that can be eaten as harvested. */
    private static final Map<String, FoodProperties> RAW_FOOD = new HashMap<>();

    static {
        RAW_FOOD.put("tomato", food(2, 0.3f));
        RAW_FOOD.put("lettuce", food(1, 0.3f));
        RAW_FOOD.put("cabbage", food(2, 0.4f));
        RAW_FOOD.put("onion", food(1, 0.2f));
        RAW_FOOD.put("garlic", food(1, 0.2f));
        RAW_FOOD.put("soybeans", food(1, 0.2f));
        RAW_FOOD.put("peas", food(1, 0.3f));
        RAW_FOOD.put("chickpeas", food(1, 0.3f));
        RAW_FOOD.put("lentils", food(1, 0.2f));
        // Raw kidney-type beans contain phytohaemagglutinin and make you sick until cooked.
        RAW_FOOD.put("beans", new FoodProperties.Builder().nutrition(1).saturationModifier(0.2f)
                .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 300), 0.5f)
                .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 200), 0.5f).build());
        RAW_FOOD.put("peanuts", food(2, 0.4f));
        RAW_FOOD.put("sunflower_seeds", food(1, 0.3f));
        RAW_FOOD.put("maize", food(2, 0.3f));
        RAW_FOOD.put("sugar_beet", food(1, 0.2f));
    }

    /** Every seed and produce item of the Tilth crops, by path. */
    public static final Map<String, DeferredItem<? extends Item>> CROP_ITEMS = new LinkedHashMap<>();

    static {
        for (DeferredBlock<?> b : TilthBlocks.TOPSOIL.values()) {
            ITEMS.registerSimpleBlockItem(b);
        }
        for (DeferredBlock<?> b : TilthBlocks.SUBSOIL.values()) {
            ITEMS.registerSimpleBlockItem(b);
        }
        for (DeferredBlock<?> b : TilthBlocks.GRASS.values()) {
            ITEMS.registerSimpleBlockItem(b);
        }
        ITEMS.registerSimpleBlockItem(TilthBlocks.GLEY);
        for (CropSpecies sp : Crops.all()) {
            registerCropItem(sp, sp.seedItem, true);
            if (!sp.seedIsProduce()) {
                registerCropItem(sp, sp.produceItem, false);
            }
        }
    }

    // ------------------------------------------------------------------ tools
    public static final DeferredItem<CraftingToolItem> QUERN = ITEMS.register("quern", () -> new CraftingToolItem(new Item.Properties().durability(256)));
    public static final DeferredItem<CraftingToolItem> KITCHEN_KNIFE = ITEMS.register("kitchen_knife", () -> new CraftingToolItem(new Item.Properties().durability(160)));
    public static final DeferredItem<SoilProbeItem> SOIL_PROBE = ITEMS.register("soil_probe", () -> new SoilProbeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SurveyorsChainItem> SURVEYORS_CHAIN = ITEMS.register("surveyors_chain", () -> new SurveyorsChainItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ArdItem> ARD = ITEMS.register("ard", () -> new ArdItem(new Item.Properties().durability(200)));
    public static final DeferredItem<PlowItem> PLOW = ITEMS.register("plow", () -> new PlowItem(new Item.Properties().stacksTo(1)));

    // ------------------------------------------------------------------ soil amendments
    public static final DeferredItem<AmendmentItem> MANURE = amendment("manure", Amendment.MANURE);
    public static final DeferredItem<AmendmentItem> COMPOST = amendment("compost", Amendment.COMPOST);
    public static final DeferredItem<AmendmentItem> WOOD_ASH = amendment("wood_ash", Amendment.WOOD_ASH);
    public static final DeferredItem<AmendmentItem> AGRICULTURAL_LIME = amendment("agricultural_lime", Amendment.AGRICULTURAL_LIME);
    public static final DeferredItem<AmendmentItem> ELEMENTAL_SULFUR = amendment("elemental_sulfur", Amendment.SULFUR);
    public static final DeferredItem<AmendmentItem> STRAW = amendment("straw", Amendment.STRAW);

    // ------------------------------------------------------------------ processing intermediates
    public static final DeferredItem<Item> WHEAT_FLOUR = simple("wheat_flour");
    public static final DeferredItem<Item> DURUM_SEMOLINA = simple("durum_semolina");
    public static final DeferredItem<Item> RYE_FLOUR = simple("rye_flour");
    public static final DeferredItem<Item> CORNMEAL = simple("cornmeal");
    public static final DeferredItem<Item> CHICKPEA_FLOUR = simple("chickpea_flour");
    public static final DeferredItem<Item> LENTIL_FLOUR = simple("lentil_flour");
    public static final DeferredItem<Item> MILLED_RICE = simple("milled_rice");
    public static final DeferredItem<Item> FLAX_FIBER = simple("flax_fiber");
    public static final DeferredItem<Item> VEGETABLE_OIL = ITEMS.register("vegetable_oil", () -> new Item(new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16)));
    public static final DeferredItem<Item> PEANUT_BUTTER = ITEMS.register("peanut_butter", () -> new Item(new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16)));
    public static final DeferredItem<Item> WHEAT_DOUGH = simple("wheat_dough");
    public static final DeferredItem<Item> RYE_DOUGH = simple("rye_dough");
    public static final DeferredItem<Item> PASTA_DOUGH = simple("pasta_dough");
    public static final DeferredItem<Item> PROTEIN_PASTA_DOUGH = simple("protein_pasta_dough");
    public static final DeferredItem<Item> MASA_DOUGH = simple("masa_dough");
    public static final DeferredItem<Item> CORNBREAD_BATTER = simple("cornbread_batter");
    public static final DeferredItem<Item> RAW_PASTA = simple("raw_pasta");
    public static final DeferredItem<Item> RAW_PROTEIN_PASTA = simple("raw_protein_pasta");
    public static final DeferredItem<Item> RAW_FRIES = simple("raw_fries");
    public static final DeferredItem<Item> UNCOOKED_RICE = ITEMS.register("uncooked_rice", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SOAKED_BEANS = ITEMS.register("soaked_beans", () -> new Item(new Item.Properties().stacksTo(16)));

    // ------------------------------------------------------------------ foods
    public static final DeferredItem<Item> ROLLED_OATS = food("rolled_oats", 2, 0.3f);
    public static final DeferredItem<DrinkItem> SOY_MILK = ITEMS.register("soy_milk", () -> new DrinkItem(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f).usingConvertsTo(Items.GLASS_BOTTLE).build())));
    public static final DeferredItem<Item> COOKED_PASTA = food("cooked_pasta", 5, 0.5f);
    public static final DeferredItem<Item> PROTEIN_PASTA = food("protein_pasta", 7, 0.7f);
    public static final DeferredItem<Item> PASTA_MARINARA = bowlFood("pasta_marinara", 9, 0.7f);
    public static final DeferredItem<Item> RYE_BREAD = food("rye_bread", 5, 0.7f);
    public static final DeferredItem<Item> CORNBREAD = food("cornbread", 6, 0.6f);
    public static final DeferredItem<Item> TORTILLA = food("tortilla", 3, 0.4f);
    public static final DeferredItem<Item> BURRITO = food("burrito", 10, 0.8f);
    public static final DeferredItem<Item> PORRIDGE = bowlFood("porridge", 6, 0.6f);
    public static final DeferredItem<Item> COOKED_RICE = bowlFood("cooked_rice", 5, 0.5f);
    public static final DeferredItem<Item> COOKED_BEANS = bowlFood("cooked_beans", 6, 0.7f);
    public static final DeferredItem<Item> RICE_AND_BEANS = bowlFood("rice_and_beans", 9, 0.8f);
    public static final DeferredItem<Item> BAKED_BEANS = bowlFood("baked_beans", 7, 0.6f);
    public static final DeferredItem<Item> LENTIL_SOUP = bowlFood("lentil_soup", 7, 0.7f);
    public static final DeferredItem<Item> PEA_SOUP = bowlFood("pea_soup", 7, 0.6f);
    public static final DeferredItem<Item> TOMATO_SOUP = bowlFood("tomato_soup", 6, 0.5f);
    public static final DeferredItem<Item> ONION_SOUP = bowlFood("onion_soup", 6, 0.5f);
    public static final DeferredItem<Item> TOMATO_SAUCE = bowlFood("tomato_sauce", 2, 0.2f);
    public static final DeferredItem<Item> HUMMUS = bowlFood("hummus", 5, 0.5f);
    public static final DeferredItem<Item> GARDEN_SALAD = bowlFood("garden_salad", 4, 0.5f);
    public static final DeferredItem<Item> COLESLAW = bowlFood("coleslaw", 4, 0.4f);
    public static final DeferredItem<Item> TOFU = food("tofu", 5, 0.6f);
    public static final DeferredItem<Item> POPCORN = ITEMS.register("popcorn", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).fast().build())));
    public static final DeferredItem<Item> ROASTED_MAIZE = food("roasted_maize", 5, 0.5f);
    public static final DeferredItem<Item> FRIES = food("fries", 5, 0.4f);
    public static final DeferredItem<Item> ROASTED_SUNFLOWER_SEEDS = ITEMS.register("roasted_sunflower_seeds", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4f).fast().build())));
    public static final DeferredItem<Item> ROASTED_PEANUTS = ITEMS.register("roasted_peanuts", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.6f).fast().build())));
    public static final DeferredItem<Item> GARLIC_BREAD = food("garlic_bread", 6, 0.6f);
    public static final DeferredItem<Item> GRANOLA_BAR = food("granola_bar", 5, 0.6f);
    public static final DeferredItem<Item> PEANUT_BUTTER_SANDWICH = food("peanut_butter_sandwich", 8, 0.7f);

    private TilthItems() {
    }

    private static FoodProperties food(int nutrition, float saturation) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
    }

    private static DeferredItem<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().food(food(nutrition, saturation))));
    }

    private static DeferredItem<Item> bowlFood(String name, int nutrition, float saturation) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                .nutrition(nutrition).saturationModifier(saturation).usingConvertsTo(Items.BOWL).build())));
    }

    private static DeferredItem<AmendmentItem> amendment(String name, Amendment amendment) {
        return ITEMS.register(name, () -> new AmendmentItem(amendment, new Item.Properties()));
    }

    private static void registerCropItem(CropSpecies sp, String fullId, boolean seed) {
        if (!fullId.startsWith(Tilth.MOD_ID + ":")) {
            return;
        }
        String name = fullId.substring(Tilth.MOD_ID.length() + 1);
        if (CROP_ITEMS.containsKey(name)) {
            return;
        }
        FoodProperties food = RAW_FOOD.get(name);
        DeferredItem<? extends Item> item;
        if (seed) {
            item = ITEMS.register(name, () -> {
                Item.Properties props = new Item.Properties();
                if (food != null) {
                    props.food(food);
                }
                return new ItemNameBlockItem(TilthBlocks.crop(sp).get(), props);
            });
        } else if (name.equals("alfalfa") || name.equals("clover")) {
            item = ITEMS.register(name, () -> new AmendmentItem(Amendment.GREEN_MANURE, new Item.Properties()));
        } else {
            item = ITEMS.register(name, () -> {
                Item.Properties props = new Item.Properties();
                if (food != null) {
                    props.food(food);
                }
                return new Item(props);
            });
        }
        CROP_ITEMS.put(name, item);
    }
}
