package com.bloodypunch.tags;

import com.bloodypunch.BloodyPunch;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.Block;

/**
 * Custom block tags for BloodyPunch.
 *
 * A TagKey is just a typed name; its actual contents live in a JSON resource
 * (data/bloodypunch/tags/block/hard_blocks.json) that the datapack system loads.
 * That means the block list can be edited without recompiling the mod.
 */
public class ModTags {

    /** Blocks that cause Bleeding when punched with an empty hand. */
    public static final TagKey<Block> HARD_BLOCKS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(BloodyPunch.MODID, "hard_blocks"));

    /**
     * Blocks that require the CORRECT tool (e.g. an axe for wood) to mine fast and
     * drop anything. Without an effective tool — bare hand or any wrong item — they
     * mine very slowly and drop nothing, exactly like stone without a pickaxe.
     */
    public static final TagKey<Block> BARE_HAND_RESISTANT = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(BloodyPunch.MODID, "bare_hand_resistant"));

    /**
     * Damage types (cutting/piercing) that build the "received damage" bleeding
     * streak. Our own bloodypunch:bleeding is intentionally NOT listed here, which
     * is what makes the feedback loop impossible.
     */
    public static final TagKey<DamageType> CAUSES_BLEEDING = TagKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(BloodyPunch.MODID, "causes_bleeding"));

    private ModTags() {}
}
