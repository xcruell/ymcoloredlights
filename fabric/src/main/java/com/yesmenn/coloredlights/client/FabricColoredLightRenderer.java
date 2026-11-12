package com.yesmenn.coloredlights.client;

import com.yesmenn.coloredlights.block.entity.ColoredLightBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Fabric has no render bounding box per block entity: the fluorescent lamps reach into their neighbours, so they are
 * drawn even while their own chunk section is out of view.
 */
public class FabricColoredLightRenderer extends ColoredLightSpotlightRenderer {

    public FabricColoredLightRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRenderOffScreen(ColoredLightBlockEntity blockEntity) {
        return reachesIntoNeighbours(blockEntity);
    }
}
