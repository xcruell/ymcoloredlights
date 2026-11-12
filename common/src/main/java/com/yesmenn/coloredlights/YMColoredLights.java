package com.yesmenn.coloredlights;

import com.yesmenn.coloredlights.compat.veil.ColoredLightCompatRegistry;
import com.yesmenn.coloredlights.registry.ModItems;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Loader-independent mod setup; called by the NeoForge and Fabric entrypoints after {@code Platform.init}. */
public final class YMColoredLights {
    public static final String MODID = "ymcoloredlights";
    public static final Logger LOGGER = LogUtils.getLogger();

    private YMColoredLights() {
    }

    public static void init() {
        ColoredLightCompatRegistry.init();
        ModItems.init();
    }
}
