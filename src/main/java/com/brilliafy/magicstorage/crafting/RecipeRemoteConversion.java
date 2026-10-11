package com.brilliafy.magicstorage.crafting;

import com.brilliafy.magicstorage.init.RegistryHandler;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class RecipeRemoteConversion extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    private final int meta;

    public RecipeRemoteConversion(ResourceLocation name, int meta) {
        this.meta = meta;
        this.setRegistryName(name);
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        boolean hasRemote = false;
        boolean hasTable = false;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() == RegistryHandler.PORTABLE_ACCESS && stack.getMetadata() == meta && !hasRemote) {
                    hasRemote = true;
                } else if (stack.getItem() == Item.getItemFromBlock(Blocks.CRAFTING_TABLE) && !hasTable) {
                    hasTable = true;
                } else {
                    return false;
                }
            }
        }
        return hasRemote && hasTable;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack result = new ItemStack(RegistryHandler.PORTABLE_CRAFTING_ACCESS, 1, meta);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == RegistryHandler.PORTABLE_ACCESS) {
                if (stack.hasTagCompound()) {
                    result.setTagCompound(stack.getTagCompound().copy());
                }
                break;
            }
        }
        return result;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(RegistryHandler.PORTABLE_CRAFTING_ACCESS, 1, meta);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        return net.minecraftforge.common.ForgeHooks.defaultRecipeGetRemainingItems(inv);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(Ingredient.fromStacks(new ItemStack(RegistryHandler.PORTABLE_ACCESS, 1, meta)));
        list.add(Ingredient.fromItem(Item.getItemFromBlock(Blocks.CRAFTING_TABLE)));
        return list;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }
}
