package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.entity.WagonEntity;
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
 * A four-wheeled box wagon: plank bed with low sides, two axles, spoked wheels, a shaft to the hitch and two canvas
 * hoops. Same conventions as {@link SpreaderModel}; the 128x128 texture comes from tools/wagon.py.
 */
public class WagonModel extends EntityModel<WagonEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tilth.id("wagon"), "main");
    private final ModelPart root;
    private final ModelPart[] wheels;

    public WagonModel(ModelPart root) {
        this.root = root;
        this.wheels = new ModelPart[]{root.getChild("wheel_fl"), root.getChild("wheel_fr"), root.getChild("wheel_bl"), root.getChild("wheel_br")};
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p = mesh.getRoot();
        p.addOrReplaceChild("bed", CubeListBuilder.create().texOffs(0, 0).addBox(-10f, 10f, -14f, 20, 2, 28), PartPose.ZERO);
        p.addOrReplaceChild("side_left", CubeListBuilder.create().texOffs(0, 30).addBox(-10f, 4f, -14f, 1, 6, 28), PartPose.ZERO);
        p.addOrReplaceChild("side_right", CubeListBuilder.create().texOffs(0, 30).mirror().addBox(9f, 4f, -14f, 1, 6, 28), PartPose.ZERO);
        p.addOrReplaceChild("front", CubeListBuilder.create().texOffs(60, 30).addBox(-9f, 4f, -14f, 18, 6, 1), PartPose.ZERO);
        p.addOrReplaceChild("back", CubeListBuilder.create().texOffs(60, 30).addBox(-9f, 4f, 13f, 18, 6, 1), PartPose.ZERO);
        p.addOrReplaceChild("shaft", CubeListBuilder.create().texOffs(60, 40).addBox(-1f, 14f, -30f, 2, 2, 16), PartPose.ZERO);
        p.addOrReplaceChild("axle_front", CubeListBuilder.create().texOffs(0, 70).addBox(-11f, 16f, -10f, 22, 2, 2), PartPose.ZERO);
        p.addOrReplaceChild("axle_back", CubeListBuilder.create().texOffs(0, 70).addBox(-11f, 16f, 8f, 22, 2, 2), PartPose.ZERO);
        for (String hoop : new String[]{"hoop_front", "hoop_back"}) {
            float z = hoop.equals("hoop_front") ? -9f : 8f;
            p.addOrReplaceChild(hoop, CubeListBuilder.create().texOffs(0, 80).addBox(-10f, -6f, z, 20, 1, 1)
                    .texOffs(0, 84).addBox(-10f, -5f, z, 1, 9, 1).texOffs(0, 84).addBox(9f, -5f, z, 1, 9, 1), PartPose.ZERO);
        }
        // Wheels turn about their own axle, so each sits at its hub.
        float[][] hubs = {{-11.5f, -9f}, {11.5f, -9f}, {-11.5f, 9f}, {11.5f, 9f}};
        String[] names = {"wheel_fl", "wheel_fr", "wheel_bl", "wheel_br"};
        for (int i = 0; i < 4; i++) {
            p.addOrReplaceChild(names[i], CubeListBuilder.create().texOffs(100, 0).addBox(-1f, -5f, -5f, 2, 10, 10),
                    PartPose.offset(hubs[i][0], 17f, hubs[i][1]));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(WagonEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Wheels roll with distance travelled: one turn per ~2 blocks.
        float roll = limbSwing;
        for (ModelPart wheel : wheels) {
            wheel.xRot = roll;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
