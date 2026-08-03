# Magic Storage v1.0.21 Changelog

## 🚀 Overview
Version **1.0.21** introduces optional integration with 5 major 1.12.2 mods (**Rustic**, **Disenchanter**, **Bountiful Baubles**, **Quality Tools**, and **Reskillable**). All mod integrations are **100% optional** — if a mod is not present in your modpack, Magic Storage operates seamlessly without requiring any additional dependencies or causing crashes.

---

## ✨ Features & Mod Support

### 🧪 1. Rustic Integration
- **Advanced & Simple Condenser Alchemy**:
  - Insert Condensers (`rustic:condenser`, `rustic:condenser_advanced`) and Retorts (`rustic:retort`, `rustic:retort_advanced`) into the Storage Heart to unlock alchemy crafting directly in the Crafting Interface.
  - Requires **3 Retorts + 1 Condenser** (or Advanced equivalents) in the heart. Advanced stations also grant access to simple alchemy recipes.
  - **Ingredient Consumption Probabilities**:
    - Coal fuel has a **20.0%** chance to be consumed per craft.
    - Water buckets have a **12.5%** chance to be consumed per craft (leaving behind an empty bucket when consumed).
- **Brewing Barrel & Deterministic Quality**:
  - Quality calculations for wines and brews are seeded deterministically using a combination of the Storage Heart's world coordinates (`toLong()`) and a persistent craft counter.
  - Re-rolling quality by removing/re-adding items in the crafting matrix is impossible; players must complete a craft to advance the roll counter.
  - **Batch Brewing & Blaze Powder Consumption**:
    - Blaze powder fuel consumption probability scales dynamically with bottle count: **N * 6.25%** (e.g., 1 bottle = 6.25%, 4 bottles = 25.0%, 8 bottles = 50.0%).
    - Crafting with N bottles advances the brewing counter by 1 and produces N brewed bottles in a single craft with the exact same quality calculation.
- **Crushing Tub**:
  - Support for crushing recipes (e.g. crushing grapes/berries into juices).
  - Plays the authentic block crushing sound (`SoundEvents.BLOCK_SLIME_FALL`).

---

### 📜 2. Disenchanter Support
- Place a **Disenchantment Table** (`disenchanter:disenchantmenttable`) inside the Storage Heart to enable disenchanting recipes in the Crafting Interface.
- **Recipe Layout**: Place the item to be disenchanted in **Slot 5** (center slot) and an unenchanted **Book** in **Slot 3** (center-left) to extract enchantments into an **Enchanted Book** (Slot 0).
- **Table Attributes & Variant Support**:
  - **Standard Table**: Disenchants the top enchantment into a book while damaging the source item.
  - **Voiding Table**: Consumes the item in Slot 5 completely.
  - **Bulk Disenchanting Table**: Extracts **ALL** enchantments from the item into the book at once.

---

### 🛡️ 3. Bountiful Baubles Reforging
- Insert a **Reforger** (`bountifulbaubles:reforger`) into the Storage Heart to reforge Baubles in **Slot 9** (bottom-right).
- **Deterministic Quality**: Seeded by Heart position and craft counter to prevent client-side re-rolling.
- **XP Cost & Locking**:
  - Displays the required level cost in the item tooltip.
  - If the player has insufficient XP, the result displays `✖ Insufficient XP` in red bold text and locks slot interactions.
  - Correctly consumes the input bauble in Slot 9 upon crafting and syncs newly applied potion/attribute modifiers to the player's held cursor stack.

---

### 🔨 4. Quality Tools Reforging
- Insert a **Reforging Station** (`qualitytools:reforging_station`) into the Storage Heart to reforge tools, weapons, and armor.
- **Recipe Layout**: Place the item to be reforged in **Slot 5** (center) and its matching repair material in **Slot 9** (bottom-right).
- Consumes the input tool/armor in Slot 5 and 1 material in Slot 9 upon crafting.
- Synchronizes newly assigned quality tags (`qualitytools:luck`, `qualitytools:rusty`, etc.) in-place to prevent cursor desync or item dropping when closing the container GUI.

---

### 🎓 5. Reskillable Integration
- **Dynamic Skill Requirement Enforcement**:
  - Queries Reskillable's live `LevelLockHandler.canPlayerUseItem` registry dynamically to support custom user configurations and block requirement changes.
  - If a player does not meet the skill level required to use a station placed in the Storage Heart, the crafting output displays:
    ```
    ✖    Insufficient Skill Level
    ```
  - **Dual Station Verification**: For Rustic Alchemy, **BOTH** the Condenser and the Retort skill requirements must be satisfied for the recipe to be craftable.
- **Seamless UI & Anti-Desync Locking**:
  - Result slot clicks, shift-clicking, and hotkeying are blocked on both client and server sides.
  - `canTakeStack` and `decrStackSize` inspect the result item NBT lore locally on the client to prevent ghost items or cursor desyncs.
- **Strictly Optional Loading**:
  - Utilizes a private static bridge class (`ReskillableBridge`) to isolate Reskillable API calls. The JVM ClassLoader will never attempt to load Reskillable classes if the mod is not present, guaranteeing 0 crash risk.

---

### 🏰 6. Storage Heart Whitelist & Safety
- The Storage Heart inventory now strictly enforces an allowed item whitelist. Only supported crafting stations can be inserted into the Heart:
  - **Vanilla**: Crafting Table, Furnace, Brewing Stand, Anvil, Enchanting Table
  - **Rustic**: Condenser, Advanced Condenser, Retort, Advanced Retort, Brewing Barrel, Crushing Tub
  - **Disenchanter**: Disenchantment Table
  - **Bountiful Baubles**: Reforger
  - **Quality Tools**: Reforging Station
- Non-station items are blocked from drag-and-drop, shift-clicking, and hotkey insertion into the Storage Heart.
