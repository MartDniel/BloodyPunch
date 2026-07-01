package com.bloodypunch.client;

import com.bloodypunch.BloodyPunch;
import com.bloodypunch.network.ClientBloodinessStore;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Phase 11: third-person blood on the player model's arms, so OTHER players see how
 * bloody you are. Reads the same server-synced value as the first-person arm.
 *
 * The model is already animated/posed when a layer runs, so we just re-render the
 * arm + sleeve parts with the blood texture.
 */
public class BloodyPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation BLOOD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BloodyPunch.MODID, "textures/entity/bloody_arm.png");

    public BloodyPlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
            AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick,
            float ageInTicks, float netHeadYaw, float headPitch) {
        float bloodiness = ClientBloodinessStore.get(player.getId());
        if (bloodiness <= 0.0F || player.isInvisible()) {
            return;
        }

        float intensity = Math.min(1.0F, 0.3F + bloodiness * 1.2F);
        int alpha = (int) (intensity * 255.0F);
        int color = (alpha << 24) | 0xFFFFFF;

        PlayerModel<AbstractClientPlayer> model = getParentModel();
        var consumer = bufferSource.getBuffer(RenderType.entityTranslucent(BLOOD_TEXTURE));
        // Only the punching (main) arm, to match the first-person view.
        if (player.getMainArm() == HumanoidArm.RIGHT) {
            model.rightArm.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color);
            model.rightSleeve.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color);
        } else {
            model.leftArm.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color);
            model.leftSleeve.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color);
        }
    }
}
