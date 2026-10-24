package com.claudacious.tilth.registry;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.entity.PlowEntity;
import com.claudacious.tilth.entity.SpreaderEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TilthEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Tilth.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<PlowEntity>> PLOW = ENTITIES.register("plow",
            () -> EntityType.Builder.<PlowEntity>of(PlowEntity::new, MobCategory.MISC)
                    .sized(1.4f, 0.9f).clientTrackingRange(10).updateInterval(2).build("plow"));

    public static final DeferredHolder<EntityType<?>, EntityType<SpreaderEntity>> SPREADER = ENTITIES.register("spreader",
            () -> EntityType.Builder.<SpreaderEntity>of(SpreaderEntity::new, MobCategory.MISC)
                    .sized(1.4f, 1.1f).clientTrackingRange(10).updateInterval(2).build("spreader"));

    private TilthEntities() {
    }
}
