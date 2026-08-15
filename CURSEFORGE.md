<div align="center">

# Magic Storage (Minecraft 1.12.2)

![Magic Storage Banner](https://raw.githubusercontent.com/Brilliafy/MagicStorage/master/docs/images/wallpaper.png)

[![Ko-Fi](https://img.shields.io/badge/Support_on_Ko--Fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/S2X12424XK)
[![GitHub Source](https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Brilliafy/MagicStorage)
[![Documentation Wiki](https://img.shields.io/badge/Wiki-Full_Documentation-0078D4?style=for-the-badge&logo=gitbook&logoColor=white)](https://github.com/Brilliafy/MagicStorage/tree/master/wiki)

</div>

---

### What is Magic Storage?

**Magic Storage** is a modular, high-performance storage network solution for **Minecraft 1.12.2 (Forge)** inspired by Terraria's Magic Storage mod. It replaces cluttered chest rooms with an expandable block network, unified terminal interfaces, wireless cross-dimensional remotes, and multi-station crafting with automated item replenishment.

---

### Key Features at a Glance

* **Unified Storage Network:** Connect modular Storage Units (40 to 600 slots per unit across 8 tiers) to a central **Storage Heart**.
* **Instant Terminal Access:** Search by item name, tooltip text, or Mod ID. Sort by quantity, display name, or mod origin with instant backspace and search persistence.
* **Multi-Station Crafting Matrix:** Feed crafting stations (Crafting Tables, Furnaces, Anvils, Enchanting Tables, Brewing Stands, and modded stations) directly into the Storage Heart to craft directly on the terminal grid.
* **3-State Autofill Engine (`A` Key):** Automatically replenishes consumed crafting ingredients and fuels up to the exact recipe requirement from network storage or player inventory.
* **Wireless Remotes:** Access storage and crafting matrices remotely across 200 blocks, unlimited distances, or across dimensions. Press **`Left Alt`** to open bound remotes instantly.
* **Native Mod Hooks:** Full JEI `+` recipe transfer, Reskillable skill requirements, Spartan Weaponry NBT sanitization, Quality Tools reforging, Rustic elixirs/brewing/crushing, Bountiful Baubles reforgers, and Disenchanter tables.
* **Automation Ready:** Every storage unit exposes Forge `IItemHandler` capabilities for Hoppers, Itemducts, and item transport conduits.

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
