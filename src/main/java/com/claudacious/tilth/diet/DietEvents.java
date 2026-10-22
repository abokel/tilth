package com.claudacious.tilth.diet;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.quality.Quality;
import com.claudacious.tilth.registry.TilthAttachments;
import com.claudacious.tilth.registry.TilthMisc;
import com.claudacious.tilth.sim.diet.DietNutrient;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Feeding the diet stores when eating, draining them over time and applying their effects. */
public final class DietEvents {
    private static final int UPDATE_INTERVAL = 40;
    private static final double DEFICIENT = 0.2;

    private DietEvents() {
    }

    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (!TilthConfig.DIET.getAsBoolean() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItem();
        NutrientProfile profile = TilthMisc.nutrition(stack.getItem());
        if (profile == null) {
            return;
        }
        PlayerDiet diet = player.getData(TilthAttachments.DIET);
        diet.eat(profile, Quality.nutritionMultiplier(Quality.get(stack)));
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % UPDATE_INTERVAL != 0
                || !TilthConfig.DIET.getAsBoolean() || player.isCreative() || player.isSpectator()) {
            return;
        }
        PlayerDiet diet = player.getData(TilthAttachments.DIET);
        diet.decay(UPDATE_INTERVAL, TilthConfig.DIET_DECAY.getAsDouble());
        if (!TilthConfig.DIET_EFFECTS.getAsBoolean()) {
            return;
        }
        if (diet.min() >= 0.5 && diet.average() >= 0.7) {
            refresh(player, TilthMisc.WELL_NOURISHED);
        }
        if (diet.average() < 0.25) {
            refresh(player, TilthMisc.MALNOURISHED);
        }
        if (diet.get(DietNutrient.PROTEIN) < DEFICIENT) {
            refresh(player, TilthMisc.PROTEIN_DEFICIENCY);
        }
        if (diet.get(DietNutrient.CARBOHYDRATE) < DEFICIENT || diet.get(DietNutrient.FAT) < DEFICIENT * 0.5) {
            refresh(player, TilthMisc.LOW_ENERGY);
        }
        if (diet.get(DietNutrient.FIBER) < DEFICIENT * 0.5) {
            refresh(player, TilthMisc.DIGESTIVE_DISTRESS);
        }
        if (diet.get(DietNutrient.VITAMIN_C) < DEFICIENT) {
            refresh(player, TilthMisc.SCURVY);
        }
        if (diet.get(DietNutrient.IRON) < DEFICIENT) {
            refresh(player, TilthMisc.ANEMIA);
        }
        if (diet.get(DietNutrient.VITAMIN_A) < DEFICIENT) {
            refresh(player, TilthMisc.NIGHT_BLINDNESS);
        }
        if (diet.get(DietNutrient.CALCIUM) < DEFICIENT) {
            refresh(player, TilthMisc.BRITTLE_BONES);
        }
    }

    private static void refresh(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() < UPDATE_INTERVAL + 20) {
            player.addEffect(new MobEffectInstance(effect, UPDATE_INTERVAL * 3, 0, true, false, true));
        }
    }

    /** Scurvy: wounds heal slowly without vitamin C (collagen synthesis). */
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(TilthMisc.SCURVY)) {
            event.setAmount(event.getAmount() * 0.5f);
        }
    }

    /** Brittle bones: falls hurt more without enough calcium. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.IS_FALL) && event.getEntity().hasEffect(TilthMisc.BRITTLE_BONES)) {
            event.setAmount(event.getAmount() * 1.5f);
        }
    }
}
