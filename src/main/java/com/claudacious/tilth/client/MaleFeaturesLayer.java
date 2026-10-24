package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.lang.reflect.Field;

/**
 * Draws what tells a male farm animal apart: a bull's nose ring and heavier horns, a ram's curled horns, a
 * boar's tusks, a rooster's tall comb and tail plume. The parts ride on the vanilla model's head (or body) so
 * they follow every animation. Females look as in vanilla.
 */
public final class MaleFeaturesLayer<T extends Animal, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation TEXTURE = Tilth.id("textures/entity/male_features.png");
    static final ModelLayerLocation BULL = new ModelLayerLocation(Tilth.id("bull"), "main");
    static final ModelLayerLocation RAM = new ModelLayerLocation(Tilth.id("ram"), "main");
    static final ModelLayerLocation BOAR = new ModelLayerLocation(Tilth.id("boar"), "main");
    static final ModelLayerLocation ROOSTER_HEAD = new ModelLayerLocation(Tilth.id("rooster"), "head");
    static final ModelLayerLocation ROOSTER_TAIL = new ModelLayerLocation(Tilth.id("rooster"), "tail");

    private final ModelPart features;
    private final String anchorField;
    private final Class<?> anchorOwner;
    private Field anchor;

    private MaleFeaturesLayer(RenderLayerParent<T, M> parent, ModelPart features, Class<?> anchorOwner, String anchorField) {
        super(parent);
        this.features = features;
        this.anchorOwner = anchorOwner;
        this.anchorField = anchorField;
    }

    // Texture: 32x64. Rows of colour: gold at v 0, horn at v 4, white at v 12, red at v 16, dark plumage from v 24.

    static LayerDefinition bull() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("ring", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5f, 2.0f, -7.3f, 3, 2, 1), PartPose.ZERO);
        root.addOrReplaceChild("horn_left", CubeListBuilder.create().texOffs(0, 4).addBox(-8.0f, -5.0f, -4.0f, 4, 1, 1)
                .texOffs(10, 4).addBox(-8.0f, -7.0f, -4.0f, 1, 2, 1), PartPose.ZERO);
        root.addOrReplaceChild("horn_right", CubeListBuilder.create().texOffs(0, 4).addBox(4.0f, -5.0f, -4.0f, 4, 1, 1)
                .texOffs(10, 4).addBox(7.0f, -7.0f, -4.0f, 1, 2, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 64);
    }

    static LayerDefinition ram() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int side : new int[]{-1, 1}) {
            float x = side < 0 ? -5.0f : 3.0f;
            root.addOrReplaceChild("horn_" + side, CubeListBuilder.create()
                    .texOffs(0, 4).addBox(x, -5.0f, -4.0f, 2, 2, 4)
                    .texOffs(0, 4).addBox(x, -3.0f, -1.0f, 2, 3, 2)
                    .texOffs(0, 4).addBox(x, 0.0f, -4.0f, 2, 2, 4), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 32, 64);
    }

    static LayerDefinition boar() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("tusks", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-3.5f, 0.5f, -9.5f, 1, 3, 1)
                .texOffs(0, 12).addBox(2.5f, 0.5f, -9.5f, 1, 3, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 64);
    }

    static LayerDefinition roosterHead() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("comb", CubeListBuilder.create().texOffs(0, 16).addBox(-0.5f, -9.0f, -3.0f, 1, 3, 4), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 64);
    }

    static LayerDefinition roosterTail() {
        MeshDefinition mesh = new MeshDefinition();
        // Body space of the chicken model: the body box is turned 90 degrees about x, so local +y points back (its rear
        // end is at y = +4) and local +z points up (its back is at z = +3). The plume rises up and back from the rump.
        mesh.getRoot().addOrReplaceChild("plume", CubeListBuilder.create().texOffs(0, 24).addBox(-1.5f, 2.0f, 1.0f, 3, 5, 6), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 64);
    }

    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BULL, MaleFeaturesLayer::bull);
        event.registerLayerDefinition(RAM, MaleFeaturesLayer::ram);
        event.registerLayerDefinition(BOAR, MaleFeaturesLayer::boar);
        event.registerLayerDefinition(ROOSTER_HEAD, MaleFeaturesLayer::roosterHead);
        event.registerLayerDefinition(ROOSTER_TAIL, MaleFeaturesLayer::roosterTail);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        add(event, EntityType.COW, BULL, QuadrupedModel.class, "head");
        add(event, EntityType.SHEEP, RAM, QuadrupedModel.class, "head");
        add(event, EntityType.PIG, BOAR, QuadrupedModel.class, "head");
        add(event, EntityType.CHICKEN, ROOSTER_HEAD, ChickenModel.class, "head");
        add(event, EntityType.CHICKEN, ROOSTER_TAIL, ChickenModel.class, "body");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void add(EntityRenderersEvent.AddLayers event, EntityType<?> type, ModelLayerLocation layer, Class<?> owner, String field) {
        if (event.getRenderer(type) instanceof LivingEntityRenderer renderer) {
            renderer.addLayer(new MaleFeaturesLayer(renderer, event.getEntityModels().bakeLayer(layer), owner, field));
        }
    }

    private ModelPart anchor() {
        if (anchor == null) {
            try {
                anchor = anchorOwner.getDeclaredField(anchorField);
                anchor.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
        try {
            return (ModelPart) anchor.get(getParentModel());
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, T animal, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (animal.isBaby() || animal.isInvisible() || !ClientSexes.isMale(animal.getId())) {
            return;
        }
        ModelPart part = anchor();
        if (part == null) {
            return;
        }
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        features.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight,
                LivingEntityRenderer.getOverlayCoords(animal, 0.0f));
        poseStack.popPose();
    }
}
