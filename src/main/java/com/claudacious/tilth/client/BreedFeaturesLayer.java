package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.livestock.Breed;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.lang.reflect.Field;
import java.util.function.Supplier;

/**
 * Breed body parts that go beyond the vanilla boxes, so breeds differ in shape as well as colour: a Highland's long
 * coat hanging below its belly, forelock and wide horns; a Brahman's hump, dewlap and drooping ears; a Mangalica's
 * curly fleece; a Merino's extra-full fleece while unshorn; a Shire's feathered hooves.
 *
 * <p>Parts ride on a vanilla part (body, head or a leg) so they follow every animation. Body boxes in vanilla are
 * turned 90 degrees about x; parts on a body sit in a child turned back by -90 degrees, so they are laid out with y
 * down and z forward like everything else, relative to the body's pivot. Textures come from tools/breed_features.py.
 */
public final class BreedFeaturesLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private final Breed breed;
    private final ModelPart parts;
    private final ResourceLocation texture;
    private final Class<?> owner;
    private final String anchorField;
    private final boolean onlyWoolly;
    private Field anchor;

    private BreedFeaturesLayer(RenderLayerParent<T, M> parent, Breed breed, ModelPart parts, String textureName, Class<?> owner, String anchorField,
                               boolean onlyWoolly) {
        super(parent);
        this.breed = breed;
        this.parts = parts;
        this.texture = Tilth.id("textures/entity/breed/features/" + textureName + ".png");
        this.owner = owner;
        this.anchorField = anchorField;
        this.onlyWoolly = onlyWoolly;
    }

    private ModelPart anchor() {
        try {
            if (anchor == null) {
                anchor = owner.getDeclaredField(anchorField);
                anchor.setAccessible(true);
            }
            return (ModelPart) anchor.get(getParentModel());
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isBaby() || entity.isInvisible() || ClientSexes.breed(entity.getId()) != breed
                || (onlyWoolly && entity instanceof Sheep sheep && sheep.isSheared())) {
            return;
        }
        ModelPart part = anchor();
        if (part == null || !part.visible) {
            return;
        }
        pose.pushPose();
        part.translateAndRotate(pose);
        parts.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, LivingEntityRenderer.getOverlayCoords(entity, 0.0f));
        pose.popPose();
    }

    // ------------------------------------------------------------------ geometry

    private static final float UNTURN = (float) (-Math.PI / 2);

    private static LayerDefinition onBody(java.util.function.Consumer<PartDefinition> build, int w, int h) {
        MeshDefinition mesh = new MeshDefinition();
        build.accept(mesh.getRoot().addOrReplaceChild("upright", CubeListBuilder.create(), PartPose.rotation(UNTURN, 0, 0)));
        return LayerDefinition.create(mesh, w, h);
    }

    /** Highland coat: a shaggy shell over the body (cow body spans x -6..6, y -3 top..7 belly, z -10..8) hanging below the belly. */
    static LayerDefinition highlandCoat() {
        return onBody(p -> p.addOrReplaceChild("coat", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-6f, -3f, -10f, 12, 14, 18, new CubeDeformation(0.7f)), PartPose.ZERO), 64, 64);
    }

    /** Highland head (head box x -4..4, y -4..4, z -6..0): forelock over the eyes and long upswept horns. */
    static LayerDefinition highlandHead() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p = mesh.getRoot();
        p.addOrReplaceChild("forelock", CubeListBuilder.create().texOffs(0, 32).addBox(-4f, -5f, -6.5f, 8, 6, 1, new CubeDeformation(0.4f)), PartPose.ZERO);
        p.addOrReplaceChild("horns", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-11f, -3f, -4f, 7, 1, 1).texOffs(0, 40).addBox(4f, -3f, -4f, 7, 1, 1)
                .texOffs(0, 44).addBox(-11f, -6f, -4f, 1, 3, 1).texOffs(0, 44).addBox(10f, -6f, -4f, 1, 3, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Brahman body: a hump over the shoulders and a dewlap hanging under the chest. */
    static LayerDefinition brahmanBody() {
        return onBody(p -> {
            p.addOrReplaceChild("hump", CubeListBuilder.create().texOffs(0, 0).addBox(-3f, -6f, -8f, 6, 3, 5), PartPose.ZERO);
            p.addOrReplaceChild("dewlap", CubeListBuilder.create().texOffs(0, 10).addBox(-1f, 5f, -11f, 2, 5, 3), PartPose.ZERO);
        }, 32, 32);
    }

    /** Brahman head: long drooping ears. */
    static LayerDefinition brahmanHead() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("ears", CubeListBuilder.create().texOffs(0, 20).addBox(-6f, -2f, -3f, 2, 5, 1)
                .texOffs(0, 20).addBox(4f, -2f, -3f, 2, 5, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** Mangalica fleece over the pig body (x -5..5, y -1 top..7 belly, z -10..6). */
    static LayerDefinition mangalicaFleece() {
        return onBody(p -> p.addOrReplaceChild("fleece", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5f, -1f, -10f, 10, 8, 16, new CubeDeformation(0.9f)), PartPose.ZERO), 64, 32);
    }

    /** Merino's extra-full fleece, outside the vanilla wool (sheep body x -4..4, y 1 top..7 belly, z -10..6). */
    static LayerDefinition merinoFleece() {
        return onBody(p -> p.addOrReplaceChild("fleece", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4f, 1f, -10f, 8, 6, 16, new CubeDeformation(2.6f)), PartPose.ZERO), 64, 32);
    }

    /** Shire feathering: long hair over the hooves (horse leg boxes run y -1..10). */
    static LayerDefinition shireFeather(boolean left, boolean front) {
        MeshDefinition mesh = new MeshDefinition();
        float x = left ? -3f : -1f;
        float z = front ? -1.9f : -1f;
        mesh.getRoot().addOrReplaceChild("feather", CubeListBuilder.create().texOffs(0, 0).addBox(x, 6f, z, 4, 4, 4, new CubeDeformation(0.8f)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 16);
    }

    // ------------------------------------------------------------------ registration

    private record Spec(String id, Breed breed, EntityType<?> type, Class<?> owner, String field, String texture, boolean woolly,
                        Supplier<LayerDefinition> layer) {
        ModelLayerLocation location() {
            return new ModelLayerLocation(Tilth.id("breed_" + id), "main");
        }
    }

    private static final Spec[] SPECS = {
            new Spec("highland_coat", Breed.HIGHLAND, EntityType.COW, QuadrupedModel.class, "body", "highland", false, BreedFeaturesLayer::highlandCoat),
            new Spec("highland_head", Breed.HIGHLAND, EntityType.COW, QuadrupedModel.class, "head", "highland", false, BreedFeaturesLayer::highlandHead),
            new Spec("brahman_body", Breed.BRAHMAN, EntityType.COW, QuadrupedModel.class, "body", "brahman", false, BreedFeaturesLayer::brahmanBody),
            new Spec("brahman_head", Breed.BRAHMAN, EntityType.COW, QuadrupedModel.class, "head", "brahman", false, BreedFeaturesLayer::brahmanHead),
            new Spec("mangalica", Breed.MANGALICA, EntityType.PIG, QuadrupedModel.class, "body", "mangalica", false, BreedFeaturesLayer::mangalicaFleece),
            new Spec("merino", Breed.MERINO, EntityType.SHEEP, QuadrupedModel.class, "body", "merino", true, BreedFeaturesLayer::merinoFleece),
            new Spec("shire_lh", Breed.SHIRE, EntityType.HORSE, HorseModel.class, "leftHindLeg", "shire", false, () -> shireFeather(true, false)),
            new Spec("shire_rh", Breed.SHIRE, EntityType.HORSE, HorseModel.class, "rightHindLeg", "shire", false, () -> shireFeather(false, false)),
            new Spec("shire_lf", Breed.SHIRE, EntityType.HORSE, HorseModel.class, "leftFrontLeg", "shire", false, () -> shireFeather(true, true)),
            new Spec("shire_rf", Breed.SHIRE, EntityType.HORSE, HorseModel.class, "rightFrontLeg", "shire", false, () -> shireFeather(false, true)),
    };

    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (Spec s : SPECS) {
            event.registerLayerDefinition(s.location(), s.layer());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (Spec s : SPECS) {
            if (event.getRenderer(s.type()) instanceof LivingEntityRenderer renderer) {
                renderer.addLayer(new BreedFeaturesLayer(renderer, s.breed(), event.getEntityModels().bakeLayer(s.location()), s.texture(),
                        s.owner(), s.field(), s.woolly()));
            }
        }
    }
}
