package com.claudacious.tilth.compat.jei;

import com.claudacious.tilth.item.AmendmentItem;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.Crops;
import com.claudacious.tilth.sim.crop.GrowthForm;
import com.claudacious.tilth.sim.soil.Amendment;
import com.claudacious.tilth.sim.soil.Hydraulics;
import com.claudacious.tilth.sim.soil.SoilTexture;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Builds the JEI recipe lists and information pages from Tilth's registries. */
final class JeiRecipes {
    private JeiRecipes() {
    }

    record CropRecipe(CropSpecies species, ItemStack seed, List<ItemStack> outputs) {
    }

    record TillageRecipe(SoilTexture texture, List<ItemStack> soils, ItemStack farmland) {
    }

    record AmendmentRecipe(ItemStack item, Amendment amendment) {
    }

    static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    static List<CropRecipe> crops() {
        List<CropRecipe> out = new ArrayList<>();
        for (CropSpecies sp : Crops.all()) {
            List<ItemStack> outputs = new ArrayList<>();
            outputs.add(new ItemStack(item(sp.produceItem), Math.max(1, sp.baseProduce)));
            if (!sp.seedIsProduce()) {
                outputs.add(new ItemStack(item(sp.seedItem), Math.max(1, sp.baseSeeds)));
            }
            if (sp.form == GrowthForm.CEREAL || sp.form == GrowthForm.TALL_GRASS) {
                outputs.add(new ItemStack(TilthItems.STRAW.get()));
            }
            out.add(new CropRecipe(sp, new ItemStack(item(sp.seedItem)), outputs));
        }
        return out;
    }

    static List<TillageRecipe> tillage() {
        List<TillageRecipe> out = new ArrayList<>();
        for (SoilTexture t : SoilTexture.values()) {
            List<ItemStack> soils = new ArrayList<>();
            soils.add(new ItemStack(TilthBlocks.GRASS.get(t).get()));
            soils.add(new ItemStack(TilthBlocks.TOPSOIL.get(t).get()));
            if (TilthBlocks.SUBSOIL.containsKey(t)) {
                soils.add(new ItemStack(TilthBlocks.SUBSOIL.get(t).get()));
            }
            out.add(new TillageRecipe(t, soils, new ItemStack(TilthBlocks.FARMLAND.get(t).get())));
        }
        return out;
    }

    static List<AmendmentRecipe> amendments() {
        List<AmendmentRecipe> out = new ArrayList<>();
        for (var holder : TilthItems.ITEMS.getEntries()) {
            if (holder.get() instanceof AmendmentItem a) {
                out.add(new AmendmentRecipe(new ItemStack(a), a.amendment()));
            }
        }
        out.add(new AmendmentRecipe(new ItemStack(Items.BONE_MEAL), Amendment.BONE_MEAL));
        out.add(new AmendmentRecipe(new ItemStack(TilthItems.SLURRY_BUCKET.get()), Amendment.SLURRY));
        return out;
    }

    static void info(IRecipeRegistration r) {
        for (SoilTexture t : SoilTexture.values()) {
            Hydraulics h = t.hydraulics(t.defaultOrganicMatter());
            Component c = Component.literal(String.format(
                    "%s: %d%% sand, %d%% silt, %d%% clay. Holds water between %.2f (field capacity) and %.2f (wilting point); "
                            + "drains at %.0f mm/h. Water seeps %d blocks sideways through it. "
                            + "Topsoil is the humus-rich A horizon; subsoil the clay-enriched B horizon. Till it for farmland.",
                    t.displayName(), t.sand(), t.silt(), t.clay(), h.fieldCapacity(), h.wiltingPoint(), h.ksat(), t.irrigationReach()));
            List<ItemStack> stacks = new ArrayList<>();
            stacks.add(new ItemStack(TilthBlocks.TOPSOIL.get(t).get()));
            stacks.add(new ItemStack(TilthBlocks.GRASS.get(t).get()));
            if (TilthBlocks.SUBSOIL.containsKey(t)) {
                stacks.add(new ItemStack(TilthBlocks.SUBSOIL.get(t).get()));
            }
            r.addItemStackInfo(stacks, c);
        }
        r.addItemStackInfo(new ItemStack(TilthBlocks.GLEY.get()), Component.literal(
                "Gley: subsoil that stays waterlogged turns grey-blue with rusty mottles as its iron is chemically reduced. "
                        + "It marks poorly drained land and turns back into subsoil if the ground drains."));
        r.addItemStackInfo(new ItemStack(TilthItems.SOIL_PROBE.get()), Component.literal(
                "Right-click soil, farmland or a crop for a soil test: texture, pH, organic matter, N-P-K, calcium, "
                        + "magnesium, sulfur, moisture against field capacity, compaction and the crop history of the field."));
        r.addItemStackInfo(new ItemStack(TilthItems.ARD.get()), Component.literal(
                "The scratch plough: right-click soil to till a three-block strip in front of you. Shallow: it buries little "
                        + "residue and only partly loosens compaction."));
        r.addItemStackInfo(new ItemStack(TilthItems.PLOW.get()), Component.literal(
                "Place it, then right-click it while riding a horse, donkey or mule to hitch it. Right-click it again from "
                        + "the saddle to lower the shares: it turns a three-block swath as it is pulled. Avoid wet clay."));
        r.addItemStackInfo(new ItemStack(TilthItems.SURVEYORS_CHAIN.get()), Component.literal(
                "Right-click the corner blocks of a field in order, then sneak and use it in the air to record a plot. "
                        + "Areas are measured in acres; boundaries show in particles while you hold the chain."));
        r.addItemStackInfo(List.of(new ItemStack(TilthItems.QUERN.get()), new ItemStack(TilthItems.KITCHEN_KNIFE.get())),
                Component.literal("Kitchen tools stay in the crafting grid and wear out slowly. The quern mills grain, oilseeds "
                        + "and limestone; the knife cuts pasta, fries and flax."));
        r.addItemStackInfo(new ItemStack(TilthItems.MANURE.get()), Component.literal(
                "Farm animals leave dung in layers on the ground. Shovel it up, then mix four with a water bucket into slurry to "
                        + "spread, or compost it with straw."));
        r.addItemStackInfo(new ItemStack(TilthItems.STRAW.get()), Component.literal(
                "Straw from cereal harvests. Spread it as mulch to keep soil moist (it ties up a little nitrogen while it "
                        + "decays), compost it, or bale it into hay."));
    }
}
