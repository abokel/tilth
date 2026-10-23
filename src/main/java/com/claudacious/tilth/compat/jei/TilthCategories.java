package com.claudacious.tilth.compat.jei;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.crop.Bbch;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.soil.Nutrient;
import com.claudacious.tilth.sim.soil.SoilTexture;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** JEI categories for Tilth's world mechanics, which are not crafting recipes. */
final class TilthCategories {
    static final int WIDTH = 170;
    private static final int TEXT = 0xFF404040;
    private static final int LINE = 10;
    private static final String DEG = "°C";

    private TilthCategories() {
    }

    /** Draws each paragraph word-wrapped to the category width; returns the y below the last line. */
    static int paragraphs(GuiGraphics g, List<String> paragraphs, int x, int y) {
        Font font = Minecraft.getInstance().font;
        for (String p : paragraphs) {
            for (FormattedCharSequence line : font.split(Component.literal(p), WIDTH - x)) {
                g.drawString(font, line, x, y, TEXT, false);
                y += LINE;
            }
        }
        return y;
    }

    static void arrow(GuiGraphics g, int x) {
        g.drawString(Minecraft.getInstance().font, "->", x, 6, TEXT, false);
    }

    static String n(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    static List<ItemStack> farmland() {
        return TilthBlocks.FARMLAND.values().stream().map(b -> new ItemStack(b.get())).toList();
    }

    /** Seed in, harvest out, with the crop's agronomy. */
    static final class Crop implements IRecipeCategory<JeiRecipes.CropRecipe> {
        static final RecipeType<JeiRecipes.CropRecipe> TYPE = RecipeType.create(Tilth.MOD_ID, "crop", JeiRecipes.CropRecipe.class);
        private static final int TEXT_Y = 24;
        private final IDrawable icon;

        Crop(IGuiHelper gui) {
            icon = gui.createDrawableItemStack(new ItemStack(TilthItems.CROP_ITEMS.get("durum_wheat").get()));
        }

        @Override
        public RecipeType<JeiRecipes.CropRecipe> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.tilth.crop");
        }

        @Override
        public int getWidth() {
            return WIDTH;
        }

        @Override
        public int getHeight() {
            return 96;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, JeiRecipes.CropRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStack(recipe.seed());
            builder.addInputSlot(21, 1).setStandardSlotBackground().addItemStacks(farmland());
            int x = WIDTH - 19 * recipe.outputs().size();
            for (ItemStack out : recipe.outputs()) {
                builder.addOutputSlot(x, 1).setOutputSlotBackground().addItemStack(out);
                x += 19;
            }
        }

        static List<String> lines(CropSpecies sp) {
            return List.of(
                    sp.name + (sp.tBase <= 5 ? " (cool-season)" : " (warm-season)"),
                    "Base " + n(sp.tBase) + DEG + ", best " + n(sp.tOpt) + DEG + ", max " + n(sp.tMax) + DEG,
                    n(sp.gddToMaturity) + " degree-days to ripen",
                    "Frost kills it below " + n(sp.frostKillVegetative) + DEG,
                    "pH " + n(sp.phLow) + "-" + n(sp.phHigh) + ", roots to " + n(sp.rootDepthMax) + " m",
                    sp.isLegume() ? "Legume: fixes " + Math.round(sp.fixationShare * 100) + "% of its own N"
                            : "Takes N " + n(sp.uptake(Nutrient.N)) + ", P " + n(sp.uptake(Nutrient.P))
                            + ", K " + n(sp.uptake(Nutrient.K)) + " kg/ha");
        }

        @Override
        public void draw(JeiRecipes.CropRecipe recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
            arrow(g, 46);
            paragraphs(g, lines(recipe.species()), 1, TEXT_Y);
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, JeiRecipes.CropRecipe recipe, IRecipeSlotsView view, double mouseX, double mouseY) {
            CropSpecies sp = recipe.species();
            int line = mouseY < TEXT_Y ? -1 : (int) ((mouseY - TEXT_Y) / LINE);
            if (line == 0) {
                tooltip.add(Component.literal(sp.scientificName).withStyle(ChatFormatting.ITALIC));
                tooltip.add(Component.literal("Harvest at " + Bbch.describe(sp.bbchFor(sp.harvestStage))));
            } else if (line == 2) {
                tooltip.add(Component.literal("Each day adds its mean temperature above " + n(sp.tBase) + DEG + ","));
                tooltip.add(Component.literal("with less credit above " + n(sp.tOpt) + DEG + "."));
            }
        }
    }

    /** Soil block plus tool gives farmland of the same texture. */
    static final class Tillage implements IRecipeCategory<JeiRecipes.TillageRecipe> {
        static final RecipeType<JeiRecipes.TillageRecipe> TYPE = RecipeType.create(Tilth.MOD_ID, "tillage", JeiRecipes.TillageRecipe.class);
        private final IDrawable icon;

        Tillage(IGuiHelper gui) {
            icon = gui.createDrawableItemStack(new ItemStack(TilthItems.PLOW.get()));
        }

        @Override
        public RecipeType<JeiRecipes.TillageRecipe> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.tilth.tillage");
        }

        @Override
        public int getWidth() {
            return WIDTH;
        }

        @Override
        public int getHeight() {
            return 66;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, JeiRecipes.TillageRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStacks(recipe.soils());
            builder.addInputSlot(21, 1).setStandardSlotBackground().addItemStacks(List.of(
                    new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.IRON_HOE), new ItemStack(TilthItems.ARD.get()),
                    new ItemStack(TilthItems.PLOW.get())));
            builder.addOutputSlot(70, 1).setOutputSlotBackground().addItemStack(recipe.farmland());
        }

        @Override
        public void draw(JeiRecipes.TillageRecipe recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
            SoilTexture t = recipe.texture();
            arrow(g, 46);
            paragraphs(g, List.of("Hoe: one block. Ard: a 3-block strip. Plough: a 3-wide swath behind a horse.",
                    t.clay() >= 35 ? "Heavy clay: never work it wet." : "Irrigation reaches " + t.irrigationReach() + " blocks."), 1, 24);
        }
    }

    /** What each fertiliser or amendment does to the soil. */
    static final class Amendments implements IRecipeCategory<JeiRecipes.AmendmentRecipe> {
        static final RecipeType<JeiRecipes.AmendmentRecipe> TYPE = RecipeType.create(Tilth.MOD_ID, "amendment", JeiRecipes.AmendmentRecipe.class);
        private final IDrawable icon;

        Amendments(IGuiHelper gui) {
            icon = gui.createDrawableItemStack(new ItemStack(TilthItems.COMPOST.get()));
        }

        @Override
        public RecipeType<JeiRecipes.AmendmentRecipe> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.tilth.amendment");
        }

        @Override
        public int getWidth() {
            return WIDTH;
        }

        @Override
        public int getHeight() {
            return 86;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, JeiRecipes.AmendmentRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStack(recipe.item());
            builder.addInputSlot(21, 1).setStandardSlotBackground().addItemStacks(farmland());
        }

        @Override
        public void draw(JeiRecipes.AmendmentRecipe recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
            paragraphs(g, List.of(recipe.amendment().displayName(), "One dose per block: " + recipe.amendment().summary() + "."), 1, 24);
        }
    }
}
