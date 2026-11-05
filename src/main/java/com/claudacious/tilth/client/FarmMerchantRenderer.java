package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.entity.FarmMerchant;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.resources.ResourceLocation;

/** The farm merchant: the villager model in a straw hat, checked shirt and overalls (texture from tools/merchant.py). */
public class FarmMerchantRenderer extends MobRenderer<FarmMerchant, VillagerModel<FarmMerchant>> {
    private static final ResourceLocation TEXTURE = Tilth.id("textures/entity/farm_merchant.png");

    public FarmMerchantRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.WANDERING_TRADER)), 0.5f);
        addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(FarmMerchant entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(FarmMerchant entity, PoseStack pose, float partialTick) {
        pose.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
