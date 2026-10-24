package com.claudacious.tilth.network;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.client.ClientWeather;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Tells a client whether precipitation where it stands falls as snow under the Tilth climate. */
public record WeatherSync(boolean snowing) implements CustomPacketPayload {
    public static final Type<WeatherSync> TYPE = new Type<>(Tilth.id("weather"));
    public static final StreamCodec<ByteBuf, WeatherSync> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, WeatherSync::snowing, WeatherSync::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, CODEC, WeatherSync::handle);
    }

    private static void handle(WeatherSync payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientWeather.setSnowing(payload.snowing()));
    }
}
