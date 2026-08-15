# Configuration Reference

The configuration file is generated at `.minecraft/config/magicstorage.cfg` on first launch.

---

## 1. Configuration File Structure

```hocon
# Storage network sorting options
sorting {
    # When enabled, items are automatically consolidated into storage units that already contain matching items with space. Does not move items to new slots.
    B:softAutoSortEnabled=true

    # How often in seconds the soft auto-sort runs to consolidate storage units (10 - 3600).
    I:softAutoSortIntervalSeconds=60
}

# Crafting station recipe toggles (Crafting Table is always enabled)
stations {
    B:enableFurnace=true
    B:enableBrewingStand=true
    B:enableAnvil=true
    B:enableEnchantingTable=true
    B:enableDisenchanterTable=true
    B:enableBountifulBaublesReforger=true
    B:enableQualityToolsReforger=true
    B:enableRusticAlchemy=true
    B:enableRusticBrewing=true
    B:enableRusticCrushing=true
}
```

---

## 2. Option Details

| Category | Property Key | Type | Default | Range / Description |
| :--- | :--- | :---: | :---: | :--- |
| `sorting` | `softAutoSortEnabled` | Boolean | `true` | Enables background consolidation of matching item stacks across connected storage units. |
| `sorting` | `softAutoSortIntervalSeconds` | Integer | `60` | Interval in seconds between background inventory consolidation sweeps (min: 10, max: 3600). |
| `stations` | `enableFurnace` | Boolean | `true` | Enables furnace smelting in the Crafting Access when a Furnace is in the Heart. |
| `stations` | `enableBrewingStand` | Boolean | `true` | Enables potion brewing in the Crafting Access when a Brewing Stand is in the Heart. |
| `stations` | `enableAnvil` | Boolean | `true` | Enables tool repair and enchantment combining when an Anvil is in the Heart. |
| `stations` | `enableEnchantingTable` | Boolean | `true` | Enables enchanting in the Crafting Access when an Enchanting Table is in the Heart. |
| `stations` | `enableDisenchanterTable` | Boolean | `true` | Enables enchantment extraction when a Disenchantment Table is in the Heart. |
| `stations` | `enableBountifulBaublesReforger` | Boolean | `true` | Enables bauble modifier reforging when a Bountiful Baubles Reforger is in the Heart. |
| `stations` | `enableQualityToolsReforger` | Boolean | `true` | Enables item quality reforging when a Quality Tools Reforging Station is in the Heart. |
| `stations` | `enableRusticAlchemy` | Boolean | `true` | Enables Rustic alchemy elixir crafting when Condensers and Retorts are in the Heart. |
| `stations` | `enableRusticBrewing` | Boolean | `true` | Enables Rustic alcohol and juice brewing when a Brewing Barrel is in the Heart. |
| `stations` | `enableRusticCrushing` | Boolean | `true` | Enables Rustic fruit crushing when a Crushing Tub is in the Heart. |
