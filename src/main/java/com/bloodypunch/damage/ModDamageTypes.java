package com.bloodypunch.damage;

import com.bloodypunch.BloodyPunch;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * Reference to our custom damage type. DamageTypes are purely data-driven: the
 * actual definition lives in data/bloodypunch/damage_type/bleeding.json. This key
 * just lets code resolve that JSON at runtime via
 * {@code entity.damageSources().source(ModDamageTypes.BLEEDING)}.
 *
 * Using a dedicated type (instead of vanilla magic()) gives a proper death
 * message ("... bled to death") instead of "... was killed by magic".
 */
public class ModDamageTypes {

    public static final ResourceKey<DamageType> BLEEDING = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(BloodyPunch.MODID, "bleeding"));

    private ModDamageTypes() {}
}
