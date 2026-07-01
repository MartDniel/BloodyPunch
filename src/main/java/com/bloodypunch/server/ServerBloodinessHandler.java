package com.bloodypunch.server;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Drives server-side bloodiness over time: decays it each tick, resets it on death,
 * and frees the map entry on logout. Punch increments live in PunchHandler.
 */
public class ServerBloodinessHandler {

    private static final float DECAY_PER_TICK = 0.0015F;
    /** Decay changes every tick but we only push a packet this often, to limit traffic. */
    private static final int SYNC_INTERVAL = 5;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        float current = ServerBloodiness.get(player.getUUID());

        // Death washes the hands clean.
        if (player.isDeadOrDying()) {
            if (current > 0.0F) {
                ServerBloodiness.setAndSync(player, 0.0F);
            }
            return;
        }

        if (current <= 0.0F) {
            return;
        }

        float decayed = Math.max(0.0F, current - DECAY_PER_TICK);
        ServerBloodiness.setQuiet(player.getUUID(), decayed);

        // Sync periodically, plus once more when it just reached zero (to clear it).
        if (decayed <= 0.0F || player.tickCount % SYNC_INTERVAL == 0) {
            ServerBloodiness.sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerBloodiness.remove(event.getEntity().getUUID());
    }
}
