package com.claudacious.tilth.event;

import com.claudacious.tilth.plot.PlotRules;
import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.block.PendingTill;
import com.claudacious.tilth.block.TilthCropBlock;
import com.claudacious.tilth.block.TilthFarmlandBlock;
import com.claudacious.tilth.item.AmendmentItem;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.Crops;
import com.claudacious.tilth.sim.soil.Amendment;
import com.claudacious.tilth.sim.soil.SoilHorizon;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.sim.soil.Tillage;
import com.claudacious.tilth.world.SoilService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;

/** Glue between vanilla farming and Tilth. */
public final class FarmingEvents {
    private static final Map<Item, CropSpecies> VANILLA_SEEDS = Map.of(
            Items.WHEAT_SEEDS, Crops.COMMON_WHEAT,
            Items.POTATO, Crops.POTATO,
            Items.CARROT, Crops.CARROT,
            Items.BEETROOT_SEEDS, Crops.BEETROOT);

    private FarmingEvents() {
    }

    /** Hoeing vanilla grass, dirt or paths makes Tilth farmland of the texture the terrain implies. */
    public static void onToolModify(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.HOE_TILL || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState original = event.getState();
        BlockPos pos = event.getPos();
        if (!event.isSimulated() && !PlotRules.mayFarm(event.getPlayer(), level, pos)) {
            event.setCanceled(true);
            return;
        }
        if (!level.getBlockState(pos.above()).isAir() || event.getContext().getClickedFace() == Direction.DOWN) {
            return;
        }
        SoilTexture texture = null;
        if (TilthConfig.CONVERT_VANILLA_ON_TILL.getAsBoolean()
                && (original.is(Blocks.GRASS_BLOCK) || original.is(Blocks.DIRT) || original.is(Blocks.DIRT_PATH))) {
            texture = SoilService.guessTexture(level, pos);
        } else if (TilthConfig.TILL_VANILLA_SAND.getAsBoolean() && original.is(Blocks.SAND)) {
            texture = SoilTexture.SAND;
        }
        if (texture != null) {
            if (!event.isSimulated()) {
                PendingTill.mark(level, pos, SoilHorizon.TOPSOIL, Tillage.Implement.HOE);
            }
            event.setFinalState(TilthBlocks.farmland(texture));
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();

        // Bone meal is a phosphorus and calcium fertiliser here, not instant growth.
        if (stack.is(Items.BONE_MEAL) && !TilthConfig.BONE_MEAL_GROWS_CROPS.getAsBoolean()
                && (state.getBlock() instanceof TilthFarmlandBlock || state.getBlock() instanceof TilthCropBlock)) {
            InteractionResult result = AmendmentItem.apply(level, pos, player, stack, Amendment.BONE_MEAL);
            event.setCancellationResult(result);
            event.setCanceled(true);
            return;
        }

        // Vanilla seeds plant the realistic Tilth crop on Tilth farmland.
        CropSpecies species = VANILLA_SEEDS.get(stack.getItem());
        if (species != null && TilthConfig.REDIRECT_VANILLA_SEEDS.getAsBoolean() && state.getBlock() instanceof TilthFarmlandBlock
                && event.getFace() == Direction.UP && level.getBlockState(pos.above()).isAir()) {
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos.above(), TilthBlocks.crop(species).get().defaultBlockState());
                level.playSound(null, pos.above(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            event.setCanceled(true);
        }
    }

    /** Adult livestock leave manure behind. */
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Animal animal) || animal.level().isClientSide || animal.isBaby()) {
            return;
        }
        int interval = TilthConfig.MANURE_INTERVAL.getAsInt();
        // With husbandry on, manure comes from Livestock and depends on the animal's condition.
        if (TilthConfig.LIVESTOCK.getAsBoolean() || interval <= 0 || !(animal instanceof Cow || animal instanceof Pig || animal instanceof Sheep || animal instanceof Goat
                || animal instanceof Chicken || animal instanceof AbstractHorse)) {
            return;
        }
        if (animal.getRandom().nextInt(interval) == 0) {
            if (!(animal.level() instanceof net.minecraft.server.level.ServerLevel server)
                    || !com.claudacious.tilth.block.ManureLayerBlock.deposit(server, animal.blockPosition())) {
                animal.spawnAtLocation(new ItemStack(TilthItems.MANURE.get()));
            }
        }
    }
}
