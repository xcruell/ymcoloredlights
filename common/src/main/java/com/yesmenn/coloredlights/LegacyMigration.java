package com.yesmenn.coloredlights;

import com.yesmenn.coloredlights.platform.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Takes over data from before the rename "Lights? YesMenn!" (mod ID {@value #LEGACY_MODID}) -> YM Colored Lights. Old
 * worlds keep their lamps through registry aliases (NeoForge, the only loader the old mod ID ran on); old config files
 * are moved to their new names once.
 */
public final class LegacyMigration {
    public static final String LEGACY_MODID = "lightsyesmenn";

    private LegacyMigration() {
    }

    /** {@code config/<name>}; an old file under one of {@code legacyNames} is renamed to it first, if it is missing. */
    public static Path configFile(String name, String... legacyNames) {
        Path target = Platform.configDir().resolve(name);
        if (Files.exists(target)) {
            return target;
        }
        for (String legacyName : legacyNames) {
            Path legacy = Platform.configDir().resolve(legacyName);
            if (Files.isRegularFile(legacy)) {
                try {
                    Files.move(legacy, target);
                    YMColoredLights.LOGGER.info("Moved old config {} to {}", legacy.getFileName(), name);
                } catch (IOException exception) {
                    YMColoredLights.LOGGER.warn("Could not move old config {} to {}", legacy, target, exception);
                }
                break;
            }
        }
        return target;
    }
}
