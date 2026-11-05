package com.claudacious.tilth.market;

import com.claudacious.tilth.registry.TilthItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Tilth's money, in steps of nine: 9 copper pennies make a silver shilling, 9 shillings a gold crown and 9 crowns a
 * gold sovereign. Nine of a coin craft into the next one up, and one breaks back into nine. The market reckons in
 * pennies and pays out in the largest coins it can.
 */
public final class Coins {
    public static final int SHILLING = 9;
    public static final int CROWN = 81;
    public static final int SOVEREIGN = 729;

    private Coins() {
    }

    /** Coin items from largest to smallest, with their worth in pennies. */
    private static List<Object[]> denominations() {
        return List.of(new Object[]{TilthItems.GOLD_SOVEREIGN.get(), SOVEREIGN}, new Object[]{TilthItems.GOLD_CROWN.get(), CROWN},
                new Object[]{TilthItems.SILVER_SHILLING.get(), SHILLING}, new Object[]{TilthItems.COPPER_PENNY.get(), 1});
    }

    /** Worth of one coin of this kind in pennies, or 0 for anything that is not a coin. */
    public static int value(Item item) {
        for (Object[] d : denominations()) {
            if (d[0] == item) {
                return (int) d[1];
            }
        }
        return 0;
    }

    /** The fewest coins making up {@code pennies}. */
    public static List<ItemStack> change(int pennies) {
        List<ItemStack> out = new ArrayList<>();
        for (Object[] d : denominations()) {
            int worth = (int) d[1];
            int n = pennies / worth;
            pennies -= n * worth;
            while (n > 0) {
                int take = Math.min(64, n);
                out.add(new ItemStack((Item) d[0], take));
                n -= take;
            }
        }
        return out;
    }

    /** Pays a player {@code pennies} in coins; what will not fit is dropped at their feet. */
    public static void pay(Player player, int pennies) {
        for (ItemStack stack : change(pennies)) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    /** "2 crowns, 3 shillings and 5 pennies". */
    public static String describe(int pennies) {
        if (pennies <= 0) {
            return "nothing";
        }
        List<String> parts = new ArrayList<>();
        int[] worth = {SOVEREIGN, CROWN, SHILLING, 1};
        String[] names = {"sovereign", "crown", "shilling", "penny"};
        for (int i = 0; i < worth.length; i++) {
            int n = pennies / worth[i];
            pennies -= n * worth[i];
            if (n > 0) {
                parts.add(n + " " + (n == 1 ? names[i] : i == 3 ? "pennies" : names[i] + "s"));
            }
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
    }
}
