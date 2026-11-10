package com.lights.yesmenn.ymtab.client;

import com.lights.yesmenn.ymtab.YmCreativeTab;
import com.lights.yesmenn.ymtab.YmCreativeTab.Section;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client side of the shared tab: lays the items out in sections (a banner row, then the section's items, the last row
 * filled up with empty slots) and draws the banners.
 */
public final class YmCreativeSections {
    private static final int COLUMNS = 9;
    private static final int VISIBLE_ROWS = 5;
    /** Item grid in the creative screen: left/top edge of the first slot cell and cell size. */
    private static final int GRID_X = 8;
    private static final int GRID_Y = 17;
    private static final int CELL = 18;
    private static final int BANNER_WIDTH = COLUMNS * CELL;
    /** Dark plate behind the title: solid part beyond the text, then the fade into the banner. */
    private static final int PLATE_PADDING = 6;
    private static final int PLATE_FADE = 28;
    private static final int PLATE_COLOR = 0xD0101418;
    /** Title colors per band from the top (glyphs are 8 px tall) and the shadow color. */
    private static final int[] METAL_BANDS = {0xFFFFFFFF, 0xFFE8ECF1, 0xFFC9D0D9, 0xFF9EA8B6};
    private static final int[] METAL_BAND_HEIGHTS = {2, 2, 2};
    private static final int METAL_SHADOW = 0xFF2B3038;

    /** Section of each banner row of the current layout (index = item row), null for item rows. */
    private static final List<Section> ROWS = new ArrayList<>();
    private static final Map<Section, BannerTexture> TEXTURES = new EnumMap<>(Section.class);
    /** Item count and scroll position of the last drawn frame; scroll -1 = no banners drawn. */
    private static int renderedItemCount;
    private static float renderedScroll = -1.0F;

    private YmCreativeSections() {
    }

    /** Rebuilds {@code items} (the tab's display items) into sections. Clears the layout for other tabs. */
    public static void layout(boolean sharedTab, NonNullList<ItemStack> items) {
        ROWS.clear();
        renderedScroll = -1.0F;
        // Cheap to reload; picks up changed banner files after a resource reload.
        TEXTURES.clear();
        if (!sharedTab || items.isEmpty()) {
            return;
        }
        Map<Section, List<ItemStack>> bySection = new LinkedHashMap<>();
        for (Section section : Section.values()) {
            bySection.put(section, new ArrayList<>());
        }
        for (ItemStack stack : items) {
            Section section = Section.of(stack);
            bySection.get(section != null ? section : Section.OTHER).add(stack);
        }
        items.clear();
        for (Map.Entry<Section, List<ItemStack>> entry : bySection.entrySet()) {
            List<ItemStack> stacks = entry.getValue();
            if (stacks.isEmpty()) {
                continue;
            }
            ROWS.add(entry.getKey());
            for (int i = 0; i < COLUMNS; i++) {
                items.add(ItemStack.EMPTY);
            }
            items.addAll(stacks);
            int rows = (stacks.size() + COLUMNS - 1) / COLUMNS;
            for (int i = stacks.size(); i < rows * COLUMNS; i++) {
                items.add(ItemStack.EMPTY);
            }
            for (int i = 0; i < rows; i++) {
                ROWS.add(null);
            }
        }
    }

    /** First item row on screen, as the creative menu computes it from the scroll position. */
    public static int firstVisibleRow(int itemCount, float scroll) {
        int scrollRows = (itemCount + COLUMNS - 1) / COLUMNS - VISIBLE_ROWS;
        return Math.max((int) ((double) (scroll * (float) scrollRows) + 0.5), 0);
    }

    /** True if {@code slot} is an item picker slot that currently shows a banner row. */
    public static boolean isBannerSlot(Slot slot, int itemCount, float scroll) {
        // Picker slots are the first 5 x 9 slots of the creative menu; the hotbar below belongs to the player.
        if (ROWS.isEmpty() || slot.index >= COLUMNS * VISIBLE_ROWS || slot.container instanceof Inventory) {
            return false;
        }
        int row = firstVisibleRow(itemCount, scroll) + slot.getContainerSlot() / COLUMNS;
        return row < ROWS.size() && ROWS.get(row) != null;
    }

    /** {@link #isBannerSlot} for the state drawn last (the slots are drawn right after the banners). */
    public static boolean isRenderedBannerSlot(Slot slot) {
        return renderedScroll >= 0.0F && isBannerSlot(slot, renderedItemCount, renderedScroll);
    }

    /** Draws the banners of the visible rows over the empty slot grid. */
    public static void render(GuiGraphics graphics, int left, int top, int itemCount, float scroll) {
        renderedItemCount = itemCount;
        renderedScroll = ROWS.isEmpty() ? -1.0F : scroll;
        if (ROWS.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int first = firstVisibleRow(itemCount, scroll);
        for (int visible = 0; visible < VISIBLE_ROWS; visible++) {
            int row = first + visible;
            if (row >= ROWS.size()) {
                break;
            }
            Section section = ROWS.get(row);
            if (section != null) {
                renderBanner(graphics, font, section, left + GRID_X, top + GRID_Y + visible * CELL);
            }
        }
    }

    private static void renderBanner(GuiGraphics graphics, Font font, Section section, int x, int y) {
        BannerTexture texture = TEXTURES.computeIfAbsent(section, YmCreativeSections::loadTexture);
        if (texture.present()) {
            graphics.blit(texture.location(), x, y, BANNER_WIDTH, CELL, 0.0F, 0.0F,
                    texture.width(), texture.height(), texture.width(), texture.height());
        } else {
            int color = section.fallbackColor();
            graphics.fillGradient(x, y, x + BANNER_WIDTH, y + CELL, color, darken(color));
        }
        // Title on a dark plate that fades out to the right into the banner, with a faint light edge on top.
        String title = section.title().getString();
        int textWidth = font.width(title);
        int solid = PLATE_PADDING + textWidth + PLATE_PADDING;
        horizontalGradient(graphics, x, y, x + solid, y + CELL, PLATE_COLOR, PLATE_COLOR);
        horizontalGradient(graphics, x + solid, y, x + solid + PLATE_FADE, y + CELL, PLATE_COLOR, PLATE_COLOR & 0x00FFFFFF);
        horizontalGradient(graphics, x, y, x + solid + PLATE_FADE, y + 1, 0x40FFFFFF, 0x00FFFFFF);
        drawMetalText(graphics, font, title, x + PLATE_PADDING, y + (CELL - font.lineHeight) / 2 + 1);
    }

    /**
     * Brushed-metal look: shadow first, then the text in horizontal bands from bright at the top to a darker steel
     * at the bottom (each band clipped with a scissor), like an engraved metal label.
     */
    private static void drawMetalText(GuiGraphics graphics, Font font, String text, int x, int y) {
        int width = font.width(text) + 1;
        graphics.drawString(font, text, x + 1, y + 1, METAL_SHADOW, false);
        graphics.flush();
        int top = y;
        for (int i = 0; i < METAL_BANDS.length; i++) {
            int bottom = i == METAL_BANDS.length - 1 ? y + font.lineHeight : top + METAL_BAND_HEIGHTS[i];
            graphics.enableScissor(x, top, x + width, bottom);
            graphics.drawString(font, text, x, y, METAL_BANDS[i], false);
            graphics.flush();
            graphics.disableScissor();
            top = bottom;
        }
    }

    private static void horizontalGradient(GuiGraphics graphics, int x0, int y0, int x1, int y1, int left, int right) {
        Matrix4f pose = graphics.pose().last().pose();
        VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
        consumer.addVertex(pose, x0, y0, 0.0F).setColor(left);
        consumer.addVertex(pose, x0, y1, 0.0F).setColor(left);
        consumer.addVertex(pose, x1, y1, 0.0F).setColor(right);
        consumer.addVertex(pose, x1, y0, 0.0F).setColor(right);
        graphics.flush();
    }

    private static int darken(int argb) {
        int r = (argb >> 16 & 0xFF) * 3 / 5;
        int g = (argb >> 8 & 0xFF) * 3 / 5;
        int b = (argb & 0xFF) * 3 / 5;
        return argb & 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Banner textures may have any size with a 9:1 ratio; the size is read from the file once. */
    private static BannerTexture loadTexture(Section section) {
        ResourceLocation location = section.banner();
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            return new BannerTexture(location, false, 0, 0);
        }
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            return new BannerTexture(location, true, image.getWidth(), image.getHeight());
        } catch (IOException e) {
            return new BannerTexture(location, false, 0, 0);
        }
    }

    private record BannerTexture(@Nullable ResourceLocation location, boolean present, int width, int height) {
    }

    /** Whether {@code tab} is the shared tab (only true in the leader mod). */
    public static boolean isSharedTab(@Nullable net.minecraft.world.item.CreativeModeTab tab) {
        return YmCreativeTab.isTab(tab);
    }
}
