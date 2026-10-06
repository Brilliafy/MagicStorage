package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.reference.ModBlocksRef;
import com.brilliafy.magicstorage.util.RusticCraftingHelper;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeRegistryPlugin;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MagicStorageJEIRegistryPlugin implements IRecipeRegistryPlugin {

    private final List<AnvilRecipeCategory.AnvilJEIRecipe> allAnvilBaselineRecipes;
    private final List<EnchantingRecipeCategory.EnchantingJEIRecipe> allEnchantingBaselineRecipes;
    private final List<RusticBrewingRecipeCategory.RusticBrewingJEIRecipe> allBrewingRecipes;
    private final List<RusticCrushingRecipeCategory.RusticCrushingJEIRecipe> allCrushingRecipes;
    private final List<DisenchanterRecipeCategory.DisenchanterJEIRecipe> allDisenchanterRecipes;

    public MagicStorageJEIRegistryPlugin(
            List<AnvilRecipeCategory.AnvilJEIRecipe> allAnvilBaselineRecipes,
            List<EnchantingRecipeCategory.EnchantingJEIRecipe> allEnchantingBaselineRecipes,
            List<RusticBrewingRecipeCategory.RusticBrewingJEIRecipe> allBrewingRecipes,
            List<RusticCrushingRecipeCategory.RusticCrushingJEIRecipe> allCrushingRecipes,
            List<DisenchanterRecipeCategory.DisenchanterJEIRecipe> allDisenchanterRecipes) {
        this.allAnvilBaselineRecipes = allAnvilBaselineRecipes != null ? allAnvilBaselineRecipes : Collections.emptyList();
        this.allEnchantingBaselineRecipes = allEnchantingBaselineRecipes != null ? allEnchantingBaselineRecipes : Collections.emptyList();
        this.allBrewingRecipes = allBrewingRecipes != null ? allBrewingRecipes : Collections.emptyList();
        this.allCrushingRecipes = allCrushingRecipes != null ? allCrushingRecipes : Collections.emptyList();
        this.allDisenchanterRecipes = allDisenchanterRecipes != null ? allDisenchanterRecipes : Collections.emptyList();
    }

    @Override
    public <V> List<String> getRecipeCategoryUids(IFocus<V> focus) {
        if (focus == null || focus.getValue() == null) return Collections.emptyList();
        if (!(focus.getValue() instanceof ItemStack)) return Collections.emptyList();

        ItemStack stack = (ItemStack) focus.getValue();
        if (stack.isEmpty()) return Collections.emptyList();

        List<String> categories = new ArrayList<>();

        if (focus.getMode() == IFocus.Mode.INPUT) {
            // Crafting access catalyst: show all categories
            if (stack.getItem() == Item.getItemFromBlock(ModBlocksRef.CRAFTING_ACCESS)) {
                categories.add(AnvilRecipeCategory.UID);
                categories.add(EnchantingRecipeCategory.UID);
                if (Loader.isModLoaded("disenchanter")) categories.add(DisenchanterRecipeCategory.UID);
                if (Loader.isModLoaded("rustic")) {
                    categories.add(RusticBrewingRecipeCategory.UID);
                    categories.add(RusticCrushingRecipeCategory.UID);
                }
                return categories;
            }

            // 1. Anvil Table input
            if (AnvilRecipeCategory.isAnvilInput(stack)) {
                categories.add(AnvilRecipeCategory.UID);
            }

            // 2. Enchanting Table input
            if (EnchantingRecipeCategory.isEnchantingInput(stack)) {
                categories.add(EnchantingRecipeCategory.UID);
            }

            // 3. Disenchanter Table input (Works for any enchanted item or regular book)
            if (Loader.isModLoaded("disenchanter")) {
                if (stack.getItem() == Items.BOOK || !net.minecraft.enchantment.EnchantmentHelper.getEnchantments(stack).isEmpty()) {
                    categories.add(DisenchanterRecipeCategory.UID);
                }
            }

            // 4. Rustic Brewing input
            if (Loader.isModLoaded("rustic")) {
                if (stack.getItem() == Items.BLAZE_POWDER) {
                    categories.add(RusticBrewingRecipeCategory.UID);
                } else {
                    String fluidName = RusticCraftingHelper.getFluidName(stack);
                    if (!fluidName.isEmpty()) {
                        for (RusticBrewingRecipeCategory.RusticBrewingJEIRecipe recipe : allBrewingRecipes) {
                            if (fluidName.equals(recipe.getInputFluidName()) || fluidName.equals(recipe.getModifierFluidName())) {
                                categories.add(RusticBrewingRecipeCategory.UID);
                                break;
                            }
                        }
                    }
                }

                // 5. Rustic Crushing input
                for (RusticCrushingRecipeCategory.RusticCrushingJEIRecipe recipe : allCrushingRecipes) {
                    if (OreDictionary.itemMatches(recipe.getInputFruit(), stack, false)) {
                        categories.add(RusticCrushingRecipeCategory.UID);
                        break;
                    }
                }
            }
        } else if (focus.getMode() == IFocus.Mode.OUTPUT) {
            // 1. Anvil Table output
            if (AnvilRecipeCategory.isAnvilOutput(stack)) {
                categories.add(AnvilRecipeCategory.UID);
            }

            // 2. Enchanting Table output
            if (EnchantingRecipeCategory.isEnchantingOutput(stack)) {
                categories.add(EnchantingRecipeCategory.UID);
            }

            // 3. Disenchanter Table output
            if (Loader.isModLoaded("disenchanter")) {
                if (stack.getItem() == Items.ENCHANTED_BOOK) {
                    categories.add(DisenchanterRecipeCategory.UID);
                }
            }

            // 4. Rustic Brewing & Crushing output
            if (Loader.isModLoaded("rustic")) {
                String fluidName = RusticCraftingHelper.getFluidName(stack);
                if (!fluidName.isEmpty()) {
                    for (RusticBrewingRecipeCategory.RusticBrewingJEIRecipe recipe : allBrewingRecipes) {
                        if (fluidName.equals(recipe.getOutputFluidName())) {
                            categories.add(RusticBrewingRecipeCategory.UID);
                            break;
                        }
                    }
                    for (RusticCrushingRecipeCategory.RusticCrushingJEIRecipe recipe : allCrushingRecipes) {
                        if (fluidName.equals(recipe.getOutputFluidName())) {
                            categories.add(RusticCrushingRecipeCategory.UID);
                            break;
                        }
                    }
                }
            }
        }

        return categories;
    }

    @Override
    public <T extends IRecipeWrapper, V> List<T> getRecipeWrappers(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
        String uid = recipeCategory.getUid();
        if (focus == null || !(focus.getValue() instanceof ItemStack)) {
            return getRecipeWrappers(recipeCategory);
        }

        ItemStack stack = (ItemStack) focus.getValue();
        if (stack.isEmpty()) return Collections.emptyList();

        // 1. Anvil Category (Dynamic Real-Time Resolution)
        if (AnvilRecipeCategory.UID.equals(uid)) {
            if (stack.getItem() == Item.getItemFromBlock(ModBlocksRef.CRAFTING_ACCESS)) {
                @SuppressWarnings("unchecked")
                List<T> result = (List<T>) allAnvilBaselineRecipes;
                return result;
            }
            if (focus.getMode() == IFocus.Mode.INPUT) {
                List<AnvilRecipeCategory.AnvilJEIRecipe> dynamic = AnvilRecipeCategory.getCachedRecipesForInput(stack, allAnvilBaselineRecipes);
                @SuppressWarnings("unchecked")
                List<T> result = (List<T>) dynamic;
                return result;
            } else if (focus.getMode() == IFocus.Mode.OUTPUT) {
                List<AnvilRecipeCategory.AnvilJEIRecipe> dynamic = AnvilRecipeCategory.getCachedRecipesForOutput(stack);
                @SuppressWarnings("unchecked")
                List<T> result = (List<T>) dynamic;
                return result;
            }
        }

        // 2. Enchanting Category (Dynamic Real-Time Resolution)
        if (EnchantingRecipeCategory.UID.equals(uid)) {
            if (stack.getItem() == Item.getItemFromBlock(ModBlocksRef.CRAFTING_ACCESS)) {
                @SuppressWarnings("unchecked")
                List<T> result = (List<T>) allEnchantingBaselineRecipes;
                return result;
            }
            if (focus.getMode() == IFocus.Mode.INPUT) {
                List<EnchantingRecipeCategory.EnchantingJEIRecipe> dynamic = EnchantingRecipeCategory.getCachedRecipesForInput(stack, allEnchantingBaselineRecipes);
                @SuppressWarnings("unchecked")
                List<T> result = (List<T>) dynamic;
                return result;
            } else if (focus.getMode() == IFocus.Mode.OUTPUT) {
                List<EnchantingRecipeCategory.EnchantingJEIRecipe> dynamic = EnchantingRecipeCategory.getCachedRecipesForOutput(stack);
                @SuppressWarnings("unchecked")
                List<T> result = (List<T>) dynamic;
                return result;
            }
        }

        // 3. Disenchanter Category
        if (DisenchanterRecipeCategory.UID.equals(uid) && Loader.isModLoaded("disenchanter")) {
            if (focus.getMode() == IFocus.Mode.INPUT) {
                if (stack.getItem() == Items.BOOK) {
                    @SuppressWarnings("unchecked")
                    List<T> result = (List<T>) allDisenchanterRecipes;
                    return result;
                }
                if (!net.minecraft.enchantment.EnchantmentHelper.getEnchantments(stack).isEmpty()) {
                    boolean isBulk = DisenchanterRecipeCategory.isCurrentTableBulk();
                    ItemStack outputBook = DisenchanterRecipeCategory.createOutputBook(stack, isBulk);
                    if (!outputBook.isEmpty()) {
                        DisenchanterRecipeCategory.DisenchanterJEIRecipe dynamicRecipe =
                            new DisenchanterRecipeCategory.DisenchanterJEIRecipe(stack, new ItemStack(Items.BOOK), outputBook);
                        @SuppressWarnings("unchecked")
                        T wrapper = (T) dynamicRecipe;
                        return Collections.singletonList(wrapper);
                    }
                }
            } else if (focus.getMode() == IFocus.Mode.OUTPUT) {
                if (stack.getItem() == Items.ENCHANTED_BOOK) {
                    java.util.Map<net.minecraft.enchantment.Enchantment, Integer> focusEnchs = net.minecraft.enchantment.EnchantmentHelper.getEnchantments(stack);
                    List<T> matched = new ArrayList<>();
                    for (DisenchanterRecipeCategory.DisenchanterJEIRecipe recipe : allDisenchanterRecipes) {
                        ItemStack out = recipe.getOutputBook();
                        if (!out.isEmpty()) {
                            java.util.Map<net.minecraft.enchantment.Enchantment, Integer> outEnchs = net.minecraft.enchantment.EnchantmentHelper.getEnchantments(out);
                            for (net.minecraft.enchantment.Enchantment fe : focusEnchs.keySet()) {
                                if (outEnchs.containsKey(fe)) {
                                    @SuppressWarnings("unchecked")
                                    T wrapper = (T) recipe;
                                    matched.add(wrapper);
                                    break;
                                }
                            }
                        }
                    }
                    return matched;
                }
            }
        }

        // 4. Rustic Brewing Category
        if (RusticBrewingRecipeCategory.UID.equals(uid) && Loader.isModLoaded("rustic")) {
            List<T> matched = new ArrayList<>();
            if (focus.getMode() == IFocus.Mode.INPUT) {
                if (stack.getItem() == Items.BLAZE_POWDER) {
                    @SuppressWarnings("unchecked")
                    List<T> result = (List<T>) allBrewingRecipes;
                    return result;
                }
                String fluidName = RusticCraftingHelper.getFluidName(stack);
                if (!fluidName.isEmpty()) {
                    for (RusticBrewingRecipeCategory.RusticBrewingJEIRecipe recipe : allBrewingRecipes) {
                        if (fluidName.equals(recipe.getInputFluidName())) {
                            @SuppressWarnings("unchecked")
                            T wrapper = (T) recipe;
                            matched.add(wrapper);
                        } else if (fluidName.equals(recipe.getModifierFluidName()) && recipe.hasModifier()) {
                            RusticBrewingRecipeCategory.RusticBrewingJEIRecipe dynamicRecipe =
                                new RusticBrewingRecipeCategory.RusticBrewingJEIRecipe(
                                    recipe.inputJuice,
                                    stack.copy(),
                                    recipe.blazePowder,
                                    recipe.outputBooze
                                );
                            @SuppressWarnings("unchecked")
                            T wrapper = (T) dynamicRecipe;
                            matched.add(wrapper);
                        }
                    }
                }
            } else if (focus.getMode() == IFocus.Mode.OUTPUT) {
                String fluidName = RusticCraftingHelper.getFluidName(stack);
                if (!fluidName.isEmpty()) {
                    for (RusticBrewingRecipeCategory.RusticBrewingJEIRecipe recipe : allBrewingRecipes) {
                        if (fluidName.equals(recipe.getOutputFluidName())) {
                            @SuppressWarnings("unchecked")
                            T wrapper = (T) recipe;
                            matched.add(wrapper);
                        }
                    }
                }
            }
            return matched;
        }

        // 5. Rustic Crushing Category
        if (RusticCrushingRecipeCategory.UID.equals(uid) && Loader.isModLoaded("rustic")) {
            List<T> matched = new ArrayList<>();
            if (focus.getMode() == IFocus.Mode.INPUT) {
                for (RusticCrushingRecipeCategory.RusticCrushingJEIRecipe recipe : allCrushingRecipes) {
                    if (OreDictionary.itemMatches(recipe.getInputFruit(), stack, false)) {
                        @SuppressWarnings("unchecked")
                        T wrapper = (T) recipe;
                        matched.add(wrapper);
                    }
                }
            } else if (focus.getMode() == IFocus.Mode.OUTPUT) {
                String fluidName = RusticCraftingHelper.getFluidName(stack);
                if (!fluidName.isEmpty()) {
                    for (RusticCrushingRecipeCategory.RusticCrushingJEIRecipe recipe : allCrushingRecipes) {
                        if (fluidName.equals(recipe.getOutputFluidName())) {
                            @SuppressWarnings("unchecked")
                            T wrapper = (T) recipe;
                            matched.add(wrapper);
                        }
                    }
                }
            }
            return matched;
        }

        return Collections.emptyList();
    }

    @Override
    public <T extends IRecipeWrapper> List<T> getRecipeWrappers(IRecipeCategory<T> recipeCategory) {
        String uid = recipeCategory.getUid();
        if (AnvilRecipeCategory.UID.equals(uid)) {
            @SuppressWarnings("unchecked")
            List<T> result = (List<T>) allAnvilBaselineRecipes;
            return result;
        }
        if (EnchantingRecipeCategory.UID.equals(uid)) {
            @SuppressWarnings("unchecked")
            List<T> result = (List<T>) allEnchantingBaselineRecipes;
            return result;
        }
        if (DisenchanterRecipeCategory.UID.equals(uid) && Loader.isModLoaded("disenchanter")) {
            @SuppressWarnings("unchecked")
            List<T> result = (List<T>) allDisenchanterRecipes;
            return result;
        }
        if (RusticBrewingRecipeCategory.UID.equals(uid)) {
            @SuppressWarnings("unchecked")
            List<T> result = (List<T>) allBrewingRecipes;
            return result;
        }
        if (RusticCrushingRecipeCategory.UID.equals(uid)) {
            @SuppressWarnings("unchecked")
            List<T> result = (List<T>) allCrushingRecipes;
            return result;
        }
        return Collections.emptyList();
    }
}
