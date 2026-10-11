package com.brilliafy.magicstorage.crafting;

import com.brilliafy.magicstorage.container.ContainerMagicStorageBase;
import com.brilliafy.magicstorage.gui.InventoryCraftingNetwork;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class RecipeHeartFurnace extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    private final IRecipe inner;

    public RecipeHeartFurnace(ResourceLocation name, IRecipe inner) {
        this.inner = inner;
        this.setRegistryName(name);
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        if (!hasFurnace(inv)) {
            return false;
        }
        return inner.matches(inv, worldIn);
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

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return inner.getCraftingResult(inv);
    }

    @Override
    public boolean canFit(int width, int height) {
        return inner.canFit(width, height);
    }

    @Override
    public ItemStack getRecipeOutput() {
        return inner.getRecipeOutput();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        return inner.getRemainingItems(inv);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inner.getIngredients();
    }

    @Override
    public boolean isDynamic() {
        return inner.isDynamic();
    }

    @Override
    public String getGroup() {
        return inner.getGroup();
    }
}
