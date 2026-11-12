package com.yesmenn.coloredlights.client;

import com.yesmenn.coloredlights.LegacyMigration;
import com.yesmenn.coloredlights.YMColoredLights;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The player's own color presets of the lamp screen (client side, one hex color per line). Older files had four fixed
 * slots with empty lines for unused ones; empty lines are skipped.
 */
public final class ColoredLightPresetStore {
    /** Upper limit, so the preset gallery stays usable. */
    public static final int MAX_PRESETS = 48;
    private static final Path FILE = LegacyMigration.configFile("ymcoloredlights-presets.txt",
            "yesmenn-colored-light-presets.txt");
    private static final List<Integer> PRESETS = new ArrayList<>();
    private static boolean loaded;

    private ColoredLightPresetStore() {
    }

    public static List<Integer> getPresets() {
        load();
        return List.copyOf(PRESETS);
    }

    /** Adds a preset at the end; false when the limit is reached. */
    public static boolean add(int color) {
        load();
        if (PRESETS.size() >= MAX_PRESETS) {
            return false;
        }
        PRESETS.add(color & 0xFFFFFF);
        save();
        return true;
    }

    public static void set(int index, int color) {
        load();
        if (index >= 0 && index < PRESETS.size()) {
            PRESETS.set(index, color & 0xFFFFFF);
            save();
        }
    }

    public static void remove(int index) {
        load();
        if (index >= 0 && index < PRESETS.size()) {
            PRESETS.remove(index);
            save();
        }
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.isRegularFile(FILE)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String value = line.trim();
                if (!value.isEmpty() && PRESETS.size() < MAX_PRESETS) {
                    PRESETS.add(Integer.parseInt(value, 16) & 0xFFFFFF);
                }
            }
        } catch (IOException | NumberFormatException exception) {
            YMColoredLights.LOGGER.warn("Could not load Colored Light presets from {}.", FILE, exception);
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            List<String> lines = PRESETS.stream().map(color -> String.format("%06X", color)).toList();
            Files.write(FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            YMColoredLights.LOGGER.warn("Could not save Colored Light presets to {}.", FILE, exception);
        }
    }
}
