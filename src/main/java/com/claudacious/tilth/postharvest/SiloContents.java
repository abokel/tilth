package com.claudacious.tilth.postharvest;

import com.claudacious.tilth.sim.postharvest.GrainDryingModel;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The grain a silo holds. Kept in the silo's controller, and carried on the dropped block item (like a shulker box)
 * when the controller is broken, so a dismantled silo never spills tens of thousands of items.
 *
 * @param profile  one grain of the stored kind (cultivar and all), or empty
 * @param grain    sound grain
 * @param spoiled  grain gone musty, waiting to be emptied
 * @param moisture moisture fraction of the sound grain
 */
public record SiloContents(ItemStack profile, int grain, int spoiled, float moisture) {
    public static final SiloContents EMPTY = new SiloContents(ItemStack.EMPTY, 0, 0, (float) GrainDryingModel.DEFAULT_HARVEST_MOISTURE);

    public static final Codec<SiloContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("profile", ItemStack.EMPTY).forGetter(SiloContents::profile),
            Codec.INT.optionalFieldOf("grain", 0).forGetter(SiloContents::grain),
            Codec.INT.optionalFieldOf("spoiled", 0).forGetter(SiloContents::spoiled),
            Codec.FLOAT.optionalFieldOf("moisture", (float) GrainDryingModel.DEFAULT_HARVEST_MOISTURE).forGetter(SiloContents::moisture)
    ).apply(i, SiloContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SiloContents> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, SiloContents::profile,
            ByteBufCodecs.VAR_INT, SiloContents::grain,
            ByteBufCodecs.VAR_INT, SiloContents::spoiled,
            ByteBufCodecs.FLOAT, SiloContents::moisture,
            SiloContents::new);

    public boolean isEmpty() {
        return grain <= 0 && spoiled <= 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SiloContents c && grain == c.grain && spoiled == c.spoiled && Float.compare(moisture, c.moisture) == 0
                && ItemStack.isSameItemSameComponents(profile, c.profile);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * grain + spoiled) + Float.hashCode(moisture) + ItemStack.hashItemAndComponents(profile);
    }
}
