package com.claudacious.tilth.block;

import com.claudacious.tilth.quality.Quality;
import com.claudacious.tilth.registry.TilthItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hand-worked farm stations with no inventory of their own: the spinning wheel and the butter churn. Each turns one
 * held item into its product on right-click, keeping the input's grade.
 * <ul>
 *   <li>Spinning wheel: a fleece spins into two yarn; sneak to felt it into a block of wool of its colour instead.</li>
 *   <li>Butter churn: a bucket of milk churns into butter (more from richer milk); the bucket comes back.</li>
 * </ul>
 */
public class CraftStationBlock extends HorizontalDirectionalBlock {
    public enum Kind { SPINNING_WHEEL, CHURN }

    public static final MapCodec<CraftStationBlock> CODEC = simpleCodec(p -> new CraftStationBlock(p, Kind.SPINNING_WHEEL));
    private final Kind kind;

    public CraftStationBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == Kind.CHURN ? Block.box(4, 0, 4, 12, 15, 12) : Block.box(1, 0, 3, 15, 15, 13);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        ItemStack out;
        SoundEvent sound;
        if (kind == Kind.SPINNING_WHEEL && stack.is(TilthItems.FLEECE.get())) {
            int grade = Math.max(1, Quality.get(stack));
            DyeColor colour = stack.getOrDefault(DataComponents.BASE_COLOR, DyeColor.WHITE);
            if (player.isSecondaryUseActive()) {
                out = new ItemStack(woolOf(colour));
            } else {
                out = new ItemStack(TilthItems.YARN.get(), 2);
                out.set(DataComponents.BASE_COLOR, colour);
                Quality.set(out, grade);
            }
            sound = SoundEvents.WOOL_PLACE;
        } else if (kind == Kind.CHURN && stack.is(Items.MILK_BUCKET)) {
            int grade = Quality.get(stack) == 0 ? 3 : Quality.get(stack);
            out = new ItemStack(TilthItems.BUTTER.get(), 1 + grade / 2);
            Quality.set(out, grade);
            sound = SoundEvents.BUCKET_EMPTY;
        } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            boolean bucket = stack.is(Items.MILK_BUCKET);
            stack.consume(1, player);
            if (bucket && !player.getAbilities().instabuild) {
                give(player, new ItemStack(Items.BUCKET));
            }
            give(player, out);
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 0.9f);
            player.displayClientMessage(Component.literal((kind == Kind.CHURN ? "Churned " : "Spun ") + out.getCount() + " "
                    + out.getHoverName().getString() + (Quality.get(out) > 0 ? " (" + Quality.name(Quality.get(out)) + ")" : ""))
                    .withStyle(ChatFormatting.GREEN), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal(kind == Kind.CHURN ? "Right-click with a bucket of milk to churn butter."
                    : "Right-click with fleece to spin yarn; sneak to felt it into wool.").withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static net.minecraft.world.item.Item woolOf(DyeColor colour) {
        return switch (colour) {
            case BLACK -> Items.BLACK_WOOL;
            case BROWN -> Items.BROWN_WOOL;
            case GRAY -> Items.GRAY_WOOL;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
            default -> Items.WHITE_WOOL;
        };
    }
}
