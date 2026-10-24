package com.claudacious.tilth.compat.jade;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.block.CropBlockEntity;
import com.claudacious.tilth.block.FarmlandBlockEntity;
import com.claudacious.tilth.block.TilthCropBlock;
import com.claudacious.tilth.block.TilthFarmlandBlock;
import com.claudacious.tilth.compat.LookInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.JadeIds;
import snownee.jade.api.IEntityComponentProvider;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Jade tooltips for Tilth crops, farmland and farm animals. Loaded only when Jade is installed. */
@WailaPlugin
public class TilthJadePlugin implements IWailaPlugin {
    private static final ResourceLocation UID = Tilth.id("field");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Provider.INSTANCE, CropBlockEntity.class);
        registration.registerBlockDataProvider(Provider.INSTANCE, FarmlandBlockEntity.class);
        registration.registerEntityDataProvider(AnimalProvider.INSTANCE, Animal.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Provider.INSTANCE, TilthCropBlock.class);
        registration.registerBlockComponent(Provider.INSTANCE, TilthFarmlandBlock.class);
        registration.registerEntityComponent(AnimalProvider.INSTANCE, Animal.class);
        // Roosters never lay: drop Jade's own "Next egg" countdown, which would never run out.
        registration.addTooltipCollectedCallback((box, accessor) -> {
            if (accessor instanceof EntityAccessor entity && entity.getEntity() instanceof Chicken && LookInfo.male(entity.getServerData())) {
                box.getTooltip().remove(JadeIds.MC_NEXT_ENTITY_DROP);
            }
        });
    }

    enum AnimalProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
        INSTANCE;
        private static final ResourceLocation ID = Tilth.id("livestock");

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            LookInfo.read(accessor.getServerData(), accessor.getLevel().registryAccess()).forEach(tooltip::add);
        }

        @Override
        public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            if (accessor.getEntity() instanceof Animal animal) {
                LookInfo.write(data, animal);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ID;
        }
    }

    enum Provider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            LookInfo.read(accessor.getServerData(), accessor.getLevel().registryAccess()).forEach(tooltip::add);
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() != null) {
                LookInfo.write(data, accessor.getBlockEntity());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
