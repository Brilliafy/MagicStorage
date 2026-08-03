# Magic Storage v1.0.21 Changelog

## Overview
Magic Storage version 1.0.21 brings optional compatibility for five popular 1.12.2 mods: Rustic, Disenchanter, Bountiful Baubles, Quality Tools, and Reskillable. All new features work automatically if the corresponding mod is installed. If a mod is not in your modpack, Magic Storage functions normally without requiring extra dependencies or causing issues.

---

## Mod Support Details

### Rustic Support
- Alchemy Condensers and Retorts: Place 3 Retorts and 1 Condenser (either Simple or Advanced) inside your Storage Heart to craft alchemy potions directly from your Crafting Interface. Having Advanced stations in your Heart allows you to craft both Simple and Advanced alchemy recipes.
- Alchemy Item Consumption: When performing alchemy, Coal fuel has a 20% chance to be consumed, while Water Buckets have a 12.5% chance to be consumed (leaving an empty bucket behind).
- Brewing Barrel: Craft wines and elixirs right inside the Crafting Interface. Wine quality is determined by your Storage Heart's location and total brews completed, preventing players from re-rolling quality by taking items out and putting them back in.
- Multi-Bottle Brewing: Brewing multiple bottles of juice at once crafts them all in a single action while advancing your brew count by one. Fuel consumption scales dynamically with the number of bottles (each bottle adds a 6.25% chance to consume Blaze Powder).
- Crushing Tub: Crushing recipes (such as turning grapes into juice) are fully supported with authentic crushing sounds.

### Disenchanter Support
- Disenchantment Table: Place a Disenchantment Table inside your Storage Heart to access disenchanting directly from the Crafting Interface.
- How to Use: Put the item you want to disenchant in the middle slot (Slot 5) and an unenchanted Book in the middle-left slot (Slot 3) to extract enchantments onto an Enchanted Book in the output slot.
- Table Upgrades: Fully supports Voiding Disenchantment Tables (which consume the original item completely) and Bulk Disenchantment Tables (which extract all enchantments into a single book).

### Bountiful Baubles Reforging
- Reforger Station: Insert a Reforger into your Storage Heart to reforge Baubles directly in the Crafting Interface.
- How to Use: Place your Bauble in the bottom-right slot (Slot 9) to reforge its modifiers.
- Experience Costs: The required experience level is shown in the output tooltip. If you do not have enough experience, the output slot locks safely and displays an Insufficient XP warning. Reforging consumes the Bauble in Slot 9 upon completing the craft.

### Quality Tools Reforging
- Reforging Station: Insert a Reforging Station into your Storage Heart to reforge tools, weapons, and armor.
- How to Use: Place the tool or armor in the center slot (Slot 5) and its repair material in the bottom-right slot (Slot 9).
- Crafting: Consumes the tool in Slot 5 and one repair material in Slot 9, applying a new quality modifier to your item.

### Reskillable Requirements
- Skill Checks: If you have Reskillable installed, crafting stations inside your Storage Heart will check your character's skill levels before allowing you to craft recipes from that station.
- Multi-Station Requirements: For Rustic Alchemy, you must meet the skill requirements for both the Condenser and the Retort to craft alchemy potions.
- Clear Feedback: If your skill level is too low, the output item remains visible in the Crafting Interface with a clear red Insufficient Skill Level message in its tooltip, and the output slot is locked so you cannot take or hotkey the item.

### Storage Heart Station Whitelist
- Restructured Inventory: The Storage Heart now only accepts supported crafting stations (such as Crafting Tables, Furnaces, Anvils, Brewing Stands, Condensers, Retorts, Disenchantment Tables, and Reforging Stations). Other non-station items can no longer be placed inside the Heart.
