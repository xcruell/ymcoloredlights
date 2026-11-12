package com.yesmenn.coloredlights.compat.veil;

import com.yesmenn.coloredlights.block.ColoredLightBlock;
import com.yesmenn.coloredlights.block.ColoredLightPanelBlock;
import com.yesmenn.coloredlights.block.ColoredLightSpotlightBlock;
import com.yesmenn.coloredlights.block.ColoredLightTubeBlock;
import com.yesmenn.coloredlights.block.FluorescentLampBlock;
import com.yesmenn.coloredlights.block.HangingFluorescentLampBlock;
import com.yesmenn.coloredlights.block.HangingLampChainBlock;
import com.yesmenn.coloredlights.block.HangingLampBlock;
import com.yesmenn.coloredlights.block.InvisibleColoredLightSpotlightBlock;
import com.yesmenn.coloredlights.block.InvisibleColoredPointLightBlock;
import com.yesmenn.coloredlights.block.entity.ColoredLightBlockEntity;
import com.yesmenn.coloredlights.item.ColoredLightTubeItem;
import com.yesmenn.coloredlights.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public final class ColoredLightCompatRegistry {
    public static final Supplier<ColoredLightBlock> COLORED_LIGHT =
            block("colored_light",
                    () -> new ColoredLightBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(1.5F, 6.0F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()));

    public static final Supplier<BlockItem> COLORED_LIGHT_ITEM =
            item("colored_light",
                    () -> new BlockItem(COLORED_LIGHT.get(), new Item.Properties()));

    public static final Supplier<ColoredLightTubeBlock> COLORED_LIGHT_TUBE =
            block("colored_light_tube",
                    () -> new ColoredLightTubeBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(1.5F, 6.0F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()));

    public static final Supplier<ColoredLightTubeItem> COLORED_LIGHT_TUBE_ITEM =
            item("colored_light_tube",
                    () -> new ColoredLightTubeItem(COLORED_LIGHT_TUBE.get(), new Item.Properties()));

    public static final Supplier<ColoredLightPanelBlock> COLORED_LIGHT_PANEL =
            block("colored_light_panel",
                    () -> new ColoredLightPanelBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(1.5F, 6.0F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()));

    public static final Supplier<BlockItem> COLORED_LIGHT_PANEL_ITEM =
            item("colored_light_panel",
                    () -> new BlockItem(COLORED_LIGHT_PANEL.get(), new Item.Properties()));

    public static final Supplier<ColoredLightSpotlightBlock> COLORED_LIGHT_SPOTLIGHT =
            block("colored_light_spotlight",
                    () -> new ColoredLightSpotlightBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(1.5F, 6.0F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()));

    public static final Supplier<BlockItem> COLORED_LIGHT_SPOTLIGHT_ITEM =
            item("colored_light_spotlight",
                    () -> new BlockItem(COLORED_LIGHT_SPOTLIGHT.get(), new Item.Properties()));

    public static final Supplier<ColoredLightSpotlightBlock> COLORED_LIGHT_SPOTLIGHT_BIG =
            block("colored_light_spotlight_big",
                    () -> new ColoredLightSpotlightBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(1.5F, 6.0F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()));

    public static final Supplier<BlockItem> COLORED_LIGHT_SPOTLIGHT_BIG_ITEM =
            item("colored_light_spotlight_big",
                    () -> new BlockItem(COLORED_LIGHT_SPOTLIGHT_BIG.get(), new Item.Properties()));

    public static final Supplier<HangingLampBlock> HANGING_LAMP =
            block("hanging_lamp",
                    () -> new HangingLampBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(1.5F, 6.0F)
                                    .sound(SoundType.LANTERN)
                                    .noOcclusion()));

    public static final Supplier<BlockItem> HANGING_LAMP_ITEM =
            item("hanging_lamp",
                    () -> new BlockItem(HANGING_LAMP.get(), new Item.Properties()));

    public static final Supplier<FluorescentLampBlock> FLUORESCENT_LAMP =
            block("fluorescent_lamp",
                    () -> new FluorescentLampBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(1.5F, 6.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));

    public static final Supplier<BlockItem> FLUORESCENT_LAMP_ITEM =
            item("fluorescent_lamp",
                    () -> new BlockItem(FLUORESCENT_LAMP.get(), new Item.Properties()));

    public static final Supplier<HangingFluorescentLampBlock> FLUORESCENT_LAMP_HANGING =
            block("fluorescent_lamp_hanging",
                    () -> new HangingFluorescentLampBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(1.5F, 6.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));

    public static final Supplier<BlockItem> FLUORESCENT_LAMP_HANGING_ITEM =
            item("fluorescent_lamp_hanging",
                    () -> new BlockItem(FLUORESCENT_LAMP_HANGING.get(), new Item.Properties()));

    /** Chain parts the hanging fluorescent lamp places above its neighbours (no item, no drops). */
    public static final Supplier<HangingLampChainBlock> FLUORESCENT_LAMP_HANGING_CHAIN =
            block("fluorescent_lamp_hanging_chain",
                    () -> new HangingLampChainBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN)
                            .noLootTable()
                            .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));

    /** Decorative chain, the vanilla chain with its own texture (matches the chains of the hanging lamps). */
    public static final Supplier<ChainBlock> CHAIN =
            block("chain", () -> new ChainBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN)));

    public static final Supplier<BlockItem> CHAIN_ITEM =
            item("chain", () -> new BlockItem(CHAIN.get(), new Item.Properties()));

    public static final Supplier<InvisibleColoredLightSpotlightBlock> INVISIBLE_COLORED_LIGHT_SPOTLIGHT =
            block("invisible_colored_light_spotlight",
                    () -> new InvisibleColoredLightSpotlightBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(1.5F, 6.0F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()));

    public static final Supplier<BlockItem> INVISIBLE_COLORED_LIGHT_SPOTLIGHT_ITEM =
            item("invisible_colored_light_spotlight",
                    () -> new BlockItem(INVISIBLE_COLORED_LIGHT_SPOTLIGHT.get(), new Item.Properties()));

    public static final Supplier<InvisibleColoredPointLightBlock> INVISIBLE_COLORED_POINTLIGHT =
            block("invisible_colored_pointlight",
                    () -> new InvisibleColoredPointLightBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(1.5F, 6.0F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()));

    public static final Supplier<BlockItem> INVISIBLE_COLORED_POINTLIGHT_ITEM =
            item("invisible_colored_pointlight",
                    () -> new BlockItem(INVISIBLE_COLORED_POINTLIGHT.get(), new Item.Properties()));

    public static final Supplier<BlockEntityType<ColoredLightBlockEntity>> COLORED_LIGHT_ENTITY =
            Platform.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "colored_light",
                    () -> BlockEntityType.Builder.of(
                            ColoredLightBlockEntity::new,
                            COLORED_LIGHT.get(),
                            COLORED_LIGHT_TUBE.get(),
                            COLORED_LIGHT_PANEL.get(),
                            COLORED_LIGHT_SPOTLIGHT.get(),
                            COLORED_LIGHT_SPOTLIGHT_BIG.get(),
                            HANGING_LAMP.get(),
                            FLUORESCENT_LAMP.get(),
                            FLUORESCENT_LAMP_HANGING.get(),
                            INVISIBLE_COLORED_LIGHT_SPOTLIGHT.get(),
                            INVISIBLE_COLORED_POINTLIGHT.get()
                    ).build(null));

    private ColoredLightCompatRegistry() {
    }

    /** Loads this class, which registers everything above. */
    public static void init() {
    }

    private static <T extends Block> Supplier<T> block(String name, Supplier<T> factory) {
        return Platform.register(BuiltInRegistries.BLOCK, name, factory);
    }

    private static <T extends Item> Supplier<T> item(String name, Supplier<T> factory) {
        return Platform.register(BuiltInRegistries.ITEM, name, factory);
    }
}