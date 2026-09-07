# Changelog - Magic Storage

All notable changes, fixes, and feature additions for **Magic Storage** are documented in this file.

## [1.1.12] - 2026-09-07

### 🛠️ Bug Fixes & Stability
- **Shift-Click Infinite Loop & Main Thread Freeze Fix**:
  - Fixed a game freeze bug occurring when shift-clicking items (such as Spartan Weaponry sabers, weapons, or any inventory item) to move them in Crafting Access, Portable Remote Access, or Storage Heart terminals.
  - Vanilla Minecraft's quick-move container loop (`Container.slotClick`) continuously polls `transferStackInSlot` while the returned stack matches the slot contents. When client-side simulation ran without an adjacent heart reference or when items could not be inserted into the network, unhandled fallthrough returned unchanged stacks, resulting in an unrecoverable infinite loop on the main thread with 0 log errors.
  - Magic Storage containers now immediately terminate quick-move simulation on the client (`world.isRemote`) with `ItemStack.EMPTY`, allowing server packets to handle the transaction authoritatively.
  - Server-side matrix and inventory transfers now safely fall back to player inventory or return `ItemStack.EMPTY` when the destination is full.
  - Added network tile update packets (`getUpdateTag`, `getUpdatePacket`, `onDataPacket`) to `TileCraftingAccess` to maintain accurate client-server tile state synchronization.

---

## [1.1.11] - 2026-09-06

### 🛠️ Bug Fixes & Quality of Life Improvements
- **Storage Unit Item Tooltips on Hover**:
  - Fixed an issue where opening individual Storage Units (or Storage Access / Remote Access) by right-clicking them in the world and hovering over items failed to display tooltips. Full item tooltips (including modded attributes, lore, enchantments, and durability) now display as expected.
- **Storage Heart Slot Isolation & Station Limit**:
  - Fixed a critical bug where network overflow items were pushed into the 20 Storage Heart slots when storage units became full.
  - Heart slots are now strictly isolated from network storage and will never store general items.
  - Heart slots now enforce a strict maximum stack limit of **1** for supported crafting stations only.
  - Shift-clicking stations into the Storage Heart now safely distributes at most 1 station per slot.
- **JEI Recipe Transfer (+) Ghost Item Extraction**:
  - Fixed a bug where clicking the `+` recipe transfer button for recipes with empty slots (such as weapons or armor like a Diamond Sword) pulled already-crafted items (e.g. Diamond Swords) from your inventory or storage network into the empty recipe slots.
  - Recipe transfer now cleanly ignores empty spaces in shaped crafting recipes.
- **Anvil Repair Material Consumption Fix**:
  - Fixed an issue where anvil repairs appeared to consume $n - 1$ materials (e.g., taking 2 iron ingots instead of 3).
  - The anvil was correctly calculating and consuming all 3 materials, but the autofill system immediately pulled 1 replenishment ingot from storage into the emptied slot. Single-use repair and combine operations now bypass autofill.
- **Cobalt Shield & Shield Extraction (Bountiful Baubles)**:
  - Fixed a bug where Cobalt Shields, Obsidian Shields, and Ankh Shields could not be picked or extracted from the storage network.
  - Resolved an NBT mismatch caused by client-side tooltip rendering mutating the stack with `HideFlags: 2`. Client transient `HideFlags` tags are now normalized during network storage queries.
- **Reforging Station Modifier Freeze (Quality Tools & Bountiful Baubles)**:
  - Fixed an issue where reforging at the Reforging Station (Quality Tools) or Bauble Reforger (Bountiful Baubles) eventually glitched and repeatedly yielded the exact same modifier over and over regardless of reroll attempts.
  - Reforging now uses an independent, high-entropy 64-bit SplitMix64 RNG stream that guarantees distinct random modifiers per roll.
  - Fixed RNG state pollution by properly resetting Quality Tools' global Random generator after rolling.
  - Reforging counter now properly advances across all terminal types, including Portable Access remotes and unlinked networks.
  - Disabled ingredient autofill during reforging to prevent unintended item restocking.
- **Rustic Winery Quality Determinism**:
  - Fixed a bug where removing and re-adding wine brewing ingredients to the crafting grid rerolled booze quality on every matrix update.
  - Wine quality is now deterministically locked per batch based on the brewing counter, ensuring consistent quality until the batch is actually crafted.

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
