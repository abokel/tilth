package com.claudacious.tilth.postharvest;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.block.GrainSiloBlockEntity;
import com.claudacious.tilth.registry.TilthAttachments;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.postharvest.GrainSpoilageModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Grain kept outside a silo: in chests, barrels and other containers, and in players' inventories. Wet grain goes
 * mouldy (into Spoiled Grain) as it would in a silo, but at the full open-storage rate, and containers also lose a
 * little grain every day to rodents and weevils.
 *
 * <p>Nothing runs while a chest sits closed. Each container remembers when its grain was last aged, and the losses
 * since then are worked out when someone next opens it, so storage costs nothing between visits.
 */
public final class GrainKeeping {
    private static final int PLAYER_INTERVAL = 1200;

    private GrainKeeping() {
    }

    /** Rough storage temperature in Celsius from the biome: warm stores spoil faster. */
    public static double storageTemperature(Level level, BlockPos pos) {
        return level.getBiome(pos).value().getModifiedClimateSettings().temperature() * 25.0 + 5.0;
    }

    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        Level level = event.getEntity().level();
        if (level.isClientSide) {
            return;
        }
        Map<Container, Boolean> seen = new IdentityHashMap<>();
        for (Slot slot : event.getContainer().slots) {
            Container c = slot.container;
            if (c instanceof BlockEntity be && !(be instanceof GrainSiloBlockEntity) && seen.put(c, Boolean.TRUE) == null) {
                age(level, be.getBlockPos(), c, be, TilthConfig.OPEN_STORAGE_PEST_LOSS.getAsDouble());
                be.setChanged();
            }
        }
    }

    /** Grain carried about goes mouldy if wet; rodents are not a worry in a pocket. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || (player.tickCount + player.getId()) % PLAYER_INTERVAL != 0) {
            return;
        }
        age(player.level(), player.blockPosition(), player.getInventory(), player, 0.0);
    }

    private static void age(Level level, BlockPos pos, Container container, IAttachmentHolder holder, double pests) {
        long now = level.getGameTime();
        long last = holder.getData(TilthAttachments.GRAIN_AGED.get());
        holder.setData(TilthAttachments.GRAIN_AGED.get(), now);
        if (last < 0 || now <= last) {
            return;
        }
        double days = (now - last) / 24000.0;
        double safe = TilthConfig.GRAIN_SAFE_STORAGE_MOISTURE.getAsDouble();
        double temperature = storageTemperature(level, pos);
        int spoiledTotal = 0;
        boolean changed = false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!GrainStacks.isStorableGrain(stack)) {
                continue;
            }
            double mould = GrainSpoilageModel.dailySpoilageFraction(GrainStacks.moisture(stack), temperature, safe)
                    * TilthConfig.GRAIN_SPOILAGE_RATE_MULTIPLIER.getAsDouble();
            int count = stack.getCount();
            int mouldy = round(level, count * (1 - GrainSpoilageModel.survival(mould, days)));
            int eaten = round(level, (count - mouldy) * (1 - GrainSpoilageModel.survival(pests, days)));
            int lost = Math.min(count, mouldy + eaten);
            if (lost > 0) {
                stack.shrink(lost);
                spoiledTotal += Math.min(lost, mouldy);
                changed = true;
            }
        }
        // Mouldy grain stays in the container as Spoiled Grain where there is room; the rest rots away.
        for (int i = 0; i < container.getContainerSize() && spoiledTotal > 0; i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                int n = Math.min(64, spoiledTotal);
                container.setItem(i, new ItemStack(TilthItems.SPOILED_GRAIN.get(), n));
                spoiledTotal -= n;
            } else if (stack.is(TilthItems.SPOILED_GRAIN.get()) && stack.getCount() < stack.getMaxStackSize()) {
                int n = Math.min(stack.getMaxStackSize() - stack.getCount(), spoiledTotal);
                stack.grow(n);
                spoiledTotal -= n;
            }
        }
        if (changed) {
            container.setChanged();
        }
    }

    /** Whole items for a fractional expected loss: the fraction is rounded up with matching probability. */
    private static int round(Level level, double expected) {
        int whole = (int) Math.floor(expected);
        return whole + (level.random.nextDouble() < expected - whole ? 1 : 0);
    }
}
