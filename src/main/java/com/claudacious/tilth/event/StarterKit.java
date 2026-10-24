package com.claudacious.tilth.event;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.registry.TilthItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Gives each player a few radish seeds, the forgiving starter crop, the first time they join. */
public final class StarterKit {
    private static final String RECEIVED = "tilth_received_starter_seeds";

    private StarterKit() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        int count = TilthConfig.STARTER_SEEDS.getAsInt();
        if (!(event.getEntity() instanceof ServerPlayer player) || count <= 0) {
            return;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(RECEIVED)) {
            return;
        }
        ItemStack seeds = new ItemStack(TilthItems.CROP_ITEMS.get("radish_seeds").get(), count);
        if (!player.getInventory().add(seeds)) {
            player.drop(seeds, false);
        }
        persisted.putBoolean(RECEIVED, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
