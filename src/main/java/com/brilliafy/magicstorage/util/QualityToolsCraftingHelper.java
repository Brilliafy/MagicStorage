package com.brilliafy.magicstorage.util;

import com.brilliafy.magicstorage.tile.TileStorageHeart;
import com.tmtravlr.qualitytools.QualityToolsHelper;
import com.tmtravlr.qualitytools.config.ConfigLoader;
import com.tmtravlr.qualitytools.config.CustomMaterial;
import com.tmtravlr.qualitytools.config.QualityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class QualityToolsCraftingHelper {

    private static boolean configLoaded = false;

    public static synchronized void ensureQualityToolsConfigLoaded() {
        if (configLoaded || !Loader.isModLoaded("qualitytools")) return;
        configLoaded = true;

        try {
            File configDir = Loader.instance().getConfigDir();
            File customReforgingFile = new File(configDir, "qualitytools/reforging materials.json");
            if (!customReforgingFile.exists()) return;

            ConfigLoader.useRepairItem = true;
            if (ConfigLoader.customReforgeMaterials == null) {
                ConfigLoader.customReforgeMaterials = com.google.common.collect.HashMultimap.create();
            }

            com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                .registerTypeAdapter(CustomMaterial.class, new CustomMaterial.Serializer())
                .create();
            com.google.gson.JsonParser parser = new com.google.gson.JsonParser();

            try (InputStream is = new FileInputStream(customReforgingFile);
                 Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                com.google.gson.JsonElement rootElement = parser.parse(reader);
                if (rootElement != null && rootElement.isJsonArray()) {
                    for (com.google.gson.JsonElement jsonElement : rootElement.getAsJsonArray()) {
                        try {
                            com.google.gson.JsonObject json = jsonElement.getAsJsonObject();
                            com.google.gson.JsonObject materialJson = json.getAsJsonObject("material");
                            if (materialJson == null) continue;
                            CustomMaterial material = gson.fromJson(materialJson, CustomMaterial.class);
                            if (material == null) continue;

                            if (json.has("tool") && json.get("tool").isJsonObject()) {
                                com.google.gson.JsonObject toolJson = json.getAsJsonObject("tool");
                                CustomMaterial tool = gson.fromJson(toolJson, CustomMaterial.class);
                                if (tool != null) {
                                    ConfigLoader.customReforgeMaterials.put(tool, material);
                                }
                            } else if (json.has("tool")) {
                                String itemName = json.get("tool").getAsString();
                                if ("any".equalsIgnoreCase(itemName)) {
                                    ConfigLoader.universalReforgeItem = material;
                                } else {
                                    Item it = Item.getByNameOrId(itemName);
                                    if (it != null) {
                                        CustomMaterial tool = new CustomMaterial();
                                        tool.item = it;
                                        tool.meta = Short.MAX_VALUE;
                                        ConfigLoader.customReforgeMaterials.put(tool, material);
                                    }
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean isQualityToolsGrid(ItemStack[] m) {
        if (m == null || m.length < 9) return false;
        if (m[4].isEmpty() || m[8].isEmpty()) return false;
        for (int i = 0; i < 9; i++) {
            if (i != 4 && i != 8 && !m[i].isEmpty()) return false;
        }
        return true;
    }

    public static boolean isReforgeable(ItemStack tool) {
        if (tool.isEmpty()) return false;
        ensureQualityToolsConfigLoaded();

        if (tool.getMaxStackSize() != 1 && (!Loader.isModLoaded("qualitytools") || !ConfigLoader.allowStackableItems)) {
            return false;
        }
        if (tool.isItemStackDamageable()) return true;
        Item item = tool.getItem();
        if (item instanceof net.minecraft.item.ItemSword || item instanceof net.minecraft.item.ItemTool || item instanceof net.minecraft.item.ItemArmor || item instanceof net.minecraft.item.ItemBow || item instanceof net.minecraft.item.ItemShield || item instanceof net.minecraft.item.ItemHoe || item instanceof net.minecraft.item.ItemShears || item instanceof net.minecraft.item.ItemFishingRod) {
            return true;
        }
        if (Loader.isModLoaded("baubles")) {
            try {
                if (item instanceof baubles.api.IBauble || tool.hasCapability(baubles.api.cap.BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null)) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        if (Loader.isModLoaded("qualitytools")) {
            try {
                if (ConfigLoader.qualityTypes != null) {
                    for (QualityType qt : ConfigLoader.qualityTypes.values()) {
                        if (qt != null && qt.itemMatches(tool)) return true;
                    }
                }
                if (ConfigLoader.customReforgeMaterials != null) {
                    for (CustomMaterial cm : ConfigLoader.customReforgeMaterials.keySet()) {
                        if (cm != null && cm.itemMatches(tool)) return true;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static boolean isReforgeMaterial(ItemStack material) {
        if (!Loader.isModLoaded("qualitytools") || material == null || material.isEmpty()) return false;
        ensureQualityToolsConfigLoaded();

        if (ConfigLoader.universalReforgeItem != null && ConfigLoader.universalReforgeItem.itemMatches(material)) {
            return true;
        }
        if (material.getItem() == net.minecraft.init.Items.NETHER_STAR) {
            return true;
        }
        if (ConfigLoader.customReforgeMaterials != null) {
            for (CustomMaterial mat : ConfigLoader.customReforgeMaterials.values()) {
                if (mat != null && mat.itemMatches(material)) return true;
            }
        }
        return false;
    }

    public static boolean canCraft(ItemStack tool, ItemStack material) {
        if (!Loader.isModLoaded("qualitytools")) return false;
        if (tool.isEmpty() || material.isEmpty()) return false;
        if (!isReforgeable(tool)) return false;

        ensureQualityToolsConfigLoaded();

        if (QualityToolsHelper.canReforgeWith(tool, material)) {
            return true;
        }
        // Fallback checks for known default materials if custom materials config has not been synced
        if (material.getItem() == net.minecraft.init.Items.NETHER_STAR) {
            return true;
        }
        String toolName = tool.getItem().getRegistryName() != null ? tool.getItem().getRegistryName().toString() : "";
        if ("qualitytools:emerald_ring".equals(toolName) || "qualitytools:emerald_amulet".equals(toolName) || "minecraft:totem_of_undying".equals(toolName)) {
            return material.getItem() == net.minecraft.init.Items.EMERALD;
        }
        if (tool.getItem() instanceof net.minecraft.item.ItemBow || tool.getItem() instanceof net.minecraft.item.ItemFishingRod || tool.getItem() instanceof net.minecraft.item.ItemHoe) {
            if (material.getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.PLANKS) ||
                material.getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.COBBLESTONE) ||
                material.getItem() == net.minecraft.init.Items.IRON_INGOT ||
                material.getItem() == net.minecraft.init.Items.GOLD_INGOT ||
                material.getItem() == net.minecraft.init.Items.DIAMOND) {
                return true;
            }
        }
        if (tool.getItem() instanceof net.minecraft.item.ItemShears) {
            return material.getItem() == net.minecraft.init.Items.IRON_INGOT;
        }
        return false;
    }

    public static ItemStack computeResult(ItemStack tool, ItemStack material, TileStorageHeart heart) {
        if (!canCraft(tool, material)) return ItemStack.EMPTY;

        ItemStack toolCopy = tool.copy();
        toolCopy.setCount(1);

        long posLong = (heart != null && heart.getPos() != null) ? heart.getPos().toLong() : 0L;
        int craftCounter = (heart != null) ? heart.getBrewingCraftCounter() : 0;
        long seed = 31L * posLong + craftCounter;

        QualityType.RAND.setSeed(seed);
        QualityToolsHelper.generateQualityTag(toolCopy, true);

        return toolCopy;
    }

    public static void consumeIngredients(ItemStack[] matrix, TileStorageHeart heart) {
        ItemStack tool = matrix[4];     // Slot 5
        ItemStack material = matrix[8]; // Slot 9

        if (tool.isEmpty() || material.isEmpty()) return;

        // Container item handling (e.g. buckets) or shrink stack by 1
        Item matItem = material.getItem();
        ItemStack container = matItem.getContainerItem(material);
        material.shrink(1);

        if (!container.isEmpty()) {
            if (material.isEmpty()) {
                matrix[8] = container;
            }
        }

        matrix[4] = ItemStack.EMPTY;

        if (heart != null) {
            heart.incrementBrewingCraftCounter();
        }
    }
}
