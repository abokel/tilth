package com.claudacious.tilth.loot;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.sim.climate.BiomeClimate;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.Crops;
import com.claudacious.tilth.world.ClimateService;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;

/** Wild grass sometimes yields seed of a crop that could actually grow in the local climate. */
public class GrassSeedModifier extends LootModifier {
    public static final MapCodec<GrassSeedModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
            .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
            .apply(inst, GrassSeedModifier::new));

    private final float chance;

    public GrassSeedModifier(LootItemCondition[] conditions, float chance) {
        super(conditions);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!TilthConfig.GRASS_DROPS_SEEDS.getAsBoolean() || context.getRandom().nextFloat() >= chance) {
            return loot;
        }
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) {
            return loot;
        }
        BiomeClimate climate = ClimateService.climateAt(context.getLevel(), BlockPos.containing(origin));
        double summer = climate.meanTemp() + climate.amplitude();
        List<CropSpecies> suited = Crops.all().stream()
                .filter(sp -> summer >= sp.tBase + 8 && climate.meanTemp() - climate.amplitude() > sp.frostKillVegetative - 25)
                .filter(sp -> !sp.seedItem.startsWith("minecraft:"))
                .toList();
        if (suited.isEmpty()) {
            return loot;
        }
        CropSpecies pick = suited.get(context.getRandom().nextInt(suited.size()));
        loot.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(pick.seedItem))));
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
