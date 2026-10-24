package com.claudacious.tilth.compat.wthit;

import com.claudacious.tilth.block.CropBlockEntity;
import com.claudacious.tilth.block.FarmlandBlockEntity;
import com.claudacious.tilth.block.TilthCropBlock;
import com.claudacious.tilth.block.TilthFarmlandBlock;
import com.claudacious.tilth.compat.LookInfo;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.entity.BlockEntity;

/** WTHIT tooltips for Tilth crops, farmland and farm animals. Declared in waila_plugins.json; loaded only with WTHIT. */
public class TilthWthitPlugin implements IWailaPlugin {
    @Override
    public void register(IRegistrar registrar) {
        IDataProvider<BlockEntity> data = (writer, accessor, config) -> LookInfo.write(writer.raw(), accessor.getTarget());
        registrar.addBlockData(data, CropBlockEntity.class);
        registrar.addBlockData(data, FarmlandBlockEntity.class);
        IBlockComponentProvider body = new IBlockComponentProvider() {
            @Override
            public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
                LookInfo.read(accessor.getData().raw(), accessor.getWorld().registryAccess()).forEach(tooltip::addLine);
            }
        };
        registrar.addComponent(body, TooltipPosition.BODY, TilthCropBlock.class);
        registrar.addComponent(body, TooltipPosition.BODY, TilthFarmlandBlock.class);

        IDataProvider<Animal> animalData = (writer, accessor, config) -> LookInfo.write(writer.raw(), accessor.getTarget());
        registrar.addEntityData(animalData, Animal.class);
        registrar.addComponent(new IEntityComponentProvider() {
            @Override
            public void appendBody(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
                LookInfo.read(accessor.getData().raw(), accessor.getWorld().registryAccess()).forEach(tooltip::addLine);
            }
        }, TooltipPosition.BODY, Animal.class);
    }
}
