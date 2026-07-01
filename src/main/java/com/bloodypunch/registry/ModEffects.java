package com.bloodypunch.registry;

import com.bloodypunch.BloodyPunch;
import com.bloodypunch.effect.BleedingMobEffect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central place where all of BloodyPunch's MobEffects are registered.
 *
 * Using a dedicated DeferredRegister per registry type (effects, items, etc.) keeps
 * the main mod class clean and makes it obvious where each kind of content lives.
 */
public class ModEffects {

    // A DeferredRegister batches our registrations and fires them at the correct time
    // during mod loading, under the "bloodypunch" namespace.
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, BloodyPunch.MODID);

    // "bloodypunch:bleeding". HARMFUL -> shown in red in the inventory effect list.
    // 0x8B0000 (dark red) is the tint used for the effect's screen/particle color.
    public static final Holder<MobEffect> BLEEDING = MOB_EFFECTS.register("bleeding",
            () -> new BleedingMobEffect(MobEffectCategory.HARMFUL, 0x8B0000));

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
