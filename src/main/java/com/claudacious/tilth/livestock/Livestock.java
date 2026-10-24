package com.claudacious.tilth.livestock;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.block.ManureLayerBlock;
import com.claudacious.tilth.block.SoilGrassBlock;
import com.claudacious.tilth.quality.Quality;
import com.claudacious.tilth.registry.TilthAttachments;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.climate.DailyWeather;
import com.claudacious.tilth.world.ClimateService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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

/**
 * Livestock husbandry. Each farm animal has a body condition that falls by about half a point a game day
 * and rises when it grazes, eats from a hay bale or is fed by hand. Grass barely grows in cold weather, so
 * animals need hay through the winter. Condition decides whether animals breed, give milk and lay, how much
 * manure they leave, and how much meat they yield and of what quality.
 */
public final class Livestock {
    private static final int UPDATE_TICKS = 200;
    /** Condition lost per tick: from top condition to starving in about two game days. */
    private static final double DECAY_PER_TICK = 1.0 / 48000.0;
    private static final long MILK_INTERVAL = 24000;
    private static final TagKey<Item> FODDER = ItemTags.create(Tilth.id("fodder"));

    private Livestock() {
    }

    public static boolean isLivestock(Entity e) {
        return e instanceof Cow || e instanceof Pig || e instanceof Sheep || e instanceof Goat || e instanceof Chicken
                || e instanceof AbstractHorse;
    }

    public static LivestockState state(Animal animal) {
        return animal.getData(TilthAttachments.LIVESTOCK.get());
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Animal animal) || !(animal.level() instanceof ServerLevel level) || !isLivestock(animal)
                || !TilthConfig.LIVESTOCK.getAsBoolean() || (animal.tickCount + animal.getId()) % UPDATE_TICKS != 0) {
            return;
        }
        LivestockState s = state(animal);
        long now = level.getGameTime();
        if (s.lastUpdate >= 0) {
            s.add(-DECAY_PER_TICK * Math.min(24000 * 10, now - s.lastUpdate) * TilthConfig.LIVESTOCK_APPETITE.get());
        }
        s.lastUpdate = now;
        if (s.condition < 0.95) {
            forage(level, animal, s);
        }
        // Roosters never lay, and thin hens stop laying.
        if (animal instanceof Chicken chicken && (s.condition < 0.35 || s.male())) {
            chicken.eggTime = Math.max(chicken.eggTime, 6000);
        }
        if (s.condition > 0.2) {
            int interval = TilthConfig.MANURE_INTERVAL.getAsInt();
            if (interval > 0 && !animal.isBaby() && animal.getRandom().nextInt(Math.max(1, interval / UPDATE_TICKS)) == 0
                    && !ManureLayerBlock.deposit(level, animal.blockPosition())) {
                animal.spawnAtLocation(new ItemStack(TilthItems.MANURE.get()));
            }
        }
        Breeding.checkBirth(level, animal, s);
        animal.setData(TilthAttachments.LIVESTOCK.get(), s);
    }

    /** Grazes the grass underfoot, or eats from a nearby hay bale. Cold weather leaves little to graze. */
    private static void forage(ServerLevel level, Animal animal, LivestockState s) {
        BlockPos feet = animal.blockPosition();
        if (!(animal instanceof Chicken) && !(animal instanceof Pig)) {
            // Alfalfa hay is richer in protein than grass hay and goes further: more condition per meal, more meals per bale.
            for (BlockPos p : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, 1, 2))) {
                BlockState bale = level.getBlockState(p);
                boolean alfalfa = bale.is(TilthBlocks.ALFALFA_BALE.get());
                if (alfalfa || bale.is(Blocks.HAY_BLOCK)) {
                    s.add(alfalfa ? 0.7 : 0.45);
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
            s.add(0.2 * growth);
        } else if (under.is(Blocks.GRASS_BLOCK) || under.getBlock() instanceof SoilGrassBlock) {
            s.add(0.12 * growth);
            // Heavy grazing wears the sward down to bare soil, which grass slowly reclaims.
            if (animal.getRandom().nextInt(6) == 0) {
                level.levelEvent(2001, feet.below(), net.minecraft.world.level.block.Block.getId(under));
                level.setBlockAndUpdate(feet.below(), under.getBlock() instanceof SoilGrassBlock g
                        ? TilthBlocks.topsoil(g.texture()) : Blocks.DIRT.defaultBlockState());
            }
            if (animal instanceof Sheep sheep && sheep.isSheared() && s.condition > 0.5) {
                sheep.setSheared(false);
            }
        } else if (animal instanceof Chicken || animal instanceof Pig) {
            // Chickens and pigs forage scraps and roots anywhere, but not enough on their own.
            s.add(0.03 * growth);
        }
    }

    /** Hand feeding: thin animals put the feed into condition instead of breeding; milking needs a fed, rested animal. */
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Animal animal) || !isLivestock(animal) || !TilthConfig.LIVESTOCK.getAsBoolean()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        LivestockState s = state(animal);
        boolean milking = (held.is(Items.BUCKET) && (animal instanceof Cow || animal instanceof Goat)) || (held.is(Items.BOWL) && animal instanceof Cow);
        if (milking && !animal.isBaby()) {
            long now = animal.level().getGameTime();
            if (s.male() || s.condition < 0.4 || now - s.lastMilked < MILK_INTERVAL) {
                if (!animal.level().isClientSide) {
                    player.displayClientMessage(Component.literal(s.male() ? "Only females give milk: that is a " + Breeding.sexName(animal, false) + "."
                            : s.condition < 0.4 ? "Too thin to give milk. Feed it first." : "Already milked today.").withStyle(ChatFormatting.YELLOW), true);
                    // The client cannot see condition or the last milking and may already show a milk bucket: put the
                    // empty one back in its hand.
                    player.containerMenu.sendAllDataToRemote();
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }
            s.lastMilked = now;
            animal.setData(TilthAttachments.LIVESTOCK.get(), s);
            return;
        }
        if (!animal.isFood(held) && !held.is(FODDER)) {
            return;
        }
        if (animal.level().isClientSide) {
            return;
        }
        // Judged before the meal: a thin animal puts this feed into condition, not into breeding.
        boolean thin = s.condition < 0.55;
        s.add(0.25);
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

    /** Meat yield and quality follow condition: a starving animal gives a scrap of poor meat, a fat one premium cuts. */
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Animal animal) || !isLivestock(animal) || !TilthConfig.LIVESTOCK.getAsBoolean()) {
            return;
        }
        double c = state(animal).condition;
        int grade = c >= 0.85 ? 5 : c >= 0.65 ? 4 : c >= 0.45 ? 3 : c >= 0.25 ? 2 : 1;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (!stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
                continue;
            }
            int count = Math.max(1, (int) Math.round(stack.getCount() * (0.4 + 0.9 * c)));
            stack.setCount(count);
            Quality.set(stack, grade);
            drop.setItem(stack);
        }
    }
}
