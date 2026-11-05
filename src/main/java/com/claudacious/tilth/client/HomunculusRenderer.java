package com.claudacious.tilth.client;

import com.claudacious.tilth.Tilth;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Slime;

/** The escaped homunculus: the slime model in pale, translucent flesh with a pink body inside. */
public class HomunculusRenderer extends SlimeRenderer {
    private static final ResourceLocation TEXTURE = Tilth.id("textures/entity/homunculus.png");

    public HomunculusRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Slime entity) {
        return TEXTURE;
    }
}
