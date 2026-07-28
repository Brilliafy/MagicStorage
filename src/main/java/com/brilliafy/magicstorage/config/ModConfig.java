package com.brilliafy.magicstorage.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ModConfig {

    private static Configuration configFile;

    public static boolean softAutoSortEnabled = true;
    public static int softAutoSortIntervalSeconds = 60;

    public static void load(FMLPreInitializationEvent event) {
        configFile = new Configuration(event.getSuggestedConfigurationFile());
        configFile.load();
        syncConfig();
    }

    private static void syncConfig() {
        String CATEGORY_SORTING = "sorting";

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

        if (configFile.hasChanged()) {
            configFile.save();
        }
    }
}
