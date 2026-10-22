package com.claudacious.tilth.block;

import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.sim.soil.SoilHorizon;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.sim.soil.Tillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

/** Grass-covered topsoil. Grass protects the soil from erosion and spreads onto bare topsoil. */
public class SoilGrassBlock extends GrassBlock implements SoilLike {
    private final SoilTexture texture;

    public SoilGrassBlock(SoilTexture texture, Properties properties) {
        super(properties);
        this.texture = texture;
    }

    @Override
    public SoilTexture texture() {
        return texture;
    }

    @Override
    public SoilHorizon horizon() {
        return SoilHorizon.TOPSOIL;
    }

    static boolean canStayGrass(LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState a = level.getBlockState(above);
        if (a.is(Blocks.SNOW) && a.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        if (a.getFluidState().getAmount() == 8) {
            return false;
        }
        return a.getLightBlock(level, above) < 15;
    }

    private static boolean canPropagate(LevelReader level, BlockPos pos) {
        return canStayGrass(level, pos) && !level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canStayGrass(level, pos)) {
            if (level.isAreaLoaded(pos, 1)) {
                level.setBlockAndUpdate(pos, TilthBlocks.topsoil(texture));
            }
            return;
        }
        if (!level.isAreaLoaded(pos, 3)) {
            return;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9) {
            for (int i = 0; i < 4; i++) {
                BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
                BlockState t = level.getBlockState(target);
                SoilTexture spreadTo = null;
                if (t.getBlock() instanceof SoilBlock soil && soil.horizon() == SoilHorizon.TOPSOIL) {
                    spreadTo = soil.texture();
                } else if (t.is(Blocks.DIRT)) {
                    spreadTo = texture;
                }
                if (spreadTo != null && canPropagate(level, target)) {
                    level.setBlockAndUpdate(target, TilthBlocks.grass(spreadTo)
                            .setValue(SNOWY, level.getBlockState(target.above()).is(Blocks.SNOW)));
                }
            }
        }
        SoilMorphing.tick(level, pos, state, this, random);
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
        if (ability == ItemAbilities.HOE_TILL && context.getClickedFace() != Direction.DOWN
                && context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
            if (!simulate) {
                PendingTill.mark(context.getLevel(), context.getClickedPos(), SoilHorizon.TOPSOIL, Tillage.Implement.HOE);
            }
            return TilthBlocks.farmland(texture);
        }
        if (ability == ItemAbilities.SHOVEL_FLATTEN && context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
            return Blocks.DIRT_PATH.defaultBlockState();
        }
        return super.getToolModifiedState(state, context, ability, simulate);
    }
}
