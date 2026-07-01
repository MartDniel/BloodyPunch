package com.bloodypunch.client;

import com.bloodypunch.BloodyPunch;
import com.bloodypunch.network.ClientBloodinessStore;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * Phase 8b: draws blood over the first-person arm, intensity = ClientBloodiness.
 *
 * IMPORTANT: RenderArmEvent is fired from INSIDE PlayerRenderer.renderRightHand
 * (via ClientHooks.renderSpecificFirstPersonArm). So calling renderRightHand from
 * this handler re-fires the event -> infinite recursion. We guard against that with
 * a reentrancy flag: on the nested call we do nothing and let vanilla draw the
 * normal arm, then we draw blood on top and cancel the OUTER event so vanilla
 * doesn't draw the arm a second time.
 */
public class BloodyArmRenderer {

    private static final ResourceLocation BLOOD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BloodyPunch.MODID, "textures/entity/bloody_arm.png");

    // True while our own renderRightHand/LeftHand call is running.
    private static boolean renderingArm = false;

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        // Nested call from our own renderRightHand below: let vanilla draw normally.
        if (renderingArm) {
            return;
        }

        AbstractClientPlayer player = event.getPlayer();
        float bloodiness = ClientBloodinessStore.get(player.getId());
        if (bloodiness <= 0.0F) {
            return; // clean -> let vanilla render the arm normally
        }

        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int light = event.getPackedLight();
        HumanoidArm arm = event.getArm();

        // 1. Draw the normal arm via vanilla. The reentrancy flag makes the event
        //    that this re-fires a no-op, so vanilla's renderHand actually runs.
        renderingArm = true;
        try {
            if (arm == HumanoidArm.RIGHT) {
                renderer.renderRightHand(pose, buffer, light, player);
            } else {
                renderer.renderLeftHand(pose, buffer, light, player);
            }
        } finally {
            renderingArm = false;
        }

        // 2. Blood layer on the same (now-posed) arm + sleeve parts.
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        ModelPart armPart = (arm == HumanoidArm.RIGHT) ? model.rightArm : model.leftArm;
        ModelPart sleevePart = (arm == HumanoidArm.RIGHT) ? model.rightSleeve : model.leftSleeve;

        // Exaggerated curve: a visible floor (0.3) so even one punch shows, ramping
        // fast to fully soaked. Tune these two numbers to taste.
        float intensity = Math.min(1.0F, 0.3F + bloodiness * 1.2F);
        int alpha = (int) (intensity * 255.0F);
        int color = (alpha << 24) | 0xFFFFFF; // white tint, alpha = intensity
        var consumer = buffer.getBuffer(RenderType.entityTranslucent(BLOOD_TEXTURE));
        armPart.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, color);
        sleevePart.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, color);

        // We drew the arm ourselves; stop vanilla from drawing it again.
        event.setCanceled(true);
    }
}
