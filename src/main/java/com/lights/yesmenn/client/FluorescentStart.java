package com.lights.yesmenn.client;

/**
 * Start-up flicker of a fluorescent tube: after switching on, the starter makes the tube blink irregularly, the
 * flashes get more frequent until it strikes and stays on. Spread over the lamp's warm-up time. Shared by the light
 * (brightness) and the block renderer (glowing tubes), so both blink together.
 */
final class FluorescentStart {
    /** Brightness share while the tube is dark between flashes (the electrodes glow faintly). */
    static final float DARK = 0.03F;
    /** Length of one on/off decision. */
    private static final long SLOT_MILLIS = 70L;
    /** Share of the warm-up after which the tube has struck and stays on. */
    private static final double STRIKE = 0.85;
    /** The very first moment stays dark: the starter needs to heat up before the first flash. */
    private static final double DARK_START = 0.08;

    private FluorescentStart() {
    }

    /** Brightness factor 0..1 for the given time since switching on; 1 once the warm-up is over. */
    static float level(long elapsedMillis, long warmupMillis, long seed) {
        if (warmupMillis <= 0L || elapsedMillis >= warmupMillis) {
            return 1.0F;
        }
        if (elapsedMillis < 0L) {
            return DARK;
        }
        double progress = elapsedMillis / (double) warmupMillis;
        if (progress >= STRIKE) {
            return 1.0F;
        }
        if (progress < DARK_START) {
            return DARK;
        }
        // Chance of a slot being lit grows from rare short flashes to almost steady light.
        double chance = 0.15 + 0.75 * (progress - DARK_START) / (STRIKE - DARK_START);
        long slot = elapsedMillis / SLOT_MILLIS;
        return random(seed, slot) < chance ? 1.0F : DARK;
    }

    private static double random(long seed, long slot) {
        long z = seed * 0x9E3779B97F4A7C15L + slot * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 11) * 0x1.0p-53;
    }
}
