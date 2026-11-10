package com.lights.yesmenn;

import com.lights.yesmenn.client.ColoredLightRenderConfig;
import com.lights.yesmenn.client.screen.ColoredLightsConfigScreen;
import com.lights.yesmenn.compat.OptionalModCompat;
import com.lights.yesmenn.compat.veil.ColoredLightClientCompat;
import com.lights.yesmenn.compat.vilc.VilcConfigBridge;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = LightsYesMenn.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = LightsYesMenn.MODID, value = Dist.CLIENT)
public class LightsYesMennClient {
    /**
     * Opens the L?YM! config screen in game. Defaults to O like VILC's key; when VILC is installed its key already
     * opens the combined screen, so this one starts unbound to avoid a double binding.
     */
    private static KeyMapping openConfigKey;

    public LightsYesMennClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new ColoredLightsConfigScreen(parent));
        VilcConfigBridge.registerCombinedScreen(ColoredLightsConfigScreen::new);
        ColoredLightRenderConfig.load();
        ColoredLightClientCompat.registerEventListeners();
        NeoForge.EVENT_BUS.addListener(LightsYesMennClient::onClientTick);
    }

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        openConfigKey = new KeyMapping(
                "key.lightsyesmenn.open_config",
                InputConstants.Type.KEYSYM,
                OptionalModCompat.vilcAvailable() ? InputConstants.UNKNOWN.getValue() : GLFW.GLFW_KEY_O,
                "key.categories.lightsyesmenn");
        event.register(openConfigKey);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (openConfigKey == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        while (openConfigKey.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new ColoredLightsConfigScreen(null));
            }
        }
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ColoredLightClientCompat.registerRenderers(event);
    }

    @SubscribeEvent
    static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        ColoredLightClientCompat.registerAdditionalModels(event);
    }

    @SubscribeEvent
    static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        ColoredLightClientCompat.registerBlockColors(event);
    }

    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        ColoredLightClientCompat.registerItemColors(event);
    }
}