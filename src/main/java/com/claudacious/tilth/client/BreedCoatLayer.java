package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.livestock.Breed;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws an animal in its breed's coat: the parent model is rendered again with the breed texture, which exactly
 * covers the vanilla one (the same trick vanilla uses for horse markings). Breeds are synced by
 * {@link com.claudacious.tilth.network.SexSync}; an animal whose breed is not known yet keeps the vanilla look.
 */
public class BreedCoatLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final Map<Breed, ResourceLocation> TEXTURES = new EnumMap<>(Breed.class);

    static {
        for (Breed b : Breed.values()) {
            if (b.hasCoat()) {
                TEXTURES.put(b, Tilth.id("textures/entity/breed/" + b.id() + ".png"));
            }
        }
    }

    public BreedCoatLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        Breed breed = ClientSexes.breed(entity.getId());
        ResourceLocation texture = breed == null ? null : TEXTURES.get(breed);
        if (texture == null) {
            return;
        }
        getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light,
                LivingEntityRenderer.getOverlayCoords(entity, 0.0f), -1);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : new EntityType<?>[]{EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN}) {
            if (event.getRenderer(type) instanceof LivingEntityRenderer renderer) {
                renderer.addLayer(new BreedCoatLayer(renderer));
            }
        }
    }
}
