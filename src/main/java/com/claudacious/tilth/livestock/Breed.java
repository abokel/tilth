package com.claudacious.tilth.livestock;

import com.claudacious.tilth.sim.genetics.LivestockGenome;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Markings;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Breeds of farm animal. Each suits a climate: an animal that first appears in the world takes a breed that fits its
 * biome's temperature and rainfall (Highland cattle in the cold, Brahman in the heat), and young take a parent's breed.
 * A breed sets the animal's look (its own coat painted over the vanilla model, the natural colour of a sheep's wool, a
 * horse's coat and markings) and nudges its genome: dairy breeds give more milk, beef breeds grow bigger, hardy breeds
 * keep condition on less. Kept outside the climate they suit, animals lose condition faster.
 *
 * <p>{@code minTemp}/{@code maxTemp} are biome temperatures (snowy plains 0.0, taiga 0.25, plains 0.8, desert 2.0).
 */
public enum Breed {
    // ---- cattle
    HOLSTEIN(Species.CATTLE, "Holstein", 0.45, 1.0, 0.3, 0.3, 0.0, 0.6, 0.15),
    JERSEY(Species.CATTLE, "Jersey", 0.6, 1.3, 0.4, 0.1, 0.25, 0.45, -0.35),
    HIGHLAND(Species.CATTLE, "Highland", -0.6, 0.45, 0.0, -0.2, 0.45, -0.2, -0.1),
    ANGUS(Species.CATTLE, "Aberdeen Angus", 0.25, 0.9, 0.2, 0.0, 0.3, -0.1, 0.25),
    HEREFORD(Species.CATTLE, "Hereford", 0.4, 1.1, 0.3, 0.0, 0.25, -0.1, 0.3),
    BRAHMAN(Species.CATTLE, "Brahman", 1.0, 2.5, 0.0, -0.2, 0.3, 0.0, 0.1),
    // ---- pigs
    LARGE_WHITE(Species.PIG, "Large White", 0.5, 1.1, 0.4, 0.2, 0.1, 0.3, 0.2),
    BERKSHIRE(Species.PIG, "Berkshire", 0.35, 1.0, 0.3, 0.0, 0.2, 0.1, 0.0),
    TAMWORTH(Species.PIG, "Tamworth", 0.2, 0.9, 0.2, -0.2, 0.35, 0.0, -0.05),
    OLD_SPOT(Species.PIG, "Gloucester Old Spot", 0.4, 1.0, 0.3, 0.0, 0.3, 0.05, 0.1),
    MANGALICA(Species.PIG, "Mangalica", -0.5, 0.5, 0.0, -0.1, 0.35, -0.1, 0.05),
    // ---- sheep
    MERINO(Species.SHEEP, "Merino", 0.8, 2.2, 0.0, 0.0, 0.2, 0.5, -0.1),
    SUFFOLK(Species.SHEEP, "Suffolk", 0.45, 1.1, 0.3, 0.1, 0.1, 0.0, 0.3),
    HEBRIDEAN(Species.SHEEP, "Hebridean", -0.6, 0.5, 0.0, -0.3, 0.45, -0.1, -0.3),
    SOAY(Species.SHEEP, "Soay", -0.2, 0.7, 0.0, -0.3, 0.4, -0.2, -0.35),
    HERDWICK(Species.SHEEP, "Herdwick", -0.4, 0.6, 0.0, -0.2, 0.5, 0.0, -0.1),
    // ---- chickens
    LEGHORN(Species.CHICKEN, "Leghorn", 0.7, 2.0, 0.0, 0.0, 0.2, 0.55, -0.2),
    RHODE_ISLAND_RED(Species.CHICKEN, "Rhode Island Red", 0.3, 1.2, 0.3, 0.0, 0.2, 0.35, 0.0),
    AUSTRALORP(Species.CHICKEN, "Australorp", 0.5, 1.6, 0.3, 0.0, 0.15, 0.3, 0.1),
    LIGHT_SUSSEX(Species.CHICKEN, "Light Sussex", 0.2, 1.0, 0.3, 0.0, 0.1, 0.2, 0.2),
    BARRED_ROCK(Species.CHICKEN, "Barred Rock", -0.4, 0.8, 0.0, 0.0, 0.3, 0.2, 0.1),
    // ---- horses (vanilla coats and markings)
    SHIRE(Species.HORSE, "Shire", -0.2, 0.9, 0.0, 0.3, 0.0, 0.0, 0.45),
    ARABIAN(Species.HORSE, "Arabian", 0.9, 2.5, 0.0, -0.2, 0.4, 0.0, -0.25),
    FRIESIAN(Species.HORSE, "Friesian", 0.2, 0.9, 0.2, 0.0, 0.1, 0.0, 0.15),
    FJORD(Species.HORSE, "Fjord", -0.6, 0.5, 0.0, -0.2, 0.45, 0.0, -0.1),
    MUSTANG(Species.HORSE, "Mustang", 0.6, 2.0, 0.0, -0.1, 0.35, 0.0, 0.0);

    public enum Species { CATTLE, PIG, SHEEP, CHICKEN, HORSE }

    public final Species species;
    public final String displayName;
    final double minTemp;
    final double maxTemp;
    /** Weight in its niche relative to other fitting breeds (common breeds turn up more often). */
    final double commonness;
    final double appetite;
    final double feedEfficiency;
    final double produce;
    final double bodySize;

    Breed(Species species, String displayName, double minTemp, double maxTemp, double commonness, double appetite,
          double feedEfficiency, double produce, double bodySize) {
        this.species = species;
        this.displayName = displayName;
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.commonness = 1.0 + commonness;
        this.appetite = appetite;
        this.feedEfficiency = feedEfficiency;
        this.produce = produce;
        this.bodySize = bodySize;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    @Nullable
    public static Breed byId(String id) {
        for (Breed b : values()) {
            if (b.id().equals(id)) {
                return b;
            }
        }
        return null;
    }

    @Nullable
    public static Species speciesOf(Animal animal) {
        if (animal instanceof MushroomCow) {
            return null;
        }
        if (animal instanceof Cow) {
            return Species.CATTLE;
        }
        if (animal instanceof Pig) {
            return Species.PIG;
        }
        if (animal instanceof Sheep) {
            return Species.SHEEP;
        }
        if (animal instanceof Chicken) {
            return Species.CHICKEN;
        }
        if (animal instanceof Horse) {
            return Species.HORSE;
        }
        return null;
    }

    /** Whether the climate at a place suits this breed: 1 inside its range, falling to 0 a little outside it. */
    public double fit(double temperature) {
        if (temperature >= minTemp && temperature <= maxTemp) {
            return 1.0;
        }
        double off = temperature < minTemp ? minTemp - temperature : temperature - maxTemp;
        return Math.max(0, 1 - off / 0.4);
    }

    /** Picks a breed for an animal appearing at {@code pos}, weighted by how well each suits the climate there. */
    @Nullable
    public static Breed choose(Species species, ServerLevel level, BlockPos pos, RandomSource random) {
        Biome biome = level.getBiome(pos).value();
        double temperature = biome.getBaseTemperature();
        List<Breed> options = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double total = 0;
        for (Breed b : values()) {
            if (b.species != species) {
                continue;
            }
            double w = b.fit(temperature) * b.commonness;
            if (w > 0) {
                options.add(b);
                weights.add(w);
                total += w;
            }
        }
        if (options.isEmpty()) {
            // Nothing suits this climate well: take the breed whose range is nearest.
            Breed best = null;
            double bestOff = Double.MAX_VALUE;
            for (Breed b : values()) {
                if (b.species == species) {
                    double off = Math.min(Math.abs(temperature - b.minTemp), Math.abs(temperature - b.maxTemp));
                    if (off < bestOff) {
                        bestOff = off;
                        best = b;
                    }
                }
            }
            return best;
        }
        double r = random.nextDouble() * total;
        for (int i = 0; i < options.size(); i++) {
            r -= weights.get(i);
            if (r <= 0) {
                return options.get(i);
            }
        }
        return options.get(options.size() - 1);
    }

    /** A founder animal's genome, nudged toward what its breed was bred for. */
    public LivestockGenome bias(LivestockGenome g) {
        return new LivestockGenome(g.appetite() * 0.5 + appetite, g.feedEfficiency() * 0.5 + feedEfficiency, g.produce() * 0.5 + produce,
                g.bodySize() * 0.5 + bodySize, g.phenotype()).normalized();
    }

    /** Condition is lost faster outside the climate a breed suits: up to half as fast again. */
    public double climateStress(double temperature) {
        return 1.0 + 0.5 * (1.0 - fit(temperature));
    }

    /** Applies what vanilla can show itself: a sheep's natural wool colour, a horse's coat and markings. */
    public void applyLook(Animal animal, RandomSource random) {
        if (animal instanceof Sheep sheep) {
            DyeColor wool = switch (this) {
                case HEBRIDEAN -> DyeColor.BLACK;
                case SOAY -> DyeColor.BROWN;
                case HERDWICK -> DyeColor.GRAY;
                default -> DyeColor.WHITE;
            };
            sheep.setColor(wool);
        } else if (animal instanceof Horse horse) {
            Variant coat = switch (this) {
                case SHIRE -> random.nextBoolean() ? Variant.BLACK : Variant.DARK_BROWN;
                case ARABIAN -> random.nextBoolean() ? Variant.WHITE : Variant.GRAY;
                case FRIESIAN -> Variant.BLACK;
                case FJORD -> Variant.CREAMY;
                default -> random.nextBoolean() ? Variant.CHESTNUT : Variant.BROWN;
            };
            Markings markings = switch (this) {
                case SHIRE -> Markings.WHITE;
                case FRIESIAN, FJORD -> Markings.NONE;
                case ARABIAN -> random.nextInt(3) == 0 ? Markings.BLACK_DOTS : Markings.NONE;
                default -> random.nextInt(3) == 0 ? Markings.WHITE_DOTS : random.nextBoolean() ? Markings.WHITE_FIELD : Markings.NONE;
            };
            // Vanilla has no public setter for markings: both live in the packed "Variant" value of the saved data.
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            horse.addAdditionalSaveData(tag);
            tag.putInt("Variant", (coat.getId() & 255) | (markings.getId() << 8 & 65280));
            horse.readAdditionalSaveData(tag);
        }
    }

    /** Whether the client draws a coat of this breed over the vanilla model (horses and sheep wool use vanilla looks). */
    public boolean hasCoat() {
        return species != Species.HORSE;
    }
}
