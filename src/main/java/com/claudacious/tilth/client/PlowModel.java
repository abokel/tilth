package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.entity.PlowEntity;
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
 * A simple three-bottom moldboard plough: wooden beam, cross frame and handles, three iron bottoms
 * (share + mouldboard) and a front gauge wheel. Model space is 1/16 block, y = 24 at the ground,
 * -z toward the hitch. The texture's top half is wood, the bottom half iron.
 */
public class PlowModel extends EntityModel<PlowEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tilth.id("plow"), "main");
    private final ModelPart root;

    public PlowModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        parts.addOrReplaceChild("beam", CubeListBuilder.create().texOffs(0, 0).addBox(-1f, 12f, -22f, 2, 2, 28), PartPose.ZERO);
        parts.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 30).addBox(-11f, 14f, -1f, 22, 2, 2), PartPose.ZERO);
        parts.addOrReplaceChild("handle_left", CubeListBuilder.create().texOffs(48, 0).addBox(-6f, 2f, 6f, 1, 12, 1), PartPose.ZERO);
        parts.addOrReplaceChild("handle_right", CubeListBuilder.create().texOffs(52, 0).addBox(5f, 2f, 6f, 1, 12, 1), PartPose.ZERO);
        parts.addOrReplaceChild("handle_bar", CubeListBuilder.create().texOffs(0, 34).addBox(-6f, 2f, 6f, 12, 1, 1), PartPose.ZERO);
        for (int i = -1; i <= 1; i++) {
            parts.addOrReplaceChild("bottom_" + (i + 1), CubeListBuilder.create().texOffs(0, 38).addBox(i * 8f - 2f, 16f, -3f, 3, 8, 7), PartPose.ZERO);
            parts.addOrReplaceChild("share_" + (i + 1), CubeListBuilder.create().texOffs(24, 38).addBox(i * 8f - 3f, 22f, -7f, 5, 2, 4), PartPose.ZERO);
        }
        parts.addOrReplaceChild("wheel", CubeListBuilder.create().texOffs(40, 48).addBox(-1f, 17f, -22f, 2, 7, 7), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(PlowEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
