package com.brilliafy.magicstorage.jei;

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
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;

import java.util.ArrayList;
import java.util.List;

public class BrewingRecipeCategory implements IRecipeCategory<BrewingRecipeCategory.BrewingJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".brewing";
    private static final ResourceLocation TEXTURE = new ResourceLocation(ModInfo.MOD_ID, "textures/gui/jei_brewing.png");
    private final IDrawable background;
    private final IDrawable icon;

    public BrewingRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(160, 60);
        icon = helper.createDrawableIngredient(new ItemStack(Items.BREWING_STAND));
    }

    @Override
    public String getUid() { return UID; }
    @Override
    public String getTitle() { return "Magic Storage Brewing"; }
    @Override
    public String getModName() { return ModInfo.MOD_NAME; }
    @Override
    public IDrawable getBackground() { return background; }
    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, BrewingJEIRecipe recipeWrapper, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        gui.init(0, true, 10, 0);   // blaze powder (fuel)
        gui.init(1, true, 10, 40);  // input potion bottle
        gui.init(2, true, 70, 20);  // brewing ingredient
        gui.init(3, false, 130, 20); // output potion

        gui.set(ingredients);
    }

    public static class BrewingJEIRecipe implements IRecipeWrapper {
        private final ItemStack blazePowder;
        private final ItemStack input;
        private final ItemStack ingredient;
        private final ItemStack output;

        public BrewingJEIRecipe(ItemStack input, ItemStack ingredient, ItemStack output) {
            this(new ItemStack(Items.BLAZE_POWDER), input, ingredient, output);
        }

        public BrewingJEIRecipe(ItemStack blazePowder, ItemStack input, ItemStack ingredient, ItemStack output) {
            this.blazePowder = blazePowder;
            this.input = input;
            this.ingredient = ingredient;
            this.output = output;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inputLists = new ArrayList<>();
            inputLists.add(java.util.Collections.singletonList(blazePowder));
            inputLists.add(java.util.Collections.singletonList(input));
            inputLists.add(java.util.Collections.singletonList(ingredient));
            ingredients.setInputLists(ItemStack.class, inputLists);
            ingredients.setOutput(ItemStack.class, output);
        }
    }

    /**
     * Generate all vanilla potion brewing recipes dynamically.
     */
    public static List<BrewingJEIRecipe> generateAllRecipes() {
        List<BrewingJEIRecipe> recipes = new ArrayList<>();
        PotionType[] potionTypes = {PotionTypes.WATER, PotionTypes.AWKWARD, PotionTypes.THICK,
            PotionTypes.MUNDANE, PotionTypes.REGENERATION, PotionTypes.SWIFTNESS,
            PotionTypes.SLOWNESS, PotionTypes.STRENGTH, PotionTypes.HEALING,
            PotionTypes.HARMING, PotionTypes.POISON, PotionTypes.WEAKNESS,
            PotionTypes.LEAPING, PotionTypes.NIGHT_VISION, PotionTypes.INVISIBILITY,
            PotionTypes.FIRE_RESISTANCE, PotionTypes.WATER_BREATHING};

        // Common brewing ingredients with MCP stable_39 names
        ItemStack[] ingredients = {
            new ItemStack(Items.NETHER_WART),
            new ItemStack(Items.GLOWSTONE_DUST),
            new ItemStack(Items.REDSTONE),
            new ItemStack(Items.GUNPOWDER),
            new ItemStack(Items.SUGAR),
            new ItemStack(Items.RABBIT_FOOT),
            new ItemStack(Items.BLAZE_POWDER),
            new ItemStack(Items.SPIDER_EYE),
            new ItemStack(Items.FERMENTED_SPIDER_EYE),
            new ItemStack(Items.GHAST_TEAR),
            new ItemStack(Items.MAGMA_CREAM),
            new ItemStack(Items.GOLDEN_CARROT),
            new ItemStack(Items.SPECKLED_MELON),
            new ItemStack(Items.DRAGON_BREATH),
        };
        for (PotionType type : potionTypes) {
            ItemStack potionStack = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), type);
            if (potionStack.isEmpty()) continue;

            for (ItemStack ing : ingredients) {
                try {
                    ItemStack result = BrewingRecipeRegistry.getOutput(potionStack, ing);
                    if (!result.isEmpty() && result.getItem() == Items.POTIONITEM
                        && PotionUtils.getPotionFromItem(result) != type) {
                        recipes.add(new BrewingJEIRecipe(potionStack, ing, result));
                    }
                } catch (Exception e) { }
            }

            // Also try splash potion variants
            ItemStack splash = PotionUtils.addPotionToItemStack(new ItemStack(Items.SPLASH_POTION), type);
            if (!splash.isEmpty()) {
                for (ItemStack ing : ingredients) {
                    try {
                        ItemStack result = BrewingRecipeRegistry.getOutput(splash, ing);
                        if (!result.isEmpty() && PotionUtils.getPotionFromItem(result) != type) {
                            recipes.add(new BrewingJEIRecipe(splash, ing, result));
                        }
                    } catch (Exception e) { }
                }
            }
        }

        return recipes;
    }
}
