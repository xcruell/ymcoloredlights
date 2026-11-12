package com.yesmenn.coloredlights;

import com.yesmenn.coloredlights.block.entity.ColoredLightBlockEntity;
import com.yesmenn.coloredlights.network.ColoredLightActionPayload;
import com.yesmenn.coloredlights.network.ColoredLightSpotlightPayload;
import com.yesmenn.coloredlights.network.FlashlightConfigPayload;
import com.yesmenn.coloredlights.network.ModNetworking;
import com.yesmenn.coloredlights.network.OpenColoredLightScreenPayload;
import com.yesmenn.coloredlights.platform.FabricPlatform;
import com.yesmenn.coloredlights.platform.Platform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class YMColoredLightsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Platform.init(new FabricPlatform());
        YMColoredLights.init();
        registerPayloads();
        // NeoForge calls BlockEntity#onLoad itself; Fabric has the load event instead.
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> {
            if (blockEntity instanceof ColoredLightBlockEntity light) {
                light.onLoad();
            }
        });
    }

    private static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(ColoredLightActionPayload.TYPE, ColoredLightActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ColoredLightSpotlightPayload.TYPE,
                ColoredLightSpotlightPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(FlashlightConfigPayload.TYPE, FlashlightConfigPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenColoredLightScreenPayload.TYPE,
                OpenColoredLightScreenPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ColoredLightActionPayload.TYPE,
                (payload, context) -> ModNetworking.handleColoredLightAction(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ColoredLightSpotlightPayload.TYPE,
                (payload, context) -> ModNetworking.handleColoredLightSpotlight(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(FlashlightConfigPayload.TYPE,
                (payload, context) -> ModNetworking.handleFlashlightConfig(payload, context.player()));
    }
}
