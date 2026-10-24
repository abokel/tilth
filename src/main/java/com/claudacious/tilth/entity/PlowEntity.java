package com.claudacious.tilth.entity;

import com.claudacious.tilth.block.Tilling;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.soil.Tillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A three-bottom moldboard plough. Hitch it to a horse, donkey or mule you are riding, lower the shares,
 * and it turns a three-block swath behind it as it moves.
 */
public class PlowEntity extends DrawnImplementEntity {
    public PlowEntity(EntityType<? extends PlowEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected ItemStack item() {
        return new ItemStack(TilthItems.PLOW.get());
    }

    @Override
    protected double draft() {
        return 0.35;
    }

    @Override
    protected String loweredMessage(boolean lowered) {
        return lowered ? "Shares lowered: ploughing" : "Shares raised: transport";
    }

    @Override
    protected boolean work(ServerLevel level, BlockPos ground, @Nullable Player operator) {
        BlockState state = level.getBlockState(ground);
        if (!Tilling.isTillable(state) || !Tilling.till(level, ground, Tillage.Implement.MOLDBOARD_PLOW, operator)) {
            return false;
        }
        if (random.nextInt(3) == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), ground.getX() + 0.5, ground.getY() + 1.0,
                    ground.getZ() + 0.5, 4, 0.2, 0.05, 0.2, 0.05);
        }
        return true;
    }

    @Override
    protected void afterWork(ServerLevel level, BlockPos ground, double x, double z) {
        if (tickCount % 12 == 0) {
            level.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 0.6f, 0.7f);
        }
    }
}
