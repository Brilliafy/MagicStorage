[← Back to Wiki Hub](README.md) | [Main Documentation](../README.md)

# Storage Network Architecture

This document covers network mechanics, storage units, tier upgrades, wireless access remotes, and item automation.

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

| Tier | Block Registry Name | Slot Capacity | Rows (9 Slots/Row) | Base / Direct Crafting Material |
| :---: | :--- | :---: | :---: | :--- |
| **1** | `magicstorage:storage_unit_basic` | **40** | 4.4 | Oak Logs + Chests + Storage Component |
| **2** | `magicstorage:storage_unit_crimtane` | **80** | 8.8 | Basic Unit + Nether Wart Block + Iron |
| **3** | `magicstorage:storage_unit_demonite` | **80** | 8.8 | Basic Unit + Purpur Block + Iron |
| **4** | `magicstorage:storage_unit_hellstone` | **120** | 13.3 | Crimtane/Demonite Unit + Hell Bricks + Blaze Powder |
| **5** | `magicstorage:storage_unit_hallowed` | **160** | 17.7 | Hellstone Unit + Gold Blocks + Quartz |
| **6** | `magicstorage:storage_unit_blue_chlorophyte` | **220** | 24.4 | Hallowed Unit + Lapis Blocks + Diamonds |
| **7** | `magicstorage:storage_unit_luminite` | **300** | 33.3 | Chlorophyte Unit + Emerald Blocks + Ender Chests |
| **8** | `magicstorage:storage_unit_terra` | **600** | 66.6 | Luminite Unit + Nether Stars + End Rods |

---

## 3. In-Place Upgrades

Holding an upgrade item and Shift-right-clicking a placed storage unit in the world upgrades the block in-place. All items stored within the unit are preserved during the upgrade process.

| Upgrade Item | Source Unit | Target Unit | Capacity Increase |
| :--- | :--- | :--- | :---: |
| `magicstorage:upgrade_crimtane` | Basic | Crimtane | +40 Slots (80 Total) |
| `magicstorage:upgrade_demonite` | Basic | Demonite | +40 Slots (80 Total) |
| `magicstorage:upgrade_hellstone` | Crimtane / Demonite | Hellstone | +40 Slots (120 Total) |
| `magicstorage:upgrade_hallowed` | Hellstone | Hallowed | +40 Slots (160 Total) |
| `magicstorage:upgrade_blue_chlorophyte` | Hallowed | Blue Chlorophyte | +60 Slots (220 Total) |
| `magicstorage:upgrade_luminite` | Blue Chlorophyte | Luminite | +80 Slots (300 Total) |
| `magicstorage:upgrade_terra` | Luminite | Terra | +300 Slots (600 Total) |

---

## 4. Wireless Portable Remotes

Portable remotes provide wireless access to the storage network. Sneak-right-click a placed **Remote Access** block while holding a remote to bind it.

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

Pressing the **`Left Alt`** keybind opens the first linked portable remote in the player inventory directly.

---

## 5. Automation & Forge Capabilities

All Storage Units expose Forge `IItemHandler` capabilities on all 6 block faces:
* **Hoppers, Droppers, and Transporters:** Can insert items into or extract items from individual storage units.
* **Carry On Compatibility:** Storage units are recognized as portable tile entities, allowing players to carry and move storage units with their items intact.
* **Network Redistribution:** The internal sorting engine (`softAutoSortEnabled`) periodically consolidates matching item stacks across connected units.
* **Safe Unit Removal:** Breaking an active storage unit transfers items to adjacent units closest to the heart first. Excess items that exceed remaining capacity will drop into the world.

---

[← Back to Wiki Hub](README.md) | [Main Documentation](../README.md)
