package com.claudacious.tilth.item;

import com.claudacious.tilth.block.HayWindrowBlock;
import com.claudacious.tilth.registry.TilthBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Grass cut with a hoe. Lay it on the ground in a windrow to make hay (see {@link HayWindrowBlock}), or compost it. */
public class CutGrassItem extends Item {
    public CutGrassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        if (HayWindrowBlock.addLayer(level, clicked)) {
            context.getItemInHand().consume(1, context.getPlayer());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        BlockPos pos = clicked.relative(context.getClickedFace());
        BlockState windrow = TilthBlocks.HAY_WINDROW.get().defaultBlockState();
        if (!level.getBlockState(pos).canBeReplaced() || !windrow.canSurvive(level, pos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, windrow, 3);
            level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
