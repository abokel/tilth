package com.claudacious.tilth.registry;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.block.SoilMemory;
import com.claudacious.tilth.diet.PlayerDiet;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class TilthAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Tilth.MOD_ID);

    public static final Supplier<AttachmentType<PlayerDiet>> DIET = ATTACHMENTS.register("diet",
            () -> AttachmentType.builder(() -> new PlayerDiet()).serialize(PlayerDiet.CODEC).copyOnDeath().build());

    public static final Supplier<AttachmentType<SoilMemory>> SOIL_MEMORY = ATTACHMENTS.register("soil_memory",
            () -> AttachmentType.builder(() -> new SoilMemory()).serialize(SoilMemory.CODEC).build());

    private TilthAttachments() {
    }
}
