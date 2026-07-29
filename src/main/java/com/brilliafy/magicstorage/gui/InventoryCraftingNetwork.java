/*
 * Portions of this file contain code adapted from Storage Network
 * by Lothrazar (https://github.com/Lothrazar/Storage-Network).
 *
 * Copyright (c) Lothrazar
 * Licensed under the MIT License.
 */
package com.brilliafy.magicstorage.gui;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class InventoryCraftingNetwork extends InventoryCrafting {

    private final NonNullList<ItemStack> stackList;
    private final IStorageContainer eventHandler;

    public InventoryCraftingNetwork(IStorageContainer container, int rows, int columns) {
        super(null, rows, columns);
        this.eventHandler = container;
        int size = rows * columns;
        this.stackList = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public int getSizeInventory() {
        return this.stackList.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.stackList) {
            if (!itemstack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index >= this.getSizeInventory() ? ItemStack.EMPTY : this.stackList.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = ItemStackHelper.getAndSplit(this.stackList, index, count);
        if (!stack.isEmpty()) this.eventHandler.slotChanged();
        return stack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.stackList.set(index, stack);
        this.eventHandler.slotChanged();
    }

    @Override
    public int getInventoryStackLimit() { return 64; }

    @Override
    public void markDirty() {}

    @Override
    public boolean isUsableByPlayer(net.minecraft.entity.player.EntityPlayer player) { return true; }

    @Override
    public void openInventory(net.minecraft.entity.player.EntityPlayer player) {}

    @Override
    public void closeInventory(net.minecraft.entity.player.EntityPlayer player) {}

    @Override
    public int getField(int id) { return 0; }

    @Override
    public void setField(int id, int value) {}

    @Override
    public int getFieldCount() { return 0; }

    @Override
    public void clear() { this.stackList.clear(); }
}
