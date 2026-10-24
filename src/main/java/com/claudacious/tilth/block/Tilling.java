package com.claudacious.tilth.block;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.plot.PlotRules;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.sim.soil.SoilHorizon;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.sim.soil.Tillage;
import com.claudacious.tilth.world.SoilService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Shared tillage logic for the hoe, ard and plough. */
public final class Tilling {
    private Tilling() {
    }

    /** Whether the block can be turned into farmland (ignoring what is above it). */
    public static boolean isTillable(BlockState state) {
        if (state.getBlock() instanceof TilthFarmlandBlock) {
            return true;
        }
        if (state.getBlock() instanceof SoilLike soil) {
            return soil.horizon() != SoilHorizon.GLEY;
        }
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
                || (state.is(Blocks.SAND) && TilthConfig.TILL_VANILLA_SAND.getAsBoolean());
    }

    /**
     * Tills one block. Plants growing on it are uprooted and become residue.
     *
     * @return true if the block is farmland afterwards
     */
    public static boolean till(ServerLevel level, BlockPos pos, Tillage.Implement implement, @Nullable Player player) {
        BlockState state = level.getBlockState(pos);
        if (!isTillable(state) || !PlotRules.mayFarm(player, level, pos)) {
            return false;
        }
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        boolean plant = aboveState.getBlock() instanceof TilthCropBlock || aboveState.getBlock() instanceof DeadCropBlock;
        if (!aboveState.isAir() && !aboveState.canBeReplaced() && !plant) {
            return false;
        }

        if (state.getBlock() instanceof TilthFarmlandBlock) {
            if (!aboveState.isAir()) {
                level.destroyBlock(above, false);
            }
            if (level.getBlockEntity(pos) instanceof FarmlandBlockEntity be) {
                be.retill(level, implement, player);
            }
            return true;
        }

        SoilTexture texture;
        SoilHorizon horizon = SoilHorizon.TOPSOIL;
        if (state.getBlock() instanceof SoilLike soil) {
            texture = soil.texture();
            horizon = soil.horizon();
        } else if (state.is(Blocks.SAND)) {
            texture = SoilTexture.SAND;
        } else if (TilthConfig.CONVERT_VANILLA_ON_TILL.getAsBoolean()) {
            texture = SoilService.guessTexture(level, pos);
        } else {
            return false;
        }
        if (!aboveState.isAir()) {
            level.destroyBlock(above, false);
        }
        PendingTill.mark(level, pos, horizon, implement);
        level.setBlock(pos, TilthBlocks.farmland(texture), 3);
        if (level.getBlockEntity(pos) instanceof FarmlandBlockEntity be) {
            String warning = be.ensureInitialized(level);
            if (warning != null && player != null) {
                player.displayClientMessage(Component.literal(warning).withStyle(ChatFormatting.GOLD), true);
            }
        }
        return true;
    }
}
