package com.brilliafy.magicstorage.crafting;

import com.brilliafy.magicstorage.container.ContainerMagicStorageBase;
import com.brilliafy.magicstorage.gui.InventoryCraftingNetwork;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import com.google.gson.JsonObject;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

public class RecipeHeartFurnace extends ShapelessRecipes {

    public RecipeHeartFurnace(String group, ItemStack result, NonNullList<Ingredient> ingredients) {
        super(group, result, ingredients);
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        if (!hasFurnace(inv)) {
            return false;
        }
        return super.matches(inv, worldIn);
    }

    public static boolean hasFurnace(InventoryCrafting inv) {
        if (inv instanceof InventoryCraftingNetwork) {
            InventoryCraftingNetwork net = (InventoryCraftingNetwork) inv;
            if (net.getStorageContainer() instanceof ContainerMagicStorageBase) {
                ContainerMagicStorageBase base = (ContainerMagicStorageBase) net.getStorageContainer();
                TileStorageHeart heart = base.getTileMaster();
                return heart != null && heart.hasFurnace();
            }
        }
        return false;
    }

    public static class Factory implements IRecipeFactory {
        @Override
        public IRecipe parse(JsonContext context, JsonObject json) {
            ShapelessRecipes recipe = ShapelessRecipes.deserialize(json);
            return new RecipeHeartFurnace(recipe.getGroup(), recipe.getRecipeOutput(), recipe.recipeItems);
        }
    }
}
