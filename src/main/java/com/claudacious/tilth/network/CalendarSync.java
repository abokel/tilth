package com.claudacious.tilth.network;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.client.ClientCalendar;
import com.claudacious.tilth.world.TilthCalendar;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Tells clients the Tilth calendar (offset and month length), so they can draw the season. */
public record CalendarSync(long offsetTicks, int daysPerMonth, int startMonth) implements CustomPacketPayload {
    public static final Type<CalendarSync> TYPE = new Type<>(Tilth.id("calendar"));
    public static final StreamCodec<ByteBuf, CalendarSync> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, CalendarSync::offsetTicks,
            ByteBufCodecs.VAR_INT, CalendarSync::daysPerMonth,
            ByteBufCodecs.VAR_INT, CalendarSync::startMonth,
            CalendarSync::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, CODEC, CalendarSync::handle);
    }

    private static void handle(CalendarSync payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientCalendar.set(payload.offsetTicks, payload.daysPerMonth, payload.startMonth));
    }

    public static CalendarSync current(MinecraftServer server) {
        return new CalendarSync(TilthCalendar.data(server).offsetTicks(), TilthConfig.DAYS_PER_MONTH.getAsInt(), TilthConfig.START_MONTH.getAsInt());
    }

    public static void sendToAll(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(current(server));
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, current(player.server));
        }
    }
}
