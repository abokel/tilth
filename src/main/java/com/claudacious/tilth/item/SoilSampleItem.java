package com.claudacious.tilth.item;

import java.util.Map;
import org.jetbrains.annotations.Nullable;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.sim.soil.SoilState;
import com.claudacious.tilth.sim.soil.Hydraulics;
import com.claudacious.tilth.block.TilthCropBlock;
import com.claudacious.tilth.block.FarmlandBlockEntity;
import com.claudacious.tilth.block.DeadCropBlock;
import com.claudacious.tilth.command.Reports;
import com.claudacious.tilth.world.TilthCalendar;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/**
 * A bag of soil taken with the probe. It records the lab analysis of the ground at the moment it was taken,
 * which a Soil Lab writes up as a report book.
 */
public class SoilSampleItem extends Item {
    private static final int PAGE_CHARS = 200;

    public SoilSampleItem(Properties properties) {
        super(properties);
    }

    public static ItemStack take(ServerLevel level, BlockPos pos, Item item) {
        CompoundTag tag = new CompoundTag();
        ListTag lines = new ListTag();
        for (Component line : Reports.labReport(level, pos)) {
            lines.add(StringTag.valueOf(Component.Serializer.toJson(line, level.registryAccess())));
        }
        tag.put("Report", lines);
        tag.putString("Where", pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        tag.putString("When", TilthCalendar.describeToday(level));
        CompoundTag measured = measure(level, pos);
        if (measured != null) {
            tag.put("Data", measured);
        }
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    /** The numbers behind a report, kept so results can be filed into a {@link SoilSurveyItem}. */
    @Nullable
    public static CompoundTag measure(ServerLevel level, BlockPos pos) {
        BlockPos soilPos = Reports.soilUnder(level, pos);
        Map.Entry<SoilTexture, SoilState> sample = Reports.sampleAt(level, soilPos);
        if (sample == null) {
            return null;
        }
        SoilTexture tex = sample.getKey();
        SoilState s = sample.getValue();
        Hydraulics h = tex.hydraulics(s.organicMatter);
        CompoundTag d = new CompoundTag();
        d.putInt("X", soilPos.getX());
        d.putInt("Y", soilPos.getY());
        d.putInt("Z", soilPos.getZ());
        d.putLong("Day", TilthCalendar.today(level));
        d.putString("When", TilthCalendar.describeToday(level));
        d.putString("Texture", tex.displayName());
        d.putBoolean("Farmland", level.getBlockEntity(soilPos) instanceof FarmlandBlockEntity);
        d.putDouble("PH", s.ph);
        d.putDouble("OM", s.organicMatter);
        d.putDouble("N", s.nitrogen);
        d.putDouble("P", s.phosphorus);
        d.putDouble("K", s.potassium);
        d.putDouble("S", s.sulfur);
        d.putDouble("Ca", s.calcium);
        d.putDouble("Mg", s.magnesium);
        d.putDouble("Compaction", s.compaction);
        d.putDouble("Water", (s.moisture - h.wiltingPoint()) / Math.max(0.01, h.fieldCapacity() - h.wiltingPoint()));
        ListTag history = new ListTag();
        s.cropHistory.forEach(c -> history.add(StringTag.valueOf(c)));
        d.put("History", history);
        return d;
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** The measurements recorded in a sample, or an empty tag for samples taken before they were recorded. */
    public static CompoundTag measurements(ItemStack sample) {
        return data(sample).getCompound("Data");
    }

    /** The written-up report for a sample, as a book. */
    public static ItemStack report(ItemStack sample, HolderLookup.Provider registries) {
        CompoundTag tag = data(sample);
        List<Filterable<Component>> pages = new ArrayList<>();
        Component cover = Component.literal("Soil Test Report\n\n").withStyle(ChatFormatting.BOLD)
                .append(Component.literal("Sampled at " + tag.getString("Where") + "\n" + tag.getString("When") + "\n\n")
                        .withStyle(s -> s.withBold(false)))
                .append(Component.literal("Tilth Soil Lab").withStyle(s -> s.withBold(false).withItalic(true)));
        pages.add(Filterable.passThrough(cover));
        Component page = Component.empty();
        int chars = 0;
        for (Tag t : tag.getList("Report", Tag.TAG_STRING)) {
            Component parsed = Component.Serializer.fromJson(t.getAsString(), registries);
            if (parsed == null) {
                continue;
            }
            Component line = forPaper(parsed);
            int len = line.getString().length();
            if (chars > 0 && chars + len > PAGE_CHARS) {
                pages.add(Filterable.passThrough(page));
                page = Component.empty();
                chars = 0;
            }
            page = page.copy().append(line).append("\n");
            chars += len + 20;
        }
        if (chars > 0) {
            pages.add(Filterable.passThrough(page));
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Soil Test Report"), "Tilth Soil Lab", 0, pages, true));
        if (tag.contains("Data")) {
            CompoundTag keep = new CompoundTag();
            keep.put(SoilSurveyItem.REPORT_KEY, tag.getCompound("Data"));
            book.set(DataComponents.CUSTOM_DATA, CustomData.of(keep));
        }
        return book;
    }

    /**
     * Report lines are coloured for chat, where light colours read best. On a book's beige page they wash out,
     * so every colour is darkened (keeping its hue) and uncoloured text becomes near-black.
     */
    private static Component forPaper(Component line) {
        MutableComponent out = Component.empty();
        line.visit((style, text) -> {
            int rgb = style.getColor() == null ? 0xFFFFFF : style.getColor().getValue();
            int r = (rgb >> 16) & 255;
            int g = (rgb >> 8) & 255;
            int b = rgb & 255;
            double lum = 0.3 * r + 0.59 * g + 0.11 * b;
            double f = lum > 200 ? 0.13 : lum > 90 ? 0.42 : 1.0;
            int dark = ((int) (r * f) << 16) | ((int) (g * f) << 8) | (int) (b * f);
            out.append(Component.literal(text).withStyle(style.withColor(TextColor.fromRgb(dark))));
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        if (tag.contains("Where")) {
            tooltip.add(Component.literal("From " + tag.getString("Where")).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(tag.getString("When")).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal("Put it in a Soil Lab for a full analysis").withStyle(ChatFormatting.GRAY));
    }
}
