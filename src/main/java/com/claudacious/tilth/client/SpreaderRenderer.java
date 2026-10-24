package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.entity.SpreaderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class SpreaderRenderer extends EntityRenderer<SpreaderEntity> {
    private static final ResourceLocation TEXTURE = Tilth.id("textures/entity/spreader.png");
    private final SpreaderModel model;

    public SpreaderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SpreaderModel(context.bakeLayer(SpreaderModel.LAYER));
        this.shadowRadius = 0.7f;
    }

    @Override
    public void render(SpreaderEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0, -1.501, 0.0);
        model.setupAnim(entity, 0, 0, entity.tickCount + partialTick, 0, 0);
        model.renderToBuffer(poseStack, buffers.getBuffer(model.renderType(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SpreaderEntity entity) {
        return TEXTURE;
    }
}
