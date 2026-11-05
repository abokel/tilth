package com.claudacious.tilth.block;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.postharvest.GrainStacks;
import com.claudacious.tilth.registry.TilthBlockEntities;
import com.claudacious.tilth.sim.postharvest.GrainDryingModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DryingRackBlockEntity extends BlockEntity {
    private static final int MAX_STACK = 16;
    private static final int STEP_TICKS = 20;

    private ItemStack stack = ItemStack.EMPTY;

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(TilthBlockEntities.DRYING_RACK.get(), pos, state);
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public ItemStack contents() {
        return stack;
    }

    public int insert(ItemStack input) {
        if (!GrainStacks.isStorableGrain(input)) {
            return 0;
        }
        if (!stack.isEmpty() && !GrainStacks.sameProfile(stack, input)) {
            return 0;
        }
        int room = MAX_STACK - stack.getCount();
        if (room <= 0) {
            return 0;
        }
        int moved = Math.min(room, input.getCount());
        float inMoisture = GrainStacks.moisture(input);
        if (stack.isEmpty()) {
            stack = GrainStacks.profileOf(input);
            stack.setCount(moved);
            GrainStacks.setMoisture(stack, inMoisture);
        } else {
            float current = GrainStacks.moisture(stack);
            int total = stack.getCount() + moved;
            float blended = (current * stack.getCount() + inMoisture * moved) / total;
            stack.grow(moved);
            GrainStacks.setMoisture(stack, blended);
        }
        setLoadedState();
        setChanged();
        return moved;
    }

    public ItemStack takeAll() {
        ItemStack out = stack;
        stack = ItemStack.EMPTY;
        setLoadedState();
        setChanged();
        return out;
    }

    public float moisturePercent() {
        return 100.0f * GrainStacks.moisture(stack);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity rack) {
        if (rack.stack.isEmpty() || level.getGameTime() % STEP_TICKS != 0) {
            return;
        }
        boolean exposed = level.canSeeSky(pos.above());
        boolean raining = exposed && level.isRainingAt(pos.above());
        float downfall = level.getBiome(pos).value().getModifiedClimateSettings().downfall();
        float humidity = Math.max(0.0f, Math.min(1.0f, downfall + (raining ? 0.3f : 0.0f)));
        double elapsedDays = STEP_TICKS / 24000.0;
        double current = GrainStacks.moisture(rack.stack);
        double next = GrainDryingModel.nextMoisture(current, humidity, raining, !exposed, elapsedDays,
                TilthConfig.GRAIN_DRYING_RATE_PER_DAY.getAsDouble());
        if (Math.abs(next - current) > 1.0e-4) {
            GrainStacks.setMoisture(rack.stack, (float) next);
            rack.setChanged();
        }
    }

    private void setLoadedState() {
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(DryingRackBlock.LOADED) && state.getValue(DryingRackBlock.LOADED) != !stack.isEmpty()) {
                level.setBlock(worldPosition, state.setValue(DryingRackBlock.LOADED, !stack.isEmpty()), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!stack.isEmpty()) {
            tag.put("Stack", stack.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stack = ItemStack.parseOptional(registries, tag.getCompound("Stack"));
        if (!stack.isEmpty() && !tag.getCompound("Stack").contains("components")) {
            GrainStacks.setMoisture(stack, (float) GrainDryingModel.DEFAULT_HARVEST_MOISTURE);
        }
    }
}
