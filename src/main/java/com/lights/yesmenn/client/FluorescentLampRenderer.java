package com.lights.yesmenn.client;

import com.lights.yesmenn.LightsYesMenn;
import com.lights.yesmenn.block.ColoredLightBlock;
import com.lights.yesmenn.block.FluorescentLampBlock;
import com.lights.yesmenn.block.HangingFluorescentLampBlock;
import com.lights.yesmenn.block.entity.ColoredLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;


/**
 * Draws the fluorescent lamp. It is rendered here instead of as a static block model so that it can lie in any
 * direction on any face (block states can only turn a model around x and y) and so that the tubes can blink while
 * the lamp starts. Draws the off model, or the on model while the tube glows. While lit, the faces with the tube
 * texture (core) and the glow texture (shells around the tubes) are drawn additively, fullbright and in the light
 * color; faces with semi-transparent texture pixels (glass) are blended over them.
 */
public final class FluorescentLampRenderer {
    public static final ModelResourceLocation OFF_MODEL = model("fluorescent_lamp");
    public static final ModelResourceLocation ON_MODEL = model("fluorescent_lamp_on");
    /** The hanging lamp has a single model; its tube and glow faces are found by texture as for the other one. */
    public static final ModelResourceLocation HANGING_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(LightsYesMenn.MODID, "block/fluorescent_lamp_hanging/fluorescent_lamp_hanging"),
            ModelResourceLocation.STANDALONE_VARIANT);
    /** The hanging model's own chain pieces; the lamp's chain part blocks draw the chains instead. */
    private static final ResourceLocation CHAIN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(LightsYesMenn.MODID, "block/chain");
    private static final ResourceLocation TUBE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(LightsYesMenn.MODID, "block/fluorescent_lamp/lamp_tube");
    /** Faces with this texture are the glow shells around the tubes, drawn additively while lit. */
    private static final ResourceLocation GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(LightsYesMenn.MODID, "block/fluorescent_lamp/lamp_glow");
    /** Strength (alpha) of the additive tube core and of the glow shells around it. */
    private static final float CORE_STRENGTH = 0.85F;
    private static final float SHELL_STRENGTH = 0.35F;
    private static final Direction[] SIDES = {
            null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private FluorescentLampRenderer() {
    }

    static void render(ColoredLightBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState state = blockEntity.getBlockState();
        boolean hanging = state.getBlock() instanceof HangingFluorescentLampBlock;
        BakedModel off = minecraft.getModelManager().getModel(hanging ? HANGING_MODEL : OFF_MODEL);
        BakedModel on = hanging ? off : minecraft.getModelManager().getModel(ON_MODEL);
        BakedModel missing = minecraft.getModelManager().getMissingModel();
        if (off == missing || on == missing) {
            return;
        }
        Direction facing = state.getValue(FluorescentLampBlock.FACING);
        Direction.Axis axis = state.getValue(FluorescentLampBlock.AXIS);
        boolean glowing = ColoredLightBlock.isLightEnabled(state)
                && VeilColoredLightManager.fluorescentStartLevel(blockEntity) > 0.5F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        // The hanging model already hangs and is long along z; it only turns for the x axis.
        poseStack.mulPose(hanging
                ? new Quaternionf().rotationY(axis == Direction.Axis.X ? (float) (Math.PI / 2.0) : 0.0F)
                : orientation(facing, axis));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        PoseStack.Pose pose = poseStack.last();
        ClientLevel level = minecraft.level;
        float[] tint = glowing ? rgb(blockEntity.getColor()) : null;
        List<BakedQuad> cutout = new ArrayList<>();
        List<BakedQuad> glass = new ArrayList<>();
        List<BakedQuad> cores = new ArrayList<>();
        List<BakedQuad> shells = new ArrayList<>();
        // The glow shells may sit in either model; they are only drawn while the tube glows.
        for (BakedQuad quad : quads(glowing ? on : off)) {
            if (hanging && hasTexture(quad, CHAIN_TEXTURE)) {
                continue;
            }
            if (hasTexture(quad, GLOW_TEXTURE)) {
                shells.add(quad);
            } else if (glowing && hasTexture(quad, TUBE_TEXTURE)) {
                cores.add(quad);
            } else if (TextureAlpha.isTranslucent(quad)) {
                glass.add(quad);
            } else {
                cutout.add(quad);
            }
        }
        if (glowing && shells.isEmpty()) {
            for (BakedQuad quad : quads(off)) {
                if (hasTexture(quad, GLOW_TEXTURE)) {
                    shells.add(quad);
                }
            }
        }

        // Housing first, then the light added on top of it, then the glass over everything (see LampRenderTypes).
        VertexConsumer consumer = bufferSource.getBuffer(LampRenderTypes.CUTOUT);
        for (BakedQuad quad : cutout) {
            putShaded(consumer, pose, quad, level, packedLight, packedOverlay);
        }
        if (tint != null) {
            consumer = bufferSource.getBuffer(LampRenderTypes.ADDITIVE_GLOW);
            // Additive and without culling, the front and back of the tube add up like a glowing 3D body.
            for (BakedQuad quad : cores) {
                consumer.putBulkData(pose, quad, tint[0], tint[1], tint[2], CORE_STRENGTH,
                        LightTexture.FULL_BRIGHT, packedOverlay);
            }
            for (BakedQuad quad : shells) {
                consumer.putBulkData(pose, quad, tint[0], tint[1], tint[2], SHELL_STRENGTH,
                        LightTexture.FULL_BRIGHT, packedOverlay);
            }
        }
        if (!glass.isEmpty()) {
            consumer = bufferSource.getBuffer(LampRenderTypes.GLASS);
            for (BakedQuad quad : glass) {
                putShaded(consumer, pose, quad, level, packedLight, packedOverlay);
            }
        }
        poseStack.popPose();
    }

    /**
     * Turns the floor-standing model (long along z) so that it sits on {@code facing} with its long side along
     * {@code axis}: first around its own up axis, then onto the face.
     */
    private static Quaternionf orientation(Direction facing, Direction.Axis axis) {
        Quaternionf mount = switch (facing) {
            case UP -> new Quaternionf();
            case DOWN -> new Quaternionf().rotationX((float) Math.PI);
            case NORTH -> new Quaternionf().rotationX((float) (-Math.PI / 2.0));
            case SOUTH -> new Quaternionf().rotationX((float) (Math.PI / 2.0));
            case WEST -> new Quaternionf().rotationZ((float) (Math.PI / 2.0));
            case EAST -> new Quaternionf().rotationZ((float) (-Math.PI / 2.0));
        };
        Quaternionf straight = new Quaternionf(mount);
        Vector3f longSide = straight.transform(new Vector3f(0.0F, 0.0F, 1.0F));
        if (Math.abs(component(longSide, axis)) > 0.5F) {
            return straight;
        }
        return mount.mul(new Quaternionf().rotationY((float) (Math.PI / 2.0)));
    }

    private static float component(Vector3f vector, Direction.Axis axis) {
        return switch (axis) {
            case X -> vector.x();
            case Y -> vector.y();
            case Z -> vector.z();
        };
    }

    private static List<BakedQuad> quads(BakedModel model) {
        List<BakedQuad> quads = new ArrayList<>();
        RandomSource random = RandomSource.create(42L);
        for (Direction side : SIDES) {
            random.setSeed(42L);
            quads.addAll(model.getQuads(null, side, random));
        }
        return quads;
    }

    private static void putShaded(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, ClientLevel level,
                                  int packedLight, int packedOverlay) {
        float shade = level != null
                ? level.getShade(Direction.rotate(pose.pose(), quad.getDirection()), quad.isShade())
                : 1.0F;
        consumer.putBulkData(pose, quad, shade, shade, shade, 1.0F, packedLight, packedOverlay);
    }

    /** Parts are recognised by their texture, whatever the model's element layout. */
    private static boolean hasTexture(BakedQuad quad, ResourceLocation texture) {
        return texture.equals(quad.getSprite().contents().name());
    }

    private static float[] rgb(int color) {
        return new float[]{((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F};
    }

    private static ModelResourceLocation model(String name) {
        return new ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath(LightsYesMenn.MODID, "block/fluorescent_lamp/" + name),
                ModelResourceLocation.STANDALONE_VARIANT);
    }
}
