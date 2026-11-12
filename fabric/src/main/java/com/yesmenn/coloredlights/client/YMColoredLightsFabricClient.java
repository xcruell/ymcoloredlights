package com.yesmenn.coloredlights.client;

import com.yesmenn.coloredlights.block.entity.ColoredLightBlockEntity;
import com.yesmenn.coloredlights.compat.veil.ColoredLightClientCompat;
import com.yesmenn.coloredlights.compat.veil.ColoredLightCompatRegistry;
import com.yesmenn.coloredlights.network.ModNetworking;
import com.yesmenn.coloredlights.network.OpenColoredLightScreenPayload;
import com.yesmenn.coloredlights.platform.ClientPlatform;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class YMColoredLightsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlatform.init(new ClientPlatform.Impl() {
            @Override
            public BakedModel getAdditionalModel(ResourceLocation id) {
                return Minecraft.getInstance().getModelManager().getModel(id);
            }

            @Override
            public void sendToServer(CustomPacketPayload payload) {
                ClientPlayNetworking.send(payload);
            }
        });
        YMColoredLightsClientCommon.init();

        KeyBindingHelper.registerKeyBinding(YMColoredLightsClientCommon.createOpenConfigKey());
        ClientTickEvents.END_CLIENT_TICK.register(client -> YMColoredLightsClientCommon.onClientTick());
        // Same point as NeoForge's AFTER_SOLID_BLOCKS: terrain is drawn, entities are next.
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> YMColoredLightsClientCommon.onAfterSolidBlocks(
                context.tickCounter().getGameTimeDeltaPartialTick(false)));
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> YMColoredLightsClientCommon.onClientLoggingOut());
        // NeoForge calls BlockEntity#onLoad itself; Fabric has the load event instead.
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> {
            if (blockEntity instanceof ColoredLightBlockEntity light) {
                light.onLoad();
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(OpenColoredLightScreenPayload.TYPE,
                (payload, context) -> ModNetworking.handleOpenColoredLightScreen(payload));

        BlockEntityRenderers.register(ColoredLightCompatRegistry.COLORED_LIGHT_ENTITY.get(),
                FabricColoredLightRenderer::new);
        ModelLoadingPlugin.register(context -> context.addModels(ColoredLightClientCompat.additionalModels()));
        // NeoForge reads "render_type" from the block models; Fabric ignores it, so the layers are set here.
        // Keep in sync with the render_type entries in the models (also used for the block items).
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.translucent(),
                ColoredLightCompatRegistry.COLORED_LIGHT.get(),
                ColoredLightCompatRegistry.COLORED_LIGHT_PANEL.get(),
                ColoredLightCompatRegistry.HANGING_LAMP.get());
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutoutMipped(),
                ColoredLightCompatRegistry.CHAIN.get(),
                ColoredLightCompatRegistry.FLUORESCENT_LAMP_HANGING_CHAIN.get());
        ColoredLightClientCompat.registerBlockColors(ColorProviderRegistry.BLOCK::register);
        ColoredLightClientCompat.registerItemColors(ColorProviderRegistry.ITEM::register);
    }
}
