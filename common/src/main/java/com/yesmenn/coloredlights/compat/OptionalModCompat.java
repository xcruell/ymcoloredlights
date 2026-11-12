package com.yesmenn.coloredlights.compat;

import com.yesmenn.coloredlights.platform.Platform;

public final class OptionalModCompat {
    private OptionalModCompat() {
    }

    public static boolean coloredLightAvailable() {
        return Loaded.VEIL;
    }

    public static boolean yesMennAvailable() {
        return Loaded.YESMENN;
    }

    public static boolean vilcAvailable() {
        return Loaded.VILC;
    }

    // Resolved lazily on first use, after the loader has finished building the mod list.
    private static final class Loaded {
        private static final boolean VEIL = Platform.isModLoaded("veil");
        private static final boolean YESMENN = Platform.isModLoaded("yesmenn");
        private static final boolean VILC = Platform.isModLoaded("ymvilc")
                // Mod ID of VILC up to 1.6.x.
                || Platform.isModLoaded("veil_iris_lights");
    }

}
