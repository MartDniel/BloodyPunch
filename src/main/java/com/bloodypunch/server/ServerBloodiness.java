package com.bloodypunch.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.bloodypunch.network.BloodinessSyncPayload;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Authoritative per-player bloodiness on the server (0..1), keyed by UUID so it
 * survives the entity being recreated on respawn. Changes are pushed to every
 * client tracking the player (and the player itself) via {@link BloodinessSyncPayload}.
 */
public final class ServerBloodiness {

    private static final Map<UUID, Float> VALUES = new HashMap<>();

    public static float get(UUID id) {
        return VALUES.getOrDefault(id, 0.0F);
    }

    /** Adds to the player's bloodiness (clamped) and syncs immediately. */
    public static void add(ServerPlayer player, float amount) {
        setAndSync(player, get(player.getUUID()) + amount);
    }

    /** Sets the value without sending a packet (used for per-tick decay). */
    public static void setQuiet(UUID id, float value) {
        VALUES.put(id, clamp(value));
    }

    /** Sets the value and pushes it to tracking clients. */
    public static void setAndSync(ServerPlayer player, float value) {
        VALUES.put(player.getUUID(), clamp(value));
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player, new BloodinessSyncPayload(player.getId(), get(player.getUUID())));
    }

    public static void remove(UUID id) {
        VALUES.remove(id);
    }

    private static float clamp(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    private ServerBloodiness() {}
}
