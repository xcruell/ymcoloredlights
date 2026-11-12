package com.yesmenn.coloredlights.client;

import com.yesmenn.coloredlights.client.screen.YMColoredLightsSettings;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class YMColoredLightsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return YMColoredLightsSettings::create;
    }
}
