package com.brilliafy.magicstorage.gui;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

public class InventoryCraftingNetwork extends InventoryCrafting {

    private final IStorageContainer storageContainer;

    public InventoryCraftingNetwork(IStorageContainer container, int width, int height) {
        super(container instanceof Container ? (Container) container : null, width, height);
        this.storageContainer = container;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = super.decrStackSize(index, count);
        if (!stack.isEmpty() && this.storageContainer != null) {
            this.storageContainer.slotChanged();
        }
        return stack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        super.setInventorySlotContents(index, stack);
        if (this.storageContainer != null) {
            this.storageContainer.slotChanged();
        }
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = super.removeStackFromSlot(index);
        if (!stack.isEmpty() && this.storageContainer != null) {
            this.storageContainer.slotChanged();
        }
        return stack;
    }

    @Override
    public void clear() {
        super.clear();
        if (this.storageContainer != null) {
            this.storageContainer.slotChanged();
        }
    }
}
