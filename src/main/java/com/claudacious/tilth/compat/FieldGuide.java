package com.claudacious.tilth.compat;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.TilthConfig;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The Tilth Field Guide is a Patchouli book (data-driven, no compile dependency). When Patchouli is
 * installed, each player receives it once on first joining a world.
 */
public final class FieldGuide {
    public static final ResourceLocation BOOK_ID = Tilth.id("field_guide");
    private static final String RECEIVED = "tilth_received_field_guide";

    private FieldGuide() {
    }

    public static boolean available() {
        return ModList.get().isLoaded("patchouli");
    }

    @SuppressWarnings("unchecked")
    public static ItemStack create() {
        Item book = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book"));
        DataComponentType<?> component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.fromNamespaceAndPath("patchouli", "book"));
        if (book == Items.AIR || component == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(book);
        stack.set((DataComponentType<ResourceLocation>) component, BOOK_ID);
        return stack;
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !available() || !TilthConfig.GIVE_GUIDE.getAsBoolean()) {
            return;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(RECEIVED)) {
            return;
        }
        ItemStack guide = create();
        if (guide.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(guide)) {
            player.drop(guide, false);
        }
        persisted.putBoolean(RECEIVED, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
