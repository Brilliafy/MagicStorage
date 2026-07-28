package com.brilliafy.magicstorage.container;

import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerStorageHeart extends Container {

    private final TileStorageHeart heart;

    public ContainerStorageHeart(InventoryPlayer playerInv, TileStorageHeart heart) {
        this.heart = heart;
        IItemHandler inv = heart.getInventory();

        // 2 rows × 10 columns of storage slots
        // Texture positions: row 1 at y=18, row 2 at y=36
        // Each slot x=8 + col*18, slot width 16px, 18px spacing
        int index = 0;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 10; col++) {
                addSlotToContainer(new SlotItemHandler(inv, index++,
                    8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory: y=84, 102, 120 (18px spacing)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9,
                    8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar: y=142
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col,
                8 + col * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) { return true; }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < 20) {
                if (!mergeItemStack(itemstack1, 20, inventorySlots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                if (!mergeItemStack(itemstack1, 0, 20, false))
                    return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
        }
        return itemstack;
    }
}
