# Mod Integrations & Compatibility

Magic Storage includes native integration hooks and compatibility layers for prominent Minecraft 1.12.2 mods.

---

## 1. Just Enough Items (JEI)

* **Recipe Transfer (`+` button):** Full support across all standard and station recipe categories.
* **Focused Instance Prioritization:** When opening JEI on a specific item instance using **`U`** or **`R`** (e.g. an item with custom quality modifiers, damage values, or enchantments), clicking `+` pulls that exact item into the Crafting Access grid.
* **Smart Fuel Selection:** Furnace recipes automatically select optimal fuel sources (Coal, Charcoal, Coal Blocks) and exclude high-value items or books with bookshelf power.

---

## 2. Reskillable

* **Network-Wide Skill Validation:** If any block or unit in the active storage network exceeds the player's Reskillable skill requirements, terminal access is blocked with a detailed chat notification listing all missing skill levels.
* **Direct Container Fallback:** Players can still open individual basic storage units whose requirements they meet.
* **Station Lock Tooltips:** Crafting stations inside the Storage Heart enforce skill requirements and display `✖ Insufficient Skill Level` warnings.

---

## 3. Spartan Weaponry & Item Matching

* **Transient NBT Tag Stripping:** An internal `ItemMatchHelper` filters client-side temporary tags (`enchChecked`, `enchantmentsInvalid`, dynamic UUIDs), preventing item search, stack matching, or pickup issues in network storage.

---

## 4. Quality Tools

* **Dynamic Material Discovery:** An integrated parser reads `qualitytools/general.cfg` and custom quality definitions to identify all valid reforging materials for tools, armor, weapons, and baubles.
* **Multi-Ingredient Stillness:** JEI recipe listings for reforging display separate, non-cycling entries for distinct material types.

---

## 5. Rustic

* **Elixir Alchemy:** Condensers and retorts support automated batch crafting of simple and advanced elixirs.
* **Brewing Barrel:** Quality parameters from modifier wine bottles are preserved across batches.
* **Crushing Tub:** Requires 4-fruit batches with authentic audio and visual fluid processing.

---

## 6. Bountiful Baubles & Disenchanter

* **Reforger:** Bauble modifier rerolling powered by player experience levels.
* **Disenchantment Table:** Extraction of enchantments onto books with support for Voiding and Bulk table variants.
