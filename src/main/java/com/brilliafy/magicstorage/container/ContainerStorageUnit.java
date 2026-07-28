package com.brilliafy.magicstorage.container;

import com.brilliafy.magicstorage.gui.IStorageContainer;
import com.brilliafy.magicstorage.network.NetworkHandler;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import com.brilliafy.magicstorage.tile.TileStorageUnit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

public class ContainerStorageUnit extends Container implements IStorageContainer {

    private final TileStorageUnit unit;
    private List<ItemStack> cachedStacks = new ArrayList<>();

    public ContainerStorageUnit(InventoryPlayer playerInv, TileStorageUnit unit) {
        this.unit = unit;

        // Populate cachedStacks from the single storage unit's inventory
        if (!unit.getWorld().isRemote) {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < unit.getSlotCount(); i++) {
                ItemStack s = unit.getInventory().getStackInSlot(i);
                if (!s.isEmpty()) {
                    // Merge identical items
                    boolean found = false;
                    for (ItemStack existing : items) {
                        if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(existing, s)) {
                            existing.grow(s.getCount());
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        items.add(s.copy());
                    }
                }
            }
            this.cachedStacks = items;

            // Send items to client
            NetworkHandler.INSTANCE.sendTo(
                new NetworkHandler.StackRefreshClientMessage(items, new ArrayList<>()),
                (EntityPlayerMP) playerInv.player);
        }

        // No container slots for storage unit items — they're displayed via the custom GUI
        // Player inventory slots only
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9,
                    8 + col * 18, 174 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col,
                8 + col * 18, 232));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return unit.getWorld().getTileEntity(unit.getPos()) == unit
            && playerIn.getDistanceSq(unit.getPos().add(0.5, 0.5, 0.5)) <= 64.0;
    }

    public TileStorageUnit getUnit() { return unit; }
    public List<ItemStack> getCachedStacks() { return cachedStacks; }

    // IStorageContainer implementation
    @Override public InventoryCrafting getCraftMatrix() { return null; }
    @Override public IItemHandler getItemHandler() { return null; }
    @Override public void setStacks(List<ItemStack> stacks) { this.cachedStacks = stacks; }
    @Override public void setCraftableStacks(List<ItemStack> stacks) {}
    @Override public void slotChanged() {}
    @Override public boolean isRequest() { return true; }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        if (playerIn.world.isRemote) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            // Manual slot-by-slot insertion with count tracking (same pattern as InsertMessage)
            net.minecraftforge.items.ItemStackHandler inv = unit.getInventory();
            ItemStack slotStack = slot.getStack();
            ItemStack toInsert = slotStack.copy();
            int startCount = toInsert.getCount();
            
            for (int i = 0; i < unit.getSlotCount() && !toInsert.isEmpty(); i++) {
                toInsert = inv.insertItem(i, toInsert, false);
            }
            int inserted = startCount - toInsert.getCount();
            
            if (inserted > 0) {
                // Shrink the slot's actual stack by the inserted amount
                slotStack.shrink(inserted);
                if (slotStack.isEmpty()) {
                    slot.putStack(ItemStack.EMPTY);
                }
                slot.onSlotChanged();
                detectAndSendChanges();
                
                // Send updated item list to client
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < unit.getSlotCount(); i++) {
                    ItemStack s = inv.getStackInSlot(i);
                    if (!s.isEmpty()) {
                        boolean found = false;
                        for (ItemStack existing : items) {
                            if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(existing, s)) {
                                existing.grow(s.getCount());
                                found = true;
                                break;
                            }
                        }
                        if (!found) items.add(s.copy());
                    }
                }
                com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                    new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(items, new ArrayList<>()),
                    (net.minecraft.entity.player.EntityPlayerMP) playerIn);
            }
        }
        return ItemStack.EMPTY;
    }
}
