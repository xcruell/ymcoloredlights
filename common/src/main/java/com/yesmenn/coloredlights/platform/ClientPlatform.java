package com.yesmenn.coloredlights.platform;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client-only loader services. Set by the client entrypoint of each loader. */
public final class ClientPlatform {
    private static volatile Impl instance;

    private ClientPlatform() {
    }

    public static void init(Impl impl) {
        instance = impl;
    }

    /** A standalone model registered through {@code YMColoredLightsClientCommon#ADDITIONAL_MODELS}. */
    public static BakedModel getAdditionalModel(ResourceLocation id) {
        return instance.getAdditionalModel(id);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        instance.sendToServer(payload);
    }

    public interface Impl {
        BakedModel getAdditionalModel(ResourceLocation id);

        void sendToServer(CustomPacketPayload payload);
    }
}
