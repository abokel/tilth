package com.claudacious.tilth.entity;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.block.Tilling;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.soil.Tillage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A three-bottom moldboard plough. Hitch it to a horse, donkey or mule you are riding (or drag it
 * yourself, slowly), lower the shares, and it turns a three-block swath behind it as it moves. Draft
 * resistance slows whatever is pulling it.
 */
public class PlowEntity extends Entity {
    private static final EntityDataAccessor<Boolean> LOWERED = SynchedEntityData.defineId(PlowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PULLER = SynchedEntityData.defineId(PlowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(PlowEntity.class, EntityDataSerializers.FLOAT);
    private static final ResourceLocation DRAG = Tilth.id("plow_draft");
    private static final double HITCH_LENGTH = 2.6;
    private static final double MAX_HITCH_DISTANCE = 10.0;

    /** Blocks tilled in this pass, with the tick they were tilled, so a slow pass counts once per block. */
    private final java.util.Map<BlockPos, Integer> recentlyTilled = new java.util.HashMap<>();
    @Nullable
    private UUID pullerId;
    @Nullable
    private LivingEntity dragged;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;

    public PlowEntity(EntityType<? extends PlowEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LOWERED, false);
        builder.define(PULLER, -1);
        builder.define(DAMAGE, 0.0f);
    }

    public boolean isLowered() {
        return entityData.get(LOWERED);
    }

    public void setLowered(boolean lowered) {
        entityData.set(LOWERED, lowered);
    }

    @Nullable
    public Entity puller() {
        int id = entityData.get(PULLER);
        return id < 0 ? null : level().getEntity(id);
    }

    @Override
    public float maxUpStep() {
        return 1.0f;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(TilthItems.PLOW.get());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            setLowered(!isLowered());
            playSound(isLowered() ? SoundEvents.ANVIL_PLACE : SoundEvents.CHAIN_PLACE, 0.4f, isLowered() ? 0.7f : 1.2f);
            player.displayClientMessage(Component.literal(isLowered() ? "Shares lowered: ploughing" : "Shares raised: transport")
                    .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.SUCCESS;
        }
        Entity current = puller();
        Entity candidate = player.getVehicle() instanceof AbstractHorse horse ? horse : player;
        if (current != null) {
            if (current == candidate || current == player || current == player.getVehicle()) {
                unhitch();
                player.displayClientMessage(Component.literal("Unhitched").withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (candidate.distanceTo(this) > 6.0) {
            player.displayClientMessage(Component.literal("Too far away to hitch").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        hitch(candidate);
        player.displayClientMessage(Component.literal(candidate == player ? "Hitched to you. Sneak + right-click to lower the shares."
                : "Hitched to your " + candidate.getName().getString() + ". Sneak + right-click to lower the shares.").withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    private void hitch(Entity puller) {
        pullerId = puller.getUUID();
        entityData.set(PULLER, puller.getId());
        playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0f, 1.0f);
    }

    private void unhitch() {
        releaseDrag();
        pullerId = null;
        entityData.set(PULLER, -1);
        playSound(SoundEvents.LEASH_KNOT_BREAK, 1.0f, 1.0f);
    }

    @Nullable
    private Entity resolvePuller() {
        Entity p = puller();
        if (p == null && pullerId != null && level() instanceof ServerLevel server) {
            p = server.getEntity(pullerId);
            if (p != null) {
                entityData.set(PULLER, p.getId());
            }
        }
        return p;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return true;
        }
        if (isInvulnerableTo(source)) {
            return false;
        }
        boolean creative = source.getEntity() instanceof Player p && p.getAbilities().instabuild;
        float damage = entityData.get(DAMAGE) + amount * 10.0f;
        entityData.set(DAMAGE, damage);
        if (creative || damage > 40.0f) {
            if (!creative) {
                spawnAtLocation(new ItemStack(TilthItems.PLOW.get()));
            }
            releaseDrag();
            discard();
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickLerp();
            return;
        }
        float damage = entityData.get(DAMAGE);
        if (damage > 0) {
            entityData.set(DAMAGE, Math.max(0, damage - 1));
        }
        Entity puller = resolvePuller();
        if (puller != null && (!puller.isAlive() || puller.level() != level() || puller.distanceTo(this) > MAX_HITCH_DISTANCE)) {
            unhitch();
            puller = null;
        }
        double fall = onGround() ? 0 : getDeltaMovement().y - 0.08;
        Vec3 before = position();
        if (puller != null) {
            Vec3 toPuller = new Vec3(puller.getX() - getX(), 0, puller.getZ() - getZ());
            double dist = toPuller.length();
            if (dist > 0.05) {
                setYRot((float) (Mth.atan2(toPuller.z, toPuller.x) * Mth.RAD_TO_DEG) - 90.0f);
            }
            if (dist > HITCH_LENGTH) {
                Vec3 target = puller.position().subtract(toPuller.normalize().scale(HITCH_LENGTH));
                move(MoverType.SELF, new Vec3(target.x - getX(), fall, target.z - getZ()));
            } else {
                move(MoverType.SELF, new Vec3(0, fall, 0));
            }
            applyDrag(puller);
        } else {
            releaseDrag();
            move(MoverType.SELF, new Vec3(0, fall, 0));
        }
        setDeltaMovement(0, onGround() ? 0 : fall, 0);
        double moved = position().subtract(before).horizontalDistance();
        if (isLowered() && puller != null && moved > 0.01) {
            plough(puller);
        }
    }

    private void plough(Entity puller) {
        ServerLevel level = (ServerLevel) level();
        float yawRad = getYRot() * Mth.DEG_TO_RAD;
        double forwardX = -Mth.sin(yawRad);
        double forwardZ = Mth.cos(yawRad);
        double rightX = -forwardZ;
        double rightZ = forwardX;
        Player operator = puller instanceof Player p ? p : (puller.getControllingPassenger() instanceof Player p2 ? p2 : null);
        if (tickCount % 20 == 0) {
            recentlyTilled.values().removeIf(t -> tickCount - t > 60);
        }
        for (int offset = -1; offset <= 1; offset++) {
            double x = getX() + rightX * offset - forwardX * 0.6;
            double z = getZ() + rightZ * offset - forwardZ * 0.6;
            BlockPos ground = BlockPos.containing(x, getY() - 0.3, z);
            BlockState state = level.getBlockState(ground);
            if (!Tilling.isTillable(state) || recentlyTilled.containsKey(ground)) {
                continue;
            }
            if (Tilling.till(level, ground, Tillage.Implement.MOLDBOARD_PLOW, operator)) {
                recentlyTilled.put(ground.immutable(), tickCount);
                if (random.nextInt(3) == 0) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, ground.getY() + 1.0, z, 4, 0.2, 0.05, 0.2, 0.05);
                }
            }
        }
        if (tickCount % 12 == 0) {
            level.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 0.6f, 0.7f);
        }
    }

    private void applyDrag(Entity puller) {
        if (!(puller instanceof LivingEntity living)) {
            return;
        }
        if (dragged != null && dragged != living) {
            releaseDrag();
        }
        AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean shouldDrag = isLowered();
        if (shouldDrag && !speed.hasModifier(DRAG)) {
            double amount = living instanceof Player ? -0.6 : -0.35;
            speed.addTransientModifier(new AttributeModifier(DRAG, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            dragged = living;
        } else if (!shouldDrag && speed.hasModifier(DRAG)) {
            speed.removeModifier(DRAG);
        }
    }

    private void releaseDrag() {
        if (dragged != null) {
            AttributeInstance speed = dragged.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(DRAG);
            }
            dragged = null;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseDrag();
        super.remove(reason);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = 10;
    }

    private void tickLerp() {
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, getXRot());
            lerpSteps--;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setLowered(tag.getBoolean("Lowered"));
        if (tag.hasUUID("Puller")) {
            pullerId = tag.getUUID("Puller");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Lowered", isLowered());
        if (pullerId != null) {
            tag.putUUID("Puller", pullerId);
        }
    }
}
