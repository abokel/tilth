package com.claudacious.tilth.livestock;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.quality.Quality;
import com.claudacious.tilth.registry.TilthItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Wool and milk with a grade. Shearing a sheep gives graded fleece instead of wool: a Merino's is the finest, a
 * Herdwick's the coarsest, and a thin or sick sheep's is poorer and lighter. Milk is graded by breed and condition
 * too, a Jersey's richest. Fleece is spun into yarn on the spinning wheel; milk goes to the churn and the cheese press.
 */
public final class WoolAndDairy {
    private WoolAndDairy() {
    }

    /** Fleece grade (1-5) from breed fineness and the sheep's condition and health. */
    static int fleeceGrade(LivestockState s) {
        int base = switch (Breed.byId(s.breed) == null ? "" : s.breed) {
            case "merino" -> 5;
            case "suffolk", "hebridean" -> 3;
            case "soay", "herdwick" -> 2;
            default -> 3;
        };
        return clampGrade(base - penalty(s));
    }

    /** Milk grade (1-5): butterfat and protein, richest from Jerseys, then by condition. */
    public static int milkGrade(LivestockState s, boolean goat) {
        int base = goat ? 4 : switch (s.breed) {
            case "jersey" -> 5;
            case "holstein", "highland", "brahman" -> 3;
            default -> 3;
        };
        return clampGrade(base - penalty(s));
    }

    private static int penalty(LivestockState s) {
        int p = s.condition < 0.3 ? 2 : s.condition < 0.45 ? 1 : 0;
        return p + (s.diseaseBurden() > 0.3 ? 1 : 0);
    }

    private static int clampGrade(int g) {
        return Math.max(1, Math.min(5, g));
    }

    /** Shears on a woolly sheep: graded fleece, more from a Merino and from a sheep in good condition. */
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack held = event.getItemStack();
        if (!(event.getTarget() instanceof Sheep sheep) || !held.canPerformAction(ItemAbilities.SHEARS_HARVEST)
                || !TilthConfig.LIVESTOCK.getAsBoolean() || !sheep.readyForShearing()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(sheep.level().isClientSide));
        if (sheep.level().isClientSide) {
            return;
        }
        Player player = event.getEntity();
        LivestockState s = Livestock.state(sheep);
        int count = 1 + sheep.getRandom().nextInt(3) + ("merino".equals(s.breed) ? 1 : 0) + (s.condition > 0.8 ? 1 : 0) - (s.condition < 0.35 ? 1 : 0);
        ItemStack fleece = new ItemStack(TilthItems.FLEECE.get(), Math.max(1, count));
        fleece.set(DataComponents.BASE_COLOR, sheep.getColor());
        Quality.set(fleece, fleeceGrade(s));
        sheep.setSheared(true);
        sheep.level().playSound(null, sheep, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0f, 1.0f);
        sheep.spawnAtLocation(fleece, 1.0f);
        held.hurtAndBreak(1, player, event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        player.displayClientMessage(Component.literal(String.format("Shorn: %d fleece, %s.", fleece.getCount(),
                Quality.name(Quality.get(fleece)))).withStyle(ChatFormatting.GREEN), true);
    }

    /** Fills a bucket with graded milk, as Livestock's milking rules allow. Returns false to leave milking to vanilla. */
    static boolean milk(Animal animal, Player player, net.minecraft.world.InteractionHand hand, ItemStack bucket, LivestockState s) {
        if (!bucket.is(Items.BUCKET)) {
            return false;
        }
        ItemStack milk = new ItemStack(Items.MILK_BUCKET);
        Quality.set(milk, milkGrade(s, animal instanceof net.minecraft.world.entity.animal.goat.Goat));
        player.setItemInHand(hand, ItemUtils.createFilledResult(bucket, player, milk));
        animal.level().playSound(null, animal, animal instanceof net.minecraft.world.entity.animal.goat.Goat
                ? SoundEvents.GOAT_MILK : SoundEvents.COW_MILK, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
