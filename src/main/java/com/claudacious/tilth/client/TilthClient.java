package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.diet.NutrientProfile;
import com.claudacious.tilth.quality.Quality;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.registry.TilthEntities;
import com.claudacious.tilth.registry.TilthMisc;
import com.claudacious.tilth.sim.diet.DietNutrient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.BiomeColors;
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
        modBus.addListener(ClientWeather::register);
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> MaleFeaturesLayer.layers(e));
        modBus.addListener((EntityRenderersEvent.AddLayers e) -> MaleFeaturesLayer.addLayers(e));
        NeoForge.EVENT_BUS.addListener(ClientSexes::clear);
        NeoForge.EVENT_BUS.addListener(TilthClient::tooltip);
        NeoForge.EVENT_BUS.addListener(TilthClient::fog);
        NeoForge.EVENT_BUS.addListener(SeasonalColors::onClientTick);
    }

    private static void blockColors(RegisterColorHandlersEvent.Block event) {
        SeasonalColors.register(event);
    }

    private static void itemColors(RegisterColorHandlersEvent.Item event) {
        ItemLike[] grass = TilthBlocks.GRASS.values().stream().map(b -> (ItemLike) b.get()).toArray(ItemLike[]::new);
        event.register((stack, tint) -> tint == 0 ? 0xFF000000 | GrassColor.getDefaultColor() : -1, grass);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TilthEntities.PLOW.get(), PlowRenderer::new);
        event.registerEntityRenderer(TilthEntities.SPREADER.get(), SpreaderRenderer::new);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PlowModel.LAYER, PlowModel::createLayer);
        event.registerLayerDefinition(SpreaderModel.LAYER, SpreaderModel::createLayer);
    }

    private static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        int grade = Quality.get(stack);
        if (grade > 0) {
            tooltip.add(Math.min(1, tooltip.size()), Component.literal(Quality.stars(grade) + " " + Quality.name(grade))
                    .withStyle(Quality.color(grade)));
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
