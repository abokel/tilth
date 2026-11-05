package com.claudacious.tilth.block;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.postharvest.GrainKeeping;
import com.claudacious.tilth.postharvest.GrainStacks;
import com.claudacious.tilth.postharvest.SiloContents;
import com.claudacious.tilth.registry.TilthBlockEntities;
import com.claudacious.tilth.registry.TilthDataComponents;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.postharvest.GrainDryingModel;
import com.claudacious.tilth.sim.postharvest.GrainSpoilageModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Storage for a grain silo. Only the controller of a built silo (see {@link GrainSiloBlock}) holds grain in normal
 * use; a loose segment holds grain only when it was placed from the item of a broken, full controller.
 *
 * <p>In a built silo, aeration dries the grain to a safe moisture and spoilage runs at a fraction of the open-storage
 * rate, with no rodent losses. Grain in a loose segment keeps like grain in a chest.
 */
public class GrainSiloBlockEntity extends BlockEntity {
    private ItemStack profile = ItemStack.EMPTY;
    private int grain;
    private int spoiled;
    private double moisture = GrainDryingModel.DEFAULT_HARVEST_MOISTURE;
    private double carry;
    private long lastTick = Long.MIN_VALUE;
    private final IItemHandler handler = new Handler();

    public GrainSiloBlockEntity(BlockPos pos, BlockState state) {
        super(TilthBlockEntities.GRAIN_SILO.get(), pos, state);
    }

    private boolean built() {
        BlockState state = getBlockState();
        return state.getValue(GrainSiloBlock.FORMED) && state.getValue(GrainSiloBlock.QUAD) == 0 && state.getValue(GrainSiloBlock.TIER) == 0;
    }

    public int grainCount() {
        return grain;
    }

    public int spoiledCount() {
        return spoiled;
    }

    public int capacity() {
        return TilthConfig.GRAIN_SILO_CAPACITY.getAsInt();
    }

    public float moisturePercent() {
        return (float) (moisture * 100.0);
    }

    public String grainName() {
        return profile.isEmpty() ? "nothing" : profile.getHoverName().getString();
    }

    /** One line for the player: what is in it, how full, how wet, and whether it is keeping. */
    public String summary() {
        if (grain <= 0 && spoiled <= 0) {
            return "Silo: empty (holds " + capacity() + " grain).";
        }
        double safe = TilthConfig.GRAIN_SAFE_STORAGE_MOISTURE.getAsDouble();
        String state = moisture > safe ? String.format("%.1f%% moisture, drying by aeration", moisture * 100)
                : String.format("%.1f%% moisture, keeping well", moisture * 100);
        return String.format("Silo: %d / %d %s (%d%%), %s.%s", grain, capacity(), grainName(), 100 * grain / capacity(), state,
                spoiled > 0 ? " " + spoiled + " spoiled: sneak and right-click with an empty hand to clear it." : "");
    }

    public int insert(ItemStack input) {
        if (!GrainStacks.isStorableGrain(input) || input.isEmpty()) {
            return 0;
        }
        if (!profile.isEmpty() && grain > 0 && !GrainStacks.sameProfile(profile, input)) {
            return 0;
        }
        int moved = Math.min(capacity() - grain, input.getCount());
        if (moved <= 0) {
            return 0;
        }
        double inMoisture = GrainStacks.moisture(input);
        if (profile.isEmpty() || grain <= 0) {
            profile = GrainStacks.profileOf(input);
            moisture = inMoisture;
            carry = 0;
        } else {
            moisture = (moisture * grain + inMoisture * moved) / (grain + moved);
        }
        grain += moved;
        input.shrink(moved);
        changed();
        return moved;
    }

    public ItemStack extractGrain(int max) {
        if (grain <= 0 || profile.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int amount = Math.min(Math.min(max, grain), profile.getMaxStackSize());
        ItemStack out = profile.copyWithCount(amount);
        GrainStacks.setMoisture(out, (float) moisture);
        grain -= amount;
        if (grain <= 0) {
            clearGrain();
        }
        changed();
        return out;
    }

    public ItemStack extractSpoiled() {
        if (spoiled <= 0) {
            return ItemStack.EMPTY;
        }
        int amount = Math.min(64, spoiled);
        spoiled -= amount;
        changed();
        return new ItemStack(TilthItems.SPOILED_GRAIN.get(), amount);
    }

    private void clearGrain() {
        grain = 0;
        profile = ItemStack.EMPTY;
        moisture = GrainDryingModel.DEFAULT_HARVEST_MOISTURE;
        carry = 0;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    /** Moves this segment's grain into a silo's controller as the silo is built; what will not fit is dropped here. */
    void pourInto(GrainSiloBlockEntity controller, BlockPos at) {
        if (grain > 0) {
            ItemStack all = profile.copyWithCount(grain);
            GrainStacks.setMoisture(all, (float) moisture);
            controller.insert(all);
            while (!all.isEmpty() && level != null) {
                Containers.dropItemStack(level, at.getX(), at.getY() + 1, at.getZ(), all.split(all.getMaxStackSize()));
            }
        }
        controller.spoiled += spoiled;
        clearGrain();
        spoiled = 0;
        controller.changed();
        changed();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GrainSiloBlockEntity silo) {
        // Grain ages in the background once every 10 seconds; a silo is checked far less often than it is filled.
        if ((level.getGameTime() + pos.asLong()) % 200 != 0) {
            return;
        }
        long now = level.getGameTime();
        if (silo.lastTick == Long.MIN_VALUE || silo.grain <= 0) {
            silo.lastTick = now;
            return;
        }
        double days = Math.max(0, now - silo.lastTick) / 24000.0;
        silo.lastTick = now;
        boolean built = silo.built();
        double safe = TilthConfig.GRAIN_SAFE_STORAGE_MOISTURE.getAsDouble();
        double daily = GrainSpoilageModel.dailySpoilageFraction(silo.moisture, GrainKeeping.storageTemperature(level, pos), safe)
                * TilthConfig.GRAIN_SPOILAGE_RATE_MULTIPLIER.getAsDouble();
        double pests = 0;
        if (built) {
            daily *= TilthConfig.SILO_SPOILAGE_FACTOR.getAsDouble();
            silo.moisture = GrainSpoilageModel.aerate(silo.moisture, safe, TilthConfig.SILO_AERATION_PER_DAY.getAsDouble(), days);
        } else {
            pests = TilthConfig.OPEN_STORAGE_PEST_LOSS.getAsDouble();
        }
        double lostToMould = silo.grain * (1 - GrainSpoilageModel.survival(daily, days));
        double lostToPests = silo.grain * (1 - GrainSpoilageModel.survival(pests, days));
        double expected = lostToMould + lostToPests + silo.carry;
        int lost = (int) Math.min(silo.grain, Math.floor(expected));
        silo.carry = expected - lost;
        if (lost > 0) {
            int mouldy = (int) Math.round(lost * lostToMould / Math.max(1e-9, lostToMould + lostToPests));
            silo.grain -= lost;
            silo.spoiled += mouldy;
            if (silo.grain <= 0) {
                silo.clearGrain();
            }
        }
        silo.changed();
    }

    // ------------------------------------------------------------------ the dropped item carries the grain

    public SiloContents contents() {
        return new SiloContents(profile.isEmpty() ? ItemStack.EMPTY : profile.copyWithCount(1), grain, spoiled, (float) moisture);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (grain > 0 || spoiled > 0) {
            components.set(TilthDataComponents.SILO_CONTENTS.get(), contents());
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        SiloContents c = input.getOrDefault(TilthDataComponents.SILO_CONTENTS.get(), SiloContents.EMPTY);
        profile = c.profile().isEmpty() ? ItemStack.EMPTY : c.profile().copyWithCount(1);
        grain = profile.isEmpty() ? 0 : c.grain();
        spoiled = c.spoiled();
        moisture = c.moisture();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Contents");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (grain > 0 || spoiled > 0) {
            SiloContents.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), contents())
                    .ifSuccess(t -> tag.put("Contents", t));
        }
        tag.putDouble("Carry", carry);
        if (lastTick != Long.MIN_VALUE) {
            tag.putLong("LastTick", lastTick);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        SiloContents c = tag.contains("Contents")
                ? SiloContents.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("Contents")).result().orElse(SiloContents.EMPTY)
                : SiloContents.EMPTY;
        profile = c.profile().isEmpty() ? ItemStack.EMPTY : c.profile().copyWithCount(1);
        grain = profile.isEmpty() ? 0 : c.grain();
        spoiled = c.spoiled();
        moisture = c.moisture();
        carry = tag.getDouble("Carry");
        lastTick = tag.contains("LastTick") ? tag.getLong("LastTick") : Long.MIN_VALUE;
    }

    // ------------------------------------------------------------------ hoppers and pipes

    public IItemHandler itemHandler() {
        return handler;
    }

    /** One slot: grain goes in from any side and comes out as stacks. */
    private final class Handler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (grain <= 0 || profile.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack shown = profile.copyWithCount(Math.min(grain, profile.getMaxStackSize()));
            GrainStacks.setMoisture(shown, (float) moisture);
            return shown;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!built() || !isItemValid(slot, stack)) {
                return stack;
            }
            if (simulate) {
                int room = capacity() - grain;
                return room >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - Math.max(0, room));
            }
            ItemStack rest = stack.copy();
            insert(rest);
            return rest;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!built() || grain <= 0) {
                return ItemStack.EMPTY;
            }
            if (simulate) {
                ItemStack shown = getStackInSlot(slot);
                return shown.copyWithCount(Math.min(amount, shown.getCount()));
            }
            return extractGrain(amount);
        }

        @Override
        public int getSlotLimit(int slot) {
            return capacity();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return GrainStacks.isStorableGrain(stack) && (grain <= 0 || profile.isEmpty() || GrainStacks.sameProfile(profile, stack));
        }
    }
}
