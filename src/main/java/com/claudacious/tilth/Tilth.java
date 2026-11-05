package com.claudacious.tilth;

import com.claudacious.tilth.network.SexSync;
import com.claudacious.tilth.livestock.Breeding;
import com.claudacious.tilth.command.DebugHud;
import com.claudacious.tilth.command.TilthCommands;
import com.claudacious.tilth.compat.FieldGuide;
import com.claudacious.tilth.diet.DietEvents;
import com.claudacious.tilth.event.FarmingEvents;
import com.claudacious.tilth.event.StarterKit;
import com.claudacious.tilth.livestock.Livestock;
import com.claudacious.tilth.plot.PlotEvents;
import com.claudacious.tilth.registry.TilthAttachments;
import com.claudacious.tilth.registry.TilthBlockEntities;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.registry.TilthDataComponents;
import com.claudacious.tilth.registry.TilthEntities;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.registry.TilthMisc;
import com.claudacious.tilth.water.SurfaceWater;
import com.claudacious.tilth.water.WaterSourceRules;
import com.claudacious.tilth.world.ClimateService;
import com.claudacious.tilth.world.MacroTerrain;
import com.claudacious.tilth.world.TilthCalendar;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

/**
 * Tilth: realistic agronomy. Catena-based soils, soil chemistry and hydrology, finite water, crops
 * driven by thermal time with BBCH growth stages, player diet nutrition, harvest quality, ards and
 * animal-drawn ploughs, and acre-based polygon field plots.
 */
@Mod(Tilth.MOD_ID)
public final class Tilth {
    public static final String MOD_ID = "tilth";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Tilth(IEventBus modBus, ModContainer container) {
        TilthBlocks.BLOCKS.register(modBus);
        TilthItems.ITEMS.register(modBus);
        TilthBlockEntities.BLOCK_ENTITIES.register(modBus);
        TilthEntities.ENTITIES.register(modBus);
        TilthDataComponents.COMPONENTS.register(modBus);
        TilthAttachments.ATTACHMENTS.register(modBus);
        TilthMisc.FEATURES.register(modBus);
        TilthMisc.RECIPE_SERIALIZERS.register(modBus);
        TilthMisc.LOOT_MODIFIERS.register(modBus);
        TilthMisc.EFFECTS.register(modBus);
        TilthMisc.TABS.register(modBus);
        modBus.addListener(TilthMisc::registerDataMaps);
        modBus.addListener(TilthMisc::registerDatapackRegistries);
        modBus.addListener(SexSync::register);
        modBus.addListener(ModConfigEvent.Loading.class, TilthConfig::onLoad);
        modBus.addListener(ModConfigEvent.Reloading.class, TilthConfig::onLoad);
        modBus.addListener(TilthEntities::registerAttributes);
        modBus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e) -> e.registerBlock(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
                    com.claudacious.tilth.block.GrainSiloBlockEntity silo = com.claudacious.tilth.block.GrainSiloBlock.storage(level, pos);
                    return silo == null ? null : silo.itemHandler();
                }, TilthBlocks.GRAIN_SILO.get()));
        container.registerConfig(ModConfig.Type.COMMON, TilthConfig.SPEC);

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(TilthCommands::register);
        game.addListener(TilthCalendar::onServerTick);
        game.addListener(com.claudacious.tilth.market.Market::onServerTick);
        game.addListener(com.claudacious.tilth.entity.FarmMerchant::onServerTick);
        game.addListener(com.claudacious.tilth.market.VillageStalls::onServerAboutToStart);
        game.addListener(Tilth::onServerStopped);
        game.addListener(WaterSourceRules::onCreateSource);
        game.addListener(SurfaceWater::onLevelTick);
        game.addListener(FarmingEvents::onToolModify);
        game.addListener(EventPriority.HIGH, PlotEvents::onRightClick);
        game.addListener(EventPriority.HIGH, com.claudacious.tilth.event.FieldWork::onRightClickBlock);
        game.addListener(FarmingEvents::onRightClickBlock);
        game.addListener(FarmingEvents::onEntityTick);
        game.addListener(PlotEvents::onPlayerTick);
        game.addListener(PlotEvents::onBreak);
        game.addListener(PlotEvents::onPlace);
        game.addListener(DietEvents::onFinishUsing);
        game.addListener(DietEvents::onPlayerTick);
        game.addListener(DietEvents::onHeal);
        game.addListener(DietEvents::onIncomingDamage);
        game.addListener(DebugHud::onPlayerTick);
        game.addListener(FieldGuide::onPlayerLoggedIn);
        game.addListener(StarterKit::onPlayerLoggedIn);
        game.addListener(com.claudacious.tilth.postharvest.GrainKeeping::onContainerOpen);
        game.addListener(com.claudacious.tilth.postharvest.GrainKeeping::onPlayerTick);
        game.addListener(Livestock::onEntityTick);
        game.addListener(EventPriority.HIGH, com.claudacious.tilth.livestock.WoolAndDairy::onInteract);
        game.addListener(Livestock::onInteract);
        game.addListener(com.claudacious.tilth.event.FieldWork::onBreak);
        game.addListener(Livestock::onDrops);
        game.addListener(Breeding::onJoin);
        game.addListener(Breeding::onStartTracking);
        game.addListener(Breeding::onBabySpawn);
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        TilthCalendar.onServerStopped(event);
        MacroTerrain.clear();
        ClimateService.clearCache();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
