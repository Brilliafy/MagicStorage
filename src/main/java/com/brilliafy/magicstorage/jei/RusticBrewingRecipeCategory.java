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
import rustic.common.crafting.IBrewingBarrelRecipe;
import rustic.common.crafting.Recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RusticBrewingRecipeCategory implements IRecipeCategory<RusticBrewingRecipeCategory.RusticBrewingJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".rustic_brewing";
    private final IDrawable background;
    private final IDrawable icon;

    public RusticBrewingRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(160, 50);
        icon = helper.createDrawableIngredient(new ItemStack(ModBlocksRef.CRAFTING_ACCESS));
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return "Magic Storage Rustic Brewing"; }
    @Override public String getModName() { return ModInfo.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, RusticBrewingJEIRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        if (recipe.hasModifier()) {
            gui.init(0, true, 5, 15);   // modifier booze bottle (Slot 1 in JEI)
            gui.init(1, true, 30, 15);  // juice bottle (Slot 2 in JEI)
            gui.init(2, true, 55, 15);  // blaze powder (Slot 3 in JEI)
            gui.init(3, false, 85, 15); // required station: brewing barrel
            gui.init(4, false, 125, 15);// output booze bottle

            gui.set(0, recipe.modifierBooze);
            gui.set(1, recipe.inputJuice);
            gui.set(2, recipe.blazePowder);

            Block barrelBlock = Block.getBlockFromName("rustic:brewing_barrel");
            ItemStack stationStack = (barrelBlock != null) ? new ItemStack(barrelBlock) : new ItemStack(net.minecraft.init.Blocks.CAULDRON);
            gui.set(3, stationStack);
            gui.set(4, recipe.outputBooze);

            gui.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
                if (slotIndex == 0) {
                    tooltip.add(TextFormatting.AQUA + "Quality Modifier Bottle (Slot 4)");
                    tooltip.add(TextFormatting.GRAY + "Influences and improves output brew quality.");
                } else if (slotIndex == 3) {
                    tooltip.add(TextFormatting.GOLD + "Requires Brewing Barrel in Storage Heart");
                    tooltip.add(TextFormatting.GRAY + "Place a Brewing Barrel inside the Storage Heart to craft.");
                }
            });
        } else {
            gui.init(0, true, 15, 15);  // juice bottle
            gui.init(1, true, 45, 15);  // blaze powder
            gui.init(2, false, 80, 15); // required station: brewing barrel
            gui.init(3, false, 125, 15);// output booze bottle

            gui.set(0, recipe.inputJuice);
            gui.set(1, recipe.blazePowder);

            Block barrelBlock = Block.getBlockFromName("rustic:brewing_barrel");
            ItemStack stationStack = (barrelBlock != null) ? new ItemStack(barrelBlock) : new ItemStack(net.minecraft.init.Blocks.CAULDRON);
            gui.set(2, stationStack);
            gui.set(3, recipe.outputBooze);

            gui.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
                if (slotIndex == 2) {
                    tooltip.add(TextFormatting.GOLD + "Requires Brewing Barrel in Storage Heart");
                    tooltip.add(TextFormatting.GRAY + "Place a Brewing Barrel inside the Storage Heart to craft.");
                }
            });
        }
    }

    public static class RusticBrewingJEIRecipe implements IRecipeWrapper {
        public final ItemStack inputJuice;
        public final ItemStack modifierBooze;
        public final ItemStack blazePowder;
        public final ItemStack outputBooze;

        public RusticBrewingJEIRecipe(ItemStack inputJuice, ItemStack modifierBooze, ItemStack blazePowder, ItemStack outputBooze) {
            this.inputJuice = inputJuice;
            this.modifierBooze = modifierBooze != null ? modifierBooze : ItemStack.EMPTY;
            this.blazePowder = blazePowder;
            this.outputBooze = outputBooze;
        }

        public boolean hasModifier() {
            return !modifierBooze.isEmpty();
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inputs = new ArrayList<>();
            if (!modifierBooze.isEmpty()) {
                inputs.add(Collections.singletonList(modifierBooze));
            }
            inputs.add(Collections.singletonList(inputJuice));
            inputs.add(Collections.singletonList(blazePowder));
            ingredients.setInputLists(ItemStack.class, inputs);
            ingredients.setOutput(ItemStack.class, outputBooze);
        }

        public String getInputFluidName() {
            return com.brilliafy.magicstorage.util.RusticCraftingHelper.getFluidName(inputJuice);
        }

        public String getModifierFluidName() {
            return com.brilliafy.magicstorage.util.RusticCraftingHelper.getFluidName(modifierBooze);
        }

        public String getOutputFluidName() {
            return com.brilliafy.magicstorage.util.RusticCraftingHelper.getFluidName(outputBooze);
        }
    }

    public static List<RusticBrewingJEIRecipe> generateAllRecipes() {
        List<RusticBrewingJEIRecipe> list = new ArrayList<>();
        if (!Loader.isModLoaded("rustic")) return list;

        ItemStack blaze = new ItemStack(Items.BLAZE_POWDER);

        for (IBrewingBarrelRecipe recipe : Recipes.brewingRecipes) {
            FluidStack inFluid = recipe.getInput();
            FluidStack outFluid = recipe.getResult(inFluid);
            if (inFluid != null && inFluid.getFluid() != null && outFluid != null && outFluid.getFluid() != null) {
                ItemStack inBottle = com.brilliafy.magicstorage.util.RusticCraftingHelper.getFilledBottle(inFluid.getFluid());
                ItemStack outBottle = com.brilliafy.magicstorage.util.RusticCraftingHelper.getFilledBottle(outFluid.getFluid());
                if (!inBottle.isEmpty() && !outBottle.isEmpty()) {
                    // Recipe 1: Standard brewing from juice (without modifier)
                    list.add(new RusticBrewingJEIRecipe(inBottle, ItemStack.EMPTY, blaze, outBottle));
                    // Recipe 2: Brewing with modifier booze bottle (to refine/improve quality)
                    list.add(new RusticBrewingJEIRecipe(inBottle, outBottle.copy(), blaze, outBottle));
                }
            }
        }
        return list;
    }
}
