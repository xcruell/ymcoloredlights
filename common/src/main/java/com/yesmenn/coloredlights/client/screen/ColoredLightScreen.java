package com.yesmenn.coloredlights.client.screen;

import com.yesmenn.coloredlights.block.ColoredLightPanelBlock;
import com.yesmenn.coloredlights.block.FluorescentLampBlock;
import com.yesmenn.coloredlights.block.HangingFluorescentLampBlock;
import com.yesmenn.coloredlights.item.FlashlightSettings;
import com.yesmenn.coloredlights.network.OpenColoredLightScreenPayload;
import com.yesmenn.ducttape.client.kit.YmContainer;
import com.yesmenn.ducttape.client.kit.YmElement;
import com.yesmenn.ducttape.client.kit.YmScreen;
import com.yesmenn.ducttape.client.kit.YmScrollBox;
import com.yesmenn.ducttape.client.kit.element.YmButton;
import com.yesmenn.ducttape.client.kit.element.YmPreview;
import com.yesmenn.ducttape.client.kit.element.YmText;
import com.yesmenn.ducttape.client.kit.element.YmWindow;
import com.yesmenn.ducttape.client.kit.layout.YmLayout;
import com.yesmenn.ducttape.client.kit.style.YmColors;
import com.yesmenn.ducttape.client.kit.style.YmStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Settings screen of a colored light (lamp block or held flashlight), built with the YM kit: a preview column on the
 * left (the lamp, its color and its light cone) and the {@link LampModules} on the right, one collapsible block per
 * feature the light has. Changes show at once in the world; while a value is dragged the window fades out so the
 * light can be watched.
 */
public class ColoredLightScreen extends YmScreen {
    private static final int SIDE_WIDTH = 116;

    private final LampEditor editor;
    private int pageScroll;
    private YmScrollBox page;

    public ColoredLightScreen(OpenColoredLightScreenPayload payload) {
        this(Component.translatable("screen.ymcoloredlights.colored_light"), LampEditor.forLamp(payload));
    }

    public ColoredLightScreen(InteractionHand hand, FlashlightSettings settings) {
        this(Component.translatable("screen.ymcoloredlights.colorful_flashlight"),
                LampEditor.forFlashlight(hand, settings));
    }

    private ColoredLightScreen(Component title, LampEditor editor) {
        super(title, null);
        this.editor = editor;
        setAccent(YmStyle.ACCENT_LIGHTS);
        setShowWorld(true);
        setFadeWhileDragging(true);
    }

    @Override
    protected void build(YmContainer root) {
        // Keep the scroll position when the page is built again (e.g. after a preset change).
        if (page != null) {
            pageScroll = page.getScroll();
        }
        int w = Math.min(460, width - 16);
        int h = Math.min(340, height - 16);
        YmWindow window = root.add(new YmWindow(getTitle(), w, h)).closable();
        window.centerIn(root);
        window.footer().add(new YmButton(Component.translatable("gui.done"), button -> onClose()).primary());

        YmContainer content = window.content();
        content.setPadding(4);
        YmContainer side = content.add(new YmContainer());
        page = content.add(new YmScrollBox());
        content.setLayout(panel -> {
            side.setBounds(0, 0, SIDE_WIDTH, panel.contentHeight());
            page.setBounds(SIDE_WIDTH + 4, 0, panel.contentWidth() - SIDE_WIDTH - 4, panel.contentHeight());
        });

        buildSide(side);
        page.setLayout(YmLayout.column(3));
        page.setPadding(0, 0, YmScrollBox.BAR_WIDTH + 2, 0);
        LampModules.SET.build(page, editor);
    }

    @Override
    protected void init() {
        super.init();
        // After the layout, when the page height is known.
        page.setScroll(pageScroll);
    }

    private void buildSide(YmContainer side) {
        side.setBackground(YmContainer.Background.INSET).setPadding(4).setLayout(YmLayout.column(4));
        // Lamps: the block in the world with its renderer (spotlight head, fluorescent tubes) and its live color.
        YmPreview preview = editor.flashlight ? YmPreview.item(editor::flashlightStack)
                : YmPreview.blockAt(() -> editor.pos, ColoredLightScreen::previewUp);
        if (editor.fluorescentLamp) {
            // The fluorescent lamp is two to three blocks long.
            preview.setView(-35.0F, 25.0F, 0.5F);
        }
        side.add(preview.setAutoRotate(true, 20.0F)).setSize(0, 92);
        side.add(new ColorBar());
        side.add(new YmText(() -> Component.literal(YmColors.toHex(editor.color))).align(YmText.Align.CENTER).dim());
        if (editor.spotlight) {
            side.add(new BeamPreview());
        }
    }

    @Override
    public void tick() {
        super.tick();
        editor.tick();
    }

    @Override
    public void onClose() {
        editor.close();
        super.onClose();
    }

    /** The current light color, as wide as the side column. */
    private final class ColorBar extends YmElement {
        private ColorBar() {
            super(0, 16);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            YmStyle style = style();
            int x = getX();
            int y = getY();
            style.drawInset(graphics, x, y, width, height);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF000000 | editor.color);
        }
    }

    /**
     * Panels and (standing) fluorescent lamps lie on the floor in the preview, also when they hang on a wall or the
     * ceiling: on their side they look odd while the preview turns.
     */
    @Nullable
    private static Direction previewUp(BlockState state) {
        Block block = state.getBlock();
        boolean flat = block instanceof ColoredLightPanelBlock
                || block instanceof FluorescentLampBlock && !(block instanceof HangingFluorescentLampBlock);
        return flat && state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING)
                : null;
    }

    /** Side view of the light cone: angle, edge softness and focus, in the light color. */
    private final class BeamPreview extends YmElement {
        private BeamPreview() {
            super(0, 84);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            YmStyle style = style();
            int x = getX();
            int y = getY();
            style.drawInset(graphics, x, y, width, height);
            int centerX = x + width / 2;
            int originY = y + 8;
            int beamLength = height - 14;
            int halfWidth = Math.max(4, editor.angle * (width - 12) / 180);
            int softness = Math.max(1, editor.softness / 4);
            int rgb = editor.color;
            float focus = editor.focus / 100.0F;
            // The light points down from the lamp at the top.
            for (int row = 0; row < beamLength; row++) {
                float progress = row / (float) beamLength;
                int rowHalf = Math.max(1, Math.round(halfWidth * progress));
                int centerAlpha = Mth.clamp(Math.round(130.0F * (float) Math.pow(1.0F - progress,
                        1.0F / Math.max(0.2F, focus))), 10, 150);
                int edgeAlpha = Mth.clamp(centerAlpha / 3, 4, 60);
                int drawY = originY + row;
                graphics.fill(centerX - rowHalf, drawY, centerX + rowHalf, drawY + 1, centerAlpha << 24 | rgb);
                graphics.fill(centerX - rowHalf - softness, drawY, centerX - rowHalf, drawY + 1, edgeAlpha << 24 | rgb);
                graphics.fill(centerX + rowHalf, drawY, centerX + rowHalf + softness, drawY + 1, edgeAlpha << 24 | rgb);
            }
            graphics.fill(centerX - 3, originY - 3, centerX + 3, originY + 1, 0xFFFFFFFF);
            graphics.drawString(font(), Component.translatable("gui.ymcoloredlights.light.spotlight_preview"),
                    x + 4, y + height - 11, style.textDim, false);
        }
    }
}
