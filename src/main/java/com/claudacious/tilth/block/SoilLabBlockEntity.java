package com.claudacious.tilth.block;

import com.claudacious.tilth.item.SoilSampleItem;
import com.claudacious.tilth.registry.TilthBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds one sample while it is analysed, then the finished report. */
public class SoilLabBlockEntity extends BlockEntity {
    /** Twenty seconds: titrations and extractions take a while. */
    public static final int ANALYSIS_TICKS = 400;

    private ItemStack sample = ItemStack.EMPTY;
    private ItemStack report = ItemStack.EMPTY;
    /** The finished analysis as numbers, for filing into a soil survey. */
    private CompoundTag result = new CompoundTag();
    private int progress;

    public SoilLabBlockEntity(BlockPos pos, BlockState state) {
        super(TilthBlockEntities.SOIL_LAB.get(), pos, state);
    }

    public boolean isEmpty() {
        return sample.isEmpty() && report.isEmpty();
    }

    public int progressPercent() {
        return progress * 100 / ANALYSIS_TICKS;
    }

    public void insert(ItemStack stack) {
        sample = stack;
        progress = 0;
        setWorking(true);
        setChanged();
    }

    public boolean hasResult() {
        return !report.isEmpty();
    }

    public CompoundTag result() {
        return result;
    }

    public ItemStack takeReport() {
        ItemStack out = report;
        report = ItemStack.EMPTY;
        result = new CompoundTag();
        setChanged();
        return out;
    }

    public ItemStack contents() {
        return !report.isEmpty() ? report : sample;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SoilLabBlockEntity lab) {
        if (lab.sample.isEmpty()) {
            return;
        }
        if (++lab.progress >= ANALYSIS_TICKS) {
            lab.report = SoilSampleItem.report(lab.sample, level.registryAccess());
            lab.result = SoilSampleItem.measurements(lab.sample);
            lab.sample = ItemStack.EMPTY;
            lab.progress = 0;
            lab.setWorking(false);
            level.playSound(null, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        lab.setChanged();
    }

    private void setWorking(boolean working) {
        if (level != null && getBlockState().getValue(SoilLabBlock.WORKING) != working) {
            level.setBlock(worldPosition, getBlockState().setValue(SoilLabBlock.WORKING, working), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!sample.isEmpty()) {
            tag.put("Sample", sample.save(registries));
        }
        if (!report.isEmpty()) {
            tag.put("Report", report.save(registries));
        }
        tag.putInt("Progress", progress);
        tag.put("Result", result);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sample = ItemStack.parseOptional(registries, tag.getCompound("Sample"));
        report = ItemStack.parseOptional(registries, tag.getCompound("Report"));
        progress = tag.getInt("Progress");
        result = tag.getCompound("Result");
    }
}
