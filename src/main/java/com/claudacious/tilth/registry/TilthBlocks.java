package com.claudacious.tilth.registry;

import net.minecraft.world.level.block.HayBlock;
import com.claudacious.tilth.block.ManureLayerBlock;
import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.block.DeadCropBlock;
import com.claudacious.tilth.block.PuddleBlock;
import com.claudacious.tilth.block.SoilBlock;
import com.claudacious.tilth.block.SoilGrassBlock;
import com.claudacious.tilth.block.SoilLabBlock;
import com.claudacious.tilth.block.TilthCropBlock;
import com.claudacious.tilth.block.TilthFarmlandBlock;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.Crops;
import com.claudacious.tilth.sim.soil.SoilHorizon;
import com.claudacious.tilth.sim.soil.SoilTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TilthBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Tilth.MOD_ID);

    public static final Map<SoilTexture, DeferredBlock<SoilBlock>> TOPSOIL = new EnumMap<>(SoilTexture.class);
    public static final Map<SoilTexture, DeferredBlock<SoilBlock>> SUBSOIL = new EnumMap<>(SoilTexture.class);
    public static final Map<SoilTexture, DeferredBlock<SoilGrassBlock>> GRASS = new EnumMap<>(SoilTexture.class);
    public static final Map<SoilTexture, DeferredBlock<TilthFarmlandBlock>> FARMLAND = new EnumMap<>(SoilTexture.class);
    public static final Map<String, DeferredBlock<TilthCropBlock>> CROPS = new LinkedHashMap<>();
    public static final DeferredBlock<SoilBlock> GLEY;
    public static final DeferredBlock<DeadCropBlock> DEAD_CROP;
    public static final DeferredBlock<PuddleBlock> PUDDLE;
    public static final DeferredBlock<HayBlock> ALFALFA_BALE = BLOCKS.register("alfalfa_bale",
            () -> new HayBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).mapColor(MapColor.COLOR_LIGHT_GREEN)));
    public static final DeferredBlock<ManureLayerBlock> MANURE_LAYER = BLOCKS.register("manure_layer",
            () -> new ManureLayerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).replaceable().strength(0.2f)
                    .sound(SoundType.MUD).randomTicks().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<SoilLabBlock> SOIL_LAB = BLOCKS.register("soil_lab",
            () -> new SoilLabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).noOcclusion()));

    static {
        for (SoilTexture t : SoilTexture.values()) {
            String topName = t.isOrganic() ? "peat" : t.id() + "_topsoil";
            TOPSOIL.put(t, BLOCKS.register(topName, () -> new SoilBlock(t, SoilHorizon.TOPSOIL, soilProperties(t, false))));
            if (!t.isOrganic()) {
                SUBSOIL.put(t, BLOCKS.register(t.id() + "_subsoil", () -> new SoilBlock(t, SoilHorizon.SUBSOIL, soilProperties(t, true))));
            }
            GRASS.put(t, BLOCKS.register(t.id() + "_grass", () -> new SoilGrassBlock(t, BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK))));
            FARMLAND.put(t, BLOCKS.register(t.id() + "_farmland", () -> new TilthFarmlandBlock(t,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND).sound(sound(t)))));
        }
        GLEY = BLOCKS.register("gley", () -> new SoilBlock(SoilTexture.SILTY_CLAY, SoilHorizon.GLEY,
                soilProperties(SoilTexture.SILTY_CLAY, true).mapColor(MapColor.CLAY)));
        for (CropSpecies sp : Crops.all()) {
            CROPS.put(sp.id, BLOCKS.register(sp.id + "_crop", () -> new TilthCropBlock(sp, BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT))));
        }
        DEAD_CROP = BLOCKS.register("dead_crop", () -> new DeadCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).mapColor(MapColor.WOOD)));
        PUDDLE = BLOCKS.register("puddle", () -> new PuddleBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WATER).noCollission().instabreak().noOcclusion().replaceable().randomTicks()
                .sound(SoundType.MUD).pushReaction(PushReaction.DESTROY).noLootTable()));
    }

    private TilthBlocks() {
    }

    private static SoundType sound(SoilTexture t) {
        if (t.isOrganic() || t.clay() >= 40) {
            return SoundType.MUD;
        }
        if (t.sand() >= 80) {
            return SoundType.SAND;
        }
        return SoundType.GRAVEL;
    }

    private static BlockBehaviour.Properties soilProperties(SoilTexture t, boolean subsoil) {
        MapColor color;
        if (t.isOrganic()) {
            color = MapColor.COLOR_BLACK;
        } else if (t.sand() >= 80) {
            color = MapColor.SAND;
        } else if (t.clay() >= 40) {
            color = subsoil ? MapColor.TERRACOTTA_ORANGE : MapColor.TERRACOTTA_BROWN;
        } else {
            color = subsoil ? MapColor.TERRACOTTA_YELLOW : MapColor.DIRT;
        }
        float hardness = 0.45f + t.clay() / 100.0f * 0.5f + (subsoil ? 0.1f : 0f);
        return BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(color).strength(hardness).sound(sound(t)).randomTicks();
    }

    public static BlockState topsoil(SoilTexture t) {
        return TOPSOIL.get(t).get().defaultBlockState();
    }

    public static BlockState subsoil(SoilTexture t) {
        DeferredBlock<SoilBlock> b = SUBSOIL.get(t);
        return b != null ? b.get().defaultBlockState() : topsoil(t);
    }

    public static BlockState grass(SoilTexture t) {
        return GRASS.get(t).get().defaultBlockState();
    }

    public static BlockState farmland(SoilTexture t) {
        return FARMLAND.get(t).get().defaultBlockState();
    }

    public static DeferredBlock<TilthCropBlock> crop(CropSpecies species) {
        return CROPS.get(species.id);
    }
}
