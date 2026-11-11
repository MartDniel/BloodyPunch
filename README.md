# BloodyPunch

A brutal survival mechanic for hardcore and expert modpacks. Your bare hands are not tools — and the world will remind you.

## What it does

Punching hard materials (stone, ore, wood) or mobs with an empty hand inflicts **Hemorrhage** — a custom, escalating bleed that ignores armor and can kill you. The more you punch, the worse it gets.

## Features

- **Escalating bleeding** — consecutive bare-fisted hits ramp up: direct damage → Bleeding I → II → III and beyond. The streak resets after a couple of seconds without a hit.
- **Bleed from taking damage too** — consecutive cutting/piercing hits (mob attacks, sweet berry bushes, cactus, dripstone…) also make you bleed.
- **Armor won't save you** — Hemorrhage bypasses armor. No tanking your way through it.
- **Tool-gated harvesting** — hard blocks can't be broken at all without the correct tool (an axe for wood, a pickaxe for stone). Try without one and you just bleed for nothing.
- **Held mining counts** — holding to mine a hard block bare-handed bleeds you too.
- **Visual feedback** — blood sprays from your hand and the point of impact, your arm gets progressively bloodier (visible to other players in multiplayer), and struck blocks get a fading blood stain.
- **Configurable** — mark "flimsy" items (paper, seeds…) that still count as a bare fist, editable in the config (supports modded items).

## ⚠️ Important — read before installing

This mod gates tool progression hard: without an axe or pickaxe you cannot harvest wood or stone. On its own, this can soft-lock a fresh world. It's meant to be paired with an alternative early-game tool source — for example Pedernal Tools (flint tools obtained from gravel). Install a tool-progression mod alongside it, or play it in a pack built around it.

## Requirements

- NeoForge 1.21.1
- Required on both client and server.

Part of my upcoming **Industry Nightmare** mod pack.

## Development

This project is built with the NeoForge MDK.

- If you're missing libraries in your IDE or run into issues, run `gradlew --refresh-dependencies` to refresh the local cache, or `gradlew clean` to reset the build (this does not affect your code).
- Mappings use the official Mojang names, which are covered by a specific license — see the reference copy [here](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md).
- Community docs: https://docs.neoforged.net/
- NeoForged Discord: https://discord.neoforged.net/

## License

MIT — see [LICENSE](LICENSE).
