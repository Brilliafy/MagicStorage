package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.reference.ModBlocksRef;
import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BountifulBaublesRecipeCategory implements IRecipeCategory<BountifulBaublesRecipeCategory.BaubleReforgeJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".bauble_reforge";
    private final IDrawable background;
    private final IDrawable icon;

    public BountifulBaublesRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(160, 50);
        icon = helper.createDrawableIngredient(new ItemStack(ModBlocksRef.CRAFTING_ACCESS));
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return "Magic Storage Bauble Reforge"; }
    @Override public String getModName() { return ModInfo.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, BaubleReforgeJEIRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        gui.init(0, true, 10, 15);   // bauble
        gui.init(1, false, 60, 15);  // required station: bauble reforger
        gui.init(2, false, 115, 15); // reforged bauble

        gui.set(0, ingredients.getInputs(ItemStack.class).get(0));

        Block reforgerBlock = Block.getBlockFromName("bountifulbaubles:reforger");
        ItemStack stationStack = (reforgerBlock != null) ? new ItemStack(reforgerBlock) : new ItemStack(net.minecraft.init.Blocks.ANVIL);
        gui.set(1, stationStack);
        gui.set(2, ingredients.getOutputs(ItemStack.class).get(0));

        gui.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (slotIndex == 1) {
                tooltip.add(TextFormatting.GOLD + "Requires Bauble Reforger in Storage Heart");
                tooltip.add(TextFormatting.GRAY + "Place a Bauble Reforger inside the Storage Heart to craft.");
            }
        });
    }

    public static class BaubleReforgeJEIRecipe implements IRecipeWrapper {
        private final ItemStack inputBauble;
        private final ItemStack outputBauble;

        public BaubleReforgeJEIRecipe(ItemStack inputBauble, ItemStack outputBauble) {
            this.inputBauble = inputBauble;
            this.outputBauble = outputBauble;
        }

        public ItemStack getInputBauble() {
            return inputBauble;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            ingredients.setInputs(ItemStack.class, Collections.singletonList(inputBauble));
            ingredients.setOutput(ItemStack.class, outputBauble);
        }
    }

    public static List<BaubleReforgeJEIRecipe> generateAllRecipes() {
        List<BaubleReforgeJEIRecipe> recipes = new ArrayList<>();
        if (!Loader.isModLoaded("bountifulbaubles")) return recipes;

        for (Item item : Item.REGISTRY) {
            if (item == null) continue;
            ItemStack stack = new ItemStack(item);
            if (BountifulBaublesCraftingHelper.isBauble(stack)) {
                recipes.add(new BaubleReforgeJEIRecipe(stack, stack.copy()));
            }
        }
        return recipes;
    }
}
