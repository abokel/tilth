package com.claudacious.tilth.client;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.registry.TilthBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Seasonal foliage and grass, following the Tilth calendar: deciduous leaves turn yellow, orange and red
 * through autumn and hang brown through winter, spring growth is a fresh light green, and grass yellows
 * in autumn and browns in winter. Evergreen and tropical leaves keep their colour.
 */
public final class SeasonalColors {
    private static final int[] OAK_AUTUMN = {0xD2A23C, 0xD86A28, 0xB03A26, 0xC88A30};
    private static int lastKey = -1;

    private SeasonalColors() {
    }

    private static double ramp(int doy, int from, int to) {
        return Mth.clamp((doy - from) / (double) (to - from), 0.0, 1.0);
    }

    static double autumn(int doy) {
        return ramp(doy, 250, 292) * (1 - ramp(doy, 322, 346));
    }

    static double winter(int doy) {
        return Math.max(ramp(doy, 322, 346), 1 - ramp(doy, 62, 92));
    }

    static double spring(int doy) {
        return ramp(doy, 80, 102) * (1 - ramp(doy, 135, 165));
    }

    private static int mix(int a, int b, double t) {
        t = Mth.clamp(t, 0, 1);
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }

    private static int doy() {
        return TilthConfig.SEASONAL_COLORS.getAsBoolean() ? ClientCalendar.dayOfYear() : -1;
    }

    static int leaves(int base, @Nullable BlockPos pos, boolean birch, boolean dark) {
        int doy = doy();
        if (doy < 0 || pos == null) {
            return base;
        }
        long h = Mth.getSeed(pos.getX(), 0, pos.getZ()) >>> 7;
        int autumnColor = birch ? 0xE6C83C : dark ? 0x9A6230 : OAK_AUTUMN[(int) (h % OAK_AUTUMN.length)];
        int lag = (int) (h % 13) * 2;
        int c = mix(base, autumnColor, autumn(doy - lag) * 0.9);
        c = mix(c, birch ? 0x9C8A5C : 0x7C5C3C, winter(doy) * 0.8);
        return mix(c, 0x86CC5A, spring(doy) * 0.3);
    }

    static int grass(int base) {
        int doy = doy();
        if (doy < 0) {
            return base;
        }
        int c = mix(base, 0xBAAA60, autumn(doy) * 0.4);
        c = mix(c, 0x9C9070, winter(doy) * 0.5);
        return mix(c, 0x7ED04E, spring(doy) * 0.2);
    }

    private static int grassAt(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        if (level == null || pos == null) {
            return GrassColor.getDefaultColor();
        }
        BlockPos at = state.hasProperty(DoublePlantBlock.HALF) && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        return grass(BiomeColors.getAverageGrassColor(level, at));
    }

    public static void register(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> leaves(level != null && pos != null ? BiomeColors.getAverageFoliageColor(level, pos)
                : FoliageColor.getDefaultColor(), pos, false, false), Blocks.OAK_LEAVES, Blocks.VINE);
        event.register((state, level, pos, tint) -> leaves(level != null && pos != null ? BiomeColors.getAverageFoliageColor(level, pos)
                : FoliageColor.getDefaultColor(), pos, false, true), Blocks.DARK_OAK_LEAVES);
        event.register((state, level, pos, tint) -> leaves(FoliageColor.getBirchColor(), pos, true, false), Blocks.BIRCH_LEAVES);
        Block[] grass = TilthBlocks.GRASS.values().stream().map(b -> (Block) b.get()).toArray(Block[]::new);
        event.register((state, level, pos, tint) -> grassAt(state, level, pos), grass);
        event.register((state, level, pos, tint) -> grassAt(state, level, pos), Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.TALL_GRASS,
                Blocks.FERN, Blocks.LARGE_FERN);
    }

    /** Re-renders the world when the colours have moved on a step. */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.getGameTime() % 100 != 0) {
            return;
        }
        int doy = doy();
        if (doy < 0) {
            return;
        }
        int key = (int) Math.round(autumn(doy) * 8) * 100 + (int) Math.round(winter(doy) * 8) * 10 + (int) Math.round(spring(doy) * 5);
        if (lastKey >= 0 && key != lastKey) {
            mc.levelRenderer.allChanged();
        }
        lastKey = key;
    }
}
