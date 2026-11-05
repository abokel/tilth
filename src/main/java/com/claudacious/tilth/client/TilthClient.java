package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.block.CropBlockEntity;
import com.claudacious.tilth.genetics.SeedGenetics;
import com.claudacious.tilth.diet.NutrientProfile;
import com.claudacious.tilth.quality.Quality;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.registry.TilthEntities;
import com.claudacious.tilth.registry.TilthMisc;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.diet.DietNutrient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Mod(value = Tilth.MOD_ID, dist = Dist.CLIENT)
public final class TilthClient {
    public TilthClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(TilthClient::blockColors);
        modBus.addListener(TilthClient::itemColors);
        modBus.addListener(TilthClient::renderers);
        modBus.addListener(TilthClient::layers);
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> MaleFeaturesLayer.layers(e));
        // Coats first, so a bull's horns and ring are added on top of his breed's coat.
        modBus.addListener((EntityRenderersEvent.AddLayers e) -> BreedCoatLayer.addLayers(e));
        modBus.addListener((EntityRenderersEvent.AddLayers e) -> MaleFeaturesLayer.addLayers(e));
        NeoForge.EVENT_BUS.addListener(ClientSexes::clear);
        NeoForge.EVENT_BUS.addListener(TilthClient::tooltip);
        NeoForge.EVENT_BUS.addListener(TilthClient::fog);
    }

    private static void blockColors(RegisterColorHandlersEvent.Block event) {
        net.minecraft.world.level.block.Block[] crops = TilthBlocks.CROPS.values().stream().map(b -> b.get())
                .toArray(net.minecraft.world.level.block.Block[]::new);
        event.register((state, level, pos, tint) -> {
            if (tint != 0 || level == null || pos == null) {
                return -1;
            }
            int base = switch (state.getValue(com.claudacious.tilth.block.TilthCropBlock.CONDITION)) {
                case HEALTHY -> 0x78C850;
                case CHLOROTIC -> 0xC9C65D;
                case PURPLED -> 0x8D67B5;
                case WILTED -> 0xA69366;
                case FROSTED -> 0xC9E2F0;
            };
            if (level.getBlockEntity(pos) instanceof CropBlockEntity crop) {
                int variant = crop.genome().phenotype();
                int delta = (variant % 6 - 2) * 6;
                int r = Math.max(40, Math.min(255, ((base >> 16) & 0xFF) + delta));
                int g = Math.max(40, Math.min(255, ((base >> 8) & 0xFF) + (variant / 6 == 0 ? 8 : -8)));
                int b = Math.max(40, Math.min(255, (base & 0xFF) - delta / 2));
                return (r << 16) | (g << 8) | b;
            }
            return base;
        }, crops);
    }

    private static void itemColors(RegisterColorHandlersEvent.Item event) {
        ItemLike[] grass = TilthBlocks.GRASS.values().stream().map(b -> (ItemLike) b.get()).toArray(ItemLike[]::new);
        event.register((stack, tint) -> tint == 0 ? 0xFF000000 | GrassColor.getDefaultColor() : -1, grass);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TilthEntities.PLOW.get(), PlowRenderer::new);
        event.registerEntityRenderer(TilthEntities.SPREADER.get(), SpreaderRenderer::new);
        event.registerEntityRenderer(TilthEntities.WAGON.get(), WagonRenderer::new);
        event.registerEntityRenderer(TilthEntities.HOMUNCULUS.get(), SlimeRenderer::new);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PlowModel.LAYER, PlowModel::createLayer);
        event.registerLayerDefinition(SpreaderModel.LAYER, SpreaderModel::createLayer);
        event.registerLayerDefinition(WagonModel.LAYER, WagonModel::createLayer);
    }

    private static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        int grade = Quality.get(stack);
        if (grade > 0) {
            tooltip.add(Math.min(1, tooltip.size()), Component.literal(Quality.stars(grade) + " " + Quality.name(grade))
                    .withStyle(Quality.color(grade)));
        }
        Float moisture = stack.get(com.claudacious.tilth.registry.TilthDataComponents.GRAIN_MOISTURE.get());
        if (moisture != null) {
            boolean safe = moisture <= com.claudacious.tilth.TilthConfig.GRAIN_SAFE_STORAGE_MOISTURE.getAsDouble() + 1e-6;
            tooltip.add(Component.literal(String.format("Moisture %.1f%% (%s)", moisture * 100,
                    safe ? "dry enough to store" : "too wet to store: dry it first")).withStyle(safe ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        }
        CropSpecies species = SeedGenetics.speciesOf(stack);
        if (species != null) {
            var genome = SeedGenetics.genomeOrLandrace(stack, species);
            tooltip.add(Component.literal("Cultivar: " + genome.cultivar()).withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(Component.literal(String.format("Traits G%+.0f%% Y%+.0f%% D%+.0f F%+.1f°C",
                    (genome.growthMultiplier() - 1.0) * 100.0,
                    (genome.yieldMultiplier() - 1.0) * 100.0,
                    genome.droughtBonusDays(),
                    genome.frostToleranceC())).withStyle(ChatFormatting.GRAY));
        }
        NutrientProfile profile = TilthMisc.nutrition(stack.getItem());
        if (profile == null || profile.values().isEmpty()) {
            return;
        }
        List<Map.Entry<DietNutrient, Float>> entries = new ArrayList<>(profile.values().entrySet());
        entries.removeIf(e -> e.getValue() <= 0);
        entries.sort(Comparator.comparing((Map.Entry<DietNutrient, Float> e) -> e.getValue()).reversed());
        double mult = Quality.nutritionMultiplier(grade);
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.literal("Nutrition per serving (% daily value):").withStyle(ChatFormatting.GRAY));
            for (Map.Entry<DietNutrient, Float> e : entries) {
                tooltip.add(Component.literal(String.format("  %s %.0f%%", e.getKey().displayName(), e.getValue() * mult))
                        .withStyle(s -> s.withColor(e.getKey().color())));
            }
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, entries.size()); i++) {
                Map.Entry<DietNutrient, Float> e = entries.get(i);
                sb.append(i == 0 ? "" : ", ").append(e.getKey().displayName()).append(String.format(" %.0f%%", e.getValue() * mult));
            }
            tooltip.add(Component.literal(sb + " (Shift for more)").withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    /** Night blindness (vitamin A deficiency): in the dark, vision closes in. */
    private static void fog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.hasEffect(TilthMisc.NIGHT_BLINDNESS)) {
            return;
        }
        int light = mc.level.getMaxLocalRawBrightness(BlockPos.containing(mc.player.getEyePosition()));
        if (light >= 7) {
            return;
        }
        event.setNearPlaneDistance(0.0f);
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), 6.0f + light * 3.0f));
        event.setCanceled(true);
    }
}
