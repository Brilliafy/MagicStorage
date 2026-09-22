package com.brilliafy.magicstorage.container;

import com.brilliafy.magicstorage.gui.InventoryCraftingNetwork;
import com.brilliafy.magicstorage.item.ItemPortableAccess;
import com.brilliafy.magicstorage.network.NetworkHandler;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ContainerPortableAccess extends ContainerMagicStorageBase {

    private final ItemStack remoteStack;
    private final int remoteSlot;
    private TileStorageHeart linkedHeart;

    public ContainerPortableAccess(InventoryPlayer playerInv, ItemStack remoteStack, int remoteSlot, World world) {
        this.remoteStack = remoteStack;
        this.remoteSlot = remoteSlot;
        this.playerInv = playerInv;
        this.result = new InventoryCraftResult();
        this.matrix = new InventoryCraftingNetwork(this, 3, 3);
        // isSimple based on item type (storage access vs crafting access), NOT metadata (tier)
        if (remoteStack.getItem() instanceof com.brilliafy.magicstorage.item.ItemPortableAccess) {
            this.isSimple = !((com.brilliafy.magicstorage.item.ItemPortableAccess) remoteStack.getItem()).isCraftingAccess();
        } else {
            this.isSimple = remoteStack.getMetadata() == 0;
        }
        if (!isSimple) {
            bindGrid();
        }
        bindPlayerInvo(playerInv);
        if (!isSimple) {
            onCraftMatrixChanged(matrix);
        }
        // SSN: don't try to find heart here — let the RequestMessage refresh handle it
    }

    @Override
    protected void bindPlayerInvo(final InventoryPlayer playerInv) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                int slot = j + i * 9 + 9;
                if (slot == remoteSlot) {
                    this.addSlotToContainer(new Slot(playerInv, slot, 8 + j * 18, 174 + i * 18) {
                        @Override
                        public boolean isItemValid(ItemStack stack) {
                            return false;
                        }

                        @Override
                        public boolean canTakeStack(EntityPlayer playerIn) {
                            return false;
                        }
                    });
                } else {
                    this.addSlotToContainer(new Slot(playerInv, slot, 8 + j * 18, 174 + i * 18));
                }
            }
        }
        for (int i = 0; i < 9; ++i) {
            if (i == remoteSlot) {
                this.addSlotToContainer(new Slot(playerInv, i, 8 + i * 18, 232) {
                    @Override
                    public boolean isItemValid(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean canTakeStack(EntityPlayer playerIn) {
                        return false;
                    }
                });
            } else {
                this.addSlotToContainer(new Slot(playerInv, i, 8 + i * 18, 232));
            }
        }
    }

    public ItemStack getRemoteStack() { return remoteStack; }
    public int getRemoteSlot() { return remoteSlot; }
    public boolean isSimple() { return isSimple; }

    @Override
    public TileStorageHeart getTileMaster() {
        if (linkedHeart == null) {
            linkedHeart = ItemPortableAccess.getHeart(remoteStack, playerInv.player.world);
        }
        return linkedHeart;
    }

    @Override
    public void slotChanged() {
        if (isSimple) return;
        onCraftMatrixChanged(matrix);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        if (!playerIn.isEntityAlive()) return false;
        if (remoteStack.isEmpty()) return false;
        if (playerIn.getHeldItemMainhand() == remoteStack || playerIn.getHeldItemOffhand() == remoteStack) return true;
        if (remoteSlot >= 0 && remoteSlot < playerIn.inventory.getSizeInventory()) {
            ItemStack stackInSlot = playerIn.inventory.getStackInSlot(remoteSlot);
            if (!stackInSlot.isEmpty() && stackInSlot.getItem() == remoteStack.getItem()) return true;
        }
        return playerIn.inventory.hasItemStack(remoteStack);
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return slot.inventory != this.result && super.canMergeSlot(stack, slot);
    }
    @Override public boolean isRequest() { return false; }
    @Override public net.minecraftforge.items.IItemHandler getItemHandler() { return null; }
}
