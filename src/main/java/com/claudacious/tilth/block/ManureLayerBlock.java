package com.claudacious.tilth.block;

import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.sim.soil.Amendment;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dung left by livestock, lying in layers like snow. Shovel it up for manure (by hand it is just scattered),
 * then mix it with water into slurry to spread. Left alone, a layer slowly rots into the ground, feeding the
 * farmland or soil beneath it.
 */
public class ManureLayerBlock extends SnowLayerBlock {
    public static final MapCodec<SnowLayerBlock> CODEC = simpleCodec(ManureLayerBlock::new);
    /** Animals pile it no deeper than this. */
    public static final int MAX_PILE = 4;

    public ManureLayerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SnowLayerBlock> codec() {
        return CODEC;
    }

    /** No melting in light; instead a layer decomposes now and then, feeding farmland underneath. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) != 0) {
            return;
        }
        if (level.getBlockEntity(pos.below()) instanceof FarmlandBlockEntity farmland) {
            farmland.advance(level);
            Amendment.SLURRY.applyTo(farmland.soil(), farmland.texture());
            farmland.setChanged();
        }
        int layers = state.getValue(LAYERS);
        if (layers > 1) {
            level.setBlockAndUpdate(pos, state.setValue(LAYERS, layers - 1));
        } else {
            level.removeBlock(pos, false);
        }
    }

    /**
     * Drops one layer of dung at an animal's feet, piling on what is there. Returns false where it cannot lie
     * (on crops, in water, on a slab...).
     */
    public static boolean deposit(ServerLevel level, BlockPos feet) {
        BlockState at = level.getBlockState(feet);
        if (at.getBlock() instanceof ManureLayerBlock) {
            int layers = at.getValue(LAYERS);
            if (layers < MAX_PILE) {
                level.setBlockAndUpdate(feet, at.setValue(LAYERS, layers + 1));
            }
            return true;
        }
        BlockState layer = TilthBlocks.MANURE_LAYER.get().defaultBlockState();
        if ((at.isAir() || at.canBeReplaced() && at.getFluidState().isEmpty()) && layer.canSurvive(level, feet)) {
            level.setBlockAndUpdate(feet, layer);
            return true;
        }
        return false;
    }
}
