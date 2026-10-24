package com.claudacious.tilth.compat;

import com.claudacious.tilth.block.CropBlockEntity;
import com.claudacious.tilth.block.CropCondition;
import com.claudacious.tilth.block.FarmlandBlockEntity;
import com.claudacious.tilth.livestock.Breeding;
import com.claudacious.tilth.livestock.Livestock;
import com.claudacious.tilth.livestock.LivestockState;
import com.claudacious.tilth.sim.crop.Bbch;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.CropState;
import com.claudacious.tilth.sim.soil.Hydraulics;
import com.claudacious.tilth.sim.soil.SoilState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * The lines Jade and WTHIT show when looking at Tilth farmland, a crop or a farm animal: what you could see or
 * feel in the field. Nutrient numbers stay with the soil lab. Built on the server and sent as text components.
 */
public final class LookInfo {
    public static final String KEY = "TilthLook";
    private static final String MALE = "TilthMale";

    private LookInfo() {
    }

    public static void write(CompoundTag data, BlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        if (be instanceof CropBlockEntity crop) {
            crop(lines, crop);
            if (level.getBlockEntity(be.getBlockPos().below()) instanceof FarmlandBlockEntity farmland) {
                soil(lines, farmland);
            }
        } else if (be instanceof FarmlandBlockEntity farmland) {
            soil(lines, farmland);
        }
        put(data, lines, level);
    }

    /** A farm animal: its sex, its condition and, if she is carrying young, how long until she gives birth. */
    public static void write(CompoundTag data, Animal animal) {
        if (!(animal.level() instanceof ServerLevel level) || !Livestock.isLivestock(animal)) {
            return;
        }
        LivestockState s = Livestock.state(animal);
        List<Component> lines = new ArrayList<>();
        if (s.sex != LivestockState.UNKNOWN) {
            String name = Breeding.sexName(animal, s.female());
            lines.add(Component.literal((s.female() ? "♀ " : "♂ ") + Character.toUpperCase(name.charAt(0)) + name.substring(1))
                    .withStyle(s.female() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
            data.putBoolean(MALE, s.male());
        }
        lines.add(Component.literal(String.format("Condition %.0f%%", s.condition * 100))
                .withStyle(s.condition >= 0.55 ? ChatFormatting.GREEN : s.condition >= 0.3 ? ChatFormatting.YELLOW : ChatFormatting.RED));
        if (s.pregnant()) {
            lines.add(Component.literal(String.format("Pregnant, due in %.1f days", Math.max(0, s.due - level.getGameTime()) / 24000.0))
                    .withStyle(ChatFormatting.GOLD));
        }
        put(data, lines, level);
    }

    /** Whether the data written for an animal says it is male (so a rooster's egg countdown can be hidden). */
    public static boolean male(CompoundTag data) {
        return data.getBoolean(MALE);
    }

    private static void put(CompoundTag data, List<Component> lines, ServerLevel level) {
        ListTag list = new ListTag();
        lines.forEach(l -> list.add(StringTag.valueOf(Component.Serializer.toJson(l, level.registryAccess()))));
        data.put(KEY, list);
    }

    public static List<Component> read(CompoundTag data, HolderLookup.Provider registries) {
        List<Component> out = new ArrayList<>();
        for (Tag t : data.getList(KEY, Tag.TAG_STRING)) {
            Component c = Component.Serializer.fromJson(t.getAsString(), registries);
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    private static void crop(List<Component> lines, CropBlockEntity be) {
        CropSpecies sp = be.species();
        CropState cs = be.cropState();
        if (!cs.germinated) {
            lines.add(Component.literal("Not yet emerged").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.literal(Bbch.describe(sp.bbchFor(sp.visualStage(cs)))).withStyle(ChatFormatting.GRAY));
        boolean ripe = sp.isHarvestable(cs);
        lines.add(Component.literal(ripe ? "Ready to harvest" : String.format("%.0f%% of the way to ripe", 100 * Math.min(1, cs.dev)))
                .withStyle(ripe ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        CropCondition condition = CropCondition.of(cs);
        String look = switch (condition) {
            case CHLOROTIC -> "Yellowing leaves: short of nitrogen";
            case PURPLED -> "Purplish leaves: short of phosphorus";
            case WILTED -> "Wilting: short of water";
            case FROSTED -> "Frost-damaged";
            case HEALTHY -> cs.lastGrowthFactor < 0.5 && cs.limitingFactor != null ? "Growing slowly: " + cs.limitingFactor : null;
        };
        if (look != null) {
            lines.add(Component.literal(look).withStyle(condition == CropCondition.HEALTHY ? ChatFormatting.YELLOW : ChatFormatting.RED));
        }
    }

    private static void soil(List<Component> lines, FarmlandBlockEntity be) {
        SoilState s = be.soil();
        Hydraulics h = be.texture().hydraulics(s.organicMatter);
        double avail = (s.moisture - h.wiltingPoint()) / Math.max(0.01, h.fieldCapacity() - h.wiltingPoint());
        String water = s.moisture >= h.saturation() - 0.03 ? "saturated" : avail > 1.0 ? "wet" : avail > 0.5 ? "moist"
                : avail > 0.15 ? "drying" : "dry";
        lines.add(Component.literal(be.texture().displayName() + ", " + water
                + (s.compaction > 0.4 ? ", compacted" : "") + (s.residue > 0.3 ? ", mulched" : "")).withStyle(ChatFormatting.DARK_AQUA));
    }
}
