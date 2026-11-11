package com.bloodypunch;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * BloodyPunch config (config/bloodypunch/bloodypunch-common.toml).
 *
 * Holds the list of "flimsy" items that still count as a bare fist for the bleeding
 * mechanic when held. We resolve the string IDs into a Set of ResourceLocations on
 * (re)load and compare by registry key at runtime, so modded item IDs work even
 * though they register after the config is parsed.
 */
public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> FLIMSY_ITEMS = BUILDER
            .comment("Items that, when held, still count as a bare fist for bleeding",
                     "(too flimsy to protect your hand). Use item IDs like",
                     "\"minecraft:paper\" or \"somemod:some_item\" for modded items.")
            .defineListAllowEmpty("flimsyItems",
                    List.of("minecraft:paper", "minecraft:feather", "minecraft:wheat_seeds",
                            "minecraft:beetroot_seeds", "minecraft:melon_seeds", "minecraft:pumpkin_seeds"),
                    () -> "minecraft:paper",
                    Config::validateItemId);

    static final ModConfigSpec SPEC = BUILDER.build();

    /** Resolved set of flimsy item ids, rebuilt whenever the config loads/reloads. */
    private static volatile Set<ResourceLocation> flimsyIds = Set.of();

    public static boolean isFlimsy(ItemStack stack) {
        return !stack.isEmpty()
                && flimsyIds.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static void onLoad(final ModConfigEvent.Loading event) {
        rebuild();
    }

    public static void onReload(final ModConfigEvent.Reloading event) {
        rebuild();
    }

    private static void rebuild() {
        flimsyIds = FLIMSY_ITEMS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(rl -> rl != null)
                .collect(Collectors.toSet());
    }

    /** Lenient: accept any parseable id so modded items aren't stripped before they register. */
    private static boolean validateItemId(final Object obj) {
        return obj instanceof String s && ResourceLocation.tryParse(s) != null;
    }
}
