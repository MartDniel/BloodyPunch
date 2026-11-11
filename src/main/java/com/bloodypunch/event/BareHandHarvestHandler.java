package com.bloodypunch.event;

import com.bloodypunch.tags.ModTags;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Makes wood/planks require the CORRECT tool (an axe) just like stone requires a
 * pickaxe. Without an effective tool — bare hand OR any wrong item (dirt, sword,
 * pickaxe, ...) — the block CANNOT be broken at all (it stays intact). You can still
 * punch it (and bleed for it), you just can't harvest it without an axe.
 *
 * "Correct tool" is detected generally via {@code tool.getDestroySpeed(state) > 1}:
 * only a tool whose TOOL component is effective on the block (the axe, including
 * modded axes) mines it faster than the default 1.0. This avoids hardcoding "axe"
 * and works for any block in the bloodypunch:bare_hand_resistant tag.
 *
 * Events: PlayerEvent.BreakSpeed (both sides — canceling makes break speed -1, i.e.
 * unbreakable) and BlockDropsEvent (server-only, clears drops as a safety net).
 */
public class BareHandHarvestHandler {

    private static boolean isEffectiveTool(ItemStack tool, BlockState state) {
        // 1.0 = the default for an item with no useful TOOL component (bare hand,
        // dirt, etc.); >1 means the tool is genuinely effective on this block.
        return tool.getDestroySpeed(state) > 1.0F;
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        BlockState state = event.getState();
        if (!state.is(ModTags.BARE_HAND_RESISTANT)) {
            return;
        }
        if (isEffectiveTool(event.getEntity().getMainHandItem(), state)) {
            return; // correct tool -> normal speed
        }
        // Wrong/no tool: cancel -> break speed becomes -1 -> the block is unbreakable.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player)) {
            return;
        }
        BlockState state = event.getState();
        if (!state.is(ModTags.BARE_HAND_RESISTANT)) {
            return;
        }
        if (isEffectiveTool(event.getTool(), state)) {
            return; // correct tool -> normal drops
        }
        event.getDrops().clear();
    }
}
