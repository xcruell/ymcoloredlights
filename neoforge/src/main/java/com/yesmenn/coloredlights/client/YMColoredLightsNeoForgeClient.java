package com.yesmenn.coloredlights.client;

import com.yesmenn.coloredlights.YMColoredLights;
import com.yesmenn.coloredlights.client.screen.YMColoredLightsSettings;
import com.yesmenn.coloredlights.compat.veil.ColoredLightClientCompat;
import com.yesmenn.coloredlights.compat.veil.ColoredLightCompatRegistry;
import com.yesmenn.coloredlights.platform.ClientPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = YMColoredLights.MODID, dist = Dist.CLIENT)
public class YMColoredLightsNeoForgeClient {

    public YMColoredLightsNeoForgeClient(IEventBus modEventBus, ModContainer container) {
        ClientPlatform.init(new ClientPlatform.Impl() {
            @Override
            public BakedModel getAdditionalModel(ResourceLocation id) {
                return Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(id));
            }

            @Override
            public void sendToServer(CustomPacketPayload payload) {
                PacketDistributor.sendToServer(payload);
            }
        });
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> YMColoredLightsSettings.create(parent));
        YMColoredLightsClientCommon.init();

        modEventBus.addListener(YMColoredLightsNeoForgeClient::onRegisterKeyMappings);
        modEventBus.addListener(YMColoredLightsNeoForgeClient::onRegisterRenderers);
        modEventBus.addListener(YMColoredLightsNeoForgeClient::onRegisterAdditionalModels);
        modEventBus.addListener(YMColoredLightsNeoForgeClient::onRegisterBlockColors);
        modEventBus.addListener(YMColoredLightsNeoForgeClient::onRegisterItemColors);

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> YMColoredLightsClientCommon.onClientTick());
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
                YMColoredLightsClientCommon.onAfterSolidBlocks(event.getPartialTick().getGameTimeDeltaPartialTick(false));
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) ->
                YMColoredLightsClientCommon.onClientLoggingOut());
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(YMColoredLightsClientCommon.createOpenConfigKey());
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ColoredLightCompatRegistry.COLORED_LIGHT_ENTITY.get(),
                NeoForgeColoredLightRenderer::new);
    }

    private static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation id : ColoredLightClientCompat.additionalModels()) {
            event.register(ModelResourceLocation.standalone(id));
        }
    }

    private static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        ColoredLightClientCompat.registerBlockColors(event::register);
    }

    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        ColoredLightClientCompat.registerItemColors(event::register);
    }
}
