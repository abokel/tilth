package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.entity.SpreaderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * A two-wheeled broadcast spreader: a wooden hopper on an axle, a shaft to the hitch and a spinning
 * iron disc under the back. Same conventions and texture layout as {@link PlowModel}.
 */
public class SpreaderModel extends EntityModel<SpreaderEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tilth.id("spreader"), "main");
    private final ModelPart root;
    private final ModelPart spinner;

    public SpreaderModel(ModelPart root) {
        this.root = root;
        this.spinner = root.getChild("spinner");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        parts.addOrReplaceChild("hopper", CubeListBuilder.create().texOffs(0, 0).addBox(-8f, 6f, -6f, 16, 8, 14), PartPose.ZERO);
        parts.addOrReplaceChild("shaft", CubeListBuilder.create().texOffs(0, 22).addBox(-1f, 15f, -22f, 2, 2, 16), PartPose.ZERO);
        parts.addOrReplaceChild("axle", CubeListBuilder.create().texOffs(36, 22).addBox(-10f, 18f, 0f, 20, 2, 2), PartPose.ZERO);
        parts.addOrReplaceChild("wheel_left", CubeListBuilder.create().texOffs(0, 40).addBox(-11f, 14f, -4f, 2, 10, 10), PartPose.ZERO);
        parts.addOrReplaceChild("wheel_right", CubeListBuilder.create().texOffs(0, 40).mirror().addBox(9f, 14f, -4f, 2, 10, 10), PartPose.ZERO);
        parts.addOrReplaceChild("chute", CubeListBuilder.create().texOffs(24, 40).addBox(-2f, 14f, 7f, 4, 4, 3), PartPose.ZERO);
        parts.addOrReplaceChild("spinner", CubeListBuilder.create().texOffs(30, 50).addBox(-3f, -0.5f, -3f, 6, 1, 6),
                PartPose.offset(0f, 19f, 11f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(SpreaderEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        spinner.yRot = entity.isLowered() && !entity.load().isEmpty() ? ageInTicks * 0.8f : 0f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
