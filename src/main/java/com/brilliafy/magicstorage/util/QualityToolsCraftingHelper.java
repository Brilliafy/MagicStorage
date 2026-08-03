package com.brilliafy.magicstorage.util;

import com.brilliafy.magicstorage.tile.TileStorageHeart;
import com.tmtravlr.qualitytools.QualityToolsHelper;
import com.tmtravlr.qualitytools.config.QualityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

public class QualityToolsCraftingHelper {

    public static boolean canCraft(ItemStack tool, ItemStack material) {
        if (!Loader.isModLoaded("qualitytools")) return false;
        if (tool.isEmpty() || material.isEmpty()) return false;
        return QualityToolsHelper.canReforgeWith(tool, material);
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
