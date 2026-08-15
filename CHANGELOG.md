# Changelog - Magic Storage

All notable changes, fixes, and feature additions for **Magic Storage** are documented in this file.

---

## [1.1.0] - 2026-08-15

### 🌟 Highlights & Major Features
- **3-State Dynamic Autofill System**: Added an interactive 3-mode toggle button (`A`) directly on the Crafting Access interface:
  - **Disabled (Off)**: No automatic refilling after crafting.
  - **Partial (Yellow/Orange icon)**: Refills consumed ingredients and fuel strictly from the connected **Storage Network**.
  - **Full / Activated (Green icon)**: Refills consumed ingredients and fuel from the **Storage Network** first, falling back to the **Player's Inventory** if network items run out.
- **Quick Drop Keybinds (`Q` & `Ctrl + Q`)**:
  - Hovering over any item in the Storage Access or Crafting Access GUI and pressing `Q` drops **1 item** directly into the world.
  - Pressing `Ctrl + Q` drops an **entire stack** into the world.
- **Focused Item JEI Recipe Transfer (`+`)**:
  - Pressing **U** (Uses) or **R** (Recipes) on any specific item (in your inventory or network) and clicking the `+` transfer button now intelligently prioritizes the **exact item instance** you focused (retaining its specific quality, bauble attributes, enchantments, and durability) across Reforging, Anvil, Enchanting, Brewing, Disenchanting, and Alchemy stations.
  - Automatically pulls the focused item from the player inventory first if held, or from the storage network.
- **Storage Heart Illumination**:
  - The Storage Heart block now emits light with the brightness of a Sea Lantern (Light Level 15).
- **Reskillable Mod Integration & Protection**:
  - **Network-Wide Skill Check**: Opening Storage Access, Crafting Access, or Remote Access verifies all placed blocks in the network. If any connected component requires higher Reskillable skills than the player possesses, access is locked and chat lists all missing required skills (e.g. `Magic Storage: Requires Mining 16, Building 12`).
  - **Individual Unit Fallback**: If a network contains an advanced storage unit (e.g. Terra Unit) exceeding your current skills, you can still right-click individual Basic Storage Units to access their items directly.
  - **Crafting Station Lock Tooltips**: Specialized crafting stations placed inside the Storage Heart (Furnace, Anvil, Brewing, etc.) respect Reskillable requirements with helpful warning tooltips.

---

### 🔨 JEI (Just Enough Items) & Custom Station Categories
- **Complete Magic Storage Recipe Categories**:
  - **Magic Storage Crafting**: Standard crafting recipes with Crafting Access catalyst.
  - **Magic Storage Smelting**: Smart furnace recipes with dynamic burn-time calculations and uniform batch distribution.
  - **Magic Storage Brewing**: Vanilla brewing recipes supporting Blaze Powder fuel and up to 3 simultaneous potion bottles on `Shift + Click`.
  - **Magic Storage Enchanting**: Dynamic enchanting preview with deterministic seeds, XP level validation, and enchantability checks.
  - **Magic Storage Anvil**: Tool/armor repairing and book combining recipes with accurate anvil level calculations and filtering of redundant max-tier recipes (e.g. Sharpness V + Sharpness V).
  - **Magic Storage Disenchanting**: Disenchanting recipes extracting enchantments to books; supports items with 1, 2, 5+ enchantments and respects the Disenchanter's "Bulk" mode attribute.
  - **Magic Storage Tool Reforging (QualityTools)**: Shows all dynamic reforge materials per tool (including OreDictionary tags and modded materials) as individual, non-cycling static recipes.
  - **Magic Storage Bauble Reforging (BountifulBaubles)**: Shows bauble modifier reforging recipes with Reforging Station requirement tooltips.
  - **Rustic Brewing**: Booze brewing recipes with support for modifier wine bottles and quality inheritance.
  - **Rustic Crushing Tub**: Fruit crushing recipes with 4-fruit input requirements and glass bottle outputs.
  - **Rustic Alchemy (Simple & Advanced)**: Condensation recipes with precise burn-tick requirements (300 ticks advanced / 400 ticks simple).
- **Intelligent Fuel Management in JEI Transfer**:
  - Fuel candidates are prioritized: **Coal** $\rightarrow$ **Charcoal** $\rightarrow$ **Highest burn-time fuels**.
  - Deprioritizes blocks with book-power (e.g. bookshelves or modded tomes) to prevent burning enchanting materials.
  - Skips single-capacity unstackable fuels (like wooden swords) when they cannot satisfy the minimum required burn ticks alone.
  - `Shift + Click` on `+` evenly distributes items across all 8 surrounding slots with matching fuel in the center.

---

### 🍷 Mod Compatibility & Integrations
- **QualityTools & RLCraft Compatibility**:
  - Built a dedicated, standalone JSON parser for `reforging materials.json` and OreDictionary entries to load all 500+ reforge recipes on client startup without invoking fragile server-side config reloads.
  - Correctly links custom reforge materials (e.g. Spectral Silt for Bezoar, Leather for Tool Belt, Diamonds, Iron, etc.) ahead of the Nether Star universal fallback.
  - Recipes in JEI display static materials without unwanted ingredient cycling animations.
- **Spartan Weaponry & Transient NBT Tag Handling**:
  - Added `ItemMatchHelper` to automatically filter client-side transient tags (`enchChecked`, `enchantmentsInvalid`, `UUID`, `UUIDMost`, `UUIDLeast`).
  - Fixed ungrabbable items (such as Spartan Weaponry Iron Throwing Knives) in network storage.
- **Rustic Booze Quality & Brewing**:
  - Researched Rustic brew mechanics and fixed quality calculations for unmodified brews (yielding baseline 0.36 or 0.72 quality).
  - Implemented exact modifier quality inheritance when using wine bottles as modifiers in brewing recipes.
  - Dynamic JEI lookup preserves exact wine bottle quality when pressing **U** or **R**.
- **Crushing Tub Crafting**:
  - Fixed recipe recognition and crafting lock for fruit juices, enforcing proper 4-berry consumption.

---

### 🎨 Balancing & Crafting Recipe Changes
- **Storage Heart**:
  - Recipe updated: Slot 2 now requires a **Sea Lantern**.
- **Crafting Access Interface**:
  - Recipe updated: Slots 7, 8, 9 require **Lapis Blocks**, Slot 2 requires a **Clock**, and Slots 1, 3, 4, 6 require **Diamonds**.
- **Remote Storage Access**:
  - Recipe updated: Slot 2 requires an **Ender Pearl** and Slot 5 requires a **Storage Component**.
- **Storage Unit Upgrades**:
  - **Hallowed Storage Upgrade**: Requires **Gold Blocks** in slots 1, 3, 7, 9.
  - **Hellstone Storage Upgrade**: Requires **Blaze Powder** in slots 2, 8.
  - **Luminite Storage Upgrade**: Requires **Ender Chests** in slots 1, 3, 7, 9 and **Emerald Blocks** in slots 2, 4, 6, 8.

---

### 🐛 Bug Fixes & Quality of Life
- **Visual Item Count Duplication**: Fixed a client-side visual desync where items momentarily appeared duplicated during multi-step custom crafting.
- **Enchanting Desync & Hotkey Exploit**:
  - Fixed an issue where hotkeying the crafting result of an enchanting recipe placed phantom items or caused client desyncs when the player lacked sufficient XP levels.
  - Prevented crafting result pickup if XP requirements are unmet.
  - Corrected Enchanted Book preview rendering to display the proper Enchanted Book item texture with glint.
  - Fixed client freeze on shift-clicking enchantment recipes with large stacks of books.
- **GUI Search Bar Polish**:
  - Search bar is automatically cleared by default upon opening Storage Access and Storage Unit GUIs.
  - Resolved `Backspace` key input issue in text search boxes.
  - Disabled unintended JEI search-bar synchronization.
- **Audio Feedback**:
  - Enhanced insertion audio pitch when shift-clicking storage units or installing storage cores.
