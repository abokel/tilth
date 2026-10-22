package com.claudacious.tilth.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A kitchen or milling tool that stays in the crafting grid and wears out with use (quern, knife). */
public class CraftingToolItem extends Item {
    public CraftingToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        int damage = copy.getDamageValue() + 1;
        if (damage >= copy.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        copy.setDamageValue(damage);
        return copy;
    }
}
