package com.claudacious.tilth.entity;

import com.claudacious.tilth.block.DeadCropBlock;
import com.claudacious.tilth.block.FarmlandBlockEntity;
import com.claudacious.tilth.block.TilthCropBlock;
import com.claudacious.tilth.item.AmendmentItem;
import com.claudacious.tilth.item.SlurryBucketItem;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.soil.Amendment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A horse-drawn broadcast spreader. Load its hopper with one kind of fertiliser or amendment (slurry, compost,
 * lime, wood ash, sulfur, bone meal, straw); opened and pulled across a field, it spreads one dose on each
 * block of a three-block swath, under growing crops too.
 */
public class SpreaderEntity extends DrawnImplementEntity {
    public static final int CAPACITY = 128;
    private static final EntityDataAccessor<ItemStack> LOAD = SynchedEntityData.defineId(SpreaderEntity.class, EntityDataSerializers.ITEM_STACK);

    public SpreaderEntity(EntityType<? extends SpreaderEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOAD, ItemStack.EMPTY);
    }

    public ItemStack load() {
        return entityData.get(LOAD);
    }

    private void setLoad(ItemStack stack) {
        entityData.set(LOAD, stack.copy());
    }

    @Nullable
    public static Amendment amendmentOf(ItemStack stack) {
        if (stack.getItem() instanceof AmendmentItem a) {
            return a.amendment();
        }
        if (stack.getItem() instanceof SlurryBucketItem) {
            return Amendment.SLURRY;
        }
        return stack.is(Items.BONE_MEAL) ? Amendment.BONE_MEAL : null;
    }

    /** Doses one item puts in the hopper: a bucket of slurry is nine, everything else one. */
    private static int dosesPer(ItemStack stack) {
        return stack.getItem() instanceof SlurryBucketItem ? SlurryBucketItem.DOSES : 1;
    }

    @Override
    protected ItemStack item() {
        return new ItemStack(TilthItems.SPREADER.get());
    }

    @Override
    protected double draft() {
        return 0.15;
    }

    @Override
    protected String loweredMessage(boolean lowered) {
        ItemStack load = load();
        String left = load.isEmpty() ? " (empty)" : " (" + load.getCount() + " doses of " + amendmentOf(load).displayName() + " left)";
        return (lowered ? "Hopper open: spreading" : "Hopper closed") + left;
    }

    @Override
    protected InteractionResult interactExtra(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (amendmentOf(held) == null || player.isSecondaryUseActive()) {
            if (held.isEmpty() && !player.isSecondaryUseActive() && player.getVehicle() == null && puller() == null) {
                ItemStack load = load();
                player.displayClientMessage(Component.literal(load.isEmpty() ? "The hopper is empty. Load it with fertiliser."
                        : "Hopper: " + load.getCount() + " " + load.getHoverName().getString()).withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        ItemStack load = load();
        if (!load.isEmpty() && !ItemStack.isSameItemSameComponents(load, held)) {
            player.displayClientMessage(Component.literal("The hopper holds " + load.getHoverName().getString() + ". Empty it first (break the spreader).")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        int per = dosesPer(held);
        int room = (CAPACITY - load.getCount()) / per;
        int moved = Math.min(room, held.getCount());
        if (moved <= 0) {
            player.displayClientMessage(Component.literal("The hopper is full").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }
        int doses = moved * per;
        ItemStack next = load.isEmpty() ? held.copyWithCount(doses) : load.copyWithCount(load.getCount() + doses);
        setLoad(next);
        if (!player.getAbilities().instabuild) {
            held.shrink(moved);
            if (per > 1) {
                ItemStack buckets = new ItemStack(Items.BUCKET, moved);
                if (!player.getInventory().add(buckets)) {
                    player.drop(buckets, false);
                }
            }
        }
        playSound(per > 1 ? SoundEvents.BUCKET_EMPTY : SoundEvents.COMPOSTER_FILL, 1.0f, 0.9f);
        player.displayClientMessage(Component.literal("Hopper: " + next.getCount() + " doses of " + amendmentOf(next).displayName())
                .withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean work(ServerLevel level, BlockPos ground, @Nullable Player operator) {
        ItemStack load = load();
        Amendment amendment = amendmentOf(load);
        if (amendment == null || load.isEmpty()) {
            return false;
        }
        BlockState state = level.getBlockState(ground);
        BlockPos pos = state.getBlock() instanceof TilthCropBlock || state.getBlock() instanceof DeadCropBlock ? ground.below() : ground;
        if (!(level.getBlockEntity(pos) instanceof FarmlandBlockEntity farmland)) {
            return false;
        }
        farmland.advance(level);
        amendment.applyTo(farmland.soil(), farmland.texture());
        farmland.setChanged();
        setLoad(load.copyWithCount(load.getCount() - 1));
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, load), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                4, 0.3, 0.05, 0.3, 0.03);
        if (load.getCount() == 1 && operator != null) {
            operator.displayClientMessage(Component.literal("The spreader is empty").withStyle(ChatFormatting.YELLOW), true);
        }
        return true;
    }

    @Override
    protected void afterWork(ServerLevel level, BlockPos ground, double x, double z) {
        if (tickCount % 10 == 0) {
            level.playSound(null, blockPosition(), SoundEvents.SAND_STEP, SoundSource.BLOCKS, 0.5f, 1.4f);
        }
    }

    @Override
    protected void dropContents() {
        if (!load().isEmpty()) {
            ItemStack load = load();
            if (dosesPer(load) > 1) {
                // Whole buckets of slurry come back; a part-bucket soaks away.
                for (int i = 0; i < load.getCount() / dosesPer(load); i++) {
                    spawnAtLocation(load.copyWithCount(1));
                }
            } else {
                while (!load.isEmpty()) {
                    spawnAtLocation(load.split(load.getMaxStackSize()));
                }
            }
            setLoad(ItemStack.EMPTY);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Load")) {
            ItemStack stack = ItemStack.parseOptional(registryAccess(), tag.getCompound("Load"));
            stack.setCount(tag.getInt("LoadCount"));
            setLoad(stack);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ItemStack load = load();
        if (!load.isEmpty()) {
            tag.put("Load", load.copyWithCount(1).save(registryAccess()));
            tag.putInt("LoadCount", load.getCount());
        }
    }
}
