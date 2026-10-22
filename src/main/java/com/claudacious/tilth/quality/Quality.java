package com.claudacious.tilth.quality;

import com.claudacious.tilth.registry.TilthDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Harvest quality grades. Produce is graded at harvest from how well the crop was nourished,
 * watered and protected through the season; recipes average the grades of their ingredients, and a
 * graded food's nutrients are scaled by its grade.
 */
public final class Quality {
    private static final String[] NAMES = {"Ungraded", "Poor", "Fair", "Good", "Excellent", "Premium"};
    private static final ChatFormatting[] COLORS = {ChatFormatting.GRAY, ChatFormatting.DARK_RED, ChatFormatting.GOLD,
            ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE};

    private Quality() {
    }

    public static int get(ItemStack stack) {
        Integer q = stack.get(TilthDataComponents.QUALITY.get());
        return q == null ? 0 : q;
    }

    public static void set(ItemStack stack, int grade) {
        if (grade >= 1 && grade <= 5) {
            stack.set(TilthDataComponents.QUALITY.get(), grade);
        }
    }

    public static String name(int grade) {
        return NAMES[Math.max(0, Math.min(5, grade))];
    }

    public static ChatFormatting color(int grade) {
        return COLORS[Math.max(0, Math.min(5, grade))];
    }

    public static String stars(int grade) {
        return "★".repeat(Math.max(0, grade)) + "☆".repeat(Math.max(0, 5 - grade));
    }

    /** Nutrient multiplier: Poor 0.8, Fair 0.9, Good 1.0, Excellent 1.1, Premium 1.2. */
    public static double nutritionMultiplier(int grade) {
        return grade <= 0 ? 1.0 : 0.7 + 0.1 * grade;
    }

    /** Rounded mean grade of the graded stacks, or 0 if none are graded. */
    public static int average(List<ItemStack> stacks) {
        int sum = 0;
        int n = 0;
        for (ItemStack s : stacks) {
            int q = get(s);
            if (q > 0) {
                sum += q;
                n++;
            }
        }
        return n == 0 ? 0 : Math.round((float) sum / n);
    }
}
