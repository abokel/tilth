package com.claudacious.tilth.market;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.postharvest.GrainStacks;
import com.claudacious.tilth.quality.Quality;
import com.claudacious.tilth.sim.calendar.CalendarMath;
import com.claudacious.tilth.sim.market.MarketModel;
import com.claudacious.tilth.world.TilthCalendar;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The world's farm market: one price index per commodity group, moved each day by the season and by events that are
 * announced to everyone. Market stalls and the travelling farm merchant buy at these prices, paying in coins
 * ({@link Coins}); fractions of a penny are kept as credit for the seller's next sale. Prices are reckoned in
 * shillings.
 */
public final class Market extends SavedData {
    private static final String NAME = "tilth_market";
    private static final SavedData.Factory<Market> FACTORY = new SavedData.Factory<>(Market::new, Market::load);

    private final double[] index = new double[MarketModel.GROUPS];
    private final double[] yesterday = new double[MarketModel.GROUPS];
    private final List<Active> events = new ArrayList<>();
    private final Map<UUID, Double> credit = new HashMap<>();
    private long day = -1;

    private record Active(String id, long until) {
    }

    private Market() {
        java.util.Arrays.fill(index, 1.0);
        java.util.Arrays.fill(yesterday, 1.0);
    }

    public static Market get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static int month(ServerLevel level) {
        return CalendarMath.month(CalendarMath.dayOfYear(TilthCalendar.realDay(level)));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().overworld();
        if (overworld.getGameTime() % 100 != 0 || !TilthConfig.MARKET.getAsBoolean()) {
            return;
        }
        Market m = get(event.getServer());
        long today = overworld.getDayTime() / 24000L;
        if (m.day < 0) {
            m.day = today;
            m.setDirty();
            return;
        }
        while (m.day < today) {
            m.day++;
            m.advance(event.getServer(), month(overworld), new Random(overworld.getSeed() ^ (m.day * 0x9E3779B97F4A7C15L)));
        }
    }

    private void advance(MinecraftServer server, int month, Random random) {
        System.arraycopy(index, 0, yesterday, 0, index.length);
        for (int g = 0; g < MarketModel.GROUPS; g++) {
            index[g] = MarketModel.step(index[g], g, month, random);
        }
        events.removeIf(a -> a.until < day);
        MarketModel.Event e = MarketModel.roll(month, random);
        if (e != null && events.stream().noneMatch(a -> a.id.equals(e.id()))) {
            events.add(new Active(e.id(), day + e.days()));
            server.getPlayerList().broadcastSystemMessage(Component.literal("[Market] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(e.headline()).withStyle(ChatFormatting.YELLOW)), false);
        }
        setDirty();
    }

    private double eventMultiplier(int group) {
        double m = 1.0;
        for (Active a : events) {
            for (MarketModel.Event e : MarketModel.EVENTS) {
                if (e.id().equals(a.id) && e.affects(group)) {
                    m *= e.multiplier();
                }
            }
        }
        return m;
    }

    /** What one of this item fetches today, in shillings, or 0 if the market does not buy it. */
    public double unitPrice(ItemStack stack) {
        Commodity.Quote q = Commodity.quote(stack);
        if (q == null) {
            return 0;
        }
        int g = q.commodity().ordinal();
        double p = MarketModel.price(q.base(), index[g], eventMultiplier(g), Quality.get(stack));
        // Damp grain is worth less: the buyer has to dry it.
        if (GrainStacks.isStorableGrain(stack) && GrainStacks.moisture(stack) > TilthConfig.GRAIN_SAFE_STORAGE_MOISTURE.getAsDouble() + 1e-6) {
            p *= 0.7;
        }
        return p;
    }

    /** Sells {@code count} of the stack to the market and pays the player in coins; returns the pennies paid. */
    public int sell(ServerPlayer player, ItemStack stack, int count) {
        double each = unitPrice(stack);
        if (each <= 0 || count <= 0) {
            return 0;
        }
        double total = each * Coins.SHILLING * count + credit.getOrDefault(player.getUUID(), 0.0);
        int pennies = (int) Math.floor(total);
        credit.put(player.getUUID(), total - pennies);
        stack.shrink(count);
        Coins.pay(player, pennies);
        setDirty();
        return pennies;
    }

    public double credit(ServerPlayer player) {
        return credit.getOrDefault(player.getUUID(), 0.0);
    }

    /** The price board: each group's price for a stack of its usual goods, the trend, and the news. */
    public List<Component> board() {
        List<Component> out = new ArrayList<>();
        out.add(Component.literal("=== Market prices ===").withStyle(ChatFormatting.GOLD));
        for (Commodity c : Commodity.values()) {
            int g = c.ordinal();
            double now = index[g] * eventMultiplier(g);
            double change = index[g] - yesterday[g];
            String arrow = change > 0.01 ? "▲" : change < -0.01 ? "▼" : "▬";
            ChatFormatting colour = change > 0.01 ? ChatFormatting.GREEN : change < -0.01 ? ChatFormatting.RED : ChatFormatting.GRAY;
            MutableComponent line = Component.literal(String.format("%-14s %3.0f%% of usual ", c.displayName, now * 100)).withStyle(ChatFormatting.WHITE);
            out.add(line.append(Component.literal(arrow).withStyle(colour)));
        }
        for (Active a : events) {
            MarketModel.EVENTS.stream().filter(e -> e.id().equals(a.id)).findFirst().ifPresent(e ->
                    out.add(Component.literal("News: " + e.headline() + " (" + Math.max(1, a.until - day + 1) + " more days)")
                            .withStyle(ChatFormatting.YELLOW)));
        }
        out.add(Component.literal("Right-click with goods to sell the stack; sneak to sell one.").withStyle(ChatFormatting.DARK_GRAY));
        return out;
    }

    public double index(Commodity c) {
        return index[c.ordinal()] * eventMultiplier(c.ordinal());
    }

    public static Market load(CompoundTag tag, HolderLookup.Provider registries) {
        Market m = new Market();
        long[] packed = tag.getLongArray("index");
        long[] old = tag.getLongArray("yesterday");
        for (int g = 0; g < MarketModel.GROUPS; g++) {
            if (g < packed.length) {
                m.index[g] = Double.longBitsToDouble(packed[g]);
            }
            if (g < old.length) {
                m.yesterday[g] = Double.longBitsToDouble(old[g]);
            }
        }
        for (Tag t : tag.getList("events", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            m.events.add(new Active(e.getString("id"), e.getLong("until")));
        }
        CompoundTag credits = tag.getCompound("credit");
        for (String k : credits.getAllKeys()) {
            m.credit.put(UUID.fromString(k), credits.getDouble(k));
        }
        m.day = tag.contains("day") ? tag.getLong("day") : -1;
        return m;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        long[] packed = new long[MarketModel.GROUPS];
        long[] old = new long[MarketModel.GROUPS];
        for (int g = 0; g < MarketModel.GROUPS; g++) {
            packed[g] = Double.doubleToLongBits(index[g]);
            old[g] = Double.doubleToLongBits(yesterday[g]);
        }
        tag.putLongArray("index", packed);
        tag.putLongArray("yesterday", old);
        ListTag list = new ListTag();
        for (Active a : events) {
            CompoundTag e = new CompoundTag();
            e.putString("id", a.id);
            e.putLong("until", a.until);
            list.add(e);
        }
        tag.put("events", list);
        CompoundTag credits = new CompoundTag();
        credit.forEach((k, v) -> credits.putDouble(k.toString(), v));
        tag.put("credit", credits);
        tag.putLong("day", day);
        return tag;
    }
}
