package com.bloodypunch.network;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Client-side set of active blood stains, keyed by (block, face) so repeatedly
 * hitting the same face REFRESHES one stain instead of stacking many coplanar quads.
 * Each fades over DURATION_MS; the map is capped (oldest dropped) so heavy mining
 * can't accumulate unbounded decals.
 *
 * Uses only net.minecraft.core types (server-safe), though it's only populated on
 * the client.
 */
public final class BloodStainStore {

    /** How long a stain takes to fully fade away. */
    public static final long DURATION_MS = 40_000L;
    private static final int MAX = 128;

    public record StainKey(BlockPos pos, Direction face) {}

    /** key -> start time (ms). LinkedHashMap keeps insertion order for capping. */
    private static final Map<StainKey, Long> STAINS = new LinkedHashMap<>();

    public static void add(BlockPos pos, Direction face) {
        STAINS.put(new StainKey(pos.immutable(), face), System.currentTimeMillis());
        if (STAINS.size() > MAX) {
            Iterator<StainKey> it = STAINS.keySet().iterator();
            it.next();
            it.remove();
        }
    }

    public static Map<StainKey, Long> stains() {
        return STAINS;
    }

    public static void clear() {
        STAINS.clear();
    }

    private BloodStainStore() {}
}
