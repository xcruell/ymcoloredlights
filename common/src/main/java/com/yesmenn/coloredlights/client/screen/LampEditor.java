package com.yesmenn.coloredlights.client.screen;

import com.yesmenn.coloredlights.block.ColoredLightBlock;
import com.yesmenn.coloredlights.block.ColoredLightSpotlightBlock;
import com.yesmenn.coloredlights.block.ColoredLightTubeBlock;
import com.yesmenn.coloredlights.block.FluorescentLampBlock;
import com.yesmenn.coloredlights.block.HangingLampBlock;
import com.yesmenn.coloredlights.block.entity.ColoredLightBlockEntity;
import com.yesmenn.coloredlights.item.ColorfulFlashlightItem;
import com.yesmenn.coloredlights.item.FlashlightSettings;
import com.yesmenn.coloredlights.network.ColoredLightActionPayload;
import com.yesmenn.coloredlights.network.ColoredLightSpotlightPayload;
import com.yesmenn.coloredlights.network.FlashlightConfigPayload;
import com.yesmenn.coloredlights.network.OpenColoredLightScreenPayload;
import com.yesmenn.coloredlights.platform.ClientPlatform;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The values the lamp screen edits (a lamp block or a held flashlight) and their sync: every change is shown at once
 * on the client (block entity or item) and sent to the server at most every {@link #SYNC_INTERVAL_MS}; closing sends
 * the last state. The screen's modules read and write the public fields and then call {@link #changed()}.
 */
public final class LampEditor {
    public static final int MIN_BRIGHTNESS = 25;
    public static final int MAX_BRIGHTNESS = 800;
    private static final long SYNC_INTERVAL_MS = 100L;

    /** Target lamp, or null when a held flashlight is edited. */
    @Nullable
    public final BlockPos pos;
    /** Hand holding the edited flashlight, or null when a lamp is edited. */
    @Nullable
    public final InteractionHand hand;
    public final boolean flashlight;
    /** Spotlights, hanging lamps and the flashlight have a light cone. */
    public final boolean spotlight;
    /** Hanging lamps always point down (no yaw/pitch, wider cone) and have a warm-up. */
    public final boolean hangingLamp;
    /** Fluorescent lamp: its warm-up is the start-up flicker. */
    public final boolean fluorescentLamp;
    public final int maxRadius;

    public int radius;
    public int color;
    public int brightness;
    public boolean redstoneInverted;
    public int angle;
    public int yaw;
    public int pitch;
    public int size;
    public int softness;
    public int focus;
    public int flicker;
    public int glow;
    /** Hanging lamp warm-up in tenths of a second. */
    public int warmup;
    /** Hanging lamp warm-up color, index into {@link HangingLampBlock#WARMUP_TINTS} (0 = off). */
    public int warmupTint;
    /** Flashlight only: brightness of the wide spill cone around the core (0-100). */
    public int spill;
    /** Flashlight only: chromatic fringe at the beam edge (0-100). */
    public int fringe;

    private boolean pendingSync;
    private long lastSyncMillis;

    private LampEditor(@Nullable BlockPos pos, @Nullable InteractionHand hand, boolean spotlight) {
        this.pos = pos;
        this.hand = hand;
        this.flashlight = hand != null;
        this.spotlight = spotlight;
        BlockState state = blockState();
        this.hangingLamp = state.getBlock() instanceof HangingLampBlock;
        this.fluorescentLamp = state.getBlock() instanceof FluorescentLampBlock;
        this.maxRadius = flashlight ? FlashlightSettings.MAX_RANGE
                : pos != null && state.getBlock() instanceof ColoredLightBlock ? ColoredLightBlock.maxRadius(state)
                : ColoredLightBlock.MAX_LAMP_RADIUS;
    }

    public static LampEditor forLamp(OpenColoredLightScreenPayload payload) {
        LampEditor editor = new LampEditor(payload.pos(), null, payload.spotlight());
        editor.radius = payload.radius();
        editor.color = payload.color() & 0xFFFFFF;
        editor.brightness = payload.brightness();
        editor.redstoneInverted = payload.redstoneInverted();
        editor.angle = payload.spotlightAngle();
        editor.yaw = payload.spotlightYaw();
        editor.pitch = payload.spotlightPitch();
        editor.size = payload.spotlightSize();
        editor.softness = payload.spotlightSoftness();
        editor.focus = payload.spotlightFocus();
        editor.flicker = payload.flicker();
        editor.glow = payload.glow();
        editor.warmup = payload.warmup();
        editor.warmupTint = payload.warmupTint();
        return editor;
    }

    public static LampEditor forFlashlight(InteractionHand hand, FlashlightSettings settings) {
        LampEditor editor = new LampEditor(null, hand, true);
        editor.radius = settings.range();
        editor.color = settings.color() & 0xFFFFFF;
        editor.brightness = settings.brightness();
        editor.angle = settings.angle();
        editor.size = settings.size();
        editor.softness = settings.softness();
        editor.focus = settings.focus();
        editor.spill = settings.spill();
        editor.glow = settings.glow();
        editor.fringe = settings.fringe();
        editor.flicker = settings.flicker();
        return editor;
    }

    /** The edited block (air for the flashlight or an unloaded position). */
    public BlockState blockState() {
        Minecraft minecraft = Minecraft.getInstance();
        return pos != null && minecraft.level != null ? minecraft.level.getBlockState(pos)
                : Blocks.AIR.defaultBlockState();
    }

    /** The held flashlight (empty for lamps or when it is gone). */
    public ItemStack flashlightStack() {
        Minecraft minecraft = Minecraft.getInstance();
        if (hand == null || minecraft.player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = minecraft.player.getItemInHand(hand);
        return stack.getItem() instanceof ColorfulFlashlightItem ? stack : ItemStack.EMPTY;
    }

    // ---- defaults (for the per-value reset) ----

    public int defaultRadius() {
        return flashlight ? FlashlightSettings.DEFAULT.range() : ColoredLightBlock.DEFAULT_RADIUS;
    }

    public int defaultBrightness() {
        return flashlight ? FlashlightSettings.DEFAULT.brightness()
                : spotlight ? ColoredLightBlockEntity.DEFAULT_SPOTLIGHT_BRIGHTNESS
                : ColoredLightBlockEntity.DEFAULT_BRIGHTNESS;
    }

    public int defaultAngle() {
        return flashlight ? FlashlightSettings.DEFAULT.angle()
                : hangingLamp ? HangingLampBlock.DEFAULT_ANGLE : ColoredLightBlockEntity.DEFAULT_SPOTLIGHT_ANGLE;
    }

    public int maxAngle() {
        return hangingLamp ? HangingLampBlock.MAX_ANGLE : 75;
    }

    public int defaultSize() {
        return flashlight ? FlashlightSettings.DEFAULT.size()
                : hangingLamp ? HangingLampBlock.DEFAULT_SIZE : ColoredLightBlockEntity.DEFAULT_SPOTLIGHT_SIZE;
    }

    public int defaultSoftness() {
        return flashlight ? FlashlightSettings.DEFAULT.softness()
                : hangingLamp ? HangingLampBlock.DEFAULT_SOFTNESS : ColoredLightBlockEntity.DEFAULT_SPOTLIGHT_SOFTNESS;
    }

    public int defaultFocus() {
        return flashlight ? FlashlightSettings.DEFAULT.focus() : ColoredLightBlockEntity.DEFAULT_SPOTLIGHT_FOCUS;
    }

    public int defaultYaw() {
        return ColoredLightBlockEntity.DEFAULT_SPOTLIGHT_YAW;
    }

    public int defaultPitch() {
        BlockState state = blockState();
        return state.getBlock() instanceof ColoredLightSpotlightBlock && !hangingLamp
                ? ColoredLightSpotlightBlock.defaultDisplayPitch(state)
                : ColoredLightBlockEntity.DEFAULT_SPOTLIGHT_PITCH;
    }

    public int defaultFlicker() {
        return flashlight ? FlashlightSettings.DEFAULT.flicker() : ColoredLightBlockEntity.DEFAULT_FLICKER;
    }

    public int defaultGlow() {
        return flashlight ? FlashlightSettings.DEFAULT.glow() : ColoredLightBlockEntity.DEFAULT_GLOW;
    }

    public int defaultWarmup() {
        return fluorescentLamp ? ColoredLightBlockEntity.DEFAULT_FLUORESCENT_WARMUP
                : ColoredLightBlockEntity.DEFAULT_WARMUP;
    }

    // ---- sync ----

    /** Call after changing any value: shows it at once and queues a throttled server sync. */
    public void changed() {
        applyPreview();
        pendingSync = true;
        flush(false);
    }

    /**
     * Every tick: writes the values into the client block entity again (a server answer can briefly carry older values
     * and would make the light jump back) and sends a queued sync once the interval has passed.
     */
    public void tick() {
        applyPreview();
        flush(false);
    }

    /** When the screen closes: sends the last state. */
    public void close() {
        flush(true);
    }

    private void applyPreview() {
        if (flashlight) {
            ItemStack stack = flashlightStack();
            if (!stack.isEmpty()) {
                flashlightSettings().write(stack);
            }
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || pos == null
                || !(minecraft.level.getBlockEntity(pos) instanceof ColoredLightBlockEntity light)) {
            return;
        }
        light.setConfig(radius, color, redstoneInverted);
        light.setBrightness(brightness);
        light.setFlicker(flicker);
        light.setGlow(glow);
        light.setWarmup(warmup);
        light.setWarmupTint(warmupTint);
        if (spotlight) {
            light.setSpotlightConfig(angle, brightness, yaw, pitch, size, softness, focus);
        }
        if (light.getBlockState().getBlock() instanceof ColoredLightTubeBlock) {
            // A row of connected tubes is one light; preview the change on all of them (the server does the same).
            for (BlockPos tubePos : ColoredLightTubeBlock.connectedRun(minecraft.level, pos)) {
                if (minecraft.level.getBlockEntity(tubePos) instanceof ColoredLightBlockEntity tube) {
                    tube.copyLightSettingsFrom(light);
                }
            }
        }
    }

    private void flush(boolean force) {
        if (!pendingSync) {
            return;
        }
        long now = Util.getMillis();
        if (!force && now - lastSyncMillis < SYNC_INTERVAL_MS) {
            return;
        }
        pendingSync = false;
        lastSyncMillis = now;
        if (flashlight) {
            ClientPlatform.sendToServer(new FlashlightConfigPayload(hand, flashlightSettings()));
            return;
        }
        ClientPlatform.sendToServer(new ColoredLightActionPayload(pos, ColoredLightActionPayload.SET_CONFIG,
                ColoredLightActionPayload.packConfig(radius, color, redstoneInverted)));
        ClientPlatform.sendToServer(new ColoredLightActionPayload(pos, ColoredLightActionPayload.SET_BRIGHTNESS,
                brightness));
        ClientPlatform.sendToServer(new ColoredLightActionPayload(pos, ColoredLightActionPayload.SET_FLICKER, flicker));
        ClientPlatform.sendToServer(new ColoredLightActionPayload(pos, ColoredLightActionPayload.SET_GLOW, glow));
        if (hangingLamp) {
            ClientPlatform.sendToServer(new ColoredLightActionPayload(pos, ColoredLightActionPayload.SET_WARMUP,
                    warmup));
            ClientPlatform.sendToServer(new ColoredLightActionPayload(pos, ColoredLightActionPayload.SET_WARMUP_TINT,
                    warmupTint));
        }
        if (spotlight) {
            ClientPlatform.sendToServer(new ColoredLightSpotlightPayload(pos, angle, brightness, yaw, pitch, size,
                    softness, focus));
        }
    }

    private FlashlightSettings flashlightSettings() {
        return new FlashlightSettings(radius, color, brightness, angle, size, softness, focus, spill, glow, fringe,
                flicker);
    }
}
