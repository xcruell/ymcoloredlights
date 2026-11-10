package com.lights.yesmenn.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Tells whether a baked quad shows semi-transparent texture pixels (alpha between 0 and 255), e.g. glass. Read from
 * the sprite's texture file once and cached per quad; both caches drop their entries when a resource reload replaces
 * the quads and sprites.
 */
final class TextureAlpha {
    private static final int VERTEX_STRIDE = 8;
    private static final int U_OFFSET = 4;
    private static final Map<BakedQuad, Boolean> QUADS = new WeakHashMap<>();
    private static final Map<SpriteContents, int[]> ALPHA = new WeakHashMap<>();
    private static final int[] NONE = new int[0];

    private TextureAlpha() {
    }

    static boolean isTranslucent(BakedQuad quad) {
        return QUADS.computeIfAbsent(quad, TextureAlpha::compute);
    }

    private static boolean compute(BakedQuad quad) {
        TextureAtlasSprite sprite = quad.getSprite();
        SpriteContents contents = sprite.contents();
        int[] alpha = ALPHA.computeIfAbsent(contents, TextureAlpha::load);
        int width = contents.width();
        int height = contents.height();
        if (alpha.length != width * height) {
            return false;
        }
        int[] vertices = quad.getVertices();
        float minU = Float.MAX_VALUE;
        float maxU = -Float.MAX_VALUE;
        float minV = Float.MAX_VALUE;
        float maxV = -Float.MAX_VALUE;
        for (int i = 0; i + U_OFFSET + 1 < vertices.length; i += VERTEX_STRIDE) {
            float u = (Float.intBitsToFloat(vertices[i + U_OFFSET]) - sprite.getU0()) / (sprite.getU1() - sprite.getU0());
            float v = (Float.intBitsToFloat(vertices[i + U_OFFSET + 1]) - sprite.getV0()) / (sprite.getV1() - sprite.getV0());
            minU = Math.min(minU, u);
            maxU = Math.max(maxU, u);
            minV = Math.min(minV, v);
            maxV = Math.max(maxV, v);
        }
        int x0 = clamp((int) Math.floor(minU * width + 1.0E-3F), width);
        int x1 = clamp((int) Math.ceil(maxU * width - 1.0E-3F) - 1, width);
        int y0 = clamp((int) Math.floor(minV * height + 1.0E-3F), height);
        int y1 = clamp((int) Math.ceil(maxV * height - 1.0E-3F) - 1, height);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int a = alpha[y * width + x];
                if (a > 0 && a < 255) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int clamp(int value, int size) {
        return Math.max(0, Math.min(size - 1, value));
    }

    private static int[] load(SpriteContents contents) {
        ResourceLocation name = contents.name();
        ResourceLocation file = name.withPath(path -> "textures/" + path + ".png");
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            return NONE;
        }
        try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
            int width = contents.width();
            int height = contents.height();
            if (image.getWidth() < width || image.getHeight() < height) {
                return NONE;
            }
            int[] alpha = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // First animation frame; the pixel format is ABGR, alpha in the top byte.
                    alpha[y * width + x] = (image.getPixelRGBA(x, y) >>> 24) & 0xFF;
                }
            }
            return alpha;
        } catch (Exception e) {
            return NONE;
        }
    }
}
