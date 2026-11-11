package com.bloodypunch.effect;

import com.bloodypunch.damage.ModDamageTypes;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * The custom "Bleeding" (Hemorragia) status effect.
 *
 * Phase 2: damage logic that scales with the effect amplifier.
 *   - Bleeding I  (amplifier 0): ½ heart/second
 *   - Bleeding II (amplifier 1): 1 heart/second
 *   - Bleeding III(amplifier 2): 1½ hearts/second  (keeps scaling linearly)
 *
 * Design choices:
 *   - Fixed cadence of once per second; we scale the *amount* of damage, not the
 *     frequency (vanilla Poison/Wither do the opposite). Hence the simple
 *     {@code duration % TICK_INTERVAL == 0} gate.
 *   - We use our custom {@code bloodypunch:bleeding} damage type, which is in the
 *     {@code bypasses_armor} tag: you cannot armor your way out of internal
 *     bleeding, and dying to it reads "... bled to death".
 *   - No health floor (unlike Poison, which never kills): bleeding CAN kill the
 *     player. This is an extreme-survival mechanic.
 */
public class BleedingMobEffect extends MobEffect {

    // 20 game ticks = 1 second. We deal damage once per second.
    private static final int TICK_INTERVAL = 20;
    // ½ heart, in health points (1 heart = 2.0F HP).
    private static final float HALF_HEART = 1.0F;

    public BleedingMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        // amplifier is 0-based: Bleeding I -> 0, Bleeding II -> 1, ...
        float damage = (amplifier + 1) * HALF_HEART;
        livingEntity.hurt(livingEntity.damageSources().source(ModDamageTypes.BLEEDING), damage);
        // true keeps the effect active for its remaining duration.
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // duration counts DOWN each game tick; fire once every second.
        return duration % TICK_INTERVAL == 0;
    }
}
