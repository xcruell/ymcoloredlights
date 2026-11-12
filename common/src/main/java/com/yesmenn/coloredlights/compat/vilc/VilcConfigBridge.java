package com.yesmenn.coloredlights.compat.vilc;

import net.minecraft.client.gui.screens.Screen;

import java.util.function.Function;

public final class VilcConfigBridge {
    private VilcConfigBridge() {
    }

    public static boolean available() {
        return com.yesmenn.coloredlights.compat.OptionalModCompat.vilcAvailable();
    }

    /** Makes VILC's mod-list entry and config key open the combined ymCL + VILC screen. */
    public static void registerCombinedScreen(Function<Screen, Screen> factory) {
        if (!available()) {
            return;
        }
        try {
            Class.forName("com.yesmenn.vilc.client.screen.ConfigScreenBridge")
                    .getMethod("registerCombinedScreen", Function.class)
                    .invoke(null, factory);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Older VILC versions keep their own screen; ymCL's screen still shows the combined view.
        }
    }
}
