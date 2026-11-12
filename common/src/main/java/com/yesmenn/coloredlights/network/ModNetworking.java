package com.yesmenn.coloredlights.network;

import com.yesmenn.coloredlights.block.ColoredLightTubeBlock;
import com.yesmenn.coloredlights.block.entity.ColoredLightBlockEntity;
import com.yesmenn.coloredlights.item.ColorfulFlashlightItem;
import com.yesmenn.coloredlights.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Packet handlers shared by both loaders; the loaders register the payload types (see {@link #PROTOCOL_VERSION}) and
 * call these on the main thread.
 */
public final class ModNetworking {
    // Version 2: open-screen payload and flashlight settings carry flicker / glow / spill / fringe.
    // Version 3: SET_CONFIG packs the radius in 7 bits (up to 128).
    // Version 4: hanging lamp warm-up time (SET_WARMUP, open-screen payload).
    // Version 5: hanging lamp warm-up color (SET_WARMUP_TINT, open-screen payload).
    public static final String PROTOCOL_VERSION = "5";

    private ModNetworking() {
    }

    public static void handleColoredLightAction(ColoredLightActionPayload payload, Player sender) {
        if (!(sender instanceof ServerPlayer player)
                || !canEdit(player, payload.pos())
                || !(player.level().getBlockEntity(payload.pos()) instanceof ColoredLightBlockEntity light)) {
            return;
        }

        // A row of connected tubes is one light: every change applies to all of them.
        for (ColoredLightBlockEntity target : rowOf(player, light)) {
            applyAction(target, payload);
        }
    }

    private static java.util.List<ColoredLightBlockEntity> rowOf(ServerPlayer player, ColoredLightBlockEntity light) {
        if (!(light.getBlockState().getBlock() instanceof ColoredLightTubeBlock)) {
            return java.util.List.of(light);
        }
        java.util.List<ColoredLightBlockEntity> row = new java.util.ArrayList<>();
        for (BlockPos pos : ColoredLightTubeBlock.connectedRun(player.level(), light.getBlockPos())) {
            if (player.level().getBlockEntity(pos) instanceof ColoredLightBlockEntity tube) {
                row.add(tube);
            }
        }
        return row;
    }

    private static void applyAction(ColoredLightBlockEntity light, ColoredLightActionPayload payload) {
        if (payload.action() == ColoredLightActionPayload.SET_RADIUS) {
            light.setRadius(payload.value());
        } else if (payload.action() == ColoredLightActionPayload.SET_COLOR) {
            light.setColor(payload.value());
        } else if (payload.action() == ColoredLightActionPayload.SET_CONFIG) {
            light.setConfig(
                    ColoredLightActionPayload.unpackRadius(payload.value()),
                    ColoredLightActionPayload.unpackColor(payload.value()),
                    ColoredLightActionPayload.unpackRedstoneInverted(payload.value()));
        } else if (payload.action() == ColoredLightActionPayload.SET_BRIGHTNESS) {
            light.setBrightness(payload.value());
        } else if (payload.action() == ColoredLightActionPayload.SET_FLICKER) {
            light.setFlicker(payload.value());
        } else if (payload.action() == ColoredLightActionPayload.SET_WARMUP_TINT) {
            light.setWarmupTint(payload.value());
        } else if (payload.action() == ColoredLightActionPayload.SET_WARMUP) {
            light.setWarmup(payload.value());
        } else if (payload.action() == ColoredLightActionPayload.SET_GLOW) {
            light.setGlow(payload.value());
        } else if (payload.action() == ColoredLightActionPayload.SET_SPOTLIGHT_CONFIG) {
            light.setSpotlightConfig(
                    ColoredLightActionPayload.unpackSpotlightAngle(payload.value()),
                    ColoredLightActionPayload.unpackSpotlightBrightness(payload.value()),
                    ColoredLightActionPayload.unpackSpotlightYaw(payload.value()),
                    ColoredLightActionPayload.unpackSpotlightPitch(payload.value()));
        }
    }

    public static void handleColoredLightSpotlight(ColoredLightSpotlightPayload payload, Player sender) {
        if (!(sender instanceof ServerPlayer player)
                || !canEdit(player, payload.pos())
                || !(player.level().getBlockEntity(payload.pos()) instanceof ColoredLightBlockEntity light)) {
            return;
        }

        light.setSpotlightConfig(
                payload.angle(),
                payload.brightness(),
                payload.yaw(),
                payload.pitch(),
                payload.size(),
                payload.softness(),
                payload.focus());
    }

    public static void handleFlashlightConfig(FlashlightConfigPayload payload, Player sender) {
        if (!(sender instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = player.getItemInHand(payload.hand());
        if (stack.getItem() instanceof ColorfulFlashlightItem) {
            payload.settings().write(stack);
        }
    }

    private static boolean canEdit(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(pos.getCenter()) <= 64.0D
                && player.mayBuild()
                && player.level().isLoaded(pos)
                && player.level().mayInteract(player, pos);
    }

    public static void handleOpenColoredLightScreen(OpenColoredLightScreenPayload payload) {
        if (!Platform.isClient()) {
            return;
        }

        try {
            Class<?> hooks = Class.forName("com.yesmenn.coloredlights.client.ColoredLightClientHooks");
            hooks.getMethod("openScreen", OpenColoredLightScreenPayload.class).invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not open Colored Light screen.", exception);
        }
    }
}