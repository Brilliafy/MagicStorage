[← Back to Wiki Hub](README.md) | [Main Documentation](../README.md)

# Mod Integrations & Compatibility

Magic Storage includes native integration hooks, dynamic station compatibility, and verified interactions with prominent Minecraft 1.12.2 mods.

---

## 1. Just Enough Items (JEI)

> [!IMPORTANT]
> **Recommended:** Installing **JEI** is strongly recommended for full dynamic recipe visibility across all station modes.

* **Dynamic Recipe Transfer (`+` button):** Full 1-click recipe population across all standard crafting, furnace mass smelting, anvil repair/combining, brewing stand potions, enchanting, and modded reforging.
* **Focused Instance Prioritization:** When opening JEI on a specific item instance using **`U`** or **`R`** (such as an item with specific quality modifiers, bauble traits, damage values, or enchantments), clicking `+` prioritizes and transfers that exact item instance into the Crafting Access grid.
* **Smart Fuel Selection:** Furnace smelting automatically selects optimal fuel sources (Coal, Charcoal, Coal Blocks) and protects valuable items or books with bookshelf power.

---

## 2. Carry On

* **Full Tile Entity Portability:** Confirmed working. Players can shift-right-click to pick up and relocate any tier of Magic Storage Unit in the world without breaking it or dropping stored items. All internal inventory contents remain intact.

---

## 3. Mouse Tweaks [Continued]

* **Inventory Slot Optimization:** Confirmed working across Storage Access, Crafting Access, Storage Heart station slots, and individual Storage Units. Enables fluid dragging, quick item distribution, and wheel scrolling.

---

## 4. Reskillable

* **Network-Wide Skill Validation:** If any block or unit in the active storage network exceeds the player's Reskillable skill requirements, terminal access is blocked with a detailed chat notification listing all missing skill levels.
* **Direct Container Fallback:** Players can still open individual basic storage units whose requirements they meet.
* **Station Lock Tooltips:** Crafting stations inside the Storage Heart enforce skill requirements and display `✖ Insufficient Skill Level` warnings.

---

## 5. Spartan Weaponry & Item Matching

* **Transient NBT Tag Stripping:** An internal `ItemMatchHelper` filters client-side temporary tags (`enchChecked`, `enchantmentsInvalid`, dynamic UUIDs), preventing item search, stack matching, or pickup issues in network storage.

---

## 6. Quality Tools

* **Dynamic Material Discovery:** An integrated parser reads `qualitytools/general.cfg` and custom quality definitions to identify all valid reforging materials for tools, armor, weapons, and baubles.
* **Multi-Ingredient Stillness:** JEI recipe listings for reforging display separate, non-cycling entries for distinct material types.

---

## 7. Rustic

* **Elixir Alchemy:** Condensers and retorts support automated batch crafting of simple and advanced elixirs.
* **Brewing Barrel:** Quality parameters from modifier wine bottles are preserved across batches.
* **Crushing Tub:** Requires 4-fruit batches with authentic audio and visual fluid processing.

---

## 8. Bountiful Baubles & Disenchanter

* **Reforger:** Bauble modifier rerolling powered by player experience levels.
* **Disenchantment Table:** Extraction of enchantments onto books with support for Voiding and Bulk table variants.

---

[← Back to Wiki Hub](README.md) | [Main Documentation](../README.md)
