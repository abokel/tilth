package com.claudacious.tilth.client;

import com.claudacious.tilth.livestock.Breed;
import com.claudacious.tilth.livestock.LivestockState;
import com.claudacious.tilth.registry.TilthAttachments;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Which farm animals the client knows to be male (entity id to female?), from {@link com.claudacious.tilth.network.SexSync}. */
public final class ClientSexes {
    private static final Int2BooleanOpenHashMap FEMALE = new Int2BooleanOpenHashMap();
    private static final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<Breed> BREED = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();

    private ClientSexes() {
    }

    public static void set(int entityId, boolean female, String breedId) {
        FEMALE.put(entityId, female);
        Breed breed = Breed.byId(breedId);
        if (breed != null) {
            BREED.put(entityId, breed);
        } else {
            BREED.remove(entityId);
        }
        // The client's copy of the animal learns its sex too, so the client refuses to milk a bull instead of
        // predicting a milk bucket the server will never give.
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getEntity(entityId) instanceof Animal animal) {
            LivestockState s = animal.getData(TilthAttachments.LIVESTOCK.get());
            s.sex = female ? LivestockState.FEMALE : LivestockState.MALE;
            s.breed = breedId;
        }
    }

    public static boolean isMale(int entityId) {
        return FEMALE.containsKey(entityId) && !FEMALE.get(entityId);
    }

    /** The breed whose coat to draw, or null (none known, or not yet synced). */
    @org.jetbrains.annotations.Nullable
    public static Breed breed(int entityId) {
        return BREED.get(entityId);
    }

    public static void clear(ClientPlayerNetworkEvent.LoggingOut event) {
        FEMALE.clear();
        BREED.clear();
    }
}
