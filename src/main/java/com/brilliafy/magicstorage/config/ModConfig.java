package com.brilliafy.magicstorage.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ModConfig {

    private static Configuration configFile;

    public static boolean autoSortEnabled = true;
    public static int autoSortIntervalSeconds = 60;

    public static void load(FMLPreInitializationEvent event) {
        configFile = new Configuration(event.getSuggestedConfigurationFile());
        configFile.load();
        syncConfig();
    }

    private static void syncConfig() {
        String CATEGORY_SORTING = "sorting";

        autoSortEnabled = configFile.getBoolean(
            "autoSortEnabled",
            CATEGORY_SORTING,
            autoSortEnabled,
            "When enabled, items will automatically move into storage units that already contain matching items and have available space."
        );

        autoSortIntervalSeconds = configFile.getInt(
            "autoSortIntervalSeconds",
            CATEGORY_SORTING,
            autoSortIntervalSeconds,
            10,
            3600,
            "How often in seconds the auto-sort runs to redistribute items across storage units."
        );

        if (configFile.hasChanged()) {
            configFile.save();
        }
    }
}
