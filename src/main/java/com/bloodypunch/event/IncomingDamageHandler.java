package com.bloodypunch.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.bloodypunch.BloodyPunch;
import com.bloodypunch.damage.ModDamageTypes;
import com.bloodypunch.registry.ModEffects;
import com.bloodypunch.tags.ModTags;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Phase 6: taking consecutive cutting/piercing damage (mob bites, sweet berry
 * bushes, cactus, dripstone, ...) makes the player bleed.
 *
 * This is a SEPARATE streak from the bare-fist PunchHandler (different wound type),
 * and it does NOT add extra direct damage — the source already dealt its hit, we
 * only stack the Bleeding effect on top:
 *   hit 1-2 : build-up only
 *   hit 3-4 : Bleeding I
 *   hit 5-6 : Bleeding II
 *   hit 7-8 : Bleeding III  (amplifier = (count - 3) / 2)
 * Streak resets after ~2 seconds without a qualifying hit.
 *
 * We listen on LivingDamageEvent.Post (after damage is applied; not cancelable).
 */
public class IncomingDamageHandler {

    private static final long RESET_TICKS = 40L;
    private static final int BLEED_DURATION_TICKS = 120;

    private static final Map<UUID, BleedStreak> STATE = new HashMap<>();

    private static final class BleedStreak {
        int count;
        long lastGameTick;
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }

        DamageSource source = event.getSource();

        // HARD anti-loop guard: our own bleeding damage must never feed this
        // counter, regardless of what the data-driven tag contains.
        if (source.is(ModDamageTypes.BLEEDING)) {
            return;
        }

        // Only cutting/piercing sources count (data-driven tag).
        if (!source.is(ModTags.CAUSES_BLEEDING)) {
            return;
        }

        long now = level.getGameTime();
        BleedStreak data = STATE.get(player.getUUID());
        if (data == null || now - data.lastGameTick > RESET_TICKS) {
            data = new BleedStreak();
            STATE.put(player.getUUID(), data);
            data.count = 1;
        } else {
            data.count++;
        }
        data.lastGameTick = now;

        if (data.count >= 3) {
            // hit 3-4 -> Bleeding I, 5-6 -> II, 7-8 -> III, ...
            int amplifier = (data.count - 3) / 2;
            player.addEffect(new MobEffectInstance(ModEffects.BLEEDING, BLEED_DURATION_TICKS, amplifier));
            BloodyPunch.LOGGER.info("[Bleed-on-hit] count={} -> Bleeding amp {}", data.count, amplifier);
        } else {
            BloodyPunch.LOGGER.info("[Bleed-on-hit] count={} (building up)", data.count);
        }
    }
}
