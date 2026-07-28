# Magic Storage

A Terraria-inspired storage network mod for Minecraft 1.12.2 — an enhancement over Simple Storage Network with more features, more tiers, chunk loading, and quality-of-life improvements.

## Features

- **Storage Heart** — Central network core. Heart chunks stay loaded via ForgeChunkManager so your items are always accessible, even across dimensions.
- **8 Storage Unit Tiers** — Basic (40), Crimtane (80), Demonite (80), Hellstone (120), Hallowed (160), Blue Chlorophyte (220), Luminite (300), Terra (600). Upgradeable in-place.
- **Auto-Sort** — Network contents automatically sorted for fast retrieval.
- **Storage Access** — View, search, sort, and retrieve items. Supports text search, sort by name/mod/quantity, shift-click, and JEI integration.
- **Crafting Access (Station System)** — Stations are activated by placing block items in the Storage Heart's inventory:
  - Vanilla Crafting with auto-refill from network
  - Furnace Smelting — place fuel + smeltables, network pulls ingredients
  - Anvil — real vanilla anvil cost via reflection
  - Enchanting Table — deterministic simulation using player xpSeed, shows enchant hint and cost
  - Brewing Stand — brew up to 3 potions at once, full modded potion support
- **Portable Access** — Handheld remote. Upgradeable tiers.
- **Remote Access** — Cross-dimensional inventory access.
- **Chunk Loading** — Storage Heart force-loads all network chunks for reliable cross-dimension access.
- **JEI Integration** — Custom recipe categories for enchanting, anvil, smelting, and brewing.

## Grid Layouts

| Station | Slot 0 | Slot 1 | Slots 3-5 |
|---------|--------|--------|-----------|
| Enchanting | Item | Lapis | Lapis |
| Anvil | Item | — | Second item |
| Furnace | Fuel | Smeltable | — |
| Brewing | Blaze Powder | Ingredient | Bottles (1-3) |

## Building

```bash
./gradlew build
```

Output: `build/libs/MagicStorage-<version>.jar`

## Dependencies

- **Required:** Forge 1.12.2-14.23.5.2847+
- **Optional:** JEI 4.15+

## License

This project is licensed under CC BY-NC-SA 4.0.
