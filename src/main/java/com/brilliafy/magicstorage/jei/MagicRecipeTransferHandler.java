package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.container.ContainerMagicStorageBase;
import com.brilliafy.magicstorage.network.RecipeMessage;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.List;
import java.util.Map;

public class MagicRecipeTransferHandler<C extends Container & com.brilliafy.magicstorage.gui.IStorageContainer> implements IRecipeTransferHandler<C> {

    Class<C> clazz;

    public MagicRecipeTransferHandler(Class<C> clazz) { this.clazz = clazz; }

    @Override
    public Class<C> getContainerClass() { return clazz; }

    @Override
    public IRecipeTransferError transferRecipe(Container container, IRecipeLayout recipeLayout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            NBTTagCompound nbt = recipeToTag(container, recipeLayout);
            com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendToServer(new RecipeMessage(nbt, maxTransfer));
        }
        return null;
    }

    private static NBTTagCompound recipeToTag(Container container, IRecipeLayout recipeLayout) {
        NBTTagCompound nbt = new NBTTagCompound();
        Map<Integer, ? extends IGuiIngredient<ItemStack>> inputs = recipeLayout.getItemStacks().getGuiIngredients();
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory instanceof InventoryCrafting) {
                IGuiIngredient<ItemStack> ingredient = inputs.get(slot.getSlotIndex() + 1);
                if (ingredient == null) continue;
                List<ItemStack> possibleItems = ingredient.getAllIngredients();
                if (possibleItems == null) continue;
                NBTTagList invList = new NBTTagList();
                for (int i = 0; i < Math.min(possibleItems.size(), 5); i++) {
                    ItemStack itemStack = possibleItems.get(i);
                    if (!itemStack.isEmpty()) {
                        NBTTagCompound stackTag = new NBTTagCompound();
                        itemStack.writeToNBT(stackTag);
                        invList.appendTag(stackTag);
                    }
                }
                nbt.setTag("s" + slot.getSlotIndex(), invList);
            }
        }
        return nbt;
    }
}
