package com.claudacious.tilth.item;

import com.claudacious.tilth.command.Reports;
import com.claudacious.tilth.entity.SpreaderEntity;
import com.claudacious.tilth.plot.Plot;
import com.claudacious.tilth.plot.PlotData;
import com.claudacious.tilth.sim.plot.PolygonMath;
import com.claudacious.tilth.sim.soil.Amendment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A soil survey ledger for one plot. Lab results are filed into it (use it on a Soil Lab whose analysis is
 * done, or use it with a Soil Test Report in the other hand). It keeps the latest test at each spot and
 * rewrites itself as a book: the plot's averages and ranges, a whole-plot plan with amounts for the plot's
 * area (doses and spreader loads), spots that need separate treatment, sampling advice and rotation advice,
 * then a page per sample.
 */
public class SoilSurveyItem extends Item {
    public static final String REPORT_KEY = "TilthSoil";
    public static final int MAX_SAMPLES = 64;
    /** One sample per this many blocks of plot (about a quarter acre) gives a reliable average. */
    private static final int BLOCKS_PER_SAMPLE = 1000;
    private static final int INK = 0x2A2A2A;
    private static final int HEAD = 0x5A3A00;
    private static final int GOOD = 0x1E5E1E;
    private static final int BAD = 0x8A1C1C;
    private static final int NOTE = 0x3A3A6A;

    public SoilSurveyItem(Properties properties) {
        super(properties);
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** The filed measurements in a Soil Test Report book, or null if it carries none. */
    @Nullable
    public static CompoundTag reportData(ItemStack stack) {
        CompoundTag tag = data(stack);
        return stack.is(Items.WRITTEN_BOOK) && tag.contains(REPORT_KEY) ? tag.getCompound(REPORT_KEY) : null;
    }

    /**
     * Files one sample's measurements. Returns null on success, or why it was refused: a survey covers one plot,
     * or, if started outside any plot, unplotted ground only.
     */
    @Nullable
    public static Component file(ServerLevel level, ItemStack survey, CompoundTag sample) {
        if (sample.isEmpty()) {
            return Component.literal("That result has no measurements to file. Take a fresh sample.");
        }
        CompoundTag tag = data(survey);
        ListTag samples = tag.getList("Samples", Tag.TAG_COMPOUND);
        Plot plot = PlotData.get(level).at(sample.getInt("X"), sample.getInt("Z"));
        String plotId = plot == null ? "" : plot.id().toString();
        if (!samples.isEmpty() && !tag.getString("PlotId").equals(plotId)) {
            String here = plot == null ? "outside any plot" : "from plot \"" + plot.name() + "\"";
            String survey1 = tag.getString("PlotId").isEmpty() ? "unplotted ground" : "plot \"" + tag.getString("Plot") + "\"";
            return Component.literal("This survey is for " + survey1 + "; that sample is " + here + ". Start a new survey.");
        }
        tag.putString("PlotId", plotId);
        tag.putString("Plot", plot == null ? "Unplotted ground" : plot.name());
        for (int i = samples.size() - 1; i >= 0; i--) {
            CompoundTag s = samples.getCompound(i);
            if (s.getInt("X") == sample.getInt("X") && s.getInt("Z") == sample.getInt("Z")) {
                samples.remove(i);
            }
        }
        if (samples.size() >= MAX_SAMPLES) {
            return Component.literal("The survey is full (" + MAX_SAMPLES + " samples).");
        }
        samples.add(sample.copy());
        tag.put("Samples", samples);
        survey.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        rewrite(survey, tag, plot);
        return null;
    }

    // ------------------------------------------------------------------ the book

    private static MutableComponent text(String s, int color) {
        return Component.literal(s).withColor(color);
    }

    private static MutableComponent bold(String s, int color) {
        return text(s, color).withStyle(ChatFormatting.BOLD);
    }

    private static double avg(List<CompoundTag> s, String key) {
        return s.stream().mapToDouble(t -> t.getDouble(key)).average().orElse(0);
    }

    private static double min(List<CompoundTag> s, String key) {
        return s.stream().mapToDouble(t -> t.getDouble(key)).min().orElse(0);
    }

    private static double max(List<CompoundTag> s, String key) {
        return s.stream().mapToDouble(t -> t.getDouble(key)).max().orElse(0);
    }

    private static String spot(CompoundTag s) {
        return "(" + s.getInt("X") + ", " + s.getInt("Z") + ")";
    }

    private static void rewrite(ItemStack survey, CompoundTag tag, @Nullable Plot plot) {
        List<CompoundTag> samples = new ArrayList<>();
        ListTag list = tag.getList("Samples", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            samples.add(list.getCompound(i));
        }
        long blocks = plot == null ? 0 : plot.blockCount();
        List<Component> pages = new ArrayList<>();

        // Cover.
        int recommended = blocks > 0 ? (int) Math.max(3, Math.ceil(blocks / (double) BLOCKS_PER_SAMPLE)) : 3;
        MutableComponent cover = Component.empty().append(bold("Soil Survey\n", INK)).append(text(tag.getString("Plot") + "\n\n", HEAD));
        if (plot != null) {
            cover.append(text(PolygonMath.formatArea(plot.areaSquareMetres()) + String.format("\n%,d blocks\n\n", blocks), INK));
        }
        cover.append(text(samples.size() + " sample" + (samples.size() == 1 ? "" : "s") + " filed", INK))
                .append(text(samples.size() < recommended ? " (aim for " + recommended + ")\n\n" : "\n\n", samples.size() < recommended ? BAD : INK));
        long first = samples.stream().mapToLong(t -> t.getLong("Day")).min().orElse(0);
        long last = samples.stream().mapToLong(t -> t.getLong("Day")).max().orElse(0);
        String firstWhen = samples.stream().filter(t -> t.getLong("Day") == first).findFirst().map(t -> t.getString("When")).orElse("");
        String lastWhen = samples.stream().filter(t -> t.getLong("Day") == last).findFirst().map(t -> t.getString("When")).orElse("");
        cover.append(text(first == last ? "Sampled " + shortDate(firstWhen) : shortDate(firstWhen) + " to\n" + shortDate(lastWhen), NOTE));
        pages.add(cover);

        // Averages and ranges.
        double ph = avg(samples, "PH");
        double n = avg(samples, "N");
        double p = avg(samples, "P");
        double k = avg(samples, "K");
        double om = avg(samples, "OM");
        double comp = avg(samples, "Compaction");
        MutableComponent summary = Component.empty().append(bold("Plot averages\n", HEAD)).append(text("(range across samples)\n\n", NOTE));
        summary.append(text(String.format("pH %.1f (%.1f-%.1f)\n%s\n\n", ph, min(samples, "PH"), max(samples, "PH"), Reports.ratePh(ph)), INK));
        summary.append(text(String.format("N %.0f ppm (%.0f-%.0f) %s\n", n, min(samples, "N"), max(samples, "N"), Reports.rateN(n)), n < 20 ? BAD : INK));
        summary.append(text(String.format("P %.0f ppm (%.0f-%.0f) %s\n", p, min(samples, "P"), max(samples, "P"), Reports.rateP(p)), p < 20 ? BAD : INK));
        summary.append(text(String.format("K %.0f ppm (%.0f-%.0f) %s\n\n", k, min(samples, "K"), max(samples, "K"), Reports.rateK(k)), k < 130 ? BAD : INK));
        summary.append(text(String.format("Organic matter %.1f%%\nCompaction %.0f%%", om, comp * 100), INK));
        pages.add(summary);

        // Whole-plot plan.
        List<String> plan = new ArrayList<>();
        if (ph < 6.0) {
            plan.add(amount("Lime the whole plot", (int) Math.ceil((6.5 - ph) / Amendment.AGRICULTURAL_LIME.phDelta()), "Agricultural Lime", blocks));
        } else if (ph > 7.8) {
            plan.add(amount("Acidify the whole plot", (int) Math.ceil((ph - 7.0) / -Amendment.SULFUR.phDelta()), "Elemental Sulfur", blocks));
        }
        if (n < 20) {
            plan.add(amount("Nitrogen is short", (int) Math.ceil((25 - n) / Amendment.SLURRY.n()), "Slurry", blocks)
                    + " Or sow a legume (peas, beans, clover) first.");
        }
        if (p < 20) {
            plan.add(amount("Phosphorus is short", (int) Math.ceil((25 - p) / Amendment.BONE_MEAL.p()), "Bone Meal", blocks));
        }
        if (k < 130) {
            plan.add(amount("Potassium is short", (int) Math.ceil((150 - k) / Amendment.WOOD_ASH.k()), "Wood Ash", blocks));
        }
        if (om < 2.0) {
            plan.add("Organic matter is low: leave crop residue on the field and spread compost each year.");
        }
        if (comp > 0.4) {
            plan.add("The plot is compacted: plough it when dry and keep animals and traffic off when wet.");
        }
        if (plan.isEmpty()) {
            plan.add("The plot is in good order for most crops. Retest next year.");
        }
        addPages(pages, bold("Plot plan\n\n", HEAD), plan, INK);

        // Spots that differ from the plot, sampling and rotation.
        List<String> notes = new ArrayList<>();
        for (CompoundTag s : samples) {
            List<String> odd = new ArrayList<>();
            if (s.getDouble("PH") < 6.0 && ph >= 6.0) {
                odd.add(String.format("acid, pH %.1f: lime here", s.getDouble("PH")));
            }
            if (s.getDouble("PH") > 7.8 && ph <= 7.8) {
                odd.add(String.format("alkaline, pH %.1f", s.getDouble("PH")));
            }
            if (s.getDouble("N") < 10 && n >= 20) {
                odd.add("very low N: extra manure");
            }
            if (s.getDouble("Water") > 1.05) {
                odd.add("waterlogged: drain or plant rice");
            }
            if (s.getDouble("Compaction") > 0.5 && comp <= 0.4) {
                odd.add("compacted");
            }
            if (!odd.isEmpty()) {
                notes.add(spot(s) + " " + String.join("; ", odd) + ".");
            }
        }
        if (notes.isEmpty() && samples.size() > 1) {
            notes.add("The samples agree: treat the plot evenly.");
        }
        if (samples.size() < recommended) {
            notes.add("Take " + (recommended - samples.size()) + " more sample" + (recommended - samples.size() == 1 ? "" : "s")
                    + " spread across the plot for a reliable average (one per 1,000 blocks).");
        }
        String rotation = rotationAdvice(samples);
        if (rotation != null) {
            notes.add(rotation);
        }
        addPages(pages, bold("Spots & advice\n\n", HEAD), notes, INK);

        // One page per sample.
        for (int i = 0; i < samples.size(); i++) {
            CompoundTag s = samples.get(i);
            pages.add(Component.empty().append(bold("Sample " + (i + 1) + "\n", HEAD)).append(text(spot(s) + "\n" + shortDate(s.getString("When")) + "\n\n", NOTE))
                    .append(text(s.getString("Texture") + (s.getBoolean("Farmland") ? " farmland" : ", untilled") + "\n", INK))
                    .append(text(String.format("pH %.1f, OM %.1f%%\nN %.0f, P %.0f, K %.0f ppm\nS %.0f, Ca %.0f, Mg %.0f ppm\n",
                            s.getDouble("PH"), s.getDouble("OM"), s.getDouble("N"), s.getDouble("P"), s.getDouble("K"),
                            s.getDouble("S"), s.getDouble("Ca"), s.getDouble("Mg")), INK))
                    .append(text(String.format("Water %.0f%% of available\nCompaction %.0f%%", 100 * s.getDouble("Water"),
                            100 * s.getDouble("Compaction")), INK)));
        }

        List<Filterable<Component>> filtered = pages.stream().map(Filterable::passThrough).toList();
        String title = ("Survey: " + tag.getString("Plot"));
        survey.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(title.length() > 32 ? title.substring(0, 32) : title), "Tilth Soil Lab", 0, filtered, true));
    }

    /** "Lime the whole plot: 2 doses of Agricultural Lime per block = 2,480 doses, about 20 spreader loads." */
    private static String amount(String what, int perBlock, String product, long blocks) {
        perBlock = Math.max(1, perBlock);
        String base = what + ": " + perBlock + " dose" + (perBlock == 1 ? "" : "s") + " of " + product + " per block";
        if (blocks <= 0) {
            return base + ".";
        }
        long total = perBlock * blocks;
        long loads = (long) Math.ceil(total / (double) SpreaderEntity.CAPACITY);
        return base + String.format(" = %,d doses, about %,d spreader load%s.", total, loads, loads == 1 ? "" : "s");
    }

    @Nullable
    private static String rotationAdvice(List<CompoundTag> samples) {
        List<String> latest = null;
        for (CompoundTag s : samples) {
            ListTag h = s.getList("History", Tag.TAG_STRING);
            if (!h.isEmpty() && (latest == null || h.size() > latest.size())) {
                latest = new ArrayList<>();
                for (int i = 0; i < h.size(); i++) {
                    latest.add(h.getString(i));
                }
            }
        }
        if (latest == null || latest.isEmpty()) {
            return null;
        }
        String last = latest.get(latest.size() - 1);
        boolean repeated = latest.size() >= 2 && latest.get(latest.size() - 2).equals(last);
        boolean legume = latest.stream().anyMatch(c -> c.contains("pea") || c.contains("bean") || c.contains("clover")
                || c.contains("alfalfa") || c.contains("lentil") || c.contains("chickpea") || c.contains("soy") || c.contains("peanut"));
        String history = "Recent crops: " + String.join(", ", latest.subList(Math.max(0, latest.size() - 4), latest.size())).replace('_', ' ') + ". ";
        if (repeated) {
            return history + "The same crop twice running invites disease and pests: rotate to a different family next.";
        }
        if (!legume) {
            return history + "No legume recently: a season of peas, beans or clover would return nitrogen.";
        }
        return history + "A sound rotation.";
    }

    /** "23 January, Year 18 (mid winter)" without the season, to fit a book line. */
    private static String shortDate(String when) {
        int paren = when.indexOf(" (");
        return paren > 0 ? when.substring(0, paren) : when;
    }

    /** Book pages show 14 lines of about 19 characters. */
    private static int lines(String s) {
        return Math.max(1, (int) Math.ceil((s.length() + 2) / 20.0));
    }

    private static void addPages(List<Component> pages, Component heading, List<String> items, int color) {
        MutableComponent page = Component.empty().append(heading);
        int used = 2;
        boolean any = false;
        for (String item : items) {
            if (any && used + lines(item) > 14) {
                pages.add(page);
                page = Component.empty();
                used = 0;
            }
            used += lines(item);
            any = true;
            page.append(text("- " + item + "\n", item.startsWith("The plot is in good order") || item.startsWith("The samples agree")
                    || item.endsWith("A sound rotation.") ? GOOD : color));
        }
        pages.add(page);
    }

    // ------------------------------------------------------------------ use

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack survey = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        CompoundTag report = reportData(other);
        if (report != null) {
            if (level instanceof ServerLevel server) {
                Component refused = file(server, survey, report);
                if (refused == null) {
                    other.shrink(1);
                    player.displayClientMessage(Component.literal("Filed into the survey of " + data(survey).getString("Plot") + " ("
                            + data(survey).getList("Samples", Tag.TAG_COMPOUND).size() + " samples)").withStyle(ChatFormatting.GREEN), true);
                } else {
                    player.displayClientMessage(refused.copy().withStyle(ChatFormatting.RED), true);
                }
            }
            return InteractionResultHolder.sidedSuccess(survey, level.isClientSide);
        }
        if (!survey.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Empty. Use it on a Soil Lab when an analysis is done, or hold a Soil Test Report "
                        + "in your other hand.").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(survey);
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            // Rewritten on every read, so the book always reflects the current layout and the plot as it is now.
            CompoundTag tag = data(survey);
            Plot plot = PlotData.get(server).all().stream().filter(p -> p.id().toString().equals(tag.getString("PlotId"))).findFirst().orElse(null);
            rewrite(survey, tag, plot);
            serverPlayer.containerMenu.broadcastChanges();
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundOpenBookPacket(hand));
        }
        return InteractionResultHolder.sidedSuccess(survey, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        int count = tag.getList("Samples", Tag.TAG_COMPOUND).size();
        if (count > 0) {
            tooltip.add(Component.literal(tag.getString("Plot") + ", " + count + " sample" + (count == 1 ? "" : "s")).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal("File lab results: use it on a Soil Lab, or with a report in the other hand").withStyle(ChatFormatting.DARK_GRAY));
    }
}
