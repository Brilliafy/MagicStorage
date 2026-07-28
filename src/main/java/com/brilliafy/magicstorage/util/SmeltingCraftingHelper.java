package com.brilliafy.magicstorage.util;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Furnace smelting in the crafting grid.
 * Requires a furnace in the Storage Heart.
 *
 * Grid layout:
 * [fuel in center slot 4 (coal/charcoal)]
 * [smeltable items in the 8 surrounding slots 0-3,5-8]
 */
public class SmeltingCraftingHelper {

    public static boolean isFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Explicit charcoal check
        if (stack.getItem() == net.minecraft.init.Items.COAL && stack.getMetadata() == 1) return true;
        try {
            int burnTime = net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(stack);
            return burnTime > 0;
        } catch (Exception e) {
            return stack.getItem() == net.minecraft.init.Items.COAL;
        }
    }

    public static boolean isSmeltable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        try {
            ItemStack result = FurnaceRecipes.instance().getSmeltingResult(stack);
            return !result.isEmpty();
        } catch (Exception e) {
            try {
                java.lang.reflect.Method m = FurnaceRecipes.class.getMethod("getSmeltingResult", ItemStack.class);
                ItemStack result = (ItemStack) m.invoke(FurnaceRecipes.instance(), stack);
                return !result.isEmpty();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    public static ItemStack getSmeltedResult(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try {
            ItemStack result = FurnaceRecipes.instance().getSmeltingResult(stack);
            if (!result.isEmpty()) {
                result = result.copy();
                result.setCount(stack.getCount());
                return result;
            }
        } catch (Exception e) {}
        return ItemStack.EMPTY;
    }

    public static boolean canCraft(ItemStack[] matrix) {
        if (matrix.length < 9) return false;
        if (!isFuel(matrix[4])) return false;
        boolean hasSmeltable = false;
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            if (!matrix[i].isEmpty()) {
                if (isSmeltable(matrix[i])) hasSmeltable = true;
                else return false;
            }
        }
        return hasSmeltable;
    }

    public static ItemStack computeResult(ItemStack[] matrix) {
        if (!canCraft(matrix)) return ItemStack.EMPTY;
        ItemStack firstResult = ItemStack.EMPTY;
        int slotCount = 0;
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            ItemStack slot = matrix[i];
            if (slot.isEmpty()) continue;
            ItemStack smelted = getSmeltedResult(slot);
            if (smelted.isEmpty()) return ItemStack.EMPTY;
            if (firstResult.isEmpty()) {
                firstResult = smelted.copy();
                slotCount++;
            } else if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(firstResult, smelted)) {
                slotCount++;
            }
        }
        if (firstResult.isEmpty()) return ItemStack.EMPTY;
        firstResult.setCount(Math.min(slotCount, firstResult.getMaxStackSize()));
        return firstResult;
    }

    public static void consumeIngredients(ItemStack[] matrix) {
        if (!matrix[4].isEmpty()) matrix[4].shrink(1);
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            if (!matrix[i].isEmpty()) matrix[i].shrink(1);
        }
    }

    public static int getSmeltableCount(ItemStack[] matrix) {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            if (!matrix[i].isEmpty() && isSmeltable(matrix[i])) {
                count++;  // Count occupied slots, not item quantities
            }
        }
        return count;
    }

    public static List<SmeltingRecipe> generateAllRecipes() {
        List<SmeltingRecipe> recipes = new ArrayList<>();
        try {
            java.lang.reflect.Field recipesField = null;
            for (java.lang.reflect.Field f : FurnaceRecipes.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    recipesField = f;
                    recipesField.setAccessible(true);
                    break;
                }
            }
            if (recipesField != null) {
                Map<ItemStack, ItemStack> recipeMap = (Map<ItemStack, ItemStack>) recipesField.get(FurnaceRecipes.instance());
                for (Map.Entry<ItemStack, ItemStack> entry : recipeMap.entrySet()) {
                    ItemStack input = entry.getKey().copy();
                    ItemStack output = entry.getValue().copy();
                    if (!input.isEmpty() && !output.isEmpty()) {
                        recipes.add(new SmeltingRecipe(input, new ItemStack(net.minecraft.init.Items.COAL), output));
                    }
                }
            }
        } catch (Exception e) { }
        if (recipes.isEmpty()) addCommonRecipes(recipes);
        return recipes;
    }

    private static void addCommonRecipes(List<SmeltingRecipe> recipes) {
        add(recipes, net.minecraft.init.Blocks.IRON_ORE, net.minecraft.init.Items.IRON_INGOT);
        add(recipes, net.minecraft.init.Blocks.GOLD_ORE, net.minecraft.init.Items.GOLD_INGOT);
        add(recipes, net.minecraft.init.Blocks.DIAMOND_ORE, net.minecraft.init.Items.DIAMOND);
        add(recipes, net.minecraft.init.Blocks.EMERALD_ORE, net.minecraft.init.Items.EMERALD);
        add(recipes, net.minecraft.init.Blocks.REDSTONE_ORE, net.minecraft.init.Items.REDSTONE);
        add(recipes, net.minecraft.init.Blocks.COAL_ORE, net.minecraft.init.Items.COAL);
        add(recipes, net.minecraft.init.Blocks.QUARTZ_ORE, net.minecraft.init.Items.QUARTZ);
        add(recipes, net.minecraft.init.Blocks.NETHERRACK, net.minecraft.init.Items.NETHERBRICK);
        add(recipes, net.minecraft.init.Blocks.SAND, net.minecraft.init.Blocks.GLASS);
        add(recipes, net.minecraft.init.Blocks.COBBLESTONE, net.minecraft.init.Blocks.STONE);
        add(recipes, net.minecraft.init.Blocks.CLAY, net.minecraft.init.Items.BRICK);
        add(recipes, net.minecraft.init.Blocks.LOG, new ItemStack(net.minecraft.init.Items.COAL, 1, 1));
        add(recipes, net.minecraft.init.Items.BEEF, net.minecraft.init.Items.COOKED_BEEF);
        add(recipes, net.minecraft.init.Items.PORKCHOP, net.minecraft.init.Items.COOKED_PORKCHOP);
        add(recipes, net.minecraft.init.Items.CHICKEN, net.minecraft.init.Items.COOKED_CHICKEN);
        add(recipes, net.minecraft.init.Items.FISH, net.minecraft.init.Items.COOKED_FISH);
        add(recipes, net.minecraft.init.Items.POTATO, net.minecraft.init.Items.BAKED_POTATO);
    }

    private static void add(List<SmeltingRecipe> r, net.minecraft.block.Block in, net.minecraft.item.Item out) {
        r.add(new SmeltingRecipe(new ItemStack(in), new ItemStack(net.minecraft.init.Items.COAL), new ItemStack(out)));
    }
    private static void add(List<SmeltingRecipe> r, net.minecraft.block.Block in, net.minecraft.block.Block out) {
        r.add(new SmeltingRecipe(new ItemStack(in), new ItemStack(net.minecraft.init.Items.COAL), new ItemStack(out)));
    }
    private static void add(List<SmeltingRecipe> r, net.minecraft.item.Item in, net.minecraft.item.Item out) {
        r.add(new SmeltingRecipe(new ItemStack(in), new ItemStack(net.minecraft.init.Items.COAL), new ItemStack(out)));
    }
    private static void add(List<SmeltingRecipe> r, net.minecraft.item.Item in, ItemStack out) {
        r.add(new SmeltingRecipe(new ItemStack(in), new ItemStack(net.minecraft.init.Items.COAL), out));
    }

    public static class SmeltingRecipe {
        public final ItemStack input, fuel, output;
        public SmeltingRecipe(ItemStack input, ItemStack fuel, ItemStack output) {
            this.input = input; this.fuel = fuel; this.output = output;
        }
    }
    private static void add(List<SmeltingRecipe> r, net.minecraft.block.Block in, ItemStack out) {
        r.add(new SmeltingRecipe(new ItemStack(in), new ItemStack(net.minecraft.init.Items.COAL), out));
    }
}
