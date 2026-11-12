package com.yesmenn.coloredlights.registry;

import com.yesmenn.coloredlights.item.ColorfulFlashlightItem;
import com.yesmenn.coloredlights.item.LightConfiguratorItem;
import com.yesmenn.coloredlights.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public final class ModItems {
    public static final Supplier<LightConfiguratorItem> LIGHT_CONFIGURATOR = Platform.register(BuiltInRegistries.ITEM,
            "light_configurator", () -> new LightConfiguratorItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<ColorfulFlashlightItem> COLORFUL_FLASHLIGHT = Platform.register(BuiltInRegistries.ITEM,
            "colorful_flashlight", () -> new ColorfulFlashlightItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    /** Loads this class, which registers everything above. */
    public static void init() {
    }
}
