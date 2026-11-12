package com.yesmenn.coloredlights.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Render types for lamps drawn by block entity renderers (block atlas). None of them is one of the buffer source's
 * fixed buffers: requesting one draws the previously requested one, so drawing a lamp in the order {@link #CUTOUT},
 * {@link #ADDITIVE_GLOW}, {@link #GLASS} puts its light on top of its own housing.
 */
final class LampRenderTypes {
    /** Like the cutout block sheet, but drawn in request order (see above). */
    static final RenderType CUTOUT = RenderType.create("ymcoloredlights_lamp_cutout",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(true));

    /**
     * Additive and fullbright (alpha-weighted: src alpha, one): brightens what lies behind instead of covering it, so
     * glow shells and light cores look like light, not like solid parts. Writes no depth, so the order of drawing does
     * not matter and faces behind each other add up.
     */
    static final RenderType ADDITIVE_GLOW = RenderType.create("ymcoloredlights_additive_glow",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(false));

    /**
     * Alpha-blended glass that writes no depth: the additive parts inside it may be drawn after it and would otherwise
     * be hidden behind its depth.
     */
    static final RenderType GLASS = RenderType.create("ymcoloredlights_lamp_glass",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    private LampRenderTypes() {
    }
}
