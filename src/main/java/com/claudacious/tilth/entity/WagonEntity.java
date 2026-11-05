package com.claudacious.tilth.entity;

import com.claudacious.tilth.registry.TilthItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A four-wheeled farm wagon with a box bed that holds as much as a chest. Hitch it from the saddle of a horse,
 * donkey or mule like the plough; from the ground, right-click to load or unload it, and sneak-right-click to unhitch.
 * From the saddle, right-click sets or releases the brake. A loaded wagon is heavier to pull.
 */
public class WagonEntity extends DrawnImplementEntity {
    public static final int SLOTS = 27;
    private final SimpleContainer cargo = new SimpleContainer(SLOTS);

    public WagonEntity(EntityType<? extends WagonEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ItemStack item() {
        return new ItemStack(TilthItems.WAGON.get());
    }

    @Override
    protected boolean work(ServerLevel level, BlockPos ground, @Nullable Player operator) {
        return false;
    }

    /** How much a full bed slows the team: an empty wagon rolls easily, a laden one is hard work. */
    @Override
    protected double draft() {
        int used = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (!cargo.getItem(i).isEmpty()) {
                used++;
            }
        }
        return 0.05 + 0.25 * used / SLOTS;
    }

    @Override
    protected String loweredMessage(boolean lowered) {
        return lowered ? "Brake on" : "Brake off";
    }

    @Override
    protected InteractionResult interactExtra(Player player, InteractionHand hand) {
        if (player.getVehicle() != null) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive() && puller() != null) {
            unhitch();
            player.displayClientMessage(Component.literal("Unhitched").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.SUCCESS;
        }
        playSound(SoundEvents.CHEST_OPEN, 0.6f, 0.8f);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, cargo), Component.literal("Wagon")));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void dropContents() {
        Containers.dropContents(level(), blockPosition(), cargo);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cargo.fromTag(tag.getList("Cargo", 10), registryAccess());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Cargo", cargo.createTag(registryAccess()));
    }
}
