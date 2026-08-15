package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.util.RusticCraftingHelper;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeRegistryPlugin;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MagicStorageJEIRegistryPlugin implements IRecipeRegistryPlugin {

    private final List<EnchantingRecipeCategory.EnchantingJEIRecipe> allEnchantingRecipes;
    private final List<RusticBrewingRecipeCategory.RusticBrewingJEIRecipe> allBrewingRecipes;
    private final List<RusticCrushingRecipeCategory.RusticCrushingJEIRecipe> allCrushingRecipes;
    private final List<DisenchanterRecipeCategory.DisenchanterJEIRecipe> allDisenchanterRecipes;

    public MagicStorageJEIRegistryPlugin(
            List<EnchantingRecipeCategory.EnchantingJEIRecipe> allEnchantingRecipes,
            List<RusticBrewingRecipeCategory.RusticBrewingJEIRecipe> allBrewingRecipes,
            List<RusticCrushingRecipeCategory.RusticCrushingJEIRecipe> allCrushingRecipes,
            List<DisenchanterRecipeCategory.DisenchanterJEIRecipe> allDisenchanterRecipes) {
        this.allEnchantingRecipes = allEnchantingRecipes != null ? allEnchantingRecipes : Collections.emptyList();
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
            // 1. Enchanting Table input
            if (!stack.isItemEnchanted() && stack.getItem() != Items.ENCHANTED_BOOK) {
                if (stack.getItem() == Items.DYE && stack.getMetadata() == 4) {
                    categories.add(EnchantingRecipeCategory.UID);
                } else if (stack.getItem() == Items.BOOK || stack.isItemStackDamageable() || stack.getItem() instanceof net.minecraft.item.ItemSword || stack.getItem() instanceof net.minecraft.item.ItemTool || stack.getItem() instanceof net.minecraft.item.ItemArmor || stack.getItem() instanceof net.minecraft.item.ItemBow) {
                    for (EnchantingRecipeCategory.EnchantingJEIRecipe recipe : allEnchantingRecipes) {
                        if (recipe.matchesInput(stack)) {
                            categories.add(EnchantingRecipeCategory.UID);
                            break;
                        }
                    }
                }
            }

            // 2. Disenchanter Table input (Works for any enchanted item with 1, 2, 5+ enchantments, or regular books)
            if (Loader.isModLoaded("disenchanter")) {
                if (stack.getItem() == Items.BOOK || !net.minecraft.enchantment.EnchantmentHelper.getEnchantments(stack).isEmpty()) {
                    categories.add(DisenchanterRecipeCategory.UID);
                }
            }

            // 3. Rustic Brewing input
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

                // 4. Rustic Crushing input
                for (RusticCrushingRecipeCategory.RusticCrushingJEIRecipe recipe : allCrushingRecipes) {
                    if (OreDictionary.itemMatches(recipe.getInputFruit(), stack, false)) {
                        categories.add(RusticCrushingRecipeCategory.UID);
                        break;
                    }
                }
            }
        } else if (focus.getMode() == IFocus.Mode.OUTPUT) {
            // 1. Enchanting Table output
            if (stack.isItemEnchanted() || stack.getItem() == Items.ENCHANTED_BOOK) {
                for (EnchantingRecipeCategory.EnchantingJEIRecipe recipe : allEnchantingRecipes) {
                    if (recipe.matchesOutput(stack)) {
                        categories.add(EnchantingRecipeCategory.UID);
                        break;
                    }
                }
            }

            // 2. Disenchanter Table output
            if (Loader.isModLoaded("disenchanter")) {
                if (stack.getItem() == Items.ENCHANTED_BOOK) {
                    categories.add(DisenchanterRecipeCategory.UID);
                }
            }

            // 3. Rustic Brewing output
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

        List<T> matched = new ArrayList<>();

        if (EnchantingRecipeCategory.UID.equals(uid)) {
            if (focus.getMode() == IFocus.Mode.INPUT) {
                if (stack.isItemEnchanted() || stack.getItem() == Items.ENCHANTED_BOOK) {
                    return Collections.emptyList();
                }
                boolean isLapis = stack.getItem() == Items.DYE && stack.getMetadata() == 4;
                for (EnchantingRecipeCategory.EnchantingJEIRecipe recipe : allEnchantingRecipes) {
                    if (isLapis || recipe.matchesInput(stack)) {
                        @SuppressWarnings("unchecked")
                        T wrapper = (T) recipe;
                        matched.add(wrapper);
                    }
                }
            } else if (focus.getMode() == IFocus.Mode.OUTPUT) {
                for (EnchantingRecipeCategory.EnchantingJEIRecipe recipe : allEnchantingRecipes) {
                    if (recipe.matchesOutput(stack)) {
                        @SuppressWarnings("unchecked")
                        T wrapper = (T) recipe;
                        matched.add(wrapper);
                    }
                }
            }
        } else if (DisenchanterRecipeCategory.UID.equals(uid) && Loader.isModLoaded("disenchanter")) {
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
                }
            }
        } else if (RusticBrewingRecipeCategory.UID.equals(uid)) {
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
                            // Create dynamic recipe with the EXACT focused bottle (matching its exact quality)
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
        } else if (RusticCrushingRecipeCategory.UID.equals(uid)) {
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
        }

        return matched;
    }

    @Override
    public <T extends IRecipeWrapper> List<T> getRecipeWrappers(IRecipeCategory<T> recipeCategory) {
        String uid = recipeCategory.getUid();
        if (EnchantingRecipeCategory.UID.equals(uid)) {
            @SuppressWarnings("unchecked")
            List<T> result = (List<T>) allEnchantingRecipes;
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
