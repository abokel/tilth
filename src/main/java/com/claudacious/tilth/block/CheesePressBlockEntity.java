package com.claudacious.tilth.block;

import com.claudacious.tilth.quality.Quality;
import com.claudacious.tilth.registry.TilthBlockEntities;
import com.claudacious.tilth.registry.TilthItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cheese press: up to four buckets of milk set into curd and age into cheese over three game days. Richer milk
 * makes more and better cheese; the grade of the batch is the average of the milk that went in.
 */
public class CheesePressBlockEntity extends BlockEntity {
    public static final int MAX_MILK = 4;
    public static final double AGE_DAYS = 3.0;
    private int milk;
    private int gradeSum;
    private long started = -1;

    public CheesePressBlockEntity(BlockPos pos, BlockState state) {
        super(TilthBlockEntities.CHEESE_PRESS.get(), pos, state);
    }

    public boolean addMilk(ItemStack bucket) {
        if (milk >= MAX_MILK || ready()) {
            return false;
        }
        milk++;
        gradeSum += Quality.get(bucket) == 0 ? 3 : Quality.get(bucket);
        if (level != null) {
            started = level.getGameTime();
        }
        setChanged();
        return true;
    }

    public boolean ready() {
        return milk > 0 && level != null && started >= 0 && level.getGameTime() - started >= AGE_DAYS * 24000;
    }

    public int milk() {
        return milk;
    }

    public String describe() {
        if (milk == 0) {
            return "Empty cheese press. Pour in up to four buckets of milk.";
        }
        if (ready()) {
            return "The cheese is ready: right-click with an empty hand.";
        }
        double left = AGE_DAYS - (level.getGameTime() - started) / 24000.0;
        return String.format("%d buckets of milk pressing; cheese in %.1f days.", milk, Math.max(0, left));
    }

    public ItemStack take() {
        if (!ready()) {
            return ItemStack.EMPTY;
        }
        int grade = Math.round(gradeSum / (float) milk);
        ItemStack cheese = new ItemStack(TilthItems.CHEESE.get(), milk * (1 + grade / 3));
        Quality.set(cheese, grade);
        milk = 0;
        gradeSum = 0;
        started = -1;
        setChanged();
        return cheese;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Milk", milk);
        tag.putInt("GradeSum", gradeSum);
        tag.putLong("Started", started);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        milk = tag.getInt("Milk");
        gradeSum = tag.getInt("GradeSum");
        started = tag.contains("Started") ? tag.getLong("Started") : -1;
    }
}
