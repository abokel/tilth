package com.claudacious.tilth.block;

import com.claudacious.tilth.postharvest.GrainStacks;
import com.claudacious.tilth.registry.TilthBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A grain silo segment. Twenty of them stacked as a 2x2 tower five blocks high form one silo: the blocks then draw as
 * a single corrugated tower with a conical roof, and the lowest north-west block (the controller) holds the grain for
 * the whole silo. Any segment of a built silo can be used to fill, empty or inspect it, and hoppers can feed it.
 *
 * <p>A built silo holds far more grain per block than chests, dries it with aeration, slows spoilage, and keeps out
 * the rodents that eat grain in open storage (see {@link com.claudacious.tilth.postharvest.GrainKeeping}).
 */
public class GrainSiloBlock extends BaseEntityBlock {
    public static final MapCodec<GrainSiloBlock> CODEC = simpleCodec(GrainSiloBlock::new);
    public static final int WIDTH = 2;
    public static final int HEIGHT = 5;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** Position in the 2x2 footprint: 0 north-west, 1 north-east, 2 south-west, 3 south-east. */
    public static final IntegerProperty QUAD = IntegerProperty.create("quad", 0, 3);
    /** Height in the tower, 0 at the bottom. */
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, HEIGHT - 1);

    public GrainSiloBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(QUAD, 0).setValue(TIER, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, QUAD, TIER);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrainSiloBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TilthBlockEntities.GRAIN_SILO.get(), GrainSiloBlockEntity::serverTick);
    }

    // ------------------------------------------------------------------ structure

    /** The controller of a built silo: the bottom north-west segment. */
    public static BlockPos controller(BlockPos pos, BlockState state) {
        int quad = state.getValue(QUAD);
        return pos.offset(-(quad & 1), -state.getValue(TIER), -(quad >> 1));
    }

    /** The silo's storage, from any of its segments: the controller's when built, the segment's own otherwise. */
    @Nullable
    public static GrainSiloBlockEntity storage(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GrainSiloBlock)) {
            return null;
        }
        BlockPos at = state.getValue(FORMED) ? controller(pos, state) : pos;
        return level.getBlockEntity(at) instanceof GrainSiloBlockEntity silo ? silo : null;
    }

    private static boolean isSegment(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof GrainSiloBlock && !state.getValue(FORMED);
    }

    /** Tries every 2x2x5 box that could contain {@code placed}; forms the first one made entirely of loose segments. */
    private static void tryForm(Level level, BlockPos placed) {
        for (int ox = 0; ox < WIDTH; ox++) {
            for (int oz = 0; oz < WIDTH; oz++) {
                for (int oy = 0; oy < HEIGHT; oy++) {
                    BlockPos origin = placed.offset(-ox, -oy, -oz);
                    if (complete(level, origin)) {
                        form(level, origin);
                        return;
                    }
                }
            }
        }
    }

    private static boolean complete(Level level, BlockPos origin) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int quad = 0; quad < 4; quad++) {
                if (!isSegment(level, origin.offset(quad & 1, y, quad >> 1))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void form(Level level, BlockPos origin) {
        GrainSiloBlockEntity controller = level.getBlockEntity(origin) instanceof GrainSiloBlockEntity silo ? silo : null;
        for (int y = 0; y < HEIGHT; y++) {
            for (int quad = 0; quad < 4; quad++) {
                BlockPos p = origin.offset(quad & 1, y, quad >> 1);
                // Grain left in a loose segment (placed from a full silo item) moves into the controller.
                if (controller != null && !p.equals(origin) && level.getBlockEntity(p) instanceof GrainSiloBlockEntity part) {
                    part.pourInto(controller, p);
                }
                BlockState state = level.getBlockState(p).setValue(FORMED, true).setValue(QUAD, quad).setValue(TIER, y);
                level.setBlock(p, state, Block.UPDATE_CLIENTS);
            }
        }
        level.playSound(null, origin.above(2), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.4f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, origin.getX() + 1.0, origin.getY() + HEIGHT + 0.3, origin.getZ() + 1.0,
                    12, 0.8, 0.3, 0.8, 0.0);
        }
    }

    /** Takes a built silo apart: every remaining segment becomes loose again. The grain stays in the controller. */
    private static void unform(Level level, BlockPos origin, BlockPos removed) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int quad = 0; quad < 4; quad++) {
                BlockPos p = origin.offset(quad & 1, y, quad >> 1);
                BlockState state = level.getBlockState(p);
                if (!p.equals(removed) && state.getBlock() instanceof GrainSiloBlock && state.getValue(FORMED)) {
                    level.setBlock(p, state.setValue(FORMED, false).setValue(QUAD, 0).setValue(TIER, 0), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        // Checked a tick later, once a placed item's grain has reached the block entity, and for any kind of placement.
        if (!oldState.is(this) && !level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!state.getValue(FORMED)) {
            tryForm(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && state.getValue(FORMED) && !level.isClientSide) {
            unform(level, controller(pos, state), pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    // ------------------------------------------------------------------ use

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!GrainStacks.isStorableGrain(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        GrainSiloBlockEntity silo = storage(level, pos);
        if (silo == null || !state.getValue(FORMED)) {
            player.displayClientMessage(unbuiltMessage(level, pos), true);
            return ItemInteractionResult.FAIL;
        }
        int moved = silo.insert(stack);
        // Sneak to pour in every matching sack from your inventory at once.
        if (player.isSecondaryUseActive()) {
            for (ItemStack other : player.getInventory().items) {
                if (other != stack && GrainStacks.isStorableGrain(other)) {
                    moved += silo.insert(other);
                }
            }
        }
        if (moved <= 0) {
            player.displayClientMessage(Component.literal(silo.grainCount() >= silo.capacity() ? "The silo is full."
                    : "This silo holds " + silo.grainName() + ". Empty it before filling it with something else.").withStyle(ChatFormatting.YELLOW), true);
            return ItemInteractionResult.FAIL;
        }
        level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.8f, 0.7f);
        player.displayClientMessage(Component.literal("Poured in " + moved + ". " + silo.summary()).withStyle(ChatFormatting.GREEN), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        GrainSiloBlockEntity silo = storage(level, pos);
        if (silo == null || !state.getValue(FORMED)) {
            player.displayClientMessage(unbuiltMessage(level, pos), true);
            return InteractionResult.CONSUME;
        }
        ItemStack out = player.isSecondaryUseActive() ? silo.extractSpoiled() : silo.extractGrain(64);
        if (out.isEmpty()) {
            player.displayClientMessage(Component.literal(silo.summary()).withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        if (!player.getInventory().add(out)) {
            player.drop(out, false);
        }
        level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.8f, 0.9f);
        player.displayClientMessage(Component.literal(silo.summary()).withStyle(ChatFormatting.GRAY), true);
        return InteractionResult.CONSUME;
    }

    private static Component unbuiltMessage(Level level, BlockPos pos) {
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -4, -1), pos.offset(1, 4, 1))) {
            if (isSegment(level, p)) {
                count++;
            }
        }
        return Component.literal("Silo segment. Build twenty of them as a 2x2 tower five blocks high (" + Math.min(count, 20)
                + " nearby).").withStyle(ChatFormatting.YELLOW);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        GrainSiloBlockEntity silo = storage(level, pos);
        if (silo == null || !state.getValue(FORMED) || silo.grainCount() <= 0) {
            return 0;
        }
        return 1 + (int) (14.0 * silo.grainCount() / silo.capacity());
    }
}
