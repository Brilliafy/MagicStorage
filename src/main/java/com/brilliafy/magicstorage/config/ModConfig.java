package com.brilliafy.magicstorage.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ModConfig {

    private static Configuration configFile;

    public static boolean softAutoSortEnabled = true;
    public static int softAutoSortIntervalSeconds = 60;

    // Station recipe support toggles (all true by default, normal crafting table is always on)
    public static boolean enableFurnace = true;
    public static boolean enableBrewingStand = true;
    public static boolean enableAnvil = true;
    public static boolean enableEnchantingTable = true;
    public static boolean enableDisenchanterTable = true;
    public static boolean enableBountifulBaublesReforger = true;
    public static boolean enableQualityToolsReforger = true;
    public static boolean enableRusticAlchemy = true;
    public static boolean enableRusticBrewing = true;
    public static boolean enableRusticCrushing = true;

    public static void load(FMLPreInitializationEvent event) {
        configFile = new Configuration(event.getSuggestedConfigurationFile());
        configFile.load();
        syncConfig();
    }

    private static void syncConfig() {
        String CATEGORY_SORTING = "sorting";
        String CATEGORY_STATIONS = "stations";

        softAutoSortEnabled = configFile.getBoolean(
            "softAutoSortEnabled",
            CATEGORY_SORTING,
            softAutoSortEnabled,
            "When enabled, items are automatically moved into storage units that already contain matching items with available space. Does not move items to new slots — only consolidates into existing ones to save storage space."
        );

        softAutoSortIntervalSeconds = configFile.getInt(
            "softAutoSortIntervalSeconds",
            CATEGORY_SORTING,
            softAutoSortIntervalSeconds,
            10,
            3600,
            "How often in seconds the soft auto-sort runs to consolidate items across storage units."
        );

        enableFurnace = configFile.getBoolean(
            "enableFurnace",
            CATEGORY_STATIONS,
            enableFurnace,
            "Enable furnace smelting recipe support in Magic Storage."
        );

        enableBrewingStand = configFile.getBoolean(
            "enableBrewingStand",
            CATEGORY_STATIONS,
            enableBrewingStand,
            "Enable vanilla brewing stand potion brewing support in Magic Storage."
        );

        enableAnvil = configFile.getBoolean(
            "enableAnvil",
            CATEGORY_STATIONS,
            enableAnvil,
            "Enable anvil repair and naming support in Magic Storage."
        );

        enableEnchantingTable = configFile.getBoolean(
            "enableEnchantingTable",
            CATEGORY_STATIONS,
            enableEnchantingTable,
            "Enable enchanting table enchanting support in Magic Storage."
        );

        enableDisenchanterTable = configFile.getBoolean(
            "enableDisenchanterTable",
            CATEGORY_STATIONS,
            enableDisenchanterTable,
            "Enable Disenchanter table disenchanting support in Magic Storage."
        );

        enableBountifulBaublesReforger = configFile.getBoolean(
            "enableBountifulBaublesReforger",
            CATEGORY_STATIONS,
            enableBountifulBaublesReforger,
            "Enable Bountiful Baubles reforger support in Magic Storage."
        );

        enableQualityToolsReforger = configFile.getBoolean(
            "enableQualityToolsReforger",
            CATEGORY_STATIONS,
            enableQualityToolsReforger,
            "Enable Quality Tools reforging station support in Magic Storage."
        );

        enableRusticAlchemy = configFile.getBoolean(
            "enableRusticAlchemy",
            CATEGORY_STATIONS,
            enableRusticAlchemy,
            "Enable Rustic alchemy condenser and retort support in Magic Storage."
        );

        enableRusticBrewing = configFile.getBoolean(
            "enableRusticBrewing",
            CATEGORY_STATIONS,
            enableRusticBrewing,
            "Enable Rustic brewing barrel support in Magic Storage."
        );

        enableRusticCrushing = configFile.getBoolean(
            "enableRusticCrushing",
            CATEGORY_STATIONS,
            enableRusticCrushing,
            "Enable Rustic crushing tub support in Magic Storage."
        );

        if (configFile.hasChanged()) {
            configFile.save();
        }
    }
}
