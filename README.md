[![Support my work](https://img.shields.io/badge/Support_my_work-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/S2X12424XK)
[![Modrinth](https://img.shields.io/badge/Modrinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/mod/magic-storage-network)
[![CurseForge](https://img.shields.io/badge/CurseForge-F44336?style=for-the-badge&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/magic-storage-network)

# Magic Storage

A modular storage network mod for Minecraft 1.12.2 (Forge) inspired by Terraria's Magic Storage. Centralizes inventory items into a unified Storage Heart network, manages storage capacity through 8 modular unit tiers, allows wireless access via portable remotes, and provides multi-station crafting with automated item replenishment.

![Magic Storage](docs/images/wallpaper.png)

---

## Documentation Wiki

Detailed guides, formulas, and visual diagrams are organized in the [Wiki Directory](wiki/README.md):

| Guide | Scope |
| :--- | :--- |
| **[Recipe Catalog](wiki/recipes/README.md)** | High-resolution visual diagrams for all 28 mod recipes and 12 station demonstrations. |
| **[Getting Started](wiki/getting-started.md)** | Network placement rules, adjacency requirements, 32-block radius, and terminal navigation. |
| **[Storage Network Architecture](wiki/storage-network.md)** | Storage unit tiers, in-place upgrades, wireless remotes, chunk tickets, and `IItemHandler` automation. |
| **[Crafting Stations & Mechanics](wiki/crafting-stations.md)** | Station inventory rules, furnace fuel math, enchanting power scanning, and 3-state autofill. |
| **[Mod Integrations](wiki/integrations.md)** | Native hooks for JEI, Reskillable, Quality Tools, Rustic, Bountiful Baubles, and Disenchanter. |
| **[Configuration Reference](wiki/configuration.md)** | Configuration parameters, sorting intervals, and station feature toggles. |

---

## System Overview

<details open>
<summary><b>1. Quick Start & Network Rules</b></summary>

### Core Setup
Place a **Storage Heart** in your base, connect at least one **Storage Unit** adjacent to it, and attach a **Storage Access** (item management) or **Crafting Access** (crafting matrix) terminal.

```
[ Storage Unit ] <---> [ Storage Heart ] <---> [ Crafting Access ]
```

### Network Constraints
* **Adjacency:** Every component block must directly touch at least one other network block.
* **Operating Radius:** Network blocks must be within **32 blocks** of the Storage Heart.
* **Single Master Brain:** Each network supports exactly one Storage Heart.
* **Chunk Loading:** The Storage Heart automatically secures chunk tickets via `ForgeChunkManager` to keep its own chunk and connected unit chunks loaded.

</details>

<details>
<summary><b>2. Blocks & Network Terminals</b></summary>

| Block Name | Registry Name | Function |
| :--- | :--- | :--- |
| **Storage Component** | `magicstorage:storage_component` | Foundational crafting material for blocks, units, remotes, and upgrades. |
| **Storage Heart** | `magicstorage:storage_heart` | Master network controller. Emits Light Level 15 and holds 20 crafting stations. |
| **Storage Access** | `magicstorage:storage_access` | Terminal for depositing, extracting, searching, and sorting network items. |
| **Crafting Access** | `magicstorage:crafting_access` | 3x3 crafting terminal connected to network items with 3-state autofill (`A`). |
| **Remote Access** | `magicstorage:remote_access` | Network transceiver required to bind portable wireless remotes. |
| **Hell Bricks** | `magicstorage:hell_brick` | Defensive block. Sets entities standing on top on fire for 3 seconds. |

</details>

<details>
<summary><b>3. Storage Units & In-Place Upgrades</b></summary>

Storage units increase total network slot capacity. Right-click any unit to inspect its contents. Shift-right-click with an empty hand to print fullness stats to chat. Shift-right-click with an upgrade item to upgrade the unit in-place without dropping stored items.

| Tier | Storage Unit | Slot Capacity | Rows (9 Slots/Row) | In-Place Upgrade Item |
| :---: | :--- | :---: | :---: | :--- |
| **1** | **Basic Storage Unit** | **40** | 4.4 | Base Recipe |
| **2** | **Crimtane Storage Unit** | **80** | 8.8 | `magicstorage:upgrade_crimtane` |
| **3** | **Demonite Storage Unit** | **80** | 8.8 | `magicstorage:upgrade_demonite` |
| **4** | **Hellstone Storage Unit** | **120** | 13.3 | `magicstorage:upgrade_hellstone` |
| **5** | **Hallowed Storage Unit** | **160** | 17.7 | `magicstorage:upgrade_hallowed` |
| **6** | **Blue Chlorophyte Storage Unit** | **220** | 24.4 | `magicstorage:upgrade_blue_chlorophyte` |
| **7** | **Luminite Storage Unit** | **300** | 33.3 | `magicstorage:upgrade_luminite` |
| **8** | **Terra Storage Unit** | **600** | 66.6 | `magicstorage:upgrade_terra` |

#### Automation & Item Migration
All Storage Units expose Forge `IItemHandler` capabilities on all 6 faces for Hoppers, Itemducts, and item conduits. Breaking an active storage unit migrates items to neighboring units closest to the heart first, dropping only excess items that exceed remaining space.

</details>

<details>
<summary><b>4. Wireless Portable Remotes</b></summary>

Bind a remote by holding it and **Sneak + Right-Clicking** a placed **Remote Access** block. Press **`Left Alt`** (configurable) in-game to open the first linked remote in your inventory directly.

| Remote Tier | Terminal Interface | Operating Range | Cross-Dimensional Access |
| :--- | :---: | :---: | :---: |
| **Basic Portable Storage Access** | Storage Access | 200 Blocks | No |
| **Advanced Portable Storage Access** | Storage Access | Unlimited | No |
| **Ultimate Portable Storage Access** | Storage Access | Unlimited | **Yes** |
| **Basic Portable Crafting Access** | Crafting Access | 200 Blocks | No |
| **Advanced Portable Crafting Access** | Crafting Access | Unlimited | No |
| **Ultimate Portable Crafting Access** | Crafting Access | Unlimited | **Yes** |

</details>

<details>
<summary><b>5. Crafting Access & Dynamic Station Processing</b></summary>

Right-click the **Storage Heart** and insert crafting station items into its 20-slot station inventory to unlock station recipes on the **Crafting Access** matrix.

| Station | Requirements & Layout | Mechanics & Formulas |
| :--- | :--- | :--- |
| **Crafting Table** | `minecraft:crafting_table` in Heart (Required) | Standard 3x3 crafting matrix using network and inventory items. |
| **Furnace** | Center Slot (5) = Fuel, Outer Slots (1-4, 6-9) = Inputs | Smelts up to 8 items simultaneously. Fuel burn probability: $\frac{\text{Inputs} \times 200}{\text{Single Fuel Burn Ticks}}$. |
| **Enchanting Table** | Slot 1 = Target Item, Slots 4, 5, 6 = Lapis Lazuli | Reads bookshelf power dynamically from all bookshelf items in network storage. |
| **Anvil** | Slot 1 = Target Item, Slot 5 = Material / Book | Authentic repair durability, enchantment combination, and XP level calculations. |
| **Brewing Stand** | Slot 1 = Blaze Powder, Slot 2 = Ingredient, Slots 4/5/6 = Bottles | Batch brews 1 to 3 bottles with a 5% Blaze Powder consumption chance. |
| **Rustic Condenser** | 1 Condenser + 3 Retorts (Simple or Advanced) | Minimum fuel burn ticks (400 Simple / 300 Advanced). Water buckets return empty buckets. |
| **Rustic Brewing Barrel** | Slot 1 = Fruits, Slot 5 = Modifier Wine Bottle | Preserves and scales wine quality ratings from modifier bottles. |
| **Rustic Crushing Tub** | Slot 1 = 4x Fruit, Slot 5 = Glass Bottle | Enforces 4-fruit batch requirement with crushing audio. |
| **Disenchanter** | Slot 5 = Enchanted Item, Slot 3 = Book | Extracts enchantments to books; supports standard, Voiding, and Bulk tables. |
| **Bountiful Baubles Reforger** | Slot 9 = Bauble Item | Rerolls bauble modifier attributes using player XP levels. |
| **Quality Tools Reforging** | Slot 5 = Item, Slot 9 = Reforging Material | Dynamic material engine supports 500+ items and OreDictionary tags. |

#### 3-State Autofill Engine (`A` Toggle)
* **Disabled (Off):** No items are refilled after a craft.
* **Partial (Yellow):** Refills consumed ingredients and fuels from **Network Storage** only.
* **Full (Green):** Refills from **Network Storage** first, falling back to **Player Inventory** if network items are depleted.

</details>

<details>
<summary><b>6. GUI Controls & Keybindings</b></summary>

| Control / Shortcut | Context | Function |
| :--- | :--- | :--- |
| **`Left Alt`** | In-Game (No GUI) | Opens first bound portable remote found in the player inventory. |
| **`Q`** | Hovering item in terminal | Drops **1 item** from the stack into the world. |
| **`Ctrl + Q`** | Hovering item in terminal | Drops the **entire stack** into the world. |
| **`Shift + Left-Click`** | Item in Terminal / Inventory | Transfers items between player inventory and network storage. |
| **`Shift + Right-Click`** | Storage Unit (Empty Hand) | Prints capacity and fullness percentage to chat with audio feedback. |
| **`Shift + Right-Click`** | Storage Unit (Holding Upgrade) | Upgrades storage unit tier in-place without dropping stored items. |
| **`R` / `U`** | Hovering item in terminal | Opens JEI Recipes (`R`) or Uses (`U`). |
| **`+` (Transfer)** | In JEI Recipe View | Transfers recipe items into Crafting Access, prioritizing focused item instances. |
| **Search Bar** | In Terminal | Real-time text search filtering by name, tooltip text, or mod ID. |
| **Sort Mode** | In Terminal | Cycles sorting by Quantity, Display Name, or Mod ID. |
| **Sort Direction** | In Terminal | Toggles between Ascending ($\uparrow$) and Descending ($\downarrow$) order. |
| **JEI Sync** | In Terminal | Synchronizes terminal search text with the JEI search bar. |

</details>

<details>
<summary><b>7. Visual Demonstration Clips</b></summary>

#### Multi-Station Setup
![Stations](docs/use/stations.gif)

#### Mass Smelting
![Smelting](docs/use/smelting.gif)

#### Batch Brewing
![Brewing](docs/use/brewing.gif)

#### Enchanting Table
![Enchanting](docs/use/enchanting.gif)

#### Anvil Repair & Combining
![Repair and Combine](docs/use/repair_combine_insufficient.gif)

#### Wireless Remotes
![Remotes](docs/use/remotes.gif)

#### In-Place Upgrades
![Unit Upgrades](docs/use/unit_upgrades.gif)

#### Modded Station Integrations
![Optional_Mods](docs/use/demo.gif)

</details>

<details>
<summary><b>8. Configuration Reference</b></summary>

Config file location: `.minecraft/config/magicstorage.cfg`

```hocon
# Storage network sorting options
sorting {
    # Consolidated item stacking across storage units.
    B:softAutoSortEnabled=true

    # Consolidation sweep interval in seconds (10 - 3600).
    I:softAutoSortIntervalSeconds=60
}

# Crafting station recipe toggles (Crafting Table is always enabled)
stations {
    B:enableFurnace=true
    B:enableBrewingStand=true
    B:enableAnvil=true
    B:enableEnchantingTable=true
    B:enableDisenchanterTable=true
    B:enableBountifulBaublesReforger=true
    B:enableQualityToolsReforger=true
    B:enableRusticAlchemy=true
    B:enableRusticBrewing=true
    B:enableRusticCrushing=true
}
```

</details>

<details>
<summary><b>9. Mod Integrations & Compatibility</b></summary>

* **Just Enough Items (JEI):** Full `+` recipe transfer support with focused instance preservation (retaining custom qualities, bauble stats, and enchantments).
* **Reskillable:** Enforces skill requirements across all network components and heart stations, presenting chat error lists and `✖ Insufficient Skill Level` tooltips.
* **Spartan Weaponry:** Strips transient client tags (`enchChecked`, `enchantmentsInvalid`, `UUID`) via `ItemMatchHelper` to maintain accurate network item stacking.
* **Quality Tools, Rustic, Bountiful Baubles, Disenchanter:** Integrated station recipes, quality preservation, and XP level mechanics.

> [!WARNING]
> This mod accesses vanilla `ContainerRepair` via Java reflection for anvil operations. Tested with Forge **14.23.5.2864**.

</details>

<details>
<summary><b>10. FAQ, Credits & License</b></summary>

### Frequently Asked Questions

**Q: Where should I report bugs or crashes?**  
A: Open an issue on the [GitHub Issue Tracker](https://github.com/Brilliafy/MagicStorage/issues) with the crash report and Forge version.

**Q: Can I include this mod in a modpack?**  
A: Yes. You can include this mod in any modpack without prior permission.

---

### Third-Party Credits
* **[Magic Storage (Terraria)](https://github.com/blushiemagic/MagicStorage)** by blushiemagic: Original mod concept, mechanics, and visual assets used under the [MIT License](https://github.com/blushiemagic/MagicStorage/blob/master/LICENSE.txt).
* **[Storage Network](https://github.com/Lothrazar/Storage-Network)** by Lothrazar: Network storage foundations adapted under the [MIT License](https://github.com/Lothrazar/Storage-Network/blob/master/LICENSE).

### License
Licensed under the **GNU General Public License v3.0 (GPL-3.0)**.
</details>
