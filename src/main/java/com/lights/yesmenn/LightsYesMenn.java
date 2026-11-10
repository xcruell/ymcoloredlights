package com.lights.yesmenn;

import com.lights.yesmenn.compat.veil.ColoredLightCompatRegistry;
import com.lights.yesmenn.light.ColoredLightSpawnProtection;
import com.lights.yesmenn.network.ModNetworking;
import com.lights.yesmenn.registry.ModItems;
import com.lights.yesmenn.ymtab.YmCreativeTab;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(LightsYesMenn.MODID)
public class LightsYesMenn {
    public static final String MODID = "lightsyesmenn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LightsYesMenn(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(ModNetworking::register);

        ColoredLightCompatRegistry.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        // Shared "YesMenn!" tab of all YM mods (registered here only if this mod leads it); contents come from the
        // yesmenn:creative/* item tags.
        YmCreativeTab.register(MODID, modEventBus,
                () -> new ItemStack(ColoredLightCompatRegistry.COLORED_LIGHT_ITEM.get()));

        NeoForge.EVENT_BUS.addListener(ColoredLightSpawnProtection::onSpawnPlacementCheck);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

}