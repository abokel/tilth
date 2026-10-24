package com.claudacious.tilth.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Snowfall that follows the Tilth climate. Vanilla decides rain or snow from each biome's fixed temperature,
 * so a plains winter at -5 °C would still show rain. When the server says it is snowing here, the overworld
 * draws vanilla's snow in place of rain (and drops the rain sounds and splashes).
 */
public final class ClientWeather {
    private static final ResourceLocation SNOW = ResourceLocation.withDefaultNamespace("textures/environment/snow.png");
    private static final float[] SIZE_X = new float[1024];
    private static final float[] SIZE_Z = new float[1024];
    private static volatile boolean snowing;

    static {
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                float f = j - 16;
                float f1 = i - 16;
                float f2 = Mth.sqrt(f * f + f1 * f1);
                SIZE_X[i << 5 | j] = -f1 / f2;
                SIZE_Z[i << 5 | j] = f / f2;
            }
        }
    }

    private ClientWeather() {
    }

    public static void setSnowing(boolean value) {
        snowing = value;
    }

    public static void register(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.withDefaultNamespace("overworld"), new Overworld());
    }

    /** The vanilla overworld, except that precipitation is snow when the Tilth climate says so. */
    static final class Overworld extends DimensionSpecialEffects.OverworldEffects {
        @Override
        public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
            return snowing;
        }

        @Override
        public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
            if (!snowing) {
                return false;
            }
            float rain = level.getRainLevel(partialTick);
            if (rain <= 0.0f) {
                return true;
            }
            renderSnow(level, ticks, partialTick, lightTexture, camX, camY, camZ, rain);
            return true;
        }
    }

    /** Vanilla's snow pass from {@code LevelRenderer.renderSnowAndRain}, drawn for every column that has weather. */
    private static void renderSnow(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ,
                                   float rain) {
        lightTexture.turnOnLightLayer();
        int cx = Mth.floor(camX);
        int cy = Mth.floor(camY);
        int cz = Mth.floor(camZ);
        int radius = Minecraft.useFancyGraphics() ? 10 : 5;
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderTexture(0, SNOW);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        boolean any = false;
        float time = ticks + partialTick;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = cz - radius; z <= cz + radius; z++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                int idx = (z - cz + 16) * 32 + x - cx + 16;
                double dx = SIZE_X[idx] * 0.5;
                double dz = SIZE_Z[idx] * 0.5;
                pos.set(x, camY, z);
                if (!level.getBiome(pos).value().hasPrecipitation()) {
                    continue;
                }
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                int bottom = Math.max(cy - radius, ground);
                int top = Math.max(cy + radius, ground);
                if (bottom == top) {
                    continue;
                }
                int lightY = Math.max(ground, cy);
                RandomSource random = RandomSource.create((long) (x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761));
                float scroll = -((ticks & 511) + partialTick) / 512.0f;
                float u = (float) (random.nextDouble() + time * 0.01 * (float) random.nextGaussian());
                float v = (float) (random.nextDouble() + (time * (float) random.nextGaussian()) * 0.001);
                double ox = x + 0.5 - camX;
                double oz = z + 0.5 - camZ;
                float dist = (float) Math.sqrt(ox * ox + oz * oz) / radius;
                float alpha = ((1.0f - dist * dist) * 0.3f + 0.5f) * rain;
                pos.set(x, lightY, z);
                int light = LevelRenderer.getLightColor(level, pos);
                int sky = (((light >> 16) & 65535) * 3 + 240) / 4;
                int block = ((light & 65535) * 3 + 240) / 4;
                buffer.addVertex((float) (x - camX - dx + 0.5), (float) (top - camY), (float) (z - camZ - dz + 0.5))
                        .setUv(u, bottom * 0.25f + scroll + v).setColor(1.0f, 1.0f, 1.0f, alpha).setUv2(block, sky);
                buffer.addVertex((float) (x - camX + dx + 0.5), (float) (top - camY), (float) (z - camZ + dz + 0.5))
                        .setUv(1.0f + u, bottom * 0.25f + scroll + v).setColor(1.0f, 1.0f, 1.0f, alpha).setUv2(block, sky);
                buffer.addVertex((float) (x - camX + dx + 0.5), (float) (bottom - camY), (float) (z - camZ + dz + 0.5))
                        .setUv(1.0f + u, top * 0.25f + scroll + v).setColor(1.0f, 1.0f, 1.0f, alpha).setUv2(block, sky);
                buffer.addVertex((float) (x - camX - dx + 0.5), (float) (bottom - camY), (float) (z - camZ - dz + 0.5))
                        .setUv(u, top * 0.25f + scroll + v).setColor(1.0f, 1.0f, 1.0f, alpha).setUv2(block, sky);
                any = true;
            }
        }
        if (any) {
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
    }
}
