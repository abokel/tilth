package com.claudacious.tilth.registry;

import com.claudacious.tilth.quality.ConsumingShapelessRecipe;
import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.affliction.Affliction;
import com.claudacious.tilth.diet.DietEffect;
import com.claudacious.tilth.diet.NutrientProfile;
import com.claudacious.tilth.loot.GrassSeedModifier;
import com.claudacious.tilth.quality.QualityCookingRecipes;
import com.claudacious.tilth.quality.QualityShapelessRecipe;
import com.claudacious.tilth.worldgen.CatenaSoilFeature;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCookingSerializer;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** Smaller registries: features, recipes, effects, loot modifiers, creative tab, data maps, afflictions. */
public final class TilthMisc {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Tilth.MOD_ID);
    public static final DeferredHolder<Feature<?>, CatenaSoilFeature> CATENA_SOILS = FEATURES.register("catena_soils",
            () -> new CatenaSoilFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Tilth.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, ConsumingShapelessRecipe.Serializer> CONSUMING_SHAPELESS =
            RECIPE_SERIALIZERS.register("consuming_shapeless", ConsumingShapelessRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, QualityShapelessRecipe.Serializer> QUALITY_SHAPELESS =
            RECIPE_SERIALIZERS.register("quality_shapeless", QualityShapelessRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCookingSerializer<QualityCookingRecipes.Smelting>> QUALITY_SMELTING =
            RECIPE_SERIALIZERS.register("quality_smelting", () -> new SimpleCookingSerializer<>(QualityCookingRecipes.Smelting::new, 200));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCookingSerializer<QualityCookingRecipes.Smoking>> QUALITY_SMOKING =
            RECIPE_SERIALIZERS.register("quality_smoking", () -> new SimpleCookingSerializer<>(QualityCookingRecipes.Smoking::new, 100));
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCookingSerializer<QualityCookingRecipes.Campfire>> QUALITY_CAMPFIRE =
            RECIPE_SERIALIZERS.register("quality_campfire_cooking", () -> new SimpleCookingSerializer<>(QualityCookingRecipes.Campfire::new, 600));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Tilth.MOD_ID);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<GrassSeedModifier>> GRASS_SEEDS =
            LOOT_MODIFIERS.register("grass_seeds", () -> GrassSeedModifier.CODEC);

    // ------------------------------------------------------------------ diet status effects
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Tilth.MOD_ID);
    public static final DeferredHolder<MobEffect, MobEffect> WELL_NOURISHED = EFFECTS.register("well_nourished",
            () -> new DietEffect(MobEffectCategory.BENEFICIAL, 0x6DBE45)
                    .addAttributeModifier(Attributes.MAX_HEALTH, Tilth.id("effect.well_nourished"), 4.0, AttributeModifier.Operation.ADD_VALUE));
    public static final DeferredHolder<MobEffect, MobEffect> MALNOURISHED = EFFECTS.register("malnourished",
            () -> new DietEffect(MobEffectCategory.HARMFUL, 0x7A6A53)
                    .addAttributeModifier(Attributes.MAX_HEALTH, Tilth.id("effect.malnourished"), -4.0, AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, Tilth.id("effect.malnourished_attack"), -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> PROTEIN_DEFICIENCY = EFFECTS.register("protein_deficiency",
            () -> new DietEffect(MobEffectCategory.HARMFUL, 0xC2563B)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, Tilth.id("effect.protein_deficiency"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> LOW_ENERGY = EFFECTS.register("low_energy",
            () -> new DietEffect(MobEffectCategory.HARMFUL, 0xE3B448, 0.002f)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, Tilth.id("effect.low_energy"), -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, Tilth.id("effect.low_energy_attack"), -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> DIGESTIVE_DISTRESS = EFFECTS.register("digestive_distress",
            () -> new DietEffect(MobEffectCategory.HARMFUL, 0x7A9A3B, 0.003f));
    public static final DeferredHolder<MobEffect, MobEffect> SCURVY = EFFECTS.register("scurvy",
            () -> new DietEffect(MobEffectCategory.HARMFUL, 0xF5C518)
                    .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, Tilth.id("effect.scurvy"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> ANEMIA = EFFECTS.register("anemia",
            () -> new DietEffect(MobEffectCategory.HARMFUL, 0x8B3A3A)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, Tilth.id("effect.anemia"), -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final DeferredHolder<MobEffect, MobEffect> NIGHT_BLINDNESS = EFFECTS.register("night_blindness",
            () -> new DietEffect(MobEffectCategory.HARMFUL, 0xF08A24));
    public static final DeferredHolder<MobEffect, MobEffect> BRITTLE_BONES = EFFECTS.register("brittle_bones",
            () -> new DietEffect(MobEffectCategory.HARMFUL, 0xE8E4D8));

    // ------------------------------------------------------------------ creative tab
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Tilth.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("tilth", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tilth"))
            .icon(() -> new ItemStack(TilthItems.PLOW.get()))
            .displayItems((params, output) -> TilthItems.ITEMS.getEntries().forEach(holder -> output.accept(holder.get())))
            .build());

    // ------------------------------------------------------------------ data maps and datapack registries
    /** Percent daily value of each diet nutrient per item eaten (data/tilth/data_maps/item/food_nutrition.json). */
    public static final DataMapType<net.minecraft.world.item.Item, NutrientProfile> FOOD_NUTRITION = DataMapType
            .builder(Tilth.id("food_nutrition"), Registries.ITEM, NutrientProfile.CODEC)
            .synced(NutrientProfile.CODEC, false)
            .build();

    /** Diseases, pests, fungi and beneficial symbionts (data/tilth/tilth/affliction/*.json). */
    public static final ResourceKey<Registry<Affliction>> AFFLICTIONS = ResourceKey.createRegistryKey(Tilth.id("affliction"));

    private TilthMisc() {
    }

    public static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(FOOD_NUTRITION);
    }

    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(AFFLICTIONS, Affliction.CODEC, Affliction.CODEC);
    }

    @SuppressWarnings("deprecation")
    public static NutrientProfile nutrition(net.minecraft.world.item.Item item) {
        return item.builtInRegistryHolder().getData(FOOD_NUTRITION);
    }
}
