[← Back to Wiki Hub](README.md) | [Main Documentation](../README.md)

# Storage Network Architecture

This document covers network mechanics, storage units, tier upgrades, wireless access remotes, and world generation materials.

---

## 1. Network Components

### Storage Heart (`magicstorage:storage_heart`)
The central controller of the storage network.
* Contains a 20-slot inventory for crafting stations (accessible by right-clicking the block).
* Emits light at **Light Level 15**.
* Obtains Forge chunk tickets to maintain chunk loading for the network.
* Breaking the Storage Heart shuts down network access safely without destroying items held in connected Storage Units.

### Storage Access (`magicstorage:storage_access`)
A dedicated item retrieval and deposit terminal. Provides item browsing, search filtering, and sorting controls.

### Crafting Access (`magicstorage:crafting_access`)
An integrated terminal combining network storage access with a 3x3 crafting matrix and multi-station processing. Includes a 3-state autofill engine (`A` toggle).

### Remote Access (`magicstorage:remote_access`)
A physical transceiver block placed adjacent to the network. Required to link wireless portable remotes.

### Hell Bricks (`magicstorage:hell_brick`)
A defensive building block. Applies 3 seconds of fire to entities standing on its surface.

---

## 2. Storage Unit Tiers & Capacity

Storage units expand the total item capacity of the network. Right-clicking a storage unit directly opens its individual container. Shift-right-clicking with an empty hand outputs capacity statistics to chat.

| Tier | Block Registry Name | Stack Slot Capacity | Rows (9 Slots/Row) | Base / Direct Crafting Material |
| :---: | :--- | :---: | :---: | :--- |
| **1** | `magicstorage:storage_unit_basic` | **36** | 4 | Oak Logs + Chests + Storage Component |
| **2** | `magicstorage:storage_unit_crimtane` | **54** | 6 | Basic Unit + Crimtane Upgrade |
| **3** | `magicstorage:storage_unit_demonite` | **54** | 6 | Basic Unit + Demonite Upgrade |
| **4** | `magicstorage:storage_unit_hellstone` | **90** | 10 | Basic Unit + Hellstone Upgrade |
| **5** | `magicstorage:storage_unit_hallowed` | **135** | 15 | Basic Unit + Hallowed Upgrade |
| **6** | `magicstorage:storage_unit_blue_chlorophyte` | **180** | 20 | Basic Unit + Blue Chlorophyte Upgrade |
| **7** | `magicstorage:storage_unit_luminite` | **252** | 28 | Basic Unit + Luminite Upgrade |
| **8** | `magicstorage:storage_unit_terra` | **360** | 40 | Basic Unit + Terra Upgrade |

---

## 3. In-Place Upgrades

Holding an upgrade item and Right-clicking a placed storage unit in the world upgrades the block in-place. All items stored within the unit are preserved during the upgrade process. Any lower-tier unit can be directly upgraded to a higher tier!

| Upgrade Item | Target Tier | Capacity | Key Ingredients |
| :--- | :---: | :---: | :--- |
| `magicstorage:upgrade_crimtane` | Crimtane | **54 Slots** | 4x Crimtane Ingots, 2x Iron Blocks, 2x Redstone Blocks |
| `magicstorage:upgrade_demonite` | Demonite | **54 Slots** | 4x Demonite Ingots, 2x Iron Blocks, 2x Obsidian |
| `magicstorage:upgrade_hellstone` | Hellstone | **90 Slots** | 4x Hellstone Bars, 2x Blaze Rods, 2x Hell Bricks |
| `magicstorage:upgrade_hallowed` | Hallowed | **135 Slots** | 4x Hallowed Ingots, 2x Diamond Blocks, 2x Ghast Tears |
| `magicstorage:upgrade_blue_chlorophyte` | Blue Chlorophyte | **180 Slots** | 4x Chlorophyte Bars, 2x Lapis Blocks, 2x Sea Lanterns |
| `magicstorage:upgrade_luminite` | Luminite | **252 Slots** | 6x Luminite Bars, 1x Radiant Jewel, 1x Shulker Shell |
| `magicstorage:upgrade_terra` | Terra | **360 Slots** | 1x Terra Singularity, 2x Radiant Jewels, 2x Luminite Bars, 1x Nether Star, 2x End Crystals |

---

## 4. Custom Materials, Jewels & World Generation Ores

Magic Storage introduces custom materials and ores to govern non-recursive, rewarding tier progression:

### Hellstone Ore (`magicstorage:hellstone_ore`) & Hellstone Bar
* **Where to Find:** Spawns exclusively in the **Nether** deep beneath the lava ocean level (**strictly below Y = 31**, depths Y: 5–30) embedded in Netherrack.
* **Mining Mechanics:** Mining the ore drops **Hellstone Fragments** (`magicstorage:fragment_hellstone`). The quantity dropped is multiplied by the **Fortune** enchantment. Mining with Silk Touch yields the ore block directly.
* **Smelting:** The raw ore block can be smelted in standard furnaces into Hellstone Fragments.
* **Hellstone Bars:** Forged by combining 3x Hellstone Fragments, 1x Obsidian, and 1x Blaze Powder inside a **Storage Heart with a Furnace** station.

### Luminite Ore (`magicstorage:luminite_ore`) & Luminite Bar
* **Where to Find:** Spawns in the **End** exclusively on the outer islands at a distance of **at least 1,024 blocks** (64 chunks) from the central island ($r \ge 1024$) embedded in End Stone.
* **Mining Mechanics:** Mining the ore drops **Luminite Fragments** (`magicstorage:fragment_luminite`). Fortune enchantment scales fragment drops. Silk Touch yields the ore block.
* **Smelting:** Ore blocks can also be smelted in furnaces into Luminite Fragments.
* **Luminite Bars:** Refined from 4x Luminite Fragments inside a **Storage Heart with a Furnace** station.

### Shadow Diamond (`magicstorage:shadow_diamond`)
An abyssal diamond infused with Demonite and Crimtane dark magic.
* **Crafting:** 1x Diamond, 2x Obsidian, 1x Demonite Ingot, 1x Crimtane Ingot (Shapeless).
* **Uses:** Required to craft the **Basic Portable Storage Access** and **Basic Portable Crafting Access** remotes.

### Radiant Jewel (`magicstorage:radiant_jewel`)
A prism radiating celestial stellar energy.
* **Crafting:** 1x Nether Star, 2x Luminite Bars, 1x Dragon's Breath, 1x End Crystal (Shapeless).
* **Uses:** Required to craft the **Ultimate Portable Storage Access**, **Ultimate Portable Crafting Access**, **Luminite Storage Upgrade**, and **Terra Storage Upgrade**.

### Terra Singularity (`magicstorage:terra_singularity`)
The cosmic synthesis of all six metal tiers (Demonite, Crimtane, Hellstone, Hallowed, Chlorophyte, Luminite) and endgame bosses (Nether Star, Dragon's Breath, End Crystal).
* **Uses:** Required for the ultimate **Terra Storage Upgrade** (360 slots) and **Ultimate Portable Crafting Access** remote.

---

## 5. Wireless Portable Remotes

Portable remotes provide wireless access to the storage network. Sneak-right-click a placed **Remote Access** block while holding a remote to bind it. Remotes preserve their network binding during crafting upgrades!

```
[Portable Remote] ---> Wireless Signal ---> [Remote Access Block] <---> [Storage Heart]
```

| Remote Item | Interface Type | Operating Range | Cross-Dimensional |
| :--- | :---: | :---: | :---: |
| **Basic Portable Storage Access** | Storage Access | 200 Blocks | No |
| **Advanced Portable Storage Access** | Storage Access | Unlimited | No |
| **Ultimate Portable Storage Access** | Storage Access | Unlimited | **Yes** |
| **Basic Portable Crafting Access** | Crafting Access | 200 Blocks | No |
| **Advanced Portable Crafting Access** | Crafting Access | Unlimited | No |
| **Ultimate Portable Crafting Access** | Crafting Access | Unlimited | **Yes** |
