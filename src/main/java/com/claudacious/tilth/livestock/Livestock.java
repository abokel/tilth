package com.claudacious.tilth.livestock;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.affliction.Affliction;
import com.claudacious.tilth.affliction.AfflictionEngine;
import com.claudacious.tilth.block.ManureLayerBlock;
import com.claudacious.tilth.block.SoilGrassBlock;
import com.claudacious.tilth.quality.Quality;
import com.claudacious.tilth.registry.TilthAttachments;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.climate.DailyWeather;
import com.claudacious.tilth.sim.genetics.GeneticsModel;
import com.claudacious.tilth.sim.livestock.LivestockDiseaseModel;
import com.claudacious.tilth.sim.livestock.PastureModel;
import com.claudacious.tilth.world.ClimateService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Livestock husbandry: condition, rotational grazing, disease pressure and veterinary care.
 */
public final class Livestock {
    private static final int UPDATE_TICKS = 200;
    private static final double DECAY_PER_TICK = 1.0 / 48000.0;
    private static final long MILK_INTERVAL = 24000;
    private static final long DIAGNOSIS_WINDOW = 2 * 24000L;
    private static final TagKey<Item> FODDER = ItemTags.create(Tilth.id("fodder"));

    private Livestock() {
    }

    public static boolean isLivestock(Entity e) {
        return e instanceof Cow || e instanceof Pig || e instanceof Sheep || e instanceof Goat || e instanceof Chicken
                || e instanceof AbstractHorse;
    }

    public static LivestockState state(Animal animal) {
        return animal.getData(TilthAttachments.LIVESTOCK.get()).sanitize();
    }

    public static String animalId(Animal animal) {
        if (animal instanceof Cow) {
            return "cow";
        }
        if (animal instanceof Sheep) {
            return "sheep";
        }
        if (animal instanceof Goat) {
            return "goat";
        }
        if (animal instanceof Pig) {
            return "pig";
        }
        if (animal instanceof Chicken) {
            return "chicken";
        }
        if (animal instanceof AbstractHorse) {
            return "horse";
        }
        return "animal";
    }

    public static String pastureStatus(LivestockState s) {
        if (PastureModel.overgrazed(s.pastureQuality, TilthConfig.LIVESTOCK_OVERGRAZE_THRESHOLD.getAsDouble())) {
            return "overgrazed";
        }
        return s.pastureQuality > 0.75 ? "lush" : s.pastureQuality > 0.5 ? "fair" : "worn";
    }

    public static String diseaseStatus(ServerLevel level, LivestockState s) {
        if (s.afflictions.isEmpty()) {
            return "none";
        }
        Registry<Affliction> registry = AfflictionEngine.registry(level);
        Map.Entry<String, Double> worst = s.afflictions.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        if (worst == null) {
            return "none";
        }
        String name = worst.getKey();
        if (registry != null) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(name);
            if (id != null) {
                Affliction a = registry.get(id);
                if (a != null) {
                    name = a.name();
                }
            }
        }
        return String.format(Locale.ROOT, "%s %.0f%%", name, worst.getValue() * 100);
    }

    public static double produceFactor(LivestockState s) {
        return LivestockDiseaseModel.impactFactor(s.diseaseBurden(), TilthConfig.LIVESTOCK_DISEASE_IMPACT_CAP.getAsDouble());
    }

    public static boolean breedingBlockedByDisease(LivestockState s) {
        return s.quarantine || s.diseaseBurden() > 0.6;
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Animal animal) || !(animal.level() instanceof ServerLevel level) || !isLivestock(animal)
                || !TilthConfig.LIVESTOCK.getAsBoolean() || (animal.tickCount + animal.getId()) % UPDATE_TICKS != 0) {
            return;
        }
        LivestockState s = state(animal);
        if (s.genome == null) {
            s.genome = GeneticsModel.randomLivestock(new java.util.Random(level.getSeed() ^ animal.getUUID().getLeastSignificantBits()));
        }
        long now = level.getGameTime();
        long elapsedTicks = s.lastUpdate >= 0 ? Math.min(24000L * 10L, now - s.lastUpdate) : 0L;
        if (elapsedTicks > 0) {
            Breed breed = Breed.byId(s.breed);
            double stress = breed == null ? 1.0 : breed.climateStress(level.getBiome(animal.blockPosition()).value().getBaseTemperature());
            s.add(-DECAY_PER_TICK * elapsedTicks * TilthConfig.LIVESTOCK_APPETITE.get() * s.genome.appetiteMultiplier() * stress);
        }
        s.lastUpdate = now;
        List<Animal> nearby = nearbyLivestock(level, animal, 6.0);
        updatePasture(level, animal, s, nearby.size(), now);
        if (animal.getAttribute(Attributes.SCALE) != null) {
            animal.getAttribute(Attributes.SCALE).setBaseValue(s.genome.scaleMultiplier() * s.genome.phenotypeScaleMultiplier());
        }
        if (!s.quarantine && s.condition < 0.95) {
            forage(level, animal, s);
        }
        runDiseaseCycle(level, animal, s, nearby, now);
        double burden = s.diseaseBurden();
        if (burden > 0) {
            s.add(-0.05 * burden * TilthConfig.LIVESTOCK_DISEASE_IMPACT_CAP.getAsDouble());
        }
        if (animal instanceof Chicken chicken && (s.condition < 0.35 || s.male() || burden > 0.55)) {
            chicken.eggTime = Math.max(chicken.eggTime, 6000);
        }
        if (s.condition > 0.2) {
            int interval = TilthConfig.MANURE_INTERVAL.getAsInt();
            if (interval > 0 && !animal.isBaby()) {
                double manureFactor = 0.5 + 0.5 * produceFactor(s);
                int adjusted = Math.max(1, (int) Math.round(interval / manureFactor));
                if (animal.getRandom().nextInt(Math.max(1, adjusted / UPDATE_TICKS)) == 0
                        && !ManureLayerBlock.deposit(level, animal.blockPosition())) {
                    animal.spawnAtLocation(new ItemStack(TilthItems.MANURE.get()));
                }
            }
        }
        Breeding.checkBirth(level, animal, s);
        animal.setData(TilthAttachments.LIVESTOCK.get(), s.sanitize());
    }

    private static void updatePasture(ServerLevel level, Animal animal, LivestockState s, int nearbyCount, long now) {
        long cell = pastureCell(animal.blockPosition());
        if (s.lastPastureUpdate < 0) {
            s.lastPastureUpdate = now;
            s.pastureCell = cell;
            return;
        }
        double days = Math.max(0.0, (now - s.lastPastureUpdate) / 24000.0);
        if (days <= 0) {
            return;
        }
        double decay = TilthConfig.LIVESTOCK_PASTURE_DECAY.getAsDouble();
        double recovery = TilthConfig.LIVESTOCK_PASTURE_RECOVERY.getAsDouble();
        if (s.quarantine) {
            s.pastureQuality = PastureModel.recover(s.pastureQuality, recovery * 1.2, days);
        } else if (cell != s.pastureCell) {
            s.pastureQuality = PastureModel.recover(s.pastureQuality, recovery * 1.8, days + 0.25);
        } else {
            double pressure = 1.0 + Math.max(0, nearbyCount - 1) * 0.2;
            s.pastureQuality = PastureModel.graze(s.pastureQuality, decay * pressure, days);
        }
        s.pastureCell = cell;
        s.lastPastureUpdate = now;
        if (level.random.nextInt(8) == 0 && s.pastureQuality > TilthConfig.LIVESTOCK_OVERGRAZE_THRESHOLD.getAsDouble()) {
            s.pastureQuality = PastureModel.recover(s.pastureQuality, recovery * 0.1, 1.0 / 6.0);
        }
    }

    /** Grazes underfoot, or eats from nearby hay bales. */
    private static void forage(ServerLevel level, Animal animal, LivestockState s) {
        BlockPos feet = animal.blockPosition();
        double pastureFactor = PastureModel.forageFactor(s.pastureQuality);
        if (!(animal instanceof Chicken) && !(animal instanceof Pig)) {
            for (BlockPos p : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, 1, 2))) {
                BlockState bale = level.getBlockState(p);
                boolean alfalfa = bale.is(TilthBlocks.ALFALFA_BALE.get());
                if (alfalfa || bale.is(Blocks.HAY_BLOCK)) {
                    s.add((alfalfa ? 0.7 : 0.45) * s.genome.feedMultiplier());
                    s.pastureQuality = PastureModel.recover(s.pastureQuality, TilthConfig.LIVESTOCK_PASTURE_RECOVERY.getAsDouble(), 0.25);
                    if (animal.getRandom().nextInt(alfalfa ? 14 : 10) == 0) {
                        level.destroyBlock(p, false);
                    }
                    return;
                }
            }
        }
        DailyWeather w = ClimateService.today(level, feet);
        double growth = w.tMean() < 5 ? 0.15 : w.tMean() > 30 ? 0.5 : 1.0;
        BlockState at = level.getBlockState(feet);
        BlockState under = level.getBlockState(feet.below());
        if (at.is(Blocks.SHORT_GRASS) || at.is(Blocks.TALL_GRASS) || at.is(Blocks.FERN)) {
            level.destroyBlock(feet, false);
            s.add(0.2 * growth * pastureFactor * s.genome.feedMultiplier());
        } else if (under.is(Blocks.GRASS_BLOCK) || under.getBlock() instanceof SoilGrassBlock) {
            s.add(0.12 * growth * pastureFactor * s.genome.feedMultiplier());
            int scalpChance = PastureModel.overgrazed(s.pastureQuality, TilthConfig.LIVESTOCK_OVERGRAZE_THRESHOLD.getAsDouble()) ? 2 : 6;
            if (animal.getRandom().nextInt(scalpChance) == 0) {
                level.levelEvent(2001, feet.below(), net.minecraft.world.level.block.Block.getId(under));
                level.setBlockAndUpdate(feet.below(), under.getBlock() instanceof SoilGrassBlock g
                        ? TilthBlocks.topsoil(g.texture()) : Blocks.DIRT.defaultBlockState());
            }
            if (animal instanceof Sheep sheep && sheep.isSheared() && s.condition > 0.5) {
                sheep.setSheared(false);
            }
        } else if (animal instanceof Chicken || animal instanceof Pig) {
            s.add(0.03 * growth * pastureFactor * s.genome.feedMultiplier());
        }
    }

    private static List<Animal> nearbyLivestock(ServerLevel level, Animal animal, double radius) {
        List<Animal> nearby = level.getEntitiesOfClass(Animal.class, animal.getBoundingBox().inflate(radius), Livestock::isLivestock);
        nearby.removeIf(a -> a == animal);
        return nearby;
    }

    private static long pastureCell(BlockPos pos) {
        long x = pos.getX() >> 4;
        long z = pos.getZ() >> 4;
        return (x << 32) ^ (z & 0xffffffffL);
    }

    private static void runDiseaseCycle(ServerLevel level, Animal animal, LivestockState s, List<Animal> nearby, long now) {
        if (!TilthConfig.LIVESTOCK_DISEASES.getAsBoolean()) {
            return;
        }
        Registry<Affliction> registry = AfflictionEngine.registry(level);
        if (registry == null) {
            return;
        }
        DailyWeather weather = ClimateService.today(level, animal.blockPosition());
        String species = animalId(animal);
        double hostStress = LivestockDiseaseModel.hostStress(s.condition, s.pastureQuality);
        double susceptibility = LivestockDiseaseModel.susceptibility(s.condition, s.pastureQuality, s.quarantine);
        for (Map.Entry<ResourceKey<Affliction>, Affliction> entry : registry.entrySet()) {
            Affliction a = entry.getValue();
            if (a.beneficial() || !a.infectsAnimal(species)) {
                continue;
            }
            String id = entry.getKey().location().toString();
            double suit = AfflictionEngine.suitability(a, weather);
            double neighbourSeverity = neighbourSeverity(id, nearby);
            boolean contact = a.transmission().contains(Affliction.Transmission.MECHANICAL_CONTACT)
                    || a.transmission().contains(Affliction.Transmission.ROOT_CONTACT);
            boolean airborne = a.transmission().contains(Affliction.Transmission.AIRBORNE_SPORES)
                    || a.transmission().contains(Affliction.Transmission.MIGRATION)
                    || a.transmission().contains(Affliction.Transmission.RAIN_SPLASH);
            boolean environmentalIntroduction = airborne || a.transmission().contains(Affliction.Transmission.SOILBORNE);
            double pressure = LivestockDiseaseModel.combinedPressure(
                    LivestockDiseaseModel.transmissionPressure(neighbourSeverity, airborne, contact, s.quarantine),
                    LivestockDiseaseModel.environmentalIntroductionPressure(suit, hostStress, environmentalIntroduction, s.quarantine));
            double severity = s.afflictions.getOrDefault(id, 0.0);
            if (severity <= 0.001) {
                double chance = LivestockDiseaseModel.infectionChance(a.lifecycle().spreadRate(), suit, susceptibility, pressure,
                        TilthConfig.LIVESTOCK_DISEASE_PRESSURE.getAsDouble());
                if (animal.getRandom().nextDouble() < chance) {
                    s.afflictions.put(id, 0.04);
                }
                continue;
            }
            double diagnosed = LivestockDiseaseModel.isRecentDiagnosis(s.diagnosed.get(id), now, DIAGNOSIS_WINDOW) ? 1.0 : 0.0;
            double next = LivestockDiseaseModel.nextSeverity(severity, a.lifecycle().spreadRate(), suit, pressure, diagnosed,
                    s.quarantine ? 0.5 : 0.0);
            if (next <= 0.01) {
                s.afflictions.remove(id);
                s.diagnosed.remove(id);
            } else {
                s.afflictions.put(id, next);
            }
            if (a.impact().canKillHost() && next > 0.98 && animal.getRandom().nextDouble() < 0.04) {
                animal.hurt(animal.damageSources().generic(), Float.MAX_VALUE);
                return;
            }
        }
        long staleCutoff = Math.max(0L, now - 10 * 24000L);
        s.diagnosed.entrySet().removeIf(e -> e.getValue() == null || e.getValue() < staleCutoff);
    }

    private static double neighbourSeverity(String afflictionId, List<Animal> nearby) {
        double sum = 0.0;
        int count = 0;
        for (Animal other : nearby) {
            LivestockState os = state(other);
            if (os.quarantine) {
                continue;
            }
            double severity = os.afflictions.getOrDefault(afflictionId, 0.0);
            if (severity > 0.0) {
                sum += severity;
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    /** Hand feeding, diagnosis, treatment and quarantine controls. */
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Animal animal) || !isLivestock(animal) || !TilthConfig.LIVESTOCK.getAsBoolean()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        LivestockState s = state(animal);
        if (held.is(TilthItems.MICROSCOPE.get())) {
            if (!animal.level().isClientSide && animal.level() instanceof ServerLevel level) {
                if (s.afflictions.isEmpty()) {
                    player.displayClientMessage(Component.literal("No clear signs of disease.").withStyle(ChatFormatting.GREEN), true);
                } else {
                    Registry<Affliction> registry = AfflictionEngine.registry(level);
                    s.afflictions.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .limit(2)
                            .forEach(e -> {
                                String name = e.getKey();
                                if (registry != null) {
                                    net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(e.getKey());
                                    if (id != null) {
                                        Affliction a = registry.get(id);
                                        if (a != null) {
                                            name = a.name();
                                        }
                                    }
                                }
                                player.displayClientMessage(Component.literal(String.format(Locale.ROOT, "Diagnosis: %s (%.0f%%)", name, e.getValue() * 100))
                                        .withStyle(ChatFormatting.YELLOW), false);
                                s.diagnosed.put(e.getKey(), animal.level().getGameTime());
                            });
                }
                animal.setData(TilthAttachments.LIVESTOCK.get(), s);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (held.is(Items.LEAD) && player.isSecondaryUseActive()) {
            if (!animal.level().isClientSide) {
                s.quarantine = !s.quarantine;
                if (s.quarantine) {
                    s.lastPastureUpdate = animal.level().getGameTime();
                }
                animal.setData(TilthAttachments.LIVESTOCK.get(), s);
                player.displayClientMessage(Component.literal(s.quarantine
                        ? "Quarantine enabled: keep this animal isolated and hand-feed it."
                        : "Quarantine lifted.").withStyle(ChatFormatting.GOLD), true);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (held.is(TilthItems.VETERINARY_DRAUGHT.get())) {
            if (animal.level().isClientSide) {
                return;
            }
            if (s.afflictions.isEmpty()) {
                player.displayClientMessage(Component.literal("No active disease to treat.").withStyle(ChatFormatting.GRAY), true);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            int treated = 0;
            long now = animal.level().getGameTime();
            for (Map.Entry<String, Double> e : new ArrayList<>(s.afflictions.entrySet())) {
                double reduction = LivestockDiseaseModel.isRecentDiagnosis(s.diagnosed.get(e.getKey()), now, DIAGNOSIS_WINDOW) ? 0.45 : 0.15;
                double next = Math.max(0.0, e.getValue() - reduction);
                if (next <= 0.01) {
                    s.afflictions.remove(e.getKey());
                    s.diagnosed.remove(e.getKey());
                } else {
                    s.afflictions.put(e.getKey(), next);
                }
                treated++;
            }
            s.add(0.05 * s.genome.feedMultiplier());
            animal.setData(TilthAttachments.LIVESTOCK.get(), s);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            player.displayClientMessage(Component.literal("Treatment given to " + treated + " condition(s).")
                    .withStyle(ChatFormatting.GREEN), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        boolean milking = (held.is(Items.BUCKET) && (animal instanceof Cow || animal instanceof Goat)) || (held.is(Items.BOWL) && animal instanceof Cow);
        if (milking && !animal.isBaby()) {
            long now = animal.level().getGameTime();
            long interval = (long) Math.max(12000, MILK_INTERVAL / s.genome.produceMultiplier());
            double burden = s.diseaseBurden();
            if (s.male() || s.condition < 0.4 || s.quarantine || burden > 0.55 || now - s.lastMilked < interval) {
                if (!animal.level().isClientSide) {
                    player.displayClientMessage(Component.literal(s.male()
                            ? "Only females give milk: that is a " + Breeding.sexName(animal, false) + "."
                            : s.condition < 0.4 ? "Too thin to give milk. Feed it first."
                            : s.quarantine ? "In quarantine: do not take milk yet."
                            : burden > 0.55 ? "Too unwell to milk safely."
                            : "Already milked today.").withStyle(ChatFormatting.YELLOW), true);
                    player.containerMenu.sendAllDataToRemote();
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            s.lastMilked = now;
            animal.setData(TilthAttachments.LIVESTOCK.get(), s);
            // Buckets fill with graded milk (by breed and condition); bowls are left to vanilla.
            if (held.is(Items.BUCKET)) {
                if (!animal.level().isClientSide) {
                    WoolAndDairy.milk(animal, player, event.getHand(), held, s);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(animal.level().isClientSide));
            }
            return;
        }
        if (!animal.isFood(held) && !held.is(FODDER)) {
            return;
        }
        if (animal.level().isClientSide) {
            return;
        }
        boolean thin = s.condition < 0.55;
        s.add(0.25 * s.genome.feedMultiplier());
        animal.setData(TilthAttachments.LIVESTOCK.get(), s);
        if (thin || !animal.isFood(held)) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            animal.level().broadcastEntityEvent(animal, (byte) 7);
            player.displayClientMessage(Component.literal("Fed: " + s.describe() + (thin ? " (too thin to breed)" : ""))
                    .withStyle(ChatFormatting.GREEN), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** Meat yield and quality follow condition and disease burden. */
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Animal animal) || !isLivestock(animal) || !TilthConfig.LIVESTOCK.getAsBoolean()) {
            return;
        }
        LivestockState s = state(animal);
        double c = s.condition;
        int grade = c >= 0.85 ? 5 : c >= 0.65 ? 4 : c >= 0.45 ? 3 : c >= 0.25 ? 2 : 1;
        double diseaseImpact = produceFactor(s);
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (!stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
                continue;
            }
            int count = Math.max(1, (int) Math.round(stack.getCount() * (0.4 + 0.9 * c) * s.genome.produceMultiplier() * diseaseImpact));
            stack.setCount(count);
            Quality.set(stack, grade);
            drop.setItem(stack);
        }
    }
}
