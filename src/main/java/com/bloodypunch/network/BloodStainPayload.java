package com.bloodypunch.network;

import com.bloodypunch.BloodyPunch;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: a blood stain just appeared on a block face. Clients add it to
 * their local {@link BloodStainStore} and render+fade it; no server persistence.
 *
 * @param pos  the block that was hit
 * @param face the struck face, as Direction#get3DDataValue
 */
public record BloodStainPayload(BlockPos pos, int face) implements CustomPacketPayload {

    public static final Type<BloodStainPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BloodyPunch.MODID, "blood_stain"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BloodStainPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BloodStainPayload::pos,
                    ByteBufCodecs.VAR_INT, BloodStainPayload::face,
                    (p, f) -> new BloodStainPayload(p, f));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
