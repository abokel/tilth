package com.claudacious.tilth.block;

import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.sim.soil.SoilHorizon;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.sim.soil.Tillage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

/** Untilled soil: an A (topsoil), B (subsoil) or Bg (gley) horizon of one texture class. */
public class SoilBlock extends Block implements SoilLike {
    public static final MapCodec<SoilBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("texture").forGetter(b -> b.texture.id()),
            Codec.STRING.fieldOf("horizon").forGetter(b -> b.horizon.name()),
            propertiesCodec()
    ).apply(i, (t, h, p) -> new SoilBlock(SoilTexture.byId(t), SoilHorizon.valueOf(h), p)));

    private final SoilTexture texture;
    private final SoilHorizon horizon;

    public SoilBlock(SoilTexture texture, SoilHorizon horizon, Properties properties) {
        super(properties);
        this.texture = texture;
        this.horizon = horizon;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public SoilTexture texture() {
        return texture;
    }

    @Override
    public SoilHorizon horizon() {
        return horizon;
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
        if (ability == ItemAbilities.HOE_TILL && horizon != SoilHorizon.GLEY && context.getClickedFace() != Direction.DOWN
                && context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
            if (!simulate) {
                PendingTill.mark(context.getLevel(), context.getClickedPos(), horizon, Tillage.Implement.HOE);
            }
            return TilthBlocks.farmland(texture);
        }
        return super.getToolModifiedState(state, context, ability, simulate);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        SoilMorphing.tick(level, pos, state, this, random);
    }
}
