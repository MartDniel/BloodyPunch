package com.bloodypunch.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

import com.bloodypunch.BloodyPunch;
import com.bloodypunch.Config;
import com.bloodypunch.damage.ModDamageTypes;
import com.bloodypunch.network.BloodStainPayload;
import com.bloodypunch.registry.ModEffects;
import com.bloodypunch.server.ServerBloodiness;
import com.bloodypunch.tags.ModTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Bare-fist punch punishment. Registered manually on NeoForge.EVENT_BUS (GAME bus).
 *
 * A "punch" is one discrete left-click press (Action.START) — confirmed in Phase 3
 * that the server never sees a per-tick mining event, only START/STOP/ABORT.
 *
 * Escalation (consecutive hard-block punches with an empty hand):
 *   punch 1-2 : ½ heart of direct, armor-bypassing damage each
 *   punch 3   : Bleeding I
 *   punch 4-5 : Bleeding II
 *   punch 6-7 : Bleeding III
 *   ...       : keeps scaling (amplifier = (count - 2) / 2)
 * The streak resets after ~2 seconds without a qualifying punch.
 *
 * Phase 5: the SAME shared streak also counts bare-fist hits on living entities
 * (mobs/players). We do NOT cancel the hit — the mob takes normal fist damage; the
 * attacker simply bleeds out, which is what makes bare-fist killing impossible.
 */
public class PunchHandler {

    // --- Tunable balance values (candidates to move into Config later) ---
    /** Streak resets after this many ticks (20 ticks = 1s) without a qualifying punch. */
    private static final long RESET_TICKS = 40L;
    /** Direct damage for punches 1-2 (½ heart = 1.0 HP). */
    private static final float DIRECT_DAMAGE = 1.0F;
    /** How long each bleeding application lasts; refreshed on every qualifying punch. */
    private static final int BLEED_DURATION_TICKS = 120;

    /** Deep blood-red dust particle spawned from the hand on each punch. */
    private static final DustParticleOptions BLOOD_PARTICLE =
            new DustParticleOptions(new Vector3f(0.55F, 0.0F, 0.0F), 1.2F);

    /** Hand bloodiness added per qualifying punch (server-authoritative, synced). */
    private static final float BLOODINESS_PER_PUNCH = 0.15F;

    // Per-player streak state. Events fire on the server thread only, so a plain
    // HashMap is safe (no concurrent access).
    private static final Map<UUID, PunchData> STATE = new HashMap<>();

    private static final class PunchData {
        int count;
        long lastGameTick;
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }

        Player player = event.getEntity();
        if (!countsAsBareHand(player.getMainHandItem())) {
            return;
        }

        // Hard-block filter: only blocks in our data-driven tag count. The tag
        // (data/bloodypunch/tags/block/hard_blocks.json) is based on block CATEGORY
        // (mineable/pickaxe + logs + bedrock), not hardness — hardness can't tell
        // netherrack (should bleed) from gravel (shouldn't), since both are soft.
        BlockState state = level.getBlockState(event.getPos());
        if (!state.is(ModTags.HARD_BLOCKS)) {
            return;
        }

        // Impact point: just OUTSIDE the struck face (0.6, so particles aren't buried
        // in the block surface).
        Direction face = event.getFace();
        Vec3 impact = Vec3.atCenterOf(event.getPos())
                .add(face.getStepX() * 0.6, face.getStepY() * 0.6, face.getStepZ() * 0.6);
        registerPunch(player, level.getGameTime(), impact);

        if (level instanceof ServerLevel serverLevel) {
            sendBloodStain(serverLevel, event.getPos(), face);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }

        // Bare fist (or a flimsy item) only.
        if (!countsAsBareHand(player.getMainHandItem())) {
            return;
        }

        // Only living targets (mobs/players). Punching boats, item frames, armor
        // stands, etc. should not trigger bleeding — the design says "mobs/jugadores".
        if (!(event.getTarget() instanceof LivingEntity)) {
            return;
        }

        // Impact point: the center of the entity we hit.
        Vec3 impact = event.getTarget().getBoundingBox().getCenter();
        registerPunch(player, level.getGameTime(), impact);
    }

    /**
     * Advances the player's shared consecutive-punch streak and applies the punishment.
     * Package-private so MiningHandler can feed held-mining into the SAME streak.
     */
    static void registerPunch(Player player, long now, Vec3 impactPos) {
        PunchData data = STATE.get(player.getUUID());
        if (data == null || now - data.lastGameTick > RESET_TICKS) {
            data = new PunchData();
            STATE.put(player.getUUID(), data);
            data.count = 1;
        } else {
            data.count++;
        }
        data.lastGameTick = now;

        applyPunishment(player, data.count);
        spawnBloodParticles(player, data.count, impactPos);

        // Server-authoritative hand bloodiness (synced to clients for rendering).
        if (player instanceof ServerPlayer serverPlayer) {
            ServerBloodiness.add(serverPlayer, BLOODINESS_PER_PUNCH);
        }
    }

    /**
     * Whether this held item counts as a bare fist for the bleeding mechanic: either
     * nothing, or an item the config flags as too flimsy to protect the hand.
     */
    static boolean countsAsBareHand(ItemStack stack) {
        return stack.isEmpty() || Config.isFlimsy(stack);
    }

    /** Tells nearby clients to spawn a persistent, fading blood stain on a block face. */
    static void sendBloodStain(ServerLevel level, BlockPos pos, Direction face) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos),
                new BloodStainPayload(pos, face.get3DDataValue()));
    }

    /**
     * Sprays blood-red particles from the player's hand AND splatters some at the
     * impact point (block face / entity). Server-broadcast, so every nearby client
     * sees them — multiplayer-visible for free.
     */
    private static void spawnBloodParticles(Player player, int count, Vec3 impactPos) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        // From the hand: just in front of and below the eyes.
        Vec3 look = player.getLookAngle();
        Vec3 hand = player.getEyePosition().add(look.scale(0.6)).add(0.0, -0.2, 0.0);
        int handAmount = Math.min(2 + count, 12);
        server.sendParticles(BLOOD_PARTICLE, hand.x, hand.y, hand.z,
                handAmount, 0.1, 0.1, 0.1, 0.02);

        // Splatter on the spot we hit. More particles, low speed so they read as a splat.
        int impactAmount = Math.min(8 + count * 2, 24);
        server.sendParticles(BLOOD_PARTICLE, impactPos.x, impactPos.y, impactPos.z,
                impactAmount, 0.2, 0.2, 0.2, 0.0);
    }

    private static void applyPunishment(Player player, int count) {
        if (count <= 2) {
            // Punches 1-2: direct, armor-bypassing impact damage (same bleeding type).
            player.hurt(player.damageSources().source(ModDamageTypes.BLEEDING), DIRECT_DAMAGE);
            BloodyPunch.LOGGER.info("[Punch] count={} -> direct {} dmg", count, DIRECT_DAMAGE);
        } else {
            // Punch 3 -> Bleeding I, 4/5 -> II, 6/7 -> III, ...
            int amplifier = (count - 2) / 2;
            player.addEffect(new MobEffectInstance(ModEffects.BLEEDING, BLEED_DURATION_TICKS, amplifier));
            BloodyPunch.LOGGER.info("[Punch] count={} -> Bleeding amp {}", count, amplifier);
        }
    }
}
