package com.brilliafy.magicstorage.container;

import com.brilliafy.magicstorage.gui.IStorageContainer;
import com.brilliafy.magicstorage.tile.TileStorageAccess;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class ContainerStorageAccess extends Container implements IStorageContainer {

    private final TileStorageAccess accessTile;
    private final TileStorageHeart directHeart; // non-null when created from RemoteAccess
    private List<ItemStack> cachedStacks = new ArrayList<>();
    
    private TileStorageHeart resolveHeart() {
        if (directHeart != null) return directHeart;
        if (accessTile != null) return accessTile.findHeart();
        return null;
    }

    public ContainerStorageAccess(InventoryPlayer playerInv, TileStorageAccess accessTile) {
        this.accessTile = accessTile;
        this.directHeart = null;

        TileStorageHeart heart = resolveHeart();
        bindPlayerSlots(playerInv);
    }

    /** Alternative constructor: pass a pre-resolved heart directly (used by RemoteAccess) */
    public ContainerStorageAccess(InventoryPlayer playerInv, TileStorageHeart heart) {
        this.accessTile = null;
        this.directHeart = heart;
        bindPlayerSlots(playerInv);
    }
    
    private void bindPlayerSlots(InventoryPlayer playerInv) {
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

    /** Alternative constructor: pass a pre-resolved heart directly (used by RemoteAccess) */
    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        if (accessTile != null) {
            return accessTile.getWorld().getTileEntity(accessTile.getPos()) == accessTile
                && playerIn.getDistanceSq(accessTile.getPos().add(0.5, 0.5, 0.5)) <= 64.0;
        }
        // For heart-based constructor (RemoteAccess), allow interaction from any reasonable distance
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        if (playerIn.world.isRemote) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack().copy();
            TileStorageHeart heart = resolveHeart();
            if (heart != null) {
                ItemStack simulated = heart.insertItem(stack.copy(), true);
                if (simulated.getCount() < stack.getCount()) {
                    ItemStack remainder = heart.insertItem(stack.copy(), false);
                    slot.putStack(remainder.isEmpty() ? ItemStack.EMPTY : net.minecraftforge.items.ItemHandlerHelper.copyStackWithSize(stack, remainder.getCount()));
                    slot.onSlotChanged();
                    detectAndSendChanges();
                    List<ItemStack> allItems = heart.getAllItems();
                    com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                        new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(allItems, new ArrayList<>()),
                        (net.minecraft.entity.player.EntityPlayerMP) playerIn);
                }
            }
        }
        return ItemStack.EMPTY;
    }


    public TileStorageAccess getAccessTile() { return accessTile; }
    public List<ItemStack> getCachedStacks() { return cachedStacks; }
    public void setCachedStacks(List<ItemStack> stacks) { this.cachedStacks = stacks; }
    // IStorageContainer implementation
    @Override public InventoryCrafting getCraftMatrix() { return null; }
    @Override public IItemHandler getItemHandler() { return null; }
    @Override public void setStacks(List<ItemStack> stacks) { this.cachedStacks = stacks; }
    @Override public void setCraftableStacks(List<ItemStack> stacks) {}
    @Override public void slotChanged() {}
    @Override public boolean isRequest() { return true; }
}
