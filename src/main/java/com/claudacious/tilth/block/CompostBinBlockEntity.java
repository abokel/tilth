package com.claudacious.tilth.block;

import com.claudacious.tilth.registry.TilthBlockEntities;
import com.claudacious.tilth.registry.TilthItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A compost heap. "Greens" (manure, cut grass, spoiled grain and hay, crop scraps) bring nitrogen; "browns" (straw,
 * dried hay, leaves, sticks) bring carbon. A heap rots fastest at about two parts brown to one part green and gets
 * faster each time it is turned with a shovel (up to three times). Ready compost is half the volume that went in.
 */
public class CompostBinBlockEntity extends BlockEntity {
    public static final int CAPACITY = 64;
    private int green;
    private int brown;
    private double progress;
    private int turns;
    private long lastTick = Long.MIN_VALUE;

    public CompostBinBlockEntity(BlockPos pos, BlockState state) {
        super(TilthBlockEntities.COMPOST_BIN.get(), pos, state);
    }

    /** 1 for a green, -1 for a brown, 0 for anything that does not belong in a heap. */
    public static int kind(ItemStack stack) {
        if (stack.is(TilthItems.MANURE.get()) || stack.is(TilthItems.SPOILED_GRAIN.get()) || stack.is(TilthItems.CUT_GRASS.get())
                || stack.is(TilthItems.MOULDY_HAY.get()) || stack.is(Items.ROTTEN_FLESH) || stack.is(Items.SHORT_GRASS)
                || stack.is(Items.FERN) || stack.is(Items.KELP) || stack.is(Items.VINE) || stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)
                || stack.is(Items.APPLE) || stack.is(Items.POTATO) || stack.is(Items.CARROT) || stack.is(Items.BEETROOT)) {
            return 1;
        }
        if (stack.is(TilthItems.STRAW.get()) || stack.is(TilthItems.DRIED_HAY.get()) || stack.is(ItemTags.LEAVES)
                || stack.is(ItemTags.SAPLINGS) || stack.is(Items.STICK) || stack.is(Items.PAPER) || stack.is(Items.DEAD_BUSH)) {
            return -1;
        }
        return 0;
    }

    public int total() {
        return green + brown;
    }

    public boolean ready() {
        return total() > 0 && progress >= 1.0;
    }

    /** Game days a heap of this mix takes, before turning. */
    public double daysToFinish() {
        if (total() == 0) {
            return 0;
        }
        double ratio = brown / (double) Math.max(1, green);
        // Two browns to a green is best; all-green heaps go slimy and slow, all-brown heaps barely rot.
        double off = Math.abs(Math.log(Math.max(0.1, ratio) / 2.0));
        return (3.0 + 4.0 * Math.min(1.5, off)) * Math.pow(0.8, turns);
    }

    public String describe() {
        if (total() == 0) {
            return "Empty compost bin. Add greens (manure, cut grass, scraps) and browns (straw, leaves, dried hay).";
        }
        if (ready()) {
            return "Ready: " + compostYield() + " compost. Right-click with an empty hand to dig it out.";
        }
        String mix = brown < green ? "too green: add straw or leaves" : brown > green * 4 ? "too brown: add manure or cut grass" : "a good mix";
        return String.format("Compost %.0f%% done, %d greens and %d browns (%s), turned %d times.", progress * 100, green, brown, mix, turns);
    }

    public int compostYield() {
        return Math.max(1, total() / 2);
    }

    public int add(ItemStack stack) {
        int k = kind(stack);
        if (k == 0 || ready()) {
            return 0;
        }
        int moved = Math.min(stack.getCount(), CAPACITY - total());
        if (moved <= 0) {
            return 0;
        }
        if (k > 0) {
            green += moved;
        } else {
            brown += moved;
        }
        // Fresh material sets the heap back a little.
        progress = Math.max(0, progress - 0.1 * moved / (double) CAPACITY);
        stack.shrink(moved);
        changed();
        return moved;
    }

    public boolean turn() {
        if (total() == 0 || ready() || turns >= 3) {
            return false;
        }
        turns++;
        changed();
        return true;
    }

    public ItemStack takeCompost() {
        if (!ready()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = new ItemStack(TilthItems.COMPOST.get(), compostYield());
        green = 0;
        brown = 0;
        progress = 0;
        turns = 0;
        changed();
        return out;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            int fill = total() == 0 ? 0 : 1 + Math.min(3, total() * 3 / CAPACITY);
            BlockState next = state.setValue(CompostBinBlock.FILL, fill).setValue(CompostBinBlock.READY, ready());
            if (next != state) {
                level.setBlock(worldPosition, next, 3);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CompostBinBlockEntity bin) {
        if ((level.getGameTime() + pos.asLong()) % 100 != 0) {
            return;
        }
        long now = level.getGameTime();
        if (bin.lastTick == Long.MIN_VALUE || bin.total() == 0 || bin.ready()) {
            bin.lastTick = now;
            return;
        }
        double days = (now - bin.lastTick) / 24000.0;
        bin.lastTick = now;
        bin.progress = Math.min(1.0, bin.progress + days / Math.max(0.5, bin.daysToFinish()));
        bin.changed();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Green", green);
        tag.putInt("Brown", brown);
        tag.putDouble("Progress", progress);
        tag.putInt("Turns", turns);
        if (lastTick != Long.MIN_VALUE) {
            tag.putLong("LastTick", lastTick);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        green = tag.getInt("Green");
        brown = tag.getInt("Brown");
        progress = tag.getDouble("Progress");
        turns = tag.getInt("Turns");
        lastTick = tag.contains("LastTick") ? tag.getLong("LastTick") : Long.MIN_VALUE;
    }
}
