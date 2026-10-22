package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.entity.PlowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class PlowRenderer extends EntityRenderer<PlowEntity> {
    private static final ResourceLocation TEXTURE = Tilth.id("textures/entity/plow.png");
    private final PlowModel model;

    public PlowRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new PlowModel(context.bakeLayer(PlowModel.LAYER));
        this.shadowRadius = 0.7f;
    }

    @Override
    public void render(PlowEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0, entity.isLowered() ? -1.38 : -1.501, 0.0);
        model.renderToBuffer(poseStack, buffers.getBuffer(model.renderType(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PlowEntity entity) {
        return TEXTURE;
    }
}
