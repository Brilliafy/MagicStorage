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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;
import rustic.common.crafting.ICondenserRecipe;
import rustic.common.crafting.Recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RusticAdvancedAlchemyRecipeCategory implements IRecipeCategory<RusticAdvancedAlchemyRecipeCategory.RusticAdvancedAlchemyJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".rustic_alchemy_advanced";
    private final IDrawable background;
    private final IDrawable icon;

    public RusticAdvancedAlchemyRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(160, 60);
        icon = helper.createDrawableIngredient(new ItemStack(ModBlocksRef.CRAFTING_ACCESS));
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return "Magic Storage Advanced Alchemy"; }
    @Override public String getModName() { return ModInfo.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, RusticAdvancedAlchemyJEIRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        gui.init(0, true, 5, 5);    // bottle
        gui.init(1, true, 25, 5);   // modifier / herb 1
        gui.init(2, true, 45, 5);   // herb 2
        gui.init(3, true, 5, 30);   // coal
        gui.init(4, true, 25, 30);  // water bucket
        gui.init(5, true, 45, 30);  // extra ingredient
        gui.init(6, false, 80, 18); // required station: advanced condenser + retort
        gui.init(7, false, 130, 18); // output elixir

        gui.set(ingredients);

        Block condenserBlock = Block.getBlockFromName("rustic:condenser_advanced");
        ItemStack stationStack = (condenserBlock != null) ? new ItemStack(condenserBlock) : new ItemStack(net.minecraft.init.Blocks.BREWING_STAND);
        gui.set(6, stationStack);

        gui.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (slotIndex == 6) {
                tooltip.add(TextFormatting.GOLD + "Requires Advanced Alchemic Condenser & Retort in Storage Heart");
                tooltip.add(TextFormatting.GRAY + "Place an Advanced Condenser & Retort inside the Storage Heart to craft.");
            }
        });
    }

    public static class RusticAdvancedAlchemyJEIRecipe implements IRecipeWrapper {
        private final List<List<ItemStack>> inputs;
        private final ItemStack output;

        public RusticAdvancedAlchemyJEIRecipe(List<List<ItemStack>> inputs, ItemStack output) {
            this.inputs = inputs;
            this.output = output;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            ingredients.setInputLists(ItemStack.class, inputs);
            ingredients.setOutput(ItemStack.class, output);
        }
    }

    public static List<RusticAdvancedAlchemyJEIRecipe> generateAllRecipes() {
        List<RusticAdvancedAlchemyJEIRecipe> list = new ArrayList<>();
        if (!Loader.isModLoaded("rustic")) return list;

        List<ItemStack> fuels = new ArrayList<>();
        for (Item item : Item.REGISTRY) {
            if (item == null) continue;
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty() && net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(stack) > 0) {
                fuels.add(stack);
            }
        }
        if (fuels.isEmpty()) fuels.add(new ItemStack(Items.COAL));

        ItemStack waterBucket = new ItemStack(Items.WATER_BUCKET);

        for (ICondenserRecipe recipe : Recipes.condenserRecipes) {
            if (recipe.isAdvanced()) {
                List<List<ItemStack>> inputs = new ArrayList<>();
                inputs.add(recipe.getBottles() != null ? recipe.getBottles() : Collections.singletonList(new ItemStack(Items.GLASS_BOTTLE)));
                if (recipe.getModifiers() != null && !recipe.getModifiers().isEmpty()) {
                    inputs.add(recipe.getModifiers());
                } else {
                    inputs.add(Collections.emptyList());
                }
                List<List<ItemStack>> recipeInputs = recipe.getInputs();
                if (recipeInputs.size() > 0) inputs.add(recipeInputs.get(0));
                else inputs.add(Collections.emptyList());
                inputs.add(fuels);
                inputs.add(Collections.singletonList(waterBucket));
                if (recipeInputs.size() > 1) inputs.add(recipeInputs.get(1));
                else inputs.add(Collections.emptyList());

                list.add(new RusticAdvancedAlchemyJEIRecipe(inputs, recipe.getResult().copy()));
            }
        }
        return list;
    }
}
