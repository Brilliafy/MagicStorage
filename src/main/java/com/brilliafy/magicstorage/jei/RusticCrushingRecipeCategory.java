package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.reference.ModBlocksRef;
import com.brilliafy.magicstorage.reference.ModInfo;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Loader;
import rustic.common.crafting.ICrushingTubRecipe;
import rustic.common.crafting.Recipes;
import rustic.common.items.ItemFluidBottle;
import rustic.common.items.ModItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RusticCrushingRecipeCategory implements IRecipeCategory<RusticCrushingRecipeCategory.RusticCrushingJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".rustic_crushing";
    private final IDrawable background;
    private final IDrawable icon;

    public RusticCrushingRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(160, 50);
        icon = helper.createDrawableIngredient(new ItemStack(ModBlocksRef.CRAFTING_ACCESS));
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return "Magic Storage Juices"; }
    @Override public String getModName() { return ModInfo.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, RusticCrushingJEIRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        gui.init(0, true, 10, 15);   // 4x fruit / material
        gui.init(1, true, 45, 15);   // 1x glass bottle
        gui.init(2, false, 80, 15);  // required station: crushing tub
        gui.init(3, false, 125, 15); // output juice/oil bottle

        gui.set(0, ingredients.getInputs(ItemStack.class).get(0));
        gui.set(1, ingredients.getInputs(ItemStack.class).get(1));

        Block tubBlock = Block.getBlockFromName("rustic:crushing_tub");
        ItemStack stationStack = (tubBlock != null) ? new ItemStack(tubBlock) : new ItemStack(net.minecraft.init.Blocks.CAULDRON);
        gui.set(2, stationStack);
        gui.set(3, ingredients.getOutputs(ItemStack.class).get(0));

        gui.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (slotIndex == 2) {
                tooltip.add(TextFormatting.GOLD + "Requires Crushing Tub in Storage Heart");
                tooltip.add(TextFormatting.GRAY + "Place a Crushing Tub inside the Storage Heart to craft.");
            }
        });
    }

    public static class RusticCrushingJEIRecipe implements IRecipeWrapper {
        private final ItemStack inputFruit;
        private final ItemStack bottle;
        private final ItemStack outputJuice;

        public RusticCrushingJEIRecipe(ItemStack inputFruit, ItemStack bottle, ItemStack outputJuice) {
            this.inputFruit = inputFruit;
            this.bottle = bottle;
            this.outputJuice = outputJuice;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inputs = new ArrayList<>();
            inputs.add(Collections.singletonList(inputFruit));
            inputs.add(Collections.singletonList(bottle));
            ingredients.setInputLists(ItemStack.class, inputs);
            ingredients.setOutput(ItemStack.class, outputJuice);
        }

        public ItemStack getInputFruit() {
            return inputFruit;
        }

        public String getOutputFluidName() {
            return com.brilliafy.magicstorage.util.RusticCraftingHelper.getFluidName(outputJuice);
        }
    }

    public static List<RusticCrushingJEIRecipe> generateAllRecipes() {
        List<RusticCrushingJEIRecipe> list = new ArrayList<>();
        if (!Loader.isModLoaded("rustic")) return list;

        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);

        for (ICrushingTubRecipe recipe : Recipes.crushingTubRecipes) {
            ItemStack inStack = recipe.getInput();
            FluidStack outFluid = recipe.getResult();
            if (inStack != null && !inStack.isEmpty() && outFluid != null && outFluid.getFluid() != null) {
                ItemStack fruit4 = inStack.copy();
                fruit4.setCount(4);
                ItemStack outBottle = com.brilliafy.magicstorage.util.RusticCraftingHelper.getFilledBottle(outFluid.getFluid());
                if (!outBottle.isEmpty()) {
                    list.add(new RusticCrushingJEIRecipe(fruit4, bottle, outBottle));
                }
            }
        }
        return list;
    }
}
