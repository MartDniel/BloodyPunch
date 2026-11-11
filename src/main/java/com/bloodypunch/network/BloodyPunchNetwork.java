package com.bloodypunch.network;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers BloodyPunch's network payloads. Called from the mod constructor on the
 * MOD event bus (RegisterPayloadHandlersEvent).
 */
public final class BloodyPunchNetwork {

    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                BloodinessSyncPayload.TYPE,
                BloodinessSyncPayload.CODEC,
                BloodyPunchNetwork::handleBloodiness);
        registrar.playToClient(
                BloodStainPayload.TYPE,
                BloodStainPayload.CODEC,
                BloodyPunchNetwork::handleStain);
    }

    private static void handleBloodiness(BloodinessSyncPayload payload, IPayloadContext ctx) {
        // Hop to the main client thread before touching the store.
        ctx.enqueueWork(() -> ClientBloodinessStore.set(payload.entityId(), payload.value()));
    }

    private static void handleStain(BloodStainPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                BloodStainStore.add(payload.pos(), Direction.from3DDataValue(payload.face())));
    }

    private BloodyPunchNetwork() {}
}
