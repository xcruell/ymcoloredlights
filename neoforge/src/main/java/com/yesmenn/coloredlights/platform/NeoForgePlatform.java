package com.yesmenn.coloredlights.platform;

import com.yesmenn.coloredlights.LegacyMigration;
import com.yesmenn.coloredlights.YMColoredLights;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class NeoForgePlatform implements Platform.Impl {
    private final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> registers = new LinkedHashMap<>();

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String modVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public Set<String> modsWithProperty(String key) {
        Set<String> mods = new HashSet<>();
        for (IModInfo mod : ModList.get().getMods()) {
            Object value = mod.getModProperties().get(key);
            if (value != null && !"false".equals(String.valueOf(value))) {
                mods.add(mod.getModId());
            }
        }
        return mods;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory) {
        DeferredRegister<R> register = (DeferredRegister<R>) registers.computeIfAbsent(registry.key(),
                key -> DeferredRegister.create((ResourceKey<? extends Registry<R>>) key, YMColoredLights.MODID));
        // Worlds from before the rename (mod ID "lightsyesmenn") keep their blocks, items and block entities.
        register.addAlias(ResourceLocation.fromNamespaceAndPath(LegacyMigration.LEGACY_MODID, name),
                ResourceLocation.fromNamespaceAndPath(YMColoredLights.MODID, name));
        return register.register(name, factory);
    }

    /** Hooks every register filled through {@link #register} to the mod bus; call after the common init. */
    public void registerAll(IEventBus modBus) {
        registers.values().forEach(register -> register.register(modBus));
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
