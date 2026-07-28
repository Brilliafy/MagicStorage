package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.reference.ModInfo;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnchantingRecipeCategory implements IRecipeCategory<EnchantingRecipeCategory.EnchantingJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".enchanting";
    private final IDrawable background;
    private final IDrawable icon;

    public EnchantingRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(140, 50);
        icon = helper.createDrawableIngredient(new ItemStack(net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.ENCHANTING_TABLE)));
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return "Magic Storage Enchanting"; }
    @Override public String getModName() { return ModInfo.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, EnchantingJEIRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        gui.init(0, true, 10, 15);   // enchantable item
        gui.init(1, true, 55, 15);   // lapis lazuli
        gui.init(2, false, 100, 15); // output (glint)
        gui.set(ingredients);
    }

    public static class EnchantingJEIRecipe implements IRecipeWrapper {
        private final ItemStack input;
        private final ItemStack lapis;
        private final ItemStack output;

        public EnchantingJEIRecipe(ItemStack input, ItemStack lapis, ItemStack output) {
            this.input = input; this.lapis = lapis; this.output = output;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inputs = new ArrayList<>();
            inputs.add(Collections.singletonList(input));
            inputs.add(Collections.singletonList(lapis));
            ingredients.setInputLists(ItemStack.class, inputs);
            ingredients.setOutput(ItemStack.class, output);
        }
    }

    /** Show one representative recipe per tool/weapon/armor type */
    public static List<EnchantingJEIRecipe> generateAllRecipes() {
        List<EnchantingJEIRecipe> recipes = new ArrayList<>();
        ItemStack lapis = new ItemStack(Items.DYE, 1, 4);

        ItemStack[] examples = {
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_PICKAXE),
            new ItemStack(Items.DIAMOND_AXE),
            new ItemStack(Items.DIAMOND_HELMET),
            new ItemStack(Items.DIAMOND_CHESTPLATE),
            new ItemStack(Items.DIAMOND_LEGGINGS),
            new ItemStack(Items.DIAMOND_BOOTS),
            new ItemStack(Items.BOW),
            new ItemStack(Items.IRON_SWORD),
            new ItemStack(Items.IRON_PICKAXE),
            new ItemStack(Items.IRON_HELMET),
            new ItemStack(Items.IRON_CHESTPLATE),
            new ItemStack(Items.IRON_LEGGINGS),
            new ItemStack(Items.IRON_BOOTS),
            new ItemStack(Items.SHIELD),
        };

        for (ItemStack input : examples) {
            if (!input.isEmpty() && input.isItemEnchantable()) {
                ItemStack output = input.copy();
                // Output is the same item - JEI will show it with glint
                recipes.add(new EnchantingJEIRecipe(input, lapis, output));
            }
        }

        return recipes;
    }
}
