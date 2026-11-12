package com.yesmenn.coloredlights.client.screen;

import net.minecraft.util.Mth;

/** Color temperature (Kelvin) of the lamp screen. */
final class LampColors {
    private LampColors() {
    }

    /** Approximate color of a black body at {@code kelvin} (1000-12000 K). */
    static int colorFromTemperature(int kelvin) {
        double temp = kelvin / 100.0;
        double r;
        double g;
        double b;
        if (temp <= 66) {
            r = 255;
            g = 99.4708025861 * Math.log(temp) - 161.1195681661;
            b = temp <= 19 ? 0 : 138.5177312231 * Math.log(temp - 10) - 305.0447927307;
        } else {
            r = 329.698727446 * Math.pow(temp - 60, -0.1332047592);
            g = 288.1221695283 * Math.pow(temp - 60, -0.0755148492);
            b = 255;
        }
        return Mth.clamp((int) Math.round(r), 0, 255) << 16
                | Mth.clamp((int) Math.round(g), 0, 255) << 8
                | Mth.clamp((int) Math.round(b), 0, 255);
    }

    /** The temperature (in 100 K steps) whose color is closest to {@code color}. */
    static int nearestTemperature(int color) {
        int targetRed = color >> 16 & 0xFF;
        int targetGreen = color >> 8 & 0xFF;
        int targetBlue = color & 0xFF;
        int best = 6500;
        long bestDistance = Long.MAX_VALUE;
        for (int kelvin = 1000; kelvin <= 12000; kelvin += 100) {
            int candidate = colorFromTemperature(kelvin);
            int dr = (candidate >> 16 & 0xFF) - targetRed;
            int dg = (candidate >> 8 & 0xFF) - targetGreen;
            int db = (candidate & 0xFF) - targetBlue;
            long distance = (long) dr * dr + (long) dg * dg + (long) db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = kelvin;
            }
        }
        return best;
    }
}
