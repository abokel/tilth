package com.claudacious.tilth.entity;

import com.claudacious.tilth.registry.TilthEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Briefly escaped incubator anomaly: leaves a little slime and dissolves harmlessly. */
public class HomunculusEntity extends Slime {
    private int remainingSteps = 5;
    private int life = 120;

    public HomunculusEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
        setSize(1, true);
        setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Monster defaults include the attack damage that Slime.setSize writes; without it the entity cannot be made.
        return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.18);
    }

    /** It is harmless, so Peaceful does not clear it away like a hostile slime. */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected boolean isDealsDamage() {
        return false;
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                                                  net.minecraft.world.DifficultyInstance difficulty,
                                                                  net.minecraft.world.entity.MobSpawnType spawnType,
                                                                  net.minecraft.world.entity.SpawnGroupData data) {
        net.minecraft.world.entity.SpawnGroupData out = super.finalizeSpawn(level, difficulty, spawnType, data);
        // Always the smallest size, whatever a slime would roll.
        setSize(1, true);
        return out;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (--life <= 0 || remainingSteps <= 0) {
            discard();
            return;
        }
        if (tickCount % 10 != 0) {
            return;
        }
        BlockPos from = blockPosition();
        int dx = random.nextInt(3) - 1;
        int dz = random.nextInt(3) - 1;
        if (dx == 0 && dz == 0) {
            dx = 1;
        }
        moveTo(getX() + dx, getY(), getZ() + dz, getYRot(), getXRot());
        if (from.distManhattan(blockPosition()) > 0 && random.nextBoolean()) {
            spawnAtLocation(new ItemStack(Items.SLIME_BALL));
        }
        remainingSteps--;
    }

    public static void spawn(ServerLevel level, BlockPos pos) {
        HomunculusEntity homunculus = TilthEntities.HOMUNCULUS.get().create(level);
        if (homunculus == null) {
            return;
        }
        homunculus.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0);
        level.addFreshEntity(homunculus);
    }
}
