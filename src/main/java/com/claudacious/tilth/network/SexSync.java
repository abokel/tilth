package com.claudacious.tilth.network;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.client.ClientSexes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Tells a client whether a farm animal it can see is female or male, so males can be drawn with their features. */
public record SexSync(int entityId, boolean female) implements CustomPacketPayload {
    public static final Type<SexSync> TYPE = new Type<>(Tilth.id("sex"));
    public static final StreamCodec<ByteBuf, SexSync> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SexSync::entityId, ByteBufCodecs.BOOL, SexSync::female, SexSync::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, CODEC, SexSync::handle);
    }

    private static void handle(SexSync payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientSexes.set(payload.entityId(), payload.female()));
    }
}
