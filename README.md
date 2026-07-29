# Magic Storage

A Terraria-inspired storage network mod for Minecraft 1.12.2. Centralize all your items into a single Storage Heart, access them from any Storage Access terminal, use the Crafting Access to craft with your entire network inventory.

## Quick Start

1. Craft a Storage Component (the base crafting material).
2. Craft a Storage Heart and place it.
3. Craft a Basic Storage Unit and place it adjacent to the Storage Heart.
4. Craft a Storage Access or Crafting Access and place it anywhere connected to the heart.
5. Open the access terminal. Items placed in storage units become accessible from every access point.
6. Feed your crafting stations into the Storage Heart's inventory (right-click the heart). Place brewing stands, furnaces, anvils, enchanting tables, and crafting tables there. The Crafting Access will detect them.

## Blocks & Items

### Storage Component (Base Material)
Iron ingots, chests, and redstone surround a chest center. Used in nearly every recipe.

### Storage Heart
The core of your network. It has a 20-slot inventory for placing crafting stations (brewing stand, furnace, anvil, enchanting table, crafting table). The heart chunk-loads itself so remote access works across dimensions. Break it to disconnect the network.

Recipe: Diamond, Redstone Block, Emerald, Storage Component

### Storage Units (8 Tiers)
Place adjacent to the Storage Heart. Each tier has more capacity:

| Tier | Slots | Upgrade From |
|------|-------|-------------|
| Basic | 40 | Craft directly |
| Crimtane | 80 | Basic + Crimtane Upgrade |
| Demonite | 80 | Basic + Demonite Upgrade |
| Hellstone | 120 | Crimtane or Demonite + Hellstone Upgrade |
| Hallowed | 160 | Hellstone + Hallowed Upgrade |
| Blue Chlorophyte | 220 | Hallowed + Blue Chlorophyte Upgrade |
| Luminite | 300 | Blue Chlorophyte + Luminite Upgrade |
| Terra | 600 | Luminite + Terra Upgrade |

Shift+right-click a storage unit to see its usage. Right-click to open and manage its contents. Use upgrades by holding them and shift+right-clicking the unit (with or without sneaking).

### Storage Access
Right-click to open a terminal. Browse all items in the network, search by name, sort by name/amount/mod. Click an item to pull it to your cursor. Shift+click to send it to your inventory. Insert items from your cursor into the network by clicking in the empty area.

Recipe: Iron Ingot, Diamond, Storage Component, Chest

### Crafting Access
A crafting table that uses your network inventory. Place a crafting table in the Storage Heart to enable the basic 3x3 crafting grid. Place additional crafting stations in the heart to unlock more features:

- **Crafting Table**: 3x3 vanilla crafting grid. Items auto-fill from the network.
- **Furnace**: Smelt items directly from the crafting grid. Place fuel in the grid too.
- **Enchanting Table**: Enchant items using the grid. Place the item in slot 1, lapis lazuli in slots 4/5/6 (any position). Bookshelf power is calculated from bookshelf items in your network storage.
- **Anvil**: Combine enchantments, repair items, and rename. Place the target item in slot 1, the material in slot 5. Uses real vanilla anvil logic including XP costs. Shows the final result before you commit.
- **Brewing Stand**: Brew potions. Place blaze powder in slot 1, the ingredient in slot 2, potion bottles in slots 4/5/6.

Recipe: Iron Ingot, Gold Ingot, Storage Component, Crafting Table

### Remote Access Block
Links portable access items to the Storage Heart. Place it adjacent to the heart. Portable items can access the network from anywhere (within range/dimension limits).

Recipe: Obsidian, Storage Component, Ender Pearl, Diamond

### Portable Access Items (3 Tiers)
Access your network from anywhere. Right-click to open. Storage variant (browse + insert/extract) and Crafting variant (with crafting grid).

| Tier | Range | Cross-Dimension |
|------|-------|----------------|
| PreHM (Basic) | 200 blocks | No |
| HM (Advanced) | Unlimited | No |
| Ultimate | Unlimited | Yes |

Crafting variants include the full crafting grid with station support.

### Hell Bricks
Decorative building block. Not part of the storage network.

Recipe: Netherbrick, Magma

## Upgrades

Shift+right-click a storage unit while holding an upgrade to apply it. The upgrade is consumed and the unit gains more slots.

| Upgrade | Upgrades From | Recipe Pattern |
|---------|--------------|----------------|
| Crimtane | Basic | Iron Ingot, Iron Block, Nether Wart Block, Redstone |
| Demonite | Basic | Iron Ingot, Iron Block, Purpur Block, Redstone |
| Hellstone | Crimtane or Demonite | Hell Brick, Gold Block, Blaze Powder, Crimtane/Demonite Upgrade |
| Hallowed | Hellstone | Emerald, Quartz, Hellstone Upgrade |
| Blue Chlorophyte | Hallowed | Lapis Block, Diamond, Hallowed Upgrade |
| Luminite | Blue Chlorophyte | Obsidian, End Stone, Ender Eye, Blue Chlorophyte Upgrade |
| Terra | Luminite | Dragon Breath, Nether Star, End Rod, Luminite Upgrade |

## JEI Integration

JEI shows recipe categories for each crafting station: Anvil, Brewing, Enchanting, Smelting. The Crafting Access acts as a catalyst for all categories.

## Configuration

The config file is at `.minecraft/config/magicstorage.cfg`.

- **softAutoSortEnabled**: (default: true) When enabled, items are automatically moved into storage units that already contain matching items with available space. Does not move items to new slots -- only consolidates into existing ones to save storage space.
- **softAutoSortIntervalSeconds**: (default: 60, range: 10-3600) How often in seconds the soft auto-sort runs to consolidate items across storage units.

## Notes

- The Storage Heart uses ForgeChunkManager to keep its chunk loaded for cross-dimensional access.
- Station items (furnace, anvil, brewing stand, etc.) must be IN the Storage Heart's inventory, not placed in the world.
- Bookshelf power for enchanting is calculated from bookshelf ITEMS in your network storage units, not from blocks placed in the world.
- The Crafting Access detects stations in real-time. Add or remove stations from the heart's inventory and the grid updates immediately.
- Items in the crafting grid return to the network when you close the GUI.
