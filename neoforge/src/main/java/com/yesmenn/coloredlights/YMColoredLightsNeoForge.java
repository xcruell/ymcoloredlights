package com.yesmenn.coloredlights;

import com.yesmenn.coloredlights.light.ColoredLightSpawnProtection;
import com.yesmenn.coloredlights.network.ColoredLightActionPayload;
import com.yesmenn.coloredlights.network.ColoredLightSpotlightPayload;
import com.yesmenn.coloredlights.network.FlashlightConfigPayload;
import com.yesmenn.coloredlights.network.ModNetworking;
import com.yesmenn.coloredlights.network.OpenColoredLightScreenPayload;
import com.yesmenn.coloredlights.platform.NeoForgePlatform;
import com.yesmenn.coloredlights.platform.Platform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(YMColoredLights.MODID)
public class YMColoredLightsNeoForge {

    public YMColoredLightsNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NeoForgePlatform platform = new NeoForgePlatform();
        Platform.init(platform);
        YMColoredLights.init();
        platform.registerAll(modEventBus);

        modEventBus.addListener(YMColoredLightsNeoForge::registerPayloads);
        NeoForge.EVENT_BUS.addListener(YMColoredLightsNeoForge::onSpawnPlacementCheck);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ModNetworking.PROTOCOL_VERSION);
        registrar.playToServer(ColoredLightActionPayload.TYPE, ColoredLightActionPayload.STREAM_CODEC,
                (payload, context) -> ModNetworking.handleColoredLightAction(payload, context.player()));
        registrar.playToServer(ColoredLightSpotlightPayload.TYPE, ColoredLightSpotlightPayload.STREAM_CODEC,
                (payload, context) -> ModNetworking.handleColoredLightSpotlight(payload, context.player()));
        registrar.playToServer(FlashlightConfigPayload.TYPE, FlashlightConfigPayload.STREAM_CODEC,
                (payload, context) -> ModNetworking.handleFlashlightConfig(payload, context.player()));
        registrar.playToClient(OpenColoredLightScreenPayload.TYPE, OpenColoredLightScreenPayload.STREAM_CODEC,
                (payload, context) -> ModNetworking.handleOpenColoredLightScreen(payload));
    }

    private static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (ColoredLightSpawnProtection.blocksSpawn(event.getEntityType(), event.getLevel().getLevel(), event.getPos())) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }
}
