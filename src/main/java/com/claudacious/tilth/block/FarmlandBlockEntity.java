package com.claudacious.tilth.block;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.affliction.AfflictionEngine;
import com.claudacious.tilth.registry.TilthBlockEntities;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.sim.FieldSimulator;
import com.claudacious.tilth.sim.Noise;
import com.claudacious.tilth.sim.calendar.CalendarMath;
import com.claudacious.tilth.sim.catena.CatenaAnalyzer;
import com.claudacious.tilth.sim.catena.SoilProfile;
import com.claudacious.tilth.sim.climate.BiomeClimate;
import com.claudacious.tilth.sim.climate.DailyWeather;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.CropState;
import com.claudacious.tilth.sim.soil.Hydraulics;
import com.claudacious.tilth.sim.soil.SoilChemistry;
import com.claudacious.tilth.sim.soil.SoilState;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.sim.soil.Tillage;
import com.claudacious.tilth.world.ClimateService;
import com.claudacious.tilth.world.SoilService;
import com.claudacious.tilth.world.TerrainService;
import com.claudacious.tilth.world.TilthCalendar;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** Soil state of one block of farmland and the driver of the daily field simulation. */
public class FarmlandBlockEntity extends BlockEntity {
    private static final int FLOWER_RADIUS = 6;
    private static final int BEE_RADIUS = 12;
    private SoilState soil = new SoilState();
    private boolean initialized;
    private double simDay;
    private double slopeDegrees;
    private double terrainCheckedDay = Double.NEGATIVE_INFINITY;
    private int bareDays;
    private double lastPonding;
    private double lastIrrigation;
    /** Weed pressure, 0 clean .. 1 overrun (see {@link com.claudacious.tilth.sim.crop.WeedModel}). */
    private double weeds;
    private boolean greenhouse;

    private record PollinationSnapshot(double habitatScore, double beeScore) {
        private static final PollinationSnapshot NONE = new PollinationSnapshot(0, 0);
    }

    public FarmlandBlockEntity(BlockPos pos, BlockState state) {
        super(TilthBlockEntities.FARMLAND.get(), pos, state);
    }

    public SoilTexture texture() {
        return getBlockState().getBlock() instanceof SoilLike s ? s.texture() : SoilTexture.LOAM;
    }

    public SoilState soil() {
        return soil;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int bareDays() {
        return bareDays;
    }

    public double slopeDegrees() {
        return slopeDegrees;
    }

    public double lastPonding() {
        return lastPonding;
    }

    public double weeds() {
        return weeds;
    }

    /** Whether the field is under a glass roof (a greenhouse), as of its last simulated day. */
    public boolean greenhouse() {
        return greenhouse;
    }

    /** Hoeing or tillage: the weeds are cut out or buried. */
    public void clearWeeds() {
        if (weeds > 0) {
            weeds = 0;
            setChanged();
        }
    }

    public double lastIrrigation() {
        return lastIrrigation;
    }

    public double simDay() {
        return simDay;
    }

    /**
     * Derives the starting soil state the first time the block is used: remembered state if this
     * spot was farmed before, otherwise the undisturbed baseline for its texture, horizon and
     * terrain position, then applies the tillage pass that created it.
     *
     * @return a warning for the player (e.g. working wet clay), or null
     */
    public String ensureInitialized(ServerLevel level) {
        if (initialized) {
            return null;
        }
        PendingTill.Entry entry = PendingTill.take(level, worldPosition);
        SoilState remembered = SoilMemory.recall(level, worldPosition);
        soil = remembered != null ? remembered : SoilService.baseline(level, worldPosition, texture(), entry.horizon());
        simDay = TilthCalendar.realDay(level);
        initialized = true;
        refreshTerrain(level, true);
        String warning = Tillage.apply(entry.implement(), soil, texture(), hydraulics());
        setChanged();
        return warning;
    }

    public Hydraulics hydraulics() {
        return texture().hydraulics(soil.organicMatter);
    }

    /** A new tillage pass over existing farmland. */
    public void retill(ServerLevel level, Tillage.Implement implement, @Nullable Player player) {
        advance(level);
        String warning = Tillage.apply(implement, soil, texture(), hydraulics());
        weeds = 0;
        if (warning != null && player != null) {
            player.displayClientMessage(Component.literal(warning).withStyle(ChatFormatting.GOLD), true);
        }
        setChanged();
    }

    public void compact(double load) {
        if (level instanceof ServerLevel server) {
            ensureInitialized(server);
            Tillage.traffic(soil, hydraulics(), load);
            setChanged();
        }
    }

    /** Catches the soil (and any crop on it) up to the current calendar day. */
    public void advance(ServerLevel level) {
        advance(level, true);
    }

    /**
     * Catches soil up to today while excluding the crop currently above this farmland.
     * Used before a newly placed crop should begin accumulating growth.
     */
    public void advanceSoilOnly(ServerLevel level) {
        advance(level, false);
        if (bareDays != 0) {
            bareDays = 0;
            setChanged();
        }
    }

    private void advance(ServerLevel level, boolean includeCrop) {
        ensureInitialized(level);
        double target = TilthCalendar.realDay(level);
        int maxCatchUp = TilthConfig.MAX_CATCH_UP_DAYS.getAsInt();
        if (target - simDay > maxCatchUp) {
            simDay = target - maxCatchUp;
        }
        if (target - simDay < 1.0) {
            return;
        }
        simulate(level, target, includeCrop);
    }

    /** Debug: simulate {@code days} extra days immediately, borrowing the weather of the days ahead. */
    public void simulateExtraDays(ServerLevel level, int days) {
        advance(level);
        simulate(level, simDay + days, true);
    }

    private void simulate(ServerLevel level, double target, boolean includeCrop) {
        CropBlockEntity crop = includeCrop && level.getBlockEntity(worldPosition.above()) instanceof CropBlockEntity c ? c : null;
        CropSpecies species = crop != null ? crop.species() : null;
        CropState cropState = crop != null ? crop.cropState() : null;
        if (crop != null) {
            crop.markInitialized();
            cropState.lightFactor = crop.lightFactor(level);
        }
        refreshTerrain(level, false);
        BiomeClimate climate = ClimateService.climateAt(level, worldPosition);
        FieldSimulator.Inputs inputs = computeInputs(level, species);
        lastIrrigation = inputs.irrigation();
        FieldSimulator.AfflictionHook hook = crop != null ? AfflictionEngine.hook(level, worldPosition, soil) : null;
        PollinationSnapshot pollination = crop != null ? pollinationSnapshot(level) : PollinationSnapshot.NONE;
        SoilTexture tex = texture();
        greenhouse = underGlass(level);
        Hydraulics h = hydraulics();
        boolean cover = species != null && com.claudacious.tilth.sim.crop.CoverCrops.isCover(species);
        double baseLight = cropState != null ? cropState.lightFactor : 1.0;
        while (simDay + 1.0 <= target) {
            long day = (long) Math.floor(simDay);
            if (cropState != null) {
                cropState.pollinationToday = pollinationService(day, target, pollination);
            }
            DailyWeather weather = ClimateService.weather(level, worldPosition, climate, day);
            if (greenhouse) {
                weather = com.claudacious.tilth.sim.crop.WeedModel.underGlass(weather);
            }
            // Weeds come up, shaded out by the crop's canopy, and take a share of light, water and nitrogen.
            double canopy = cropState != null && cropState.germinated ? Math.min(1.0, cropState.dev * 1.6) : 0.0;
            double relMoisture = (soil.moisture - h.wiltingPoint()) / Math.max(0.01, h.fieldCapacity() - h.wiltingPoint());
            weeds = com.claudacious.tilth.sim.crop.WeedModel.grow(weeds, weather.tMean(), relMoisture, soil.nitrogen, soil.residue, canopy, cover);
            double keep = com.claudacious.tilth.sim.crop.WeedModel.competition(weeds);
            if (cropState != null) {
                cropState.lightFactor = baseLight * keep;
            }
            soil.nitrogen *= 1.0 - 0.03 * (1.0 - keep);
            soil.moisture -= 0.006 * (1.0 - keep);
            FieldSimulator.Report report = FieldSimulator.step(soil, tex, climate, weather, inputs, species, cropState, hook);
            lastPonding = report.water().ponding();
            simDay += 1.0;
            bareDays = crop == null ? bareDays + 1 : 0;
        }
        updateMoistureState(level);
        setChanged();
        if (crop != null) {
            crop.afterSimulation(level);
        }
    }

    /** A glass roof within eight blocks straight above the crop: the field is a greenhouse. */
    private boolean underGlass(ServerLevel level) {
        BlockPos.MutableBlockPos m = worldPosition.mutable();
        for (int dy = 2; dy <= 9; dy++) {
            m.setY(worldPosition.getY() + dy);
            BlockState above = level.getBlockState(m);
            if (above.isAir()) {
                continue;
            }
            return above.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS) || above.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_PANES);
        }
        return false;
    }

    private void refreshTerrain(ServerLevel level, boolean force) {
        double today = TilthCalendar.realDay(level);
        if (!force && today - terrainCheckedDay < 60) {
            return;
        }
        terrainCheckedDay = today;
        CatenaAnalyzer.Result catena = TerrainService.analyze(level, worldPosition);
        slopeDegrees = catena.slopeDegrees();
        SoilProfile profile = SoilService.profile(level, worldPosition, catena);
        double depth = profile.drainage().waterTableDepth() + 0.15 * Math.max(0, catena.localRelief());
        if (TerrainService.waterWithin(level, worldPosition, 6, 1)) {
            depth = Math.min(depth, 0.5);
        }
        if (TerrainService.waterWithin(level, worldPosition, 2, 0)) {
            depth = Math.min(depth, 0.2);
        }
        soil.waterTableDepth = Noise.clamp(depth, 0, 15);
    }

    private FieldSimulator.Inputs computeInputs(ServerLevel level, @Nullable CropSpecies species) {
        SoilTexture tex = texture();
        int reach = tex.irrigationReach();
        double best = Double.MAX_VALUE;
        boolean adjacent = false;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    m.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    if (!level.isLoaded(m)) {
                        continue;
                    }
                    boolean water = level.getFluidState(m).is(FluidTags.WATER) || level.getBlockState(m).is(TilthBlocks.PUDDLE.get());
                    if (water) {
                        double d = Math.sqrt(dx * dx + dz * dz);
                        best = Math.min(best, d);
                        if (d <= 1.01 && dy >= 0) {
                            adjacent = true;
                        }
                    }
                }
            }
        }
        double irrigation = best <= reach ? Noise.clamp(1.0 - best / (reach + 1.0), 0, 1) : 0;
        boolean flooded = adjacent && species != null && species.needsFlooding;
        double slopeFactor = 1.0 + slopeDegrees / 12.0;
        double capillary = 0.5 + tex.silt() / 100.0 + (tex.isOrganic() ? 0.5 : 0);
        return new FieldSimulator.Inputs(irrigation, flooded, slopeFactor, capillary);
    }

    private PollinationSnapshot pollinationSnapshot(ServerLevel level) {
        if (!TilthConfig.POLLINATION.getAsBoolean()) {
            return PollinationSnapshot.NONE;
        }
        double flowers = flowerHabitat(level);
        AABB box = new AABB(worldPosition).inflate(BEE_RADIUS, 4, BEE_RADIUS);
        int bees = level.getEntitiesOfClass(Bee.class, box, bee -> bee.isAlive() && !bee.isBaby() && !bee.isNoAi() && !bee.isInWaterRainOrBubble()).size();
        double beeScore = Noise.clamp(bees / 3.0, 0, 1);
        return new PollinationSnapshot(flowers, beeScore);
    }

    private double pollinationService(long day, double target, PollinationSnapshot snapshot) {
        if (!TilthConfig.POLLINATION.getAsBoolean()) {
            return 1.0;
        }
        double service = snapshot.habitatScore();
        long lastFullyElapsedDay = (long) Math.floor(target) - 1;
        // Do not project today's bee count backwards across catch-up days after a chunk was unloaded.
        if (day >= lastFullyElapsedDay) {
            service = pollinationCombined(snapshot.habitatScore(), snapshot.beeScore());
        }
        return service;
    }

    private double flowerHabitat(ServerLevel level) {
        int flowers = 0;
        int samples = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -FLOWER_RADIUS; dx <= FLOWER_RADIUS; dx++) {
                for (int dz = -FLOWER_RADIUS; dz <= FLOWER_RADIUS; dz++) {
                    m.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    if (!level.isLoaded(m)) {
                        continue;
                    }
                    samples++;
                    if (level.getBlockState(m).is(BlockTags.FLOWERS)) {
                        flowers++;
                    }
                }
            }
        }
        if (samples <= 0) {
            return 0;
        }
        return Noise.clamp(flowers / 12.0, 0, 1);
    }

    private static double pollinationCombined(double habitat, double bees) {
        return Noise.clamp(0.55 * habitat + 0.55 * bees + 0.2 * Math.min(habitat, bees), 0, 1);
    }

    private void updateMoistureState(ServerLevel level) {
        Hydraulics h = hydraulics();
        double rel = Noise.clamp((soil.moisture - h.wiltingPoint()) / Math.max(0.01, h.fieldCapacity() - h.wiltingPoint()), 0, 1);
        int moisture = (int) Math.round(7 * rel);
        boolean soggy = soil.moisture >= h.saturation() - 0.03 || soil.waterloggedDays > 0;
        BlockState state = getBlockState();
        if (state.hasProperty(TilthFarmlandBlock.MOISTURE)
                && (state.getValue(TilthFarmlandBlock.MOISTURE) != moisture || state.getValue(TilthFarmlandBlock.SOGGY) != soggy)) {
            level.setBlock(worldPosition, state.setValue(TilthFarmlandBlock.MOISTURE, moisture).setValue(TilthFarmlandBlock.SOGGY, soggy),
                    Block.UPDATE_CLIENTS);
        }
    }

    /** Records a harvest: the crop joins the field history and its residue stays on the field. */
    public void recordHarvest(ServerLevel level, CropSpecies species, double residueKgM2, double residueNPercent) {
        ensureInitialized(level);
        soil.recordCrop(species.id, CalendarMath.year(TilthCalendar.realDay(level)));
        soil.addResidue(residueKgM2, residueNPercent);
        SoilChemistry.clamp(soil);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("soil", SoilNbt.write(soil));
        tag.putBoolean("initialized", initialized);
        tag.putDouble("sim_day", simDay);
        tag.putDouble("slope", slopeDegrees);
        tag.putDouble("terrain_day", terrainCheckedDay);
        tag.putInt("bare_days", bareDays);
        tag.putDouble("weeds", weeds);
        tag.putBoolean("greenhouse", greenhouse);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("soil")) {
            soil = SoilNbt.read(tag.getCompound("soil"));
        }
        initialized = tag.getBoolean("initialized");
        simDay = tag.getDouble("sim_day");
        slopeDegrees = tag.getDouble("slope");
        terrainCheckedDay = tag.contains("terrain_day") ? tag.getDouble("terrain_day") : Double.NEGATIVE_INFINITY;
        bareDays = tag.getInt("bare_days");
        weeds = tag.getDouble("weeds");
        greenhouse = tag.getBoolean("greenhouse");
    }
}
