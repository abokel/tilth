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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** A fertiliser or soil amendment spread on farmland (or on the soil under a growing crop). */
public class AmendmentItem extends Item {
    private final Amendment amendment;

    public AmendmentItem(Amendment amendment, Properties properties) {
        super(properties);
        this.amendment = amendment;
    }

    public Amendment amendment() {
        return amendment;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return apply(context.getLevel(), context.getClickedPos(), context.getPlayer(), context.getItemInHand(), amendment);
    }

    /** Applies an amendment to the farmland at (or under) a position. */
    public static InteractionResult apply(Level level, BlockPos clicked, Player player, ItemStack stack, Amendment amendment) {
        BlockPos pos = clicked;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TilthCropBlock || state.getBlock() instanceof DeadCropBlock) {
            pos = pos.below();
        }
        if (!(level.getBlockEntity(pos) instanceof FarmlandBlockEntity farmland)) {
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(Component.literal("Spread " + amendment.displayName().toLowerCase() + " on tilled farmland.")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            farmland.advance(server);
            amendment.applyTo(farmland.soil(), farmland.texture());
            farmland.setChanged();
            server.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 10, 0.3, 0.05, 0.3, 0.0);
            level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.8f, 1.0f);
            if (player != null) {
                player.displayClientMessage(Component.literal("Applied " + amendment.displayName() + ": " + amendment.summary())
                        .withStyle(ChatFormatting.DARK_GREEN), true);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(amendment.summary()).withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.literal("Use on farmland").withStyle(ChatFormatting.GRAY));
    }
}
