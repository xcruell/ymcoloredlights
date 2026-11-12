package com.yesmenn.coloredlights.client;

import com.yesmenn.coloredlights.block.entity.ColoredLightBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/** Adds the NeoForge render bounding box: the fluorescent lamps reach into their neighbours. */
public class NeoForgeColoredLightRenderer extends ColoredLightSpotlightRenderer {

    public NeoForgeColoredLightRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public AABB getRenderBoundingBox(ColoredLightBlockEntity blockEntity) {
        if (reachesIntoNeighbours(blockEntity)) {
            return new AABB(blockEntity.getBlockPos()).inflate(1.0);
        }
        return super.getRenderBoundingBox(blockEntity);
    }
}
