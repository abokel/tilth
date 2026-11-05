package com.claudacious.tilth.market;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.TilthConfig;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds a market stall (data/tilth/structure/village/market_stall.nbt) to the house pools of every village type, so
 * new villages often have one among their houses.
 */
public final class VillageStalls {
    private static final String[] POOLS = {"plains", "desert", "savanna", "snowy", "taiga"};

    private VillageStalls() {
    }

    @SuppressWarnings("unchecked")
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        int weight = TilthConfig.VILLAGE_STALL_WEIGHT.getAsInt();
        if (weight <= 0 || !TilthConfig.MARKET.getAsBoolean()) {
            return;
        }
        Registry<StructureTemplatePool> pools = event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        try {
            Field raw = StructureTemplatePool.class.getDeclaredField("rawTemplates");
            Field flat = StructureTemplatePool.class.getDeclaredField("templates");
            raw.setAccessible(true);
            flat.setAccessible(true);
            for (String village : POOLS) {
                StructureTemplatePool pool = pools.get(ResourceLocation.withDefaultNamespace("village/" + village + "/houses"));
                if (pool == null) {
                    continue;
                }
                StructurePoolElement stall = StructurePoolElement.single(Tilth.id("village/market_stall").toString())
                        .apply(StructureTemplatePool.Projection.RIGID);
                ObjectArrayList<StructurePoolElement> elements = (ObjectArrayList<StructurePoolElement>) flat.get(pool);
                for (int i = 0; i < weight; i++) {
                    elements.add(stall);
                }
                List<Pair<StructurePoolElement, Integer>> list = new ArrayList<>((List<Pair<StructurePoolElement, Integer>>) raw.get(pool));
                list.add(Pair.of(stall, weight));
                raw.set(pool, list);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            Tilth.LOGGER.warn("Could not add market stalls to villages", e);
        }
    }
}
