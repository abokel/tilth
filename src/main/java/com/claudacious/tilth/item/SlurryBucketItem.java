package com.claudacious.tilth.item;

import com.claudacious.tilth.block.DeadCropBlock;
import com.claudacious.tilth.block.FarmlandBlockEntity;
import com.claudacious.tilth.block.TilthCropBlock;
import com.claudacious.tilth.sim.soil.Amendment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Liquid manure. Poured on farmland it gives one dose to every block of a 3x3 patch (under crops too) and
 * leaves an empty bucket; in the fertiliser spreader one bucket is nine doses.
 */
public class SlurryBucketItem extends Item {
    public static final int DOSES = 9;

    public SlurryBucketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos clicked = context.getClickedPos();
        Block block = context.getLevel().getBlockState(clicked).getBlock();
        BlockPos center = block instanceof TilthCropBlock || block instanceof DeadCropBlock ? clicked.below() : clicked;
        if (!(context.getLevel().getBlockEntity(center) instanceof FarmlandBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel() instanceof ServerLevel level) {
            int dosed = 0;
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
                if (level.getBlockEntity(p) instanceof FarmlandBlockEntity farmland) {
                    farmland.advance(level);
                    Amendment.SLURRY.applyTo(farmland.soil(), farmland.texture());
                    farmland.setChanged();
                    level.sendParticles(ParticleTypes.FALLING_DRIPSTONE_WATER, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 2, 0.3, 0.05, 0.3, 0.0);
                    dosed++;
                }
            }
            level.playSound(null, center, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0f, 0.7f);
            Player player = context.getPlayer();
            if (player != null) {
                player.displayClientMessage(Component.literal("Spread slurry on " + dosed + " blocks: " + Amendment.SLURRY.summary())
                        .withStyle(ChatFormatting.DARK_GREEN), true);
                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                    ItemStack empty = new ItemStack(Items.BUCKET);
                    if (!player.getInventory().add(empty)) {
                        player.drop(empty, false);
                    }
                }
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Pour on farmland: one dose on each block of a 3x3 patch").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Or load the fertiliser spreader: " + DOSES + " doses per bucket").withStyle(ChatFormatting.DARK_GRAY));
    }
}
