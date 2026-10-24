package com.claudacious.tilth.client;

import com.claudacious.tilth.livestock.LivestockState;
import com.claudacious.tilth.registry.TilthAttachments;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Which farm animals the client knows to be male (entity id to female?), from {@link com.claudacious.tilth.network.SexSync}. */
public final class ClientSexes {
    private static final Int2BooleanOpenHashMap FEMALE = new Int2BooleanOpenHashMap();

    private ClientSexes() {
    }

    public static void set(int entityId, boolean female) {
        FEMALE.put(entityId, female);
        // The client's copy of the animal learns its sex too, so the client refuses to milk a bull instead of
        // predicting a milk bucket the server will never give.
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getEntity(entityId) instanceof Animal animal) {
            animal.getData(TilthAttachments.LIVESTOCK.get()).sex = female ? LivestockState.FEMALE : LivestockState.MALE;
        }
    }

    public static boolean isMale(int entityId) {
        return FEMALE.containsKey(entityId) && !FEMALE.get(entityId);
    }

    public static void clear(ClientPlayerNetworkEvent.LoggingOut event) {
        FEMALE.clear();
    }
}
