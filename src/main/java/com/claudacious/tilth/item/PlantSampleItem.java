package com.claudacious.tilth.item;

import com.claudacious.tilth.affliction.Affliction;
import com.claudacious.tilth.affliction.AfflictionEngine;
import com.claudacious.tilth.block.CropBlockEntity;
import com.claudacious.tilth.sim.crop.Bbch;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.CropState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Map;

/**
 * A leaf and stem cutting taken with a knife. It records the crop and whatever organisms were living on it,
 * for examination under the microscope.
 */
public class PlantSampleItem extends Item {
    public PlantSampleItem(Properties properties) {
        super(properties);
    }

    public static ItemStack take(ServerLevel level, CropBlockEntity crop, Item item) {
        CropSpecies sp = crop.species();
        CropState cs = crop.cropState();
        CompoundTag tag = new CompoundTag();
        tag.putString("Crop", sp.name);
        tag.putString("Stage", Bbch.describe(sp.bbchFor(sp.visualStage(cs))));
        ListTag list = new ListTag();
        Registry<Affliction> registry = AfflictionEngine.registry(level);
        for (Map.Entry<String, Double> e : cs.afflictions.entrySet()) {
            ResourceLocation id = e.getKey().contains(":") ? ResourceLocation.parse(e.getKey()) : ResourceLocation.fromNamespaceAndPath("tilth", e.getKey());
            Affliction a = registry == null ? null : registry.get(id);
            if (a == null) {
                continue;
            }
            CompoundTag at = new CompoundTag();
            at.putString("Id", id.toString());
            at.putString("Name", a.name());
            at.putString("Sci", a.scientificName());
            at.putString("Category", a.category().getSerializedName());
            at.putBoolean("Beneficial", a.beneficial());
            at.putDouble("Severity", e.getValue());
            at.put("Symptoms", strings(a.symptoms()));
            at.put("Control", strings(a.control().methods()));
            at.putString("Notes", a.notes());
            list.add(at);
        }
        tag.put("Afflictions", list);
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static ListTag strings(List<String> values) {
        ListTag out = new ListTag();
        values.forEach(v -> out.add(StringTag.valueOf(v)));
        return out;
    }

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        if (tag.contains("Crop")) {
            tooltip.add(Component.literal(tag.getString("Crop") + ", " + tag.getString("Stage")).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal("Hold it in your off hand and use a Microscope").withStyle(ChatFormatting.DARK_GRAY));
    }
}
