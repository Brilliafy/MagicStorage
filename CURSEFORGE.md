<div align="center">

# Magic Storage (Minecraft 1.12.2)

![Magic Storage Banner](https://raw.githubusercontent.com/Brilliafy/MagicStorage/master/docs/images/wallpaper.png)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-2C7B38?style=for-the-badge&logo=minecraft&logoColor=white)](https://github.com/Brilliafy/MagicStorage)
[![Forge](https://img.shields.io/badge/Forge-14.23.5.2864-DFA837?style=for-the-badge&logo=curseforge&logoColor=white)](https://github.com/Brilliafy/MagicStorage)
[![GitHub Source](https://img.shields.io/badge/GitHub-Source_Code-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Brilliafy/MagicStorage)
[![Documentation Wiki](https://img.shields.io/badge/Wiki-Documentation-0078D4?style=for-the-badge&logo=gitbook&logoColor=white)](https://github.com/Brilliafy/MagicStorage/tree/master/wiki)

</div>

---

> [!IMPORTANT]
> **Just Enough Items (JEI) Recommended:** Installing **JEI** is strongly advised. Magic Storage features complete dynamic JEI recipe transfer (`+` button) across all crafting matrix modes, smelting, batch brewing, enchantment extraction, and item reforging.

---

### What is Magic Storage?

**Magic Storage** is a modular storage network solution for **Minecraft 1.12.2 (Forge)** inspired by Terraria's Magic Storage mod. It replaces cluttered chest rooms with an expandable block network, unified terminal interfaces, wireless cross-dimensional remotes, and multi-station crafting with automated item replenishment.

---

### Key Features at a Glance

* **Unified Storage Network:** Connect modular Storage Units (40 to 600 slots per unit across 8 tiers) to a central **Storage Heart**.
* **Instant Terminal Access:** Search by item name, tooltip text, or Mod ID. Sort by quantity, display name, or mod origin with instant backspace and search persistence.
* **Multi-Station Crafting Matrix:** Feed crafting stations (Crafting Tables, Furnaces, Anvils, Enchanting Tables, Brewing Stands, and modded stations) directly into the Storage Heart to craft directly on the terminal grid.
* **3-State Autofill Engine (`A` Key):** Automatically replenishes consumed crafting ingredients and fuels up to the exact recipe requirement from network storage or player inventory.
* **Wireless Remotes:** Access storage and crafting matrices remotely across 200 blocks, unlimited distances, or across dimensions. Press **`Left Alt`** to open bound remotes instantly.
* **Dynamic JEI Integration:** Full `+` recipe transfer support across all custom and modded stations with focused item instance matching.
* **Automation & Mod Ready:** Storage units expose Forge `IItemHandler` capabilities for Hoppers, Itemducts, and item transport pipes.

---

### Visual Demonstration

<div align="center">

| Multi-Station Heart Integration | Mass Smelting Matrix |
| :---: | :---: |
| ![Stations](https://raw.githubusercontent.com/Brilliafy/MagicStorage/master/docs/use/stations.gif) | ![Smelting](https://raw.githubusercontent.com/Brilliafy/MagicStorage/master/docs/use/smelting.gif) |

| Wireless Remotes (`Left Alt`) | In-Place Unit Upgrades |
| :---: | :---: |
| ![Remotes](https://raw.githubusercontent.com/Brilliafy/MagicStorage/master/docs/use/remotes.gif) | ![Unit Upgrades](https://raw.githubusercontent.com/Brilliafy/MagicStorage/master/docs/use/unit_upgrades.gif) |

</div>

---

### Mod Compatibility

* **Carry On:** Confirmed working. Players can pick up and relocate Magic Storage Units with their inventory contents intact.
* **Mouse Tweaks [Continued]:** Confirmed working across all terminal interfaces and crafting slots.
* **Just Enough Items (JEI):** Full recipe transfer and synchronized search bar filtering.
* **Reskillable:** Full network and station requirement enforcement with error notifications.
* **Quality Tools & Bountiful Baubles:** Dynamic material discovery and XP-based modifier reforging.
* **Rustic & Disenchanter:** Alchemy elixirs, brewing barrels, crushing tubs, and book disenchanting.
* **Spartan Weaponry:** Client tag filtering ensures smooth network item stacking.

---

### Quick Setup

```
[ Storage Unit ] <---> [ Storage Heart ] <---> [ Crafting Access ]
```

1. Place a **Storage Heart** in your base.
2. Place a **Basic Storage Unit** directly adjacent to the heart.
3. Place a **Crafting Access** connected to the network.
4. Right-click the Storage Heart to insert your crafting stations (Crafting Table, Furnace, Anvil, etc.).
5. Right-click the Crafting Access to browse, search, deposit, withdraw, and craft with your entire network inventory.

---

### Complete Documentation & Recipe Catalog

For comprehensive technical guides, formulas, config options, and visual 3D recipe diagrams, visit the official repository:

* 📖 **[Main GitHub Repository](https://github.com/Brilliafy/MagicStorage)**
* 📦 **[Visual Recipe & Station Catalog](https://github.com/Brilliafy/MagicStorage/tree/master/wiki/recipes)**
* ⚙️ **[Configuration & Architecture Wiki](https://github.com/Brilliafy/MagicStorage/tree/master/wiki)**
* 🐛 **[Issue Tracker & Bug Reports](https://github.com/Brilliafy/MagicStorage/issues)**
