package com.brilliafy.magicstorage.inventory;

import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * A virtual crafting grid backed by the storage network.
 * Items placed in the grid are pulled from connected Storage Units.
 * When crafting, items are consumed from the network.
 */
public class NetworkCraftingInventory extends InventoryCrafting {

    private final TileStorageHeart heart;
    private final NonNullList<ItemStack> gridContents = NonNullList.withSize(9, ItemStack.EMPTY);
    private final NonNullList<Integer> networkSlotRefs = NonNullList.withSize(9, -1);
    private final NonNullList<BlockPos> networkBlockRefs = NonNullList.withSize(9, BlockPos.ORIGIN);
    private boolean skipEvents = false;
    private final Container ownerContainer;

    public NetworkCraftingInventory(Container container, @Nullable TileStorageHeart heart) {
        super(container, 3, 3);
        this.heart = heart;
        this.ownerContainer = container;
    }

    public boolean tryPlaceFromNetwork(int slot, ItemStack toPlace) {
        if (slot < 0 || slot >= 9 || toPlace.isEmpty() || heart == null) return false;

        for (java.util.Map.Entry<BlockPos, com.brilliafy.magicstorage.tile.TileStorageUnit> entry
                : getUnits().entrySet()) {
            BlockPos unitPos = entry.getKey();
            com.brilliafy.magicstorage.tile.TileStorageUnit unit = entry.getValue();

            for (int i = 0; i < unit.getInventory().getSlots(); i++) {
                ItemStack slotStack = unit.getInventory().getStackInSlot(i);
                if (!slotStack.isEmpty() && ItemStack.areItemsEqual(slotStack, toPlace)
                        && ItemStack.areItemStackTagsEqual(slotStack, toPlace)) {
                    ItemStack extracted = unit.getInventory().extractItem(i, 1, false);
                    if (!extracted.isEmpty()) {
                        gridContents.set(slot, extracted);
                        networkSlotRefs.set(slot, i);
                        networkBlockRefs.set(slot, unitPos);
                        if (!skipEvents) onCraftMatrixChanged();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void returnAllToNetwork() {
        for (int i = 0; i < 9; i++) {
            if (!gridContents.get(i).isEmpty()) {
                returnSlotToNetwork(i);
            }
        }
    }

    public void returnSlotToNetwork(int slot) {
        if (slot < 0 || slot >= 9) return;
        ItemStack stack = gridContents.get(slot);
        if (stack.isEmpty()) return;

        BlockPos unitPos = networkBlockRefs.get(slot);
        com.brilliafy.magicstorage.tile.TileStorageUnit unit = getUnitAt(unitPos);
        if (unit != null) {
            int targetSlot = networkSlotRefs.get(slot);
            if (targetSlot >= 0 && targetSlot < unit.getInventory().getSlots()) {
                ItemStack existing = unit.getInventory().getStackInSlot(targetSlot);
                if (existing.isEmpty() || (ItemStack.areItemsEqual(existing, stack)
                        && existing.getCount() + stack.getCount() <= existing.getMaxStackSize())) {
                    unit.getInventory().insertItem(targetSlot, stack, false);
                    clearSlot(slot);
                    return;
                }
            }
            unit.getInventory().insertItem(targetSlot, stack, false);
        }
        clearSlot(slot);
    }

    public void consumeGrid() {
        for (int i = 0; i < 9; i++) {
            clearSlot(i);
        }
        if (!skipEvents) onCraftMatrixChanged();
    }

    private void clearSlot(int slot) {
        gridContents.set(slot, ItemStack.EMPTY);
        networkSlotRefs.set(slot, -1);
        networkBlockRefs.set(slot, BlockPos.ORIGIN);
    }

    private void onCraftMatrixChanged() {
        if (ownerContainer != null) {
            ownerContainer.onCraftMatrixChanged(this);
        }
    }

    public TileStorageHeart getHeart() {
        return heart;
    }

    private java.util.LinkedHashMap<BlockPos, com.brilliafy.magicstorage.tile.TileStorageUnit> getUnits() {
        java.util.LinkedHashMap<BlockPos, com.brilliafy.magicstorage.tile.TileStorageUnit> units = new java.util.LinkedHashMap<>();
        if (heart == null) return units;
        for (BlockPos pos : heart.getConnectedUnitPositions()) {
            net.minecraft.tileentity.TileEntity te = heart.getWorld().getTileEntity(pos);
            if (te instanceof com.brilliafy.magicstorage.tile.TileStorageUnit) {
                units.put(pos, (com.brilliafy.magicstorage.tile.TileStorageUnit) te);
            }
        }
        return units;
    }

    @Nullable
    private com.brilliafy.magicstorage.tile.TileStorageUnit getUnitAt(BlockPos pos) {
        if (heart == null || heart.getWorld() == null) return null;
        net.minecraft.tileentity.TileEntity te = heart.getWorld().getTileEntity(pos);
        if (te instanceof com.brilliafy.magicstorage.tile.TileStorageUnit) {
            return (com.brilliafy.magicstorage.tile.TileStorageUnit) te;
        }
        return null;
    }

    // ===== InventoryCrafting overrides =====

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index < 0 || index >= 9) return ItemStack.EMPTY;
        return gridContents.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (index < 0 || index >= 9) return ItemStack.EMPTY;
        ItemStack stack = gridContents.get(index);
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack result;
        if (stack.getCount() <= count) {
            result = stack.copy();
            gridContents.set(index, ItemStack.EMPTY);
        } else {
            result = stack.splitStack(count);
        }
        if (!result.isEmpty() && !skipEvents) {
            onCraftMatrixChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (index < 0 || index >= 9) return ItemStack.EMPTY;
        ItemStack stack = gridContents.get(index).copy();
        clearSlot(index);
        return stack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 0 || index >= 9) return;

        if (!stack.isEmpty() && gridContents.get(index).isEmpty()) {
            tryPlaceFromNetwork(index, stack);
        } else if (stack.isEmpty() && !gridContents.get(index).isEmpty()) {
            returnSlotToNetwork(index);
        }
    }

    @Override
    public int getSizeInventory() {
        return 9;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : gridContents) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        returnAllToNetwork();
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return false;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        returnAllToNetwork();
    }

    @Override
    public String getName() {
        return "Network Crafting Grid";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public net.minecraft.util.text.ITextComponent getDisplayName() {
        return new net.minecraft.util.text.TextComponentString(getName());
    }

    public void setSkipEvents(boolean skip) {
        this.skipEvents = skip;
    }
}
