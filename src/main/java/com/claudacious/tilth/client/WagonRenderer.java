package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.entity.WagonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class WagonRenderer extends EntityRenderer<WagonEntity> {
    private static final ResourceLocation TEXTURE = Tilth.id("textures/entity/wagon.png");
    private final WagonModel model;

    public WagonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new WagonModel(context.bakeLayer(WagonModel.LAYER));
        this.shadowRadius = 1.0f;
    }

    @Override
    public void render(WagonEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0, -1.501, 0.0);
        // Distance rolled, from the entity's own walk-distance bookkeeping: radius 5/16 block, so 1 rad per 0.31 blocks.
        float rolled = (entity.walkDistO + (entity.walkDist - entity.walkDistO) * partialTick) / 0.3125f;
        model.setupAnim(entity, rolled, 0, entity.tickCount + partialTick, 0, 0);
        model.renderToBuffer(poseStack, buffers.getBuffer(model.renderType(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WagonEntity entity) {
        return TEXTURE;
    }
}
