package com.yesmenn.coloredlights.client;

import com.yesmenn.coloredlights.client.screen.YMColoredLightsSettings;
import com.yesmenn.coloredlights.compat.OptionalModCompat;
import com.yesmenn.coloredlights.compat.vilc.VilcConfigBridge;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Loader-independent client setup; the loaders forward their client events here. */
public final class YMColoredLightsClientCommon {
    /**
     * Opens the ymCL config screen in game. Defaults to O like VILC's key; when VILC is installed its key already
     * opens the combined screen, so this one starts unbound to avoid a double binding.
     */
    private static KeyMapping openConfigKey;

    private YMColoredLightsClientCommon() {
    }

    public static void init() {
        ColoredLightRenderConfig.load();
        YMColoredLightsSettings.register();
        VilcConfigBridge.registerCombinedScreen(YMColoredLightsSettings::createForVilc);
    }

    /** Creates the config key; the loader registers it. */
    public static KeyMapping createOpenConfigKey() {
        openConfigKey = new KeyMapping(
                "key.ymcoloredlights.open_config",
                InputConstants.Type.KEYSYM,
                OptionalModCompat.vilcAvailable() ? InputConstants.UNKNOWN.getValue() : GLFW.GLFW_KEY_O,
                "key.categories.ymcoloredlights");
        return openConfigKey;
    }

    /** End of the client tick. */
    public static void onClientTick() {
        VeilColoredLightManager.onClientTick();
        if (openConfigKey == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        while (openConfigKey.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(YMColoredLightsSettings.create(null));
            }
        }
    }

    /** Every frame, after the solid blocks are drawn. */
    public static void onAfterSolidBlocks(float partialTick) {
        VeilColoredLightManager.onAfterSolidBlocks(partialTick);
        FlashlightLightManager.onAfterSolidBlocks(partialTick);
    }

    public static void onClientLoggingOut() {
        VeilColoredLightManager.onClientLoggingOut();
        FlashlightLightManager.onClientLoggingOut();
    }
}
