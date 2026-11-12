package com.yesmenn.coloredlights.platform;

import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Loader services used by the common code. Each loader sets its implementation first thing in its entrypoint, before
 * any other mod class is touched.
 */
public final class Platform {
    private static volatile Impl instance;

    private Platform() {
    }

    public static void init(Impl impl) {
        instance = impl;
    }

    public static Path configDir() {
        return instance.configDir();
    }

    public static String modVersion(String modId) {
        return instance.modVersion(modId);
    }

    public static boolean isModLoaded(String modId) {
        return instance.isModLoaded(modId);
    }

    public static boolean isClient() {
        return instance.isClient();
    }

    /** IDs of all loaded mods whose metadata carries the given custom property (not set to false). */
    public static Set<String> modsWithProperty(String key) {
        return instance.modsWithProperty(key);
    }

    /**
     * Registers an object. NeoForge defers it to the registry event; Fabric registers it at once, so register
     * dependencies (blocks) before the objects using them (items, block entity types).
     */
    public static <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory) {
        return instance.register(registry, name, factory);
    }

    public static CreativeModeTab.Builder creativeTabBuilder() {
        return instance.creativeTabBuilder();
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        instance.sendToPlayer(player, payload);
    }

    public interface Impl {
        Path configDir();

        String modVersion(String modId);

        boolean isModLoaded(String modId);

        boolean isClient();

        Set<String> modsWithProperty(String key);

        <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory);

        CreativeModeTab.Builder creativeTabBuilder();

        void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
    }
}
