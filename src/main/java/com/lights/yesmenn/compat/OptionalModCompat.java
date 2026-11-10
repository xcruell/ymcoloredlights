package com.lights.yesmenn.compat;

import net.neoforged.fml.ModList;

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

    // Resolved lazily on first use, after FML has finished building the mod list.
    private static final class Loaded {
        private static final boolean VEIL = ModList.get().isLoaded("veil");
        private static final boolean YESMENN = ModList.get().isLoaded("yesmenn");
        private static final boolean VILC = ModList.get().isLoaded("veil_iris_lights");
    }

}
