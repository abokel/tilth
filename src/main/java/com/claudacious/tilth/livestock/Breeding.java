package com.claudacious.tilth.livestock;

import com.claudacious.tilth.network.SexSync;
import com.claudacious.tilth.registry.TilthAttachments;
import com.claudacious.tilth.sim.calendar.CalendarMath;
import com.claudacious.tilth.sim.genetics.GeneticsModel;
import com.claudacious.tilth.sim.livestock.LivestockBreedingModel;
import com.claudacious.tilth.world.TilthCalendar;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Random;

/**
 * Sexes and breeding. Every farm animal is male or female; only a male and a female breed. Sheep and goats are
 * seasonal breeders and only mate in autumn. Mammals carry their young (a few game days, longest for cattle and
 * horses) and then give birth: pigs to litters, ewes and nannies often to twins. Hens only need a rooster.
 */
public final class Breeding {
    private Breeding() {
    }

    /** Gestation in game ticks, scaled to a playable length: cattle and horses longest, pigs and small ruminants shorter. */
    static long gestation(Animal a) {
        if (a instanceof AbstractHorse) {
            return 4 * 24000L;
        }
        if (a instanceof Cow) {
            return 3 * 24000L;
        }
        return 2 * 24000L;
    }

    static boolean carriesYoung(Animal a) {
        return !(a instanceof Chicken);
    }

    static boolean seasonal(Animal a) {
        return a instanceof Sheep || a instanceof Goat;
    }

    public static String sexName(Animal a, boolean female) {
        if (a instanceof Cow) {
            return female ? "cow" : "bull";
        }
        if (a instanceof Sheep) {
            return female ? "ewe" : "ram";
        }
        if (a instanceof Pig) {
            return female ? "sow" : "boar";
        }
        if (a instanceof Goat) {
            return female ? "nanny" : "billy";
        }
        if (a instanceof Chicken) {
            return female ? "hen" : "rooster";
        }
        if (a instanceof AbstractHorse) {
            return female ? "mare" : "stallion";
        }
        return female ? "female" : "male";
    }

    /** Gives every farm animal a sex when it first appears in the world, and tells clients. */
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof Animal animal) || !Livestock.isLivestock(animal)) {
            return;
        }
        LivestockState s = Livestock.state(animal);
        boolean founder = s.sex == LivestockState.UNKNOWN;
        if (founder) {
            s.sex = animal.getRandom().nextBoolean() ? LivestockState.FEMALE : LivestockState.MALE;
            s.genome = GeneticsModel.randomLivestock(new java.util.Random(((ServerLevel) event.getLevel()).getSeed() ^ animal.getUUID().getLeastSignificantBits()));
        }
        // Animals from before breeds existed, and founders, take a breed that suits the climate they live in.
        Breed.Species species = Breed.speciesOf(animal);
        if (s.breed.isEmpty() && species != null) {
            Breed breed = Breed.choose(species, (ServerLevel) event.getLevel(), animal.blockPosition(), animal.getRandom());
            if (breed != null) {
                s.breed = breed.id();
                if (founder) {
                    s.genome = breed.bias(s.genome);
                }
                breed.applyLook(animal, animal.getRandom());
            }
        }
        animal.setData(TilthAttachments.LIVESTOCK.get(), s);
    }

    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Animal animal && Livestock.isLivestock(animal) && event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new SexSync(animal.getId(), Livestock.state(animal).female(), Livestock.state(animal).breed));
        }
    }

    public static void sync(Animal animal) {
        PacketDistributor.sendToPlayersTrackingEntity(animal, new SexSync(animal.getId(), Livestock.state(animal).female(), Livestock.state(animal).breed));
    }

    private static void tell(BabyEntitySpawnEvent event, String message) {
        Player player = event.getCausedByPlayer();
        if (player != null) {
            player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW), true);
        }
    }

    /** Vanilla breeding produces an instant baby; here it needs a pair, the right season, and (for mammals) a pregnancy. */
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof Animal a) || !(event.getParentB() instanceof Animal b) || !Livestock.isLivestock(a)
                || !(a.level() instanceof ServerLevel level)) {
            return;
        }
        LivestockState sa = Livestock.state(a);
        LivestockState sb = Livestock.state(b);
        if (sa.sex == sb.sex) {
            tell(event, "Two " + sexName(a, sa.female()) + "s cannot breed: pair a male with a female.");
            event.setCanceled(true);
            return;
        }
        Animal mother = sa.female() ? a : b;
        LivestockState sm = sa.female() ? sa : sb;
        LivestockState father = sa.female() ? sb : sa;
        if (Livestock.breedingBlockedByDisease(sm) || Livestock.breedingBlockedByDisease(father)) {
            tell(event, "Breeding paused: diagnose, treat and clear quarantine first.");
            event.setCanceled(true);
            return;
        }
        if (seasonal(mother)) {
            int month = CalendarMath.month(CalendarMath.dayOfYear(TilthCalendar.realDay(level)));
            if (month < 9) {
                tell(event, "Sheep and goats only come into season in autumn (September to December).");
                event.setCanceled(true);
                return;
            }
        }
        if (!carriesYoung(mother)) {
            if (event.getChild() instanceof Animal child && Livestock.isLivestock(child)) {
                LivestockState childState = Livestock.state(child);
                LivestockBreedingModel.OffspringInheritance inherited = LivestockBreedingModel.inheritOffspring(sm.genome, father.genome,
                        new Random(child.getRandom().nextLong()));
                childState.sex = inherited.sex();
                childState.genome = inherited.genome();
                childState.breed = inheritBreed(sm.breed, father.breed, child.getRandom());
                Breed breed = Breed.byId(childState.breed);
                if (breed != null) {
                    breed.applyLook(child, child.getRandom());
                }
                child.setData(TilthAttachments.LIVESTOCK.get(), childState);
            }
            return;
        }
        event.setCanceled(true);
        if (sm.pregnant()) {
            tell(event, "The " + sexName(mother, true) + " is already in calf.");
            return;
        }
        sm.due = level.getGameTime() + gestation(mother);
        sm.sireGenome = father.genome;
        sm.sireBreed = father.breed;
        mother.setData(TilthAttachments.LIVESTOCK.get(), sm);
        level.sendParticles(ParticleTypes.HEART, mother.getX(), mother.getY() + 1.2, mother.getZ(), 4, 0.4, 0.3, 0.4, 0.0);
        tell(event, "The " + sexName(mother, true) + " is expecting. She will give birth in about "
                + gestation(mother) / 24000 + " days.");
    }

    /** Young take the breed of one parent or the other; a cross shows whichever parent it favours. */
    static String inheritBreed(String mother, String father, net.minecraft.util.RandomSource random) {
        if (mother.isEmpty()) {
            return father;
        }
        if (father.isEmpty()) {
            return mother;
        }
        return random.nextBoolean() ? mother : father;
    }

    /** Called from the livestock tick: a pregnant female whose time has come gives birth. */
    static void checkBirth(ServerLevel level, Animal mother, LivestockState s) {
        if (!s.pregnant() || level.getGameTime() < s.due) {
            return;
        }
        s.due = -1;
        int young = 1;
        if (mother instanceof Pig) {
            young = 2 + mother.getRandom().nextInt(3);
        } else if (mother instanceof Sheep || mother instanceof Goat) {
            young = mother.getRandom().nextFloat() < 0.4f ? 2 : 1;
        } else if (mother instanceof Cow && mother.getRandom().nextFloat() < 0.03f) {
            young = 2;
        }
        if (s.condition < 0.3) {
            young = Math.max(1, young - 1);
        }
        if (s.diseaseBurden() > 0.45) {
            young = Math.max(1, young - 1);
        }
        for (int i = 0; i < young; i++) {
            AgeableMob baby = mother.getBreedOffspring(level, mother);
            if (baby == null) {
                continue;
            }
            baby.setBaby(true);
            baby.moveTo(mother.getX() + (mother.getRandom().nextDouble() - 0.5), mother.getY(), mother.getZ() + (mother.getRandom().nextDouble() - 0.5),
                    mother.getRandom().nextFloat() * 360f, 0f);
            if (baby instanceof Animal animalBaby && Livestock.isLivestock(animalBaby)) {
                LivestockState bs = Livestock.state(animalBaby);
                LivestockBreedingModel.OffspringInheritance inherited = LivestockBreedingModel.inheritOffspring(s.genome, s.sireGenome,
                        new Random(animalBaby.getRandom().nextLong()));
                bs.sex = inherited.sex();
                bs.genome = inherited.genome();
                bs.breed = inheritBreed(s.breed, s.sireBreed, animalBaby.getRandom());
                Breed breed = Breed.byId(bs.breed);
                if (breed != null) {
                    breed.applyLook(animalBaby, animalBaby.getRandom());
                }
                animalBaby.setData(TilthAttachments.LIVESTOCK.get(), bs);
            }
            level.addFreshEntityWithPassengers(baby);
        }
        s.add(-0.15);
        s.sireGenome = com.claudacious.tilth.sim.genetics.LivestockGenome.baseline();
        s.sireBreed = "";
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mother.getX(), mother.getY() + 1.0, mother.getZ(), 10, 0.6, 0.4, 0.6, 0.0);
    }
}
