package com.bloodypunch.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side store of every visible player's synced bloodiness, keyed by entity id.
 *
 * Deliberately free of client-only (Minecraft) imports so it is safe to reference
 * from side-agnostic networking code; it is only ever populated on the client.
 */
public final class ClientBloodinessStore {

    private static final Map<Integer, Float> VALUES = new ConcurrentHashMap<>();

    public static void set(int entityId, float value) {
        if (value <= 0.0F) {
            VALUES.remove(entityId);
        } else {
            VALUES.put(entityId, value);
        }
    }

    public static float get(int entityId) {
        return VALUES.getOrDefault(entityId, 0.0F);
    }

    public static void clear() {
        VALUES.clear();
    }

    private ClientBloodinessStore() {}
}
