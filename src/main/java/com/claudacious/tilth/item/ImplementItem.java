package com.claudacious.tilth.item;

import com.claudacious.tilth.entity.DrawnImplementEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/** Places a horse-drawn implement (plough, spreader) in the world. */
public class ImplementItem extends Item {
    private final Supplier<? extends EntityType<? extends DrawnImplementEntity>> type;
    private final List<String> description;

    public ImplementItem(Properties properties, Supplier<? extends EntityType<? extends DrawnImplementEntity>> type, String... description) {
        super(properties);
        this.type = type;
        this.description = List.of(description);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level instanceof ServerLevel server) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            DrawnImplementEntity implement = type.get().create(server);
            if (implement == null) {
                return InteractionResult.FAIL;
            }
            implement.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getRotation(), 0);
            if (!server.noCollision(implement, implement.getBoundingBox())) {
                return InteractionResult.FAIL;
            }
            server.addFreshEntity(implement);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (int i = 0; i < description.size(); i++) {
            tooltip.add(Component.literal(description.get(i)).withStyle(i == 0 ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
        }
    }
}
