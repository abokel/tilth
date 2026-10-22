package com.claudacious.tilth.affliction;

import com.claudacious.tilth.sim.crop.CropSpecies;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;

/**
 * A crop disease, pest, parasitic organism or beneficial symbiont, defined in datapacks at
 * {@code data/<namespace>/tilth/affliction/<id>.json}.
 *
 * <p>The model separates <em>what favours the organism</em> (its environmental envelope), <em>how
 * it spreads and survives</em> (transmission and lifecycle), <em>what it does to the host</em>
 * (impact) and <em>how farmers manage it</em> (control). The risk engine uses all four; the
 * simulation applies them only when {@code afflictions.enabled = true} in the config.
 */
public record Affliction(String name, String scientificName, Category category, boolean beneficial, List<String> hosts,
                         List<Transmission> transmission, Envelope envelope, Lifecycle lifecycle, Impact impact,
                         Control control, List<String> symptoms, String notes) {

    public enum Category implements StringRepresentable {
        FUNGUS, OOMYCETE, BACTERIUM, VIRUS, PROTIST, INSECT, MITE, NEMATODE, MOLLUSC, VERTEBRATE, WEED, PARASITIC_PLANT, SYMBIONT;

        public static final Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Transmission implements StringRepresentable {
        AIRBORNE_SPORES, RAIN_SPLASH, SOILBORNE, SEEDBORNE, CROP_RESIDUE, INSECT_VECTOR, MECHANICAL_CONTACT, MIGRATION, ROOT_CONTACT;

        public static final Codec<Transmission> CODEC = StringRepresentable.fromEnum(Transmission::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Conditions that favour the organism.
     *
     * @param leafWetnessHours hours of wet foliage needed for infection (0 = not required)
     * @param soilMoistureMin  lowest relative soil moisture (0 = wilting point, 1 = field capacity) it tolerates
     * @param soilMoistureMax  highest relative soil moisture it tolerates (above 1 = waterlogged)
     */
    public record Envelope(double tempMin, double tempOpt, double tempMax, double minHumidity, double leafWetnessHours,
                           double soilMoistureMin, double soilMoistureMax, double phMin, double phMax) {
        public static final Codec<Envelope> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("temp_min").forGetter(Envelope::tempMin),
                Codec.DOUBLE.fieldOf("temp_opt").forGetter(Envelope::tempOpt),
                Codec.DOUBLE.fieldOf("temp_max").forGetter(Envelope::tempMax),
                Codec.DOUBLE.optionalFieldOf("min_humidity", 0.0).forGetter(Envelope::minHumidity),
                Codec.DOUBLE.optionalFieldOf("leaf_wetness_hours", 0.0).forGetter(Envelope::leafWetnessHours),
                Codec.DOUBLE.optionalFieldOf("soil_moisture_min", 0.0).forGetter(Envelope::soilMoistureMin),
                Codec.DOUBLE.optionalFieldOf("soil_moisture_max", 2.0).forGetter(Envelope::soilMoistureMax),
                Codec.DOUBLE.optionalFieldOf("ph_min", 3.0).forGetter(Envelope::phMin),
                Codec.DOUBLE.optionalFieldOf("ph_max", 10.0).forGetter(Envelope::phMax)
        ).apply(i, Envelope::new));
    }

    /**
     * @param latentDays     days from infection to symptoms / new inoculum
     * @param spreadRate     relative daily multiplication rate under ideal conditions (0-1)
     * @param overwintering  where it survives between crops (residue, soil, seed, alternate_host, migration, none)
     * @param survivalYears  years it persists without a host; rotation must be at least this long
     */
    public record Lifecycle(int latentDays, double spreadRate, String overwintering, double survivalYears) {
        public static final Codec<Lifecycle> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("latent_days", 7).forGetter(Lifecycle::latentDays),
                Codec.DOUBLE.optionalFieldOf("spread_rate", 0.2).forGetter(Lifecycle::spreadRate),
                Codec.STRING.optionalFieldOf("overwintering", "residue").forGetter(Lifecycle::overwintering),
                Codec.DOUBLE.optionalFieldOf("survival_years", 1.0).forGetter(Lifecycle::survivalYears)
        ).apply(i, Lifecycle::new));
    }

    /**
     * @param maxYieldLoss  yield lost at full severity (0-1)
     * @param growthPenalty growth rate lost at full severity (0-1)
     * @param qualityPenalty quality lost at full severity (0-1)
     * @param fromStage     first visual growth stage (0-9) the host is susceptible
     * @param toStage       last susceptible stage
     * @param benefit       for symbionts: proportional improvement (e.g. 0.3 = +30% phosphorus uptake)
     */
    public record Impact(double maxYieldLoss, double growthPenalty, double qualityPenalty, boolean canKillHost, int fromStage,
                         int toStage, double benefit) {
        public static final Codec<Impact> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("max_yield_loss", 0.0).forGetter(Impact::maxYieldLoss),
                Codec.DOUBLE.optionalFieldOf("growth_penalty", 0.0).forGetter(Impact::growthPenalty),
                Codec.DOUBLE.optionalFieldOf("quality_penalty", 0.0).forGetter(Impact::qualityPenalty),
                Codec.BOOL.optionalFieldOf("can_kill_host", false).forGetter(Impact::canKillHost),
                Codec.INT.optionalFieldOf("from_stage", 1).forGetter(Impact::fromStage),
                Codec.INT.optionalFieldOf("to_stage", 9).forGetter(Impact::toStage),
                Codec.DOUBLE.optionalFieldOf("benefit", 0.0).forGetter(Impact::benefit)
        ).apply(i, Impact::new));
    }

    /**
     * @param rotationBreakYears years without a host crop needed to starve it out
     * @param tillageReduction   fraction of inoculum buried/destroyed by ploughing
     * @param resistantVarieties whether resistant cultivars exist (for future seed genetics)
     * @param methods            free-text management practices (for the field guide)
     */
    public record Control(int rotationBreakYears, double tillageReduction, boolean resistantVarieties, List<String> methods) {
        public static final Codec<Control> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("rotation_break_years", 0).forGetter(Control::rotationBreakYears),
                Codec.DOUBLE.optionalFieldOf("tillage_reduction", 0.0).forGetter(Control::tillageReduction),
                Codec.BOOL.optionalFieldOf("resistant_varieties", false).forGetter(Control::resistantVarieties),
                Codec.STRING.listOf().optionalFieldOf("methods", List.of()).forGetter(Control::methods)
        ).apply(i, Control::new));
    }

    public static final Codec<Affliction> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(Affliction::name),
            Codec.STRING.optionalFieldOf("scientific_name", "").forGetter(Affliction::scientificName),
            Category.CODEC.fieldOf("category").forGetter(Affliction::category),
            Codec.BOOL.optionalFieldOf("beneficial", false).forGetter(Affliction::beneficial),
            Codec.STRING.listOf().fieldOf("hosts").forGetter(Affliction::hosts),
            Transmission.CODEC.listOf().optionalFieldOf("transmission", List.of()).forGetter(Affliction::transmission),
            Envelope.CODEC.fieldOf("envelope").forGetter(Affliction::envelope),
            Lifecycle.CODEC.optionalFieldOf("lifecycle", new Lifecycle(7, 0.2, "residue", 1.0)).forGetter(Affliction::lifecycle),
            Impact.CODEC.optionalFieldOf("impact", new Impact(0, 0, 0, false, 1, 9, 0)).forGetter(Affliction::impact),
            Control.CODEC.optionalFieldOf("control", new Control(0, 0, false, List.of())).forGetter(Affliction::control),
            Codec.STRING.listOf().optionalFieldOf("symptoms", List.of()).forGetter(Affliction::symptoms),
            Codec.STRING.optionalFieldOf("notes", "").forGetter(Affliction::notes)
    ).apply(i, Affliction::new));

    /**
     * Host entries are crop ids ("potato"), botanical families ("family:solanaceae") or "*".
     */
    public boolean infects(CropSpecies species) {
        for (String h : hosts) {
            if (h.equals("*") || h.equals(species.id) || h.equals("tilth:" + species.id)
                    || h.equalsIgnoreCase("family:" + species.family.name())) {
                return true;
            }
        }
        return false;
    }
}
