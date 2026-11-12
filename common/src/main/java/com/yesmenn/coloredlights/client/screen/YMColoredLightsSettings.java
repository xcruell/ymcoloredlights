package com.yesmenn.coloredlights.client.screen;

import com.yesmenn.coloredlights.client.ColoredLightRenderConfig;
import com.yesmenn.coloredlights.platform.Platform;
import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.element.YmText;
import com.yesmenn.ducttape.client.kit.settings.YmRows;
import com.yesmenn.ducttape.client.kit.settings.YmSettings;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * ymCL's settings as a section of the YM kit settings (YM Duct Tape). With VILC 1.7.0+ installed (it registers its
 * own section) every entry point - ymCL's and VILC's mod list entries and keys - opens one combined screen with the
 * pages of both mods.
 */
public final class YMColoredLightsSettings {
    public static final String SECTION = "ymcoloredlights";
    /** VILC's section ID (VILC 1.7.0+ registers it). */
    private static final String VILC_SECTION = "ymvilc";

    private YMColoredLightsSettings() {
    }

    public static void register() {
        YmSettings.register(new YmSettings.Section(SECTION, Component.literal("ymCL"))
                .accent(YmStyle.ACCENT_LIGHTS)
                .page(tr("page.lamp_colors"), new ItemStack(Items.ORANGE_DYE), YMColoredLightsSettings::lampColors)
                .onSave(ColoredLightRenderConfig::save)
                .onReset(() -> ColoredLightRenderConfig.get().reset())
                .credit(Component.literal("YM Colored Lights made by xcruell - v" + Platform.modVersion(SECTION))));
    }

    /** The settings screen: ymCL alone, or combined with VILC when VILC's section is there. */
    public static Screen create(@Nullable Screen parent) {
        boolean vilc = YmSettings.has(VILC_SECTION);
        Component title = vilc ? Component.literal("YM Colored Lights + VILC")
                : Component.translatable("screen.ymcoloredlights.colored_lights_config");
        return YmSettings.screen(parent, title, VILC_SECTION, SECTION);
    }

    /**
     * For VILC's entry points: the combined screen, or null with a VILC without kit settings (it then opens its own
     * screen, whose options this screen could not show).
     */
    @Nullable
    public static Screen createForVilc(@Nullable Screen parent) {
        return YmSettings.has(VILC_SECTION) ? create(parent) : null;
    }

    private static void lampColors(YmContainer page) {
        ColoredLightRenderConfig config = ColoredLightRenderConfig.get();
        page.add(new YmText(tr("lamp_colors.help")).wrap().dim());
        YmRows.slider(page, tr("exposure"), 0.01, ColoredLightRenderConfig.MAX_EXPOSURE, 0.01,
                () -> config.exposure, v -> config.exposure = v, ColoredLightRenderConfig.DEFAULT_EXPOSURE,
                YMColoredLightsSettings::two);
        YmRows.slider(page, tr("color_strength"), 0.0, ColoredLightRenderConfig.MAX_COLOR_STRENGTH, 0.05,
                () -> config.colorStrength, v -> config.colorStrength = v,
                ColoredLightRenderConfig.DEFAULT_COLOR_STRENGTH, YMColoredLightsSettings::two);
        YmRows.slider(page, tr("color_saturation"), 0.5, 2.0, 0.01,
                () -> config.colorSaturation, v -> config.colorSaturation = v,
                ColoredLightRenderConfig.DEFAULT_COLOR_SATURATION, YMColoredLightsSettings::two);
        YmRows.slider(page, tr("neutral_lift"), 0.0, 2.0, 0.01,
                () -> config.neutralLift, v -> config.neutralLift = v, ColoredLightRenderConfig.DEFAULT_NEUTRAL_LIFT,
                YMColoredLightsSettings::two);
    }

    private static String two(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static MutableComponent tr(String key) {
        return Component.translatable("gui.ymcoloredlights.settings." + key);
    }
}
