package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.util.AnvilCraftingHelper;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnvilRecipeCategory implements IRecipeCategory<AnvilRecipeCategory.AnvilJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".anvil";
    private final IDrawable background;
    private final IDrawable icon;

    public AnvilRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(140, 50);
        icon = helper.createDrawableIngredient(new ItemStack(net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.ANVIL)));
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return "Magic Storage Anvil"; }
    @Override public String getModName() { return ModInfo.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, AnvilJEIRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        gui.init(0, true, 10, 15);   // input item
        gui.init(1, true, 55, 15);   // enchanted book
        gui.init(2, false, 100, 15); // output
        gui.set(ingredients);
    }

    public static class AnvilJEIRecipe implements IRecipeWrapper {
        private final ItemStack input;
        private final ItemStack book;
        private final ItemStack output;

        public AnvilJEIRecipe(ItemStack input, ItemStack book, ItemStack output) {
            this.input = input; this.book = book; this.output = output;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inputs = new ArrayList<>();
            inputs.add(Collections.singletonList(input));
            inputs.add(Collections.singletonList(book));
            ingredients.setInputLists(ItemStack.class, inputs);
            ingredients.setOutput(ItemStack.class, output);
        }
    }

    public static List<AnvilJEIRecipe> generateAllRecipes() {
        List<com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilRecipeDisplay> recipes = com.brilliafy.magicstorage.util.AnvilCraftingHelper.generateAllRecipes();
        List<AnvilJEIRecipe> jeiRecipes = new ArrayList<>();
        for (com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilRecipeDisplay r : recipes) {
            jeiRecipes.add(new AnvilJEIRecipe(r.input, r.secondary, r.output));
        }
        return jeiRecipes;
    }
}
