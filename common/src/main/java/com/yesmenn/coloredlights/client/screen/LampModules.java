package com.yesmenn.coloredlights.client.screen;

import com.yesmenn.coloredlights.block.HangingLampBlock;
import com.yesmenn.coloredlights.client.ColoredLightPresetStore;
import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.element.YmActionMenu;
import com.yesmenn.ducttape.client.kit.element.YmButton;
import com.yesmenn.ducttape.client.kit.element.YmColorPicker;
import com.yesmenn.ducttape.client.kit.element.YmDialog;
import com.yesmenn.ducttape.client.kit.element.YmFormRow;
import com.yesmenn.ducttape.client.kit.element.YmHeading;
import com.yesmenn.ducttape.client.kit.element.YmNumberField;
import com.yesmenn.ducttape.client.kit.element.YmSegmented;
import com.yesmenn.ducttape.client.kit.element.YmSelect;
import com.yesmenn.ducttape.client.kit.element.YmSlider;
import com.yesmenn.ducttape.client.kit.element.YmText;
import com.yesmenn.ducttape.client.kit.element.YmTileGrid;
import com.yesmenn.ducttape.client.kit.layout.YmLayout;
import com.yesmenn.ducttape.client.kit.module.YmModule;
import com.yesmenn.ducttape.client.kit.module.YmModuleSet;
import com.yesmenn.ducttape.client.kit.style.YmColors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * The blocks of the lamp screen. Base modules (order 0-99) are the settings every light of their kind has; add-ons
 * (100+) belong to single lamp types (warm-up of hanging lamps, spill of the flashlight, ...). New lamp features get
 * a new module here instead of more rows in a fixed layout; other mods can {@link YmModuleSet#register register}
 * their own modules in {@link #SET}.
 */
public final class LampModules {
    public static final YmModuleSet<LampEditor> SET = new YmModuleSet<>();

    private static final int LABEL_WIDTH = 86;
    private static final int FIELD_WIDTH = 34;
    private static final int MIN_TEMPERATURE = 1000;
    private static final int MAX_TEMPERATURE = 12000;
    private static final int[] BUILT_IN_PRESETS = {0xFFB05C, 0xFFFFFF, 0xFF3030, 0x38FF70, 0x4070FF, 0xC050FF};
    private static final String[] BUILT_IN_PRESET_NAMES = {"warm", "white", "red", "green", "blue", "purple"};

    static {
        SET.register(module("light", 0, true, Items.GLOWSTONE_DUST, editor -> true, LampModules::light,
                editor -> () -> Component.literal(editor.radius + " · " + editor.brightness + "%")));
        SET.register(module("color", 10, true, Items.RED_DYE, editor -> true, LampModules::color,
                editor -> () -> colorSummary(editor.color)));
        SET.register(module("redstone", 20, false, Items.REDSTONE, editor -> !editor.flashlight, LampModules::redstone,
                editor -> () -> tr(editor.redstoneInverted ? "redstone.inverted" : "redstone.normal")));
        SET.register(module("beam", 30, false, Items.SPYGLASS, editor -> editor.spotlight, LampModules::beam,
                editor -> () -> Component.literal(editor.angle + "°")));
        SET.register(module("direction", 40, false, Items.COMPASS,
                editor -> editor.spotlight && !editor.hangingLamp && !editor.flashlight, LampModules::direction,
                editor -> () -> Component.literal(editor.yaw + "° / " + editor.pitch + "°")));
        // Add-ons
        SET.register(module("effects", 100, false, Items.BLAZE_POWDER, editor -> true, LampModules::effects,
                editor -> () -> editor.flicker == 0 && editor.glow == 0 ? tr("off")
                        : Component.literal(editor.flicker + "% / " + editor.glow + "%")));
        SET.register(new Module("warmup", 110, false, new ItemStack(Items.CLOCK), editor -> editor.hangingLamp,
                LampModules::warmup, editor -> () -> Component.literal(seconds(editor.warmup))) {
            @Override
            public Component title() {
                return tr("module.warmup");
            }
        });
        SET.register(module("flashlight", 120, false, Items.ENDER_EYE, editor -> editor.flashlight,
                LampModules::flashlight, editor -> () -> Component.literal(editor.spill + "% / " + editor.fringe + "%")));
    }

    private LampModules() {
    }

    // ---- base modules ----

    private static void light(YmContainer body, LampEditor editor) {
        intRow(body, Component.translatable(editor.flashlight ? "gui.ymcoloredlights.light.range"
                        : "gui.ymcoloredlights.light.radius"), 1, editor.maxRadius,
                () -> editor.radius, v -> editor.radius = v, editor.defaultRadius(), editor, null);
        intRow(body, Component.translatable("gui.ymcoloredlights.light.brightness"),
                LampEditor.MIN_BRIGHTNESS, LampEditor.MAX_BRIGHTNESS,
                () -> editor.brightness, v -> editor.brightness = v, editor.defaultBrightness(), editor, null);
    }

    private static void color(YmContainer body, LampEditor editor) {
        YmColorPicker picker = body.add(new YmColorPicker(editor.color, rgb -> {
            editor.color = rgb;
            editor.changed();
        }));
        IntConsumer applyColor = rgb -> {
            editor.color = rgb & 0xFFFFFF;
            picker.setRgb(editor.color);
            editor.changed();
        };

        YmSlider temperature = new YmSlider(Component.literal(""), MIN_TEMPERATURE, MAX_TEMPERATURE, 100,
                LampColors.nearestTemperature(editor.color), kelvin -> applyColor.accept(
                LampColors.colorFromTemperature((int) Math.round(kelvin))));
        temperature.setFormat(kelvin -> Math.round(kelvin) + " K");
        body.add(new YmFormRow(Component.translatable("gui.ymcoloredlights.light.temperature"), LABEL_WIDTH, temperature));

        body.add(new YmHeading(Component.translatable("gui.ymcoloredlights.light.presets")));
        List<YmTileGrid.Tile> builtIn = new ArrayList<>();
        for (int i = 0; i < BUILT_IN_PRESETS.length; i++) {
            builtIn.add(YmTileGrid.Tile.color(
                    Component.translatable("gui.ymcoloredlights.light.preset." + BUILT_IN_PRESET_NAMES[i]),
                    BUILT_IN_PRESETS[i]));
        }
        body.add(new YmTileGrid(builtIn, -1, index -> {
            applyColor.accept(BUILT_IN_PRESETS[index]);
            temperature.setValue(LampColors.nearestTemperature(editor.color));
        }));

        body.add(new YmHeading(Component.translatable("gui.ymcoloredlights.light.custom_presets")));
        List<Integer> custom = ColoredLightPresetStore.getPresets();
        List<YmTileGrid.Tile> customTiles = new ArrayList<>();
        for (int rgb : custom) {
            customTiles.add(YmTileGrid.Tile.color(Component.literal(YmColors.toHex(rgb)), rgb));
        }
        if (custom.isEmpty()) {
            body.add(new YmText(tr("presets.empty")).wrap().dim());
        }
        YmTileGrid customGrid = body.add(new YmTileGrid(customTiles, -1, index -> {
            applyColor.accept(custom.get(index));
            temperature.setValue(LampColors.nearestTemperature(editor.color));
        }));
        customGrid.setOnAdd(() -> {
            YmScreen screen = body.screen();
            if (ColoredLightPresetStore.add(editor.color)) {
                if (screen != null) {
                    screen.toast(tr("presets.saved", YmColors.toHex(editor.color)), 0xFF000000 | editor.color);
                    screen.rebuild();
                }
            } else if (screen != null) {
                screen.toast(tr("presets.full"), 0xFFD04040);
            }
        });
        customGrid.setContextMenu((index, menu) -> presetMenu(body, editor, index, menu));
        customGrid.setTooltip(tr("presets.help"));
    }

    private static void presetMenu(YmContainer body, LampEditor editor, int index, YmActionMenu menu) {
        menu.entry(tr("presets.overwrite"), () -> {
            ColoredLightPresetStore.set(index, editor.color);
            rebuild(body);
        });
        menu.entry(tr("presets.recolor"), () -> {
            YmScreen screen = body.screen();
            if (screen != null) {
                YmDialog.pickColor(screen, tr("presets.recolor"), ColoredLightPresetStore.getPresets().get(index),
                        rgb -> {
                            ColoredLightPresetStore.set(index, rgb);
                            screen.rebuild();
                        });
            }
        });
        menu.separator();
        menu.entry(tr("presets.delete"), () -> {
            YmScreen screen = body.screen();
            if (screen != null) {
                YmDialog.confirm(screen, tr("presets.delete"),
                        tr("presets.delete.message", YmColors.toHex(ColoredLightPresetStore.getPresets().get(index))),
                        tr("presets.delete"), () -> {
                            ColoredLightPresetStore.remove(index);
                            screen.rebuild();
                        });
            }
        });
    }

    private static void redstone(YmContainer body, LampEditor editor) {
        body.add(new YmFormRow(tr("redstone.signal"), LABEL_WIDTH, new YmSegmented<>(List.of(false, true),
                editor.redstoneInverted, inverted -> tr(inverted ? "redstone.inverted" : "redstone.normal"),
                inverted -> {
                    editor.redstoneInverted = inverted;
                    editor.changed();
                })));
        body.add(new YmText(tr("redstone.help")).wrap().dim());
    }

    private static void beam(YmContainer body, LampEditor editor) {
        List<IntSliderRow> rows = new ArrayList<>();
        rows.add(intRow(body, Component.translatable("gui.ymcoloredlights.light.spotlight_angle"), 5, editor.maxAngle(),
                () -> editor.angle, v -> editor.angle = v, editor.defaultAngle(), editor, v -> v + "°"));
        rows.add(intRow(body, Component.translatable("gui.ymcoloredlights.light.spotlight_size"), 2, 60,
                () -> editor.size, v -> editor.size = v, editor.defaultSize(), editor, null));
        rows.add(intRow(body, Component.translatable("gui.ymcoloredlights.light.spotlight_softness"), 1, 50,
                () -> editor.softness, v -> editor.softness = v, editor.defaultSoftness(), editor, null));
        rows.add(intRow(body, Component.translatable("gui.ymcoloredlights.light.spotlight_focus"), 25, 500,
                () -> editor.focus, v -> editor.focus = v, editor.defaultFocus(), editor, null));
        YmContainer buttons = body.add(new YmContainer(0, YmButton.DEFAULT_HEIGHT));
        buttons.setLayout(YmLayout.rowRight(4));
        buttons.add(new YmButton(Component.translatable("gui.ymcoloredlights.light.spotlight_reset"),
                button -> rows.forEach(IntSliderRow::reset)).setSize(110, YmButton.DEFAULT_HEIGHT));
    }

    private static void direction(YmContainer body, LampEditor editor) {
        intRow(body, Component.translatable("gui.ymcoloredlights.light.spotlight_yaw"), -90, 90,
                () -> editor.yaw, v -> editor.yaw = v, editor.defaultYaw(), editor, v -> v + "°")
                .snapTo(-90, -45, 0, 45, 90);
        intRow(body, Component.translatable("gui.ymcoloredlights.light.spotlight_pitch"), -90, 90,
                () -> editor.pitch, v -> editor.pitch = v, editor.defaultPitch(), editor, v -> v + "°")
                .snapTo(-90, -45, 0, 45, 90);
    }

    // ---- add-ons ----

    private static void effects(YmContainer body, LampEditor editor) {
        intRow(body, Component.translatable("gui.ymcoloredlights.light.flicker"), 0, 100,
                () -> editor.flicker, v -> editor.flicker = v, editor.defaultFlicker(), editor, null);
        intRow(body, Component.translatable("gui.ymcoloredlights.light.glow"), 0, 100,
                () -> editor.glow, v -> editor.glow = v, editor.defaultGlow(), editor, null);
    }

    private static void warmup(YmContainer body, LampEditor editor) {
        Component label = Component.translatable(editor.fluorescentLamp ? "gui.ymcoloredlights.light.start_flicker"
                : "gui.ymcoloredlights.light.warmup");
        intRow(body, label, 0, com.yesmenn.coloredlights.block.entity.ColoredLightBlockEntity.MAX_WARMUP,
                () -> editor.warmup, v -> editor.warmup = v, editor.defaultWarmup(), editor, LampModules::seconds);
        List<Integer> tints = IntStream.range(0, HangingLampBlock.WARMUP_TINTS.length).boxed().toList();
        body.add(new YmFormRow(tr("warmup.tint"), LABEL_WIDTH, new YmSelect<>(tints, editor.warmupTint,
                LampModules::tintName, tint -> {
                    editor.warmupTint = tint;
                    editor.changed();
                })));
    }

    private static void flashlight(YmContainer body, LampEditor editor) {
        intRow(body, Component.translatable("gui.ymcoloredlights.light.flashlight_spill"), 0, 100,
                () -> editor.spill, v -> editor.spill = v,
                com.yesmenn.coloredlights.item.FlashlightSettings.DEFAULT.spill(), editor, null);
        intRow(body, Component.translatable("gui.ymcoloredlights.light.flashlight_fringe"), 0, 100,
                () -> editor.fringe, v -> editor.fringe = v,
                com.yesmenn.coloredlights.item.FlashlightSettings.DEFAULT.fringe(), editor, null);
    }

    // ---- helpers ----

    /** A slider with a number field next to it and the per-value reset icon. */
    static final class IntSliderRow {
        private final YmSlider slider;
        private final YmNumberField field;
        private final IntConsumer setter;
        private final int defaultValue;
        private final LampEditor editor;

        private IntSliderRow(YmSlider slider, YmNumberField field, IntConsumer setter, int defaultValue,
                             LampEditor editor) {
            this.slider = slider;
            this.field = field;
            this.setter = setter;
            this.defaultValue = defaultValue;
            this.editor = editor;
        }

        void set(int value) {
            setter.accept(value);
            slider.setValue(value);
            field.setIntValue(value);
            editor.changed();
        }

        void reset() {
            set(defaultValue);
        }

        IntSliderRow snapTo(double... points) {
            slider.setSnapPoints(2, points);
            return this;
        }
    }

    private static IntSliderRow intRow(YmContainer body, Component label, int min, int max, IntSupplier getter,
                                       IntConsumer setter, int defaultValue, LampEditor editor,
                                       @Nullable IntFunction<String> format) {
        IntSliderRow[] row = new IntSliderRow[1];
        YmSlider slider = YmSlider.ofInt(Component.literal(""), min, max, getter.getAsInt(), v -> {
            setter.accept(v);
            row[0].field.setIntValue(v);
            editor.changed();
        });
        if (format != null) {
            slider.setFormat(v -> format.apply((int) Math.round(v)));
        }
        YmNumberField field = new YmNumberField(min, max, getter.getAsInt(), v -> {
            setter.accept(v);
            row[0].slider.setValue(v);
            editor.changed();
        });
        row[0] = new IntSliderRow(slider, field, setter, defaultValue, editor);
        // Slider fills the row, the number field (for typing exact values) sits at its right end.
        YmContainer controls = new YmContainer(0, YmSlider.DEFAULT_HEIGHT);
        controls.add(slider);
        controls.add(field);
        controls.setLayout(panel -> {
            int w = panel.contentWidth();
            slider.setBounds(0, 0, Math.max(10, w - FIELD_WIDTH - 4), YmSlider.DEFAULT_HEIGHT);
            field.setBounds(w - FIELD_WIDTH, 0, FIELD_WIDTH, YmSlider.DEFAULT_HEIGHT);
        });
        YmFormRow formRow = body.add(new YmFormRow(label, LABEL_WIDTH, controls));
        formRow.resettable(() -> getter.getAsInt() != defaultValue, row[0]::reset);
        return row[0];
    }

    private static void rebuild(YmContainer body) {
        YmScreen screen = body.screen();
        if (screen != null) {
            screen.rebuild();
        }
    }

    private static Component colorSummary(int rgb) {
        return Component.literal("■ ").withStyle(style -> style.withColor(rgb))
                .append(Component.literal(YmColors.toHex(rgb)));
    }

    private static Component tintName(int tint) {
        MutableComponent name = Component.translatable("gui.ymcoloredlights.light.warmup_tint." + tint);
        int rgb = HangingLampBlock.WARMUP_TINTS[tint];
        return rgb < 0 ? name : Component.literal("■ ").withStyle(style -> style.withColor(rgb)).append(name);
    }

    private static String seconds(int tenths) {
        return String.format(Locale.ROOT, "%.1f s", tenths / 10.0);
    }

    static MutableComponent tr(String key, Object... args) {
        return Component.translatable("gui.ymcoloredlights.lamp." + key, args);
    }

    private static YmModule<LampEditor> module(String id, int order, boolean open, net.minecraft.world.item.Item icon,
                                               Predicate<LampEditor> applies,
                                               java.util.function.BiConsumer<YmContainer, LampEditor> builder,
                                               java.util.function.Function<LampEditor, Supplier<Component>> summary) {
        return new Module(id, order, open, new ItemStack(icon), applies, builder, summary);
    }

    private static class Module implements YmModule<LampEditor> {
        private final String id;
        private final int order;
        private final boolean open;
        private final ItemStack icon;
        private final Predicate<LampEditor> applies;
        private final java.util.function.BiConsumer<YmContainer, LampEditor> builder;
        private final java.util.function.Function<LampEditor, Supplier<Component>> summary;

        Module(String id, int order, boolean open, ItemStack icon, Predicate<LampEditor> applies,
               java.util.function.BiConsumer<YmContainer, LampEditor> builder,
               java.util.function.Function<LampEditor, Supplier<Component>> summary) {
            this.id = id;
            this.order = order;
            this.open = open;
            this.icon = icon;
            this.applies = applies;
            this.builder = builder;
            this.summary = summary;
        }

        @Override
        public String id() {
            return "ymcoloredlights:" + id;
        }

        @Override
        public Component title() {
            return tr("module." + id);
        }

        @Override
        public ItemStack icon() {
            return icon;
        }

        @Override
        public boolean appliesTo(LampEditor target) {
            return applies.test(target);
        }

        @Override
        public void build(YmContainer body, LampEditor target) {
            builder.accept(body, target);
        }

        @Override
        public Supplier<Component> summary(LampEditor target) {
            return summary.apply(target);
        }

        @Override
        public boolean openByDefault() {
            return open;
        }

        @Override
        public int order() {
            return order;
        }
    }
}
