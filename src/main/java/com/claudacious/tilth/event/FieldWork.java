package com.claudacious.tilth.event;

import com.claudacious.tilth.block.CropBlockEntity;
import com.claudacious.tilth.block.FarmlandBlockEntity;
import com.claudacious.tilth.block.TilthCropBlock;
import com.claudacious.tilth.sim.crop.CoverCrops;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.CropState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Hoe work among growing crops. Right-click a crop with a hoe to weed it and the crops around it (a 3x3 patch);
 * sneak-right-click to cut a green crop down and work it into the soil as green manure, as with a cover crop.
 */
public final class FieldWork {
    private FieldWork() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof TilthCropBlock) || !stack.canPerformAction(ItemAbilities.HOE_TILL)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        if (player.isSecondaryUseActive()) {
            greenManure(level, pos, player);
        } else {
            weed(level, pos, player);
        }
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }

    /** Grass cut with a hoe gives cut grass for hay or compost (one armful, two from tall grass). */
    public static void onBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        Player player = event.getPlayer();
        if (!(event.getLevel() instanceof ServerLevel level) || player.getAbilities().instabuild
                || !player.getMainHandItem().canPerformAction(ItemAbilities.HOE_TILL)) {
            return;
        }
        int count = state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN) ? 1
                : state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN) ? 2 : 0;
        if (count > 0) {
            net.minecraft.world.level.block.Block.popResource(level, event.getPos(),
                    new ItemStack(com.claudacious.tilth.registry.TilthItems.CUT_GRASS.get(), count));
        }
    }

    private static void weed(ServerLevel level, BlockPos crop, Player player) {
        double worst = 0;
        int fields = 0;
        for (BlockPos p : BlockPos.betweenClosed(crop.offset(-1, -1, -1), crop.offset(1, -1, 1))) {
            if (level.getBlockEntity(p) instanceof FarmlandBlockEntity farmland) {
                farmland.advance(level);
                worst = Math.max(worst, farmland.weeds());
                farmland.clearWeeds();
                fields++;
            }
        }
        level.playSound(null, crop, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 0.8f, 1.2f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SHORT_GRASS.defaultBlockState()),
                crop.getX() + 0.5, crop.getY() + 0.2, crop.getZ() + 0.5, 12, 1.0, 0.1, 1.0, 0.0);
        player.displayClientMessage(Component.literal(fields == 0 ? "Nothing to weed here."
                : worst < 0.05 ? "Hoed. The rows were already clean."
                : String.format("Hoed %d rows; the worst was %.0f%% weeds.", fields, worst * 100)).withStyle(ChatFormatting.GREEN), true);
    }

    private static void greenManure(ServerLevel level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof CropBlockEntity crop) || !(level.getBlockEntity(pos.below()) instanceof FarmlandBlockEntity farmland)) {
            return;
        }
        farmland.advance(level);
        CropSpecies species = crop.species();
        CropState cs = crop.cropState();
        double residue = CoverCrops.greenManureResidue(cs.biomass);
        double nPercent = CoverCrops.greenManureNitrogen(species.isLegume());
        farmland.soil().addResidue(residue, nPercent);
        farmland.clearWeeds();
        farmland.setChanged();
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SHORT_GRASS.defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 20, 0.3, 0.2, 0.3, 0.0);
        player.displayClientMessage(Component.literal(String.format("%s worked in as green manure: %.2f kg/m² of residue at %.1f%% nitrogen.",
                species.name, residue, nPercent)).withStyle(ChatFormatting.GREEN), true);
    }
}
