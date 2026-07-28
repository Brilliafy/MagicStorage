package com.brilliafy.magicstorage.util;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Potion/brewing crafting using BrewingRecipeRegistry (Forge API, handles
 * all modded potions via registered IBrewingRecipe).
 *
 * Grid layout (0-indexed):
 *   Slot 0: blaze powder (fuel, required, not consumed)
 *   Slot 1: brewing ingredient (required, consumed)
 *   Slots 3,4,5: potion bottles (1-3, at least one required)
 */
public class PotionCraftingHelper {

    public static boolean isAnyPotion(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() == Items.POTIONITEM
            || stack.getItem() == Items.SPLASH_POTION
            || stack.getItem() == Items.LINGERING_POTION);
    }

    /** Check if slot contents are usable for brewing */
    public static boolean canCraft(ItemStack blaze, ItemStack ingredient,
                                    ItemStack bottle3, ItemStack bottle4, ItemStack bottle5) {
        // Must have blaze powder (fuel)
        if (blaze.isEmpty() || blaze.getItem() != Items.BLAZE_POWDER) {
            com.brilliafy.magicstorage.MagicStorage.LOGGER.info("[MagicStorage] Brew canCraft: no blaze");
            return false;
        }
        // Must have ingredient
        if (ingredient.isEmpty()) {
            com.brilliafy.magicstorage.MagicStorage.LOGGER.info("[MagicStorage] Brew canCraft: no ingredient");
            return false;
        }
        // Must have at least one potion bottle
        List<ItemStack> bottles = getBottles(bottle3, bottle4, bottle5);
        if (bottles.isEmpty()) {
            com.brilliafy.magicstorage.MagicStorage.LOGGER.info("[MagicStorage] Brew canCraft: no bottles");
            return false;
        }

        // BrewingRecipeRegistry.getOutput() checks all registered IBrewingRecipe
        ItemStack testResult = BrewingRecipeRegistry.getOutput(bottles.get(0), ingredient);
        com.brilliafy.magicstorage.MagicStorage.LOGGER.info("[MagicStorage] Brew canCraft: bottle=" + bottles.get(0).getDisplayName() + " ing=" + ingredient.getDisplayName() + " result=" + (testResult.isEmpty() ? "EMPTY" : testResult.getDisplayName()));
        return !testResult.isEmpty() && isAnyPotion(testResult);
    }

    public static List<ItemStack> getBottles(ItemStack s3, ItemStack s4, ItemStack s5) {
        List<ItemStack> bottles = new ArrayList<>();
        if (isAnyPotion(s3)) bottles.add(s3);
        if (isAnyPotion(s4)) bottles.add(s4);
        if (isAnyPotion(s5)) bottles.add(s5);
        return bottles;
    }

    public static int getBottleCount(ItemStack[] matrix) {
        return getBottles(matrix[3], matrix[4], matrix[5]).size();
    }

    /**
     * Brew all bottles with the ingredient. Uses BrewingRecipeRegistry
     * (Forge API — all modded potions are registered there).
     */
    public static List<ItemStack> computeResult(ItemStack[] matrix) {
        ItemStack ingredient = matrix[1];
        List<ItemStack> bottles = getBottles(matrix[3], matrix[4], matrix[5]);
        List<ItemStack> results = new ArrayList<>();
        ItemStack firstResult = null;
        for (ItemStack bottle : bottles) {
            ItemStack result = BrewingRecipeRegistry.getOutput(bottle.copy(), ingredient);
            if (result.isEmpty() || !isAnyPotion(result)) return new ArrayList<>();
            result.setCount(1);
            // Stack identical results together so 2 water bottles → result qty 2
            if (firstResult == null) {
                firstResult = result;
                results.add(result);
            } else if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(firstResult, result)) {
                firstResult.grow(1);
            } else {
                results.add(result);
            }
        }
        return results;
    }

    /**
     * Consume the ingredient (1), each potion bottle (1 each), and
     * blaze powder with 5% chance per brew operation.
     */
    public static void consumeIngredients(ItemStack[] matrix, java.util.Random rand) {
        // Consume ingredient (slot 1)
        if (!matrix[1].isEmpty()) matrix[1].shrink(1);
        // Consume each bottle (slots 3,4,5)
        for (int i = 3; i <= 5; i++) {
            if (!matrix[i].isEmpty() && isAnyPotion(matrix[i])) {
                matrix[i].shrink(1);
            }
        }
        // 5% chance to consume blaze powder (slot 0)
        if (rand != null && rand.nextFloat() < 0.05f && !matrix[0].isEmpty()) {
            matrix[0].shrink(1);
        }
    }
}
