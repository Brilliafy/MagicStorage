package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.util.SmeltingCraftingHelper;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SmeltingRecipeCategory implements IRecipeCategory<SmeltingRecipeCategory.SmeltingJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".smelting";
    private final IDrawable background;
    private final IDrawable icon;

    public SmeltingRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(140, 50);
        icon = helper.createDrawableIngredient(new ItemStack(Item.getItemFromBlock(Blocks.FURNACE)));
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return "Magic Storage Smelting"; }
    @Override public String getModName() { return ModInfo.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, SmeltingJEIRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        gui.init(0, true, 10, 15);   // input (smeltable item)
        gui.init(1, true, 55, 15);   // fuel (coal)
        gui.init(2, false, 100, 15); // output
        gui.set(ingredients);
    }

    public static class SmeltingJEIRecipe implements IRecipeWrapper {
        private final ItemStack input;
        private final ItemStack fuel;
        private final ItemStack output;

        public SmeltingJEIRecipe(ItemStack input, ItemStack fuel, ItemStack output) {
            this.input = input; this.fuel = fuel; this.output = output;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inputs = new ArrayList<>();
            inputs.add(Collections.singletonList(input));
            inputs.add(Collections.singletonList(fuel));
            ingredients.setInputLists(ItemStack.class, inputs);
            ingredients.setOutput(ItemStack.class, output);
        }
    }

    public static List<SmeltingJEIRecipe> generateAllRecipes() {
        List<SmeltingCraftingHelper.SmeltingRecipe> recipes = SmeltingCraftingHelper.generateAllRecipes();
        List<SmeltingJEIRecipe> jeiRecipes = new ArrayList<>();
        for (SmeltingCraftingHelper.SmeltingRecipe r : recipes) {
            jeiRecipes.add(new SmeltingJEIRecipe(r.input, r.fuel, r.output));
        }
        return jeiRecipes;
    }
}
