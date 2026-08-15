[← Back to Wiki Hub](README.md) | [Main Documentation](../README.md)

# Crafting Stations & Mechanics

Inserting crafting station blocks or items into the 20-slot station inventory of the **Storage Heart** enables custom crafting modes within the **Crafting Access**.

---

## 1. Station Index & Requirements

| Station Item | Requirement in Heart | Slot Layout in Crafting Access | Special Mechanics |
| :--- | :--- | :--- | :--- |
| **Crafting Table** | `minecraft:crafting_table` | Standard 3x3 | Required to operate the Crafting Access. |
| **Furnace** | `minecraft:furnace` | Center Slot (5) = Fuel, Outer Slots (1-4, 6-9) = Inputs | Dynamic burn ticks, probabilistic fuel consumption. |
| **Enchanting Table** | `minecraft:enchanting_table` | Slot 1 = Target Item, Slots 4, 5, 6 = Lapis Lazuli | Reads bookshelf power from network inventory items. |
| **Anvil** | `minecraft:anvil` | Slot 1 = Target Item, Slot 5 = Material / Book | Authentic repair durability, enchantment combine, XP costs. |
| **Brewing Stand** | `minecraft:brewing_stand` | Slot 1 = Blaze Powder, Slot 2 = Ingredient, Slots 4/5/6 = Bottles | Batch brewing (1 to 3 bottles), 5% blaze powder cost chance. |
| **Rustic Condenser** | 1 Condenser + 3 Retorts | Slot 5 = Fuel (min ticks), Ingredients in Slots 1-4 | Simple & Advanced Elixirs, bucket returns on craft. |
| **Rustic Brewing Barrel**| `rustic:brewing_barrel` | Slot 1 = Fruits, Slot 5 = Optional modifier bottle | Quality inheritance from modifier wine bottle. |
| **Rustic Crushing Tub** | `rustic:crushing_tub` | Slot 1 = 4x Fruit, Slot 5 = Glass Bottle | 4-fruit batch requirement, crushing sound effect. |
| **Disenchanter** | `disenchanter:disenchantmenttable` | Slot 5 = Enchanted Item, Slot 3 = Book | Extracts enchants to book; supports bulk and voiding types. |
| **Bountiful Baubles Reforger** | `bountifulbaubles:reforger` | Slot 9 = Target Bauble | Reforges bauble modifier using player XP levels. |
| **Quality Tools Reforging Station** | `qualitytools:reforging_station` | Slot 5 = Item, Slot 9 = Reforging Material | Dynamic material registry (500+ items / OreDict). |

---

## 2. Detailed Station Mechanics

### Furnace Smelting
* **Input Distribution:** Outer slots (1-4, 6-9) accept up to 8 identical smeltable items simultaneously.
* **Fuel Consumption Formula:**
  $$\text{Consumption Probability} = \frac{\text{Item Count} \times 200}{\text{Single Fuel Burn Ticks}}$$
* Fuel with insufficient burn time to process at least one item (e.g. 1 Stick = 100 ticks, requiring 200 ticks minimum) cannot be used until stacked to meet minimum burn requirements.

### Enchanting Table
* **Bookshelf Power Calculation:** The network scans all stored items for bookshelf blocks and modded enchanting tomes (`bookshelf`, `enchanting_bookshelf`, etc.) to determine maximum enchantment levels up to level 30.
* **Deterministic Seeds:** Seed calculations match vanilla enchanting logic, refreshing when enchantments are applied.

### Anvil Repair & Combining
* Uses vanilla anvil combining logic via reflection on `ContainerRepair`.
* Computes repair durability points, enchantment transfer compatibility, prior work penalties, and player XP level requirements.
* Filters redundant maximum-tier recipes from JEI listings.

### Rustic Alchemy (Simple & Advanced)
* Simple Condenser + 3 Retorts enforce a minimum 400-tick burn fuel.
* Advanced Condenser + 3 Advanced Retorts enforce a minimum 300-tick burn fuel and unlock advanced elixir recipes.
* Water Buckets used as reagents return empty buckets to network storage when autofill is enabled.

### Quality Tools Reforging Station
* Uses an integrated parser to load Quality Tools configuration entries without invoking client-thread reload overhead.
* Matches items to specific materials (e.g. Spectral Silt for Bezoars, Leather for Tool Belts, Nether Stars for Baubles, and OreDictionary metal ingots for tools and weapons).

---

## 3. Autofill Engine (`A` Key)

The Crafting Access features a 3-state autofill engine:

* **Disabled (Off):** No items are refilled after completing a craft.
* **Partial (Yellow):** Automatically refills consumed ingredients and fuels from **Network Storage** only.
* **Full (Green):** Refills from **Network Storage** first, then falls back to **Player Inventory** if network supplies are exhausted.

---

[← Back to Wiki Hub](README.md) | [Main Documentation](../README.md)
