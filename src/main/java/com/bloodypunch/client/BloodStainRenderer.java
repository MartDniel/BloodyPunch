package com.bloodypunch.client;

import java.util.Iterator;
import java.util.Map;

import com.bloodypunch.BloodyPunch;
import com.bloodypunch.network.BloodStainStore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Phase 12b: draws each blood stain as a fading textured quad on its block face.
 *
 * Runs in RenderLevelStageEvent (AFTER_TRANSLUCENT_BLOCKS). For each stain we move
 * to the block's face in camera-relative space, rotate a unit XY quad (which faces
 * +Z) to align with the face normal, and draw it with alpha = remaining lifetime.
 * The render type is NO_CULL, so quad winding doesn't matter.
 */
public class BloodStainRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BloodyPunch.MODID, "textures/misc/blood_stain.png");
    private static final int MAX_ALPHA = 220;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        if (pose == null || BloodStainStore.stains().isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityTranslucent(TEXTURE);
        VertexConsumer consumer = buffer.getBuffer(renderType);

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<BloodStainStore.StainKey, Long>> it = BloodStainStore.stains().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BloodStainStore.StainKey, Long> entry = it.next();
            long age = now - entry.getValue();
            if (age >= BloodStainStore.DURATION_MS) {
                it.remove();
                continue;
            }
            // If the stained block is gone (broken), drop the stain so it doesn't
            // float in mid-air.
            if (mc.level.getBlockState(entry.getKey().pos()).isAir()) {
                it.remove();
                continue;
            }
            float fade = 1.0F - (float) age / BloodStainStore.DURATION_MS;
            int alpha = (int) (fade * MAX_ALPHA);
            if (alpha <= 0) {
                continue;
            }
            renderStain(pose, consumer, cam, entry.getKey().pos(), entry.getKey().face(), alpha, mc);
        }

        buffer.endBatch(renderType);
    }

    private static void renderStain(PoseStack pose, VertexConsumer consumer, Vec3 cam,
            BlockPos pos, Direction face, int alpha, Minecraft mc) {
        int light = LevelRenderer.getLightColor(mc.level, pos.relative(face));

        pose.pushPose();
        // Camera-relative position of the block center.
        pose.translate(pos.getX() + 0.5 - cam.x, pos.getY() + 0.5 - cam.y, pos.getZ() + 0.5 - cam.z);
        // Out to the struck face surface (slightly proud to avoid z-fighting).
        pose.translate(face.getStepX() * 0.51, face.getStepY() * 0.51, face.getStepZ() * 0.51);
        // Rotate the +Z-facing quad to align with this face's outward normal.
        switch (face) {
            case SOUTH -> { /* already +Z */ }
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(-90));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(-90));
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(90));
        }

        PoseStack.Pose last = pose.last();
        vertex(consumer, last, -0.5F, -0.5F, 0.0F, 1.0F, alpha, light);
        vertex(consumer, last, 0.5F, -0.5F, 1.0F, 1.0F, alpha, light);
        vertex(consumer, last, 0.5F, 0.5F, 1.0F, 0.0F, alpha, light);
        vertex(consumer, last, -0.5F, 0.5F, 0.0F, 0.0F, alpha, light);

        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
            float x, float y, float u, float v, int alpha, int light) {
        consumer.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }
}
