package com.claudacious.tilth.compat.jei;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.soil.SoilTexture;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * JEI integration, loaded only when JEI is installed. Tilth's quality-aware crafting and cooking
 * recipes extend the vanilla recipe classes, so they already appear in JEI's standard crafting,
 * furnace, smoker and campfire tabs; this plugin adds the mechanics that are not recipes.
 */
@JeiPlugin
public class TilthJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = Tilth.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new TilthCategories.Crop(gui), new TilthCategories.Tillage(gui), new TilthCategories.Amendments(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TilthCategories.Crop.TYPE, JeiRecipes.crops());
        registration.addRecipes(TilthCategories.Tillage.TYPE, JeiRecipes.tillage());
        registration.addRecipes(TilthCategories.Amendments.TYPE, JeiRecipes.amendments());
        JeiRecipes.info(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(TilthCategories.Crop.TYPE, TilthBlocks.FARMLAND.get(SoilTexture.LOAM).get());
        registration.addRecipeCatalysts(TilthCategories.Tillage.TYPE, Items.WOODEN_HOE, TilthItems.ARD.get(), TilthItems.PLOW.get());
        registration.addRecipeCatalysts(TilthCategories.Amendments.TYPE, TilthItems.SOIL_PROBE.get());
    }
}
