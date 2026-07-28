package com.brilliafy.magicstorage.gui;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

public interface IStorageContainer {
    InventoryCrafting getCraftMatrix();
    IItemHandler getItemHandler();
    List<ItemStack> getCachedStacks();
    void setStacks(List<ItemStack> stacks);
    void setCraftableStacks(List<ItemStack> stacks);
    void slotChanged();
    boolean isRequest();
}
