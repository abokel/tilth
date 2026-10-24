package com.claudacious.tilth.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Client-only entry points called from common code behind an {@code isClientSide} check. */
public final class ClientAccess {
    private ClientAccess() {
    }

    public static void openMicroscope(CompoundTag sample) {
        Minecraft.getInstance().setScreen(new MicroscopeScreen(sample));
    }
}
