package com.brilliafy.magicstorage.crafting;

import com.brilliafy.magicstorage.item.ItemPortableAccess;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class RecipeRemoteUpgrade extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    private final IRecipe inner;

    public RecipeRemoteUpgrade(ResourceLocation name, IRecipe inner) {
        this.inner = inner;
        this.setRegistryName(name);
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        return inner.matches(inv, worldIn);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack result = inner.getCraftingResult(inv).copy();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (!s.isEmpty() && (s.getItem() instanceof ItemPortableAccess)) {
                if (s.hasTagCompound()) {
                    result.setTagCompound(s.getTagCompound().copy());
                }
                break;
            }
        }
        return result;
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
