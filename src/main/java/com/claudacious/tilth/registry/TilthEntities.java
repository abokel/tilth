package com.claudacious.tilth.registry;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.entity.HomunculusEntity;
import com.claudacious.tilth.entity.PlowEntity;
import com.claudacious.tilth.entity.SpreaderEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
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

    public static final DeferredHolder<EntityType<?>, EntityType<com.claudacious.tilth.entity.WagonEntity>> WAGON = ENTITIES.register("wagon",
            () -> EntityType.Builder.<com.claudacious.tilth.entity.WagonEntity>of(com.claudacious.tilth.entity.WagonEntity::new, MobCategory.MISC)
                    .sized(1.6f, 1.2f).clientTrackingRange(10).updateInterval(2).build("wagon"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.claudacious.tilth.entity.FarmMerchant>> FARM_MERCHANT = ENTITIES.register("farm_merchant",
            () -> EntityType.Builder.<com.claudacious.tilth.entity.FarmMerchant>of(com.claudacious.tilth.entity.FarmMerchant::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f).eyeHeight(1.62f).clientTrackingRange(10).build("farm_merchant"));

    public static final DeferredHolder<EntityType<?>, EntityType<HomunculusEntity>> HOMUNCULUS = ENTITIES.register("homunculus",
            () -> EntityType.Builder.<HomunculusEntity>of(HomunculusEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 0.6f).clientTrackingRange(8).updateInterval(2).build("homunculus"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HOMUNCULUS.get(), HomunculusEntity.createAttributes().build());
        event.put(FARM_MERCHANT.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.5).build());
    }

    private TilthEntities() {
    }
}
