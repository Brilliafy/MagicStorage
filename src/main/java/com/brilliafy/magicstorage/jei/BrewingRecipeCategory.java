package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.reference.ModBlocksRef;
import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.util.PotionCraftingHelper;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrewingRecipeCategory implements IRecipeCategory<BrewingRecipeCategory.BrewingJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".brewing";
    private final IDrawable background;
    private final IDrawable icon;

    public BrewingRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(160, 60);
        icon = helper.createDrawableIngredient(new ItemStack(ModBlocksRef.CRAFTING_ACCESS));
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return "Magic Storage Brewing"; }
    @Override public String getModName() { return ModInfo.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, BrewingJEIRecipe recipeWrapper, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        gui.init(0, true, 10, 5);    // blaze powder (fuel)
        gui.init(1, true, 10, 35);   // input potion bottle
        gui.init(2, true, 55, 20);   // brewing ingredient
        gui.init(3, false, 90, 20);  // required station: brewing stand
        gui.init(4, false, 130, 20); // output potion

        gui.set(0, ingredients.getInputs(ItemStack.class).get(0));
        gui.set(1, ingredients.getInputs(ItemStack.class).get(1));
        gui.set(2, ingredients.getInputs(ItemStack.class).get(2));
        gui.set(3, new ItemStack(Items.BREWING_STAND));
        gui.set(4, ingredients.getOutputs(ItemStack.class).get(0));

        gui.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (slotIndex == 3) {
                tooltip.add(TextFormatting.GOLD + "Requires Brewing Stand in Storage Heart");
                tooltip.add(TextFormatting.GRAY + "Place a Brewing Stand inside the Storage Heart to craft.");
            }
        });
    }

    public static class BrewingJEIRecipe implements IRecipeWrapper {
        private final ItemStack blazePowder;
        private final ItemStack inputBottle;
        private final ItemStack ingredient;
        private final ItemStack outputPotion;

        public BrewingJEIRecipe(ItemStack blazePowder, ItemStack inputBottle, ItemStack ingredient, ItemStack outputPotion) {
            this.blazePowder = blazePowder;
            this.inputBottle = inputBottle;
            this.ingredient = ingredient;
            this.outputPotion = outputPotion;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inputs = new ArrayList<>();
            inputs.add(Collections.singletonList(blazePowder));
            inputs.add(Collections.singletonList(inputBottle));
            inputs.add(Collections.singletonList(ingredient));
            ingredients.setInputLists(ItemStack.class, inputs);
            ingredients.setOutput(ItemStack.class, outputPotion);
        }
    }

    public static List<BrewingJEIRecipe> generateAllRecipes() {
        List<BrewingJEIRecipe> recipes = new ArrayList<>();
        ItemStack blaze = new ItemStack(Items.BLAZE_POWDER);

        ItemStack[] baseIngredients = {
            new ItemStack(Items.NETHER_WART),
            new ItemStack(Items.REDSTONE),
            new ItemStack(Items.GLOWSTONE_DUST),
            new ItemStack(Items.GUNPOWDER),
            new ItemStack(Items.DRAGON_BREATH),
            new ItemStack(Items.FERMENTED_SPIDER_EYE),
            new ItemStack(Items.SUGAR),
            new ItemStack(Items.RABBIT_FOOT),
            new ItemStack(Items.SPECKLED_MELON),
            new ItemStack(Items.SPIDER_EYE),
            new ItemStack(Items.GHAST_TEAR),
            new ItemStack(Items.MAGMA_CREAM),
            new ItemStack(Items.BLAZE_POWDER),
            new ItemStack(Items.GOLDEN_CARROT),
            new ItemStack(Items.SPECKLED_MELON),
            new ItemStack(Items.FISH, 1, 3)
        };

        for (PotionType inputType : PotionType.REGISTRY) {
            if (inputType == null) continue;
            for (ItemStack ingredient : baseIngredients) {
                // Regular Potion
                ItemStack inputPotion = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), inputType);
                ItemStack outputPotion = BrewingRecipeRegistry.getOutput(inputPotion, ingredient);
                if (!outputPotion.isEmpty()) {
                    recipes.add(new BrewingJEIRecipe(blaze, inputPotion, ingredient, outputPotion));
                }

                // Splash Potion
                ItemStack inputSplash = PotionUtils.addPotionToItemStack(new ItemStack(Items.SPLASH_POTION), inputType);
                ItemStack outputSplash = BrewingRecipeRegistry.getOutput(inputSplash, ingredient);
                if (!outputSplash.isEmpty()) {
                    recipes.add(new BrewingJEIRecipe(blaze, inputSplash, ingredient, outputSplash));
                }

                // Lingering Potion
                ItemStack inputLingering = PotionUtils.addPotionToItemStack(new ItemStack(Items.LINGERING_POTION), inputType);
                ItemStack outputLingering = BrewingRecipeRegistry.getOutput(inputLingering, ingredient);
                if (!outputLingering.isEmpty()) {
                    recipes.add(new BrewingJEIRecipe(blaze, inputLingering, ingredient, outputLingering));
                }
            }
        }

        return recipes;
    }
}
