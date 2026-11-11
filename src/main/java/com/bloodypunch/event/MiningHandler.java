package com.bloodypunch.event;

import com.bloodypunch.tags.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Makes HOLDING left-click to mine a hard block escalate bleeding too, not just
 * discrete clicks.
 *
 * Why a tick handler: the server only ever sees LeftClickBlock START/STOP/ABORT,
 * never a per-tick "still mining" event. So instead we read the server's own mining
 * state each tick — ServerPlayerGameMode.isDestroyingBlock / destroyPos, exposed via
 * our Access Transformer — and count a punch every HOLD_PUNCH_INTERVAL_TICKS.
 *
 * It feeds the SAME shared streak as PunchHandler, so a held mine ramps Bleeding
 * I -> II -> III exactly like rapid clicking would.
 */
public class MiningHandler {

    /** While holding to mine a hard block bare-fisted, count one punch this often. */
    private static final int HOLD_PUNCH_INTERVAL_TICKS = 8;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Currently mining a block? (ground truth from the server game mode)
        if (!player.gameMode.isDestroyingBlock) {
            return;
        }

        // Bare fist (or a flimsy item) only.
        if (!PunchHandler.countsAsBareHand(player.getMainHandItem())) {
            return;
        }

        Level level = player.level();
        BlockPos pos = player.gameMode.destroyPos;
        if (!level.getBlockState(pos).is(ModTags.HARD_BLOCKS)) {
            return;
        }

        // Throttle: one held-punch every N ticks instead of every single tick.
        long now = level.getGameTime();
        if (now % HOLD_PUNCH_INTERVAL_TICKS != 0) {
            return;
        }

        // Impact just outside the block surface, on the side facing the player, so
        // the splatter isn't spawned buried inside the block.
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 toPlayer = player.getEyePosition().subtract(center);
        Vec3 impact = toPlayer.lengthSqr() > 1.0E-4 ? center.add(toPlayer.normalize().scale(0.6)) : center;
        PunchHandler.registerPunch(player, now, impact);

        // Stain the face that points toward the player.
        if (level instanceof ServerLevel serverLevel) {
            Direction face = Direction.getNearest(toPlayer.x, toPlayer.y, toPlayer.z);
            PunchHandler.sendBloodStain(serverLevel, pos, face);
        }
    }
}
