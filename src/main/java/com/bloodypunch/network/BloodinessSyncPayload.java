package com.bloodypunch.network;

import com.bloodypunch.BloodyPunch;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client packet carrying a player's current bloodiness, so every client
 * that can see that player knows how bloody to render them.
 *
 * @param entityId the player entity's network id
 * @param value    bloodiness 0..1
 */
public record BloodinessSyncPayload(int entityId, float value) implements CustomPacketPayload {

    public static final Type<BloodinessSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BloodyPunch.MODID, "bloodiness_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BloodinessSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BloodinessSyncPayload::entityId,
                    ByteBufCodecs.FLOAT, BloodinessSyncPayload::value,
                    (id, val) -> new BloodinessSyncPayload(id, val));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
