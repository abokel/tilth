package com.claudacious.tilth.diet;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Status effect from the diet system; optionally burns extra food energy every tick. */
public class DietEffect extends MobEffect {
    private final float exhaustionPerTick;

    public DietEffect(MobEffectCategory category, int color) {
        this(category, color, 0f);
    }

    public DietEffect(MobEffectCategory category, int color, float exhaustionPerTick) {
        super(category, color);
        this.exhaustionPerTick = exhaustionPerTick;
    }

    /** Only eating the missing nutrient fixes a deficiency; milk and totems do not. */
    @Override
    public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> cures, net.minecraft.world.effect.MobEffectInstance effectInstance) {
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return exhaustionPerTick > 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            player.causeFoodExhaustion(exhaustionPerTick * (amplifier + 1));
        }
        return true;
    }
}
