package com.yesmenn.coloredlights.compat.veil;

import com.yesmenn.coloredlights.block.entity.ColoredLightBlockEntity;
import com.yesmenn.coloredlights.client.ColoredLightSpotlightRenderer;
import com.yesmenn.coloredlights.client.FluorescentLampRenderer;
import com.yesmenn.coloredlights.item.FlashlightSettings;
import com.yesmenn.coloredlights.registry.ModItems;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class ColoredLightClientCompat {
    private ColoredLightClientCompat() {
    }

    /** Block-entity-renderer models that are not part of a block state; each loader registers them as standalone. */
    public static List<ResourceLocation> additionalModels() {
        return List.of(
                ColoredLightSpotlightRenderer.YAW_MODEL,
                ColoredLightSpotlightRenderer.BODY_MODEL,
                ColoredLightSpotlightRenderer.LENS_MODEL,
                ColoredLightSpotlightRenderer.BIG_YAW_MODEL,
                ColoredLightSpotlightRenderer.BIG_BODY_MODEL,
                ColoredLightSpotlightRenderer.BIG_LENS_MODEL,
                ColoredLightSpotlightRenderer.INVISIBLE_SPOTLIGHT_MODEL,
                ColoredLightSpotlightRenderer.INVISIBLE_POINTLIGHT_MODEL,
                FluorescentLampRenderer.OFF_MODEL,
                FluorescentLampRenderer.ON_MODEL,
                FluorescentLampRenderer.HANGING_MODEL);
    }

    @FunctionalInterface
    public interface BlockColorRegistrar {
        void register(BlockColor color, Block... blocks);
    }

    @FunctionalInterface
    public interface ItemColorRegistrar {
        void register(ItemColor color, ItemLike... items);
    }

    public static void registerBlockColors(BlockColorRegistrar registrar) {
        registrar.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null
                    && level.getBlockEntity(pos) instanceof ColoredLightBlockEntity light) {
                return light.getColor();
            }
            return ColoredLightBlockEntity.DEFAULT_COLOR;
        },
                ColoredLightCompatRegistry.COLORED_LIGHT.get(),
                ColoredLightCompatRegistry.COLORED_LIGHT_TUBE.get(),
                ColoredLightCompatRegistry.COLORED_LIGHT_PANEL.get(),
                ColoredLightCompatRegistry.COLORED_LIGHT_SPOTLIGHT.get(),
                ColoredLightCompatRegistry.COLORED_LIGHT_SPOTLIGHT_BIG.get(),
                ColoredLightCompatRegistry.INVISIBLE_COLORED_LIGHT_SPOTLIGHT.get(),
                ColoredLightCompatRegistry.INVISIBLE_COLORED_POINTLIGHT.get());
    }

    public static void registerItemColors(ItemColorRegistrar registrar) {
        registrar.register((stack, tintIndex) -> {
            CustomData blockEntityData = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
            // Item colors run every frame for every visible stack: read the tag in place instead of copying it.
            if (blockEntityData.contains("Color")) {
                return blockEntityData.getUnsafe().getInt("Color") & 0xFFFFFF;
            }
            return ColoredLightBlockEntity.DEFAULT_COLOR;
        },
                ColoredLightCompatRegistry.COLORED_LIGHT_ITEM.get(),
                ColoredLightCompatRegistry.COLORED_LIGHT_TUBE_ITEM.get(),
                ColoredLightCompatRegistry.COLORED_LIGHT_PANEL_ITEM.get(),
                ColoredLightCompatRegistry.COLORED_LIGHT_SPOTLIGHT_ITEM.get(),
                ColoredLightCompatRegistry.COLORED_LIGHT_SPOTLIGHT_BIG_ITEM.get(),
                ColoredLightCompatRegistry.INVISIBLE_COLORED_LIGHT_SPOTLIGHT_ITEM.get(),
                ColoredLightCompatRegistry.INVISIBLE_COLORED_POINTLIGHT_ITEM.get());

        // Layer 1 is the flashlight lens: full beam color when on, dimmed when off.
        registrar.register((stack, tintIndex) -> {
            if (tintIndex != 1) {
                return -1;
            }
            int color = FlashlightSettings.read(stack).color();
            if (FlashlightSettings.isEnabled(stack)) {
                return 0xFF000000 | color;
            }
            return 0xFF000000
                    | (color >> 16 & 0xFF) * 2 / 5 << 16
                    | (color >> 8 & 0xFF) * 2 / 5 << 8
                    | (color & 0xFF) * 2 / 5;
        }, ModItems.COLORFUL_FLASHLIGHT.get());
    }
}