package com.brilliafy.magicstorage.tile;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * Proxy TileEntity that mimics a vanilla furnace for GUI rendering.
 * Items are stored locally and can be backed by the network.
 */
public class TileProxyFurnace extends TileEntityFurnace implements ITickable {

    private final ItemStackHandler fuelSlot = new ItemStackHandler(1);
    private final ItemStackHandler inputSlot = new ItemStackHandler(1);
    private final ItemStackHandler outputSlot = new ItemStackHandler(1);
    private int burnTime = 0;
    private int cookProgress = 0;
    private int totalCookTime = 200;

    @Override
    public boolean isBurning() {
        return burnTime > 0;
    }

    @Override
    public int getField(int id) {
        switch (id) {
            case 0: return burnTime;
            case 1: return cookProgress;
            case 2: return totalCookTime;
            default: return 0;
        }
    }

    @Override
    public void setField(int id, int value) {
        switch (id) {
            case 0: burnTime = value; break;
            case 1: cookProgress = value; break;
            case 2: totalCookTime = value; break;
        }
    }

    @Override
    public int getFieldCount() { return 3; }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getInventoryStackLimit() { return 64; }

    @Override
    public boolean isUsableByPlayer(net.minecraft.entity.player.EntityPlayer player) { return true; }

    @Override
    public String getName() { return "container.furnace"; }

    @Override
    public boolean hasCustomName() { return false; }

    @Override
    public int getSizeInventory() { return 3; }

    @Override
    public ItemStack getStackInSlot(int index) {
        switch (index) {
            case 0: return inputSlot.getStackInSlot(0);
            case 1: return fuelSlot.getStackInSlot(0);
            case 2: return outputSlot.getStackInSlot(0);
            default: return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        switch (index) {
            case 0: return inputSlot.extractItem(0, count, false);
            case 1: return fuelSlot.extractItem(0, count, false);
            case 2: return outputSlot.extractItem(0, count, false);
            default: return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return decrStackSize(index, 64);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        switch (index) {
            case 0: inputSlot.setStackInSlot(0, stack); break;
            case 1: fuelSlot.setStackInSlot(0, stack); break;
            case 2: outputSlot.setStackInSlot(0, stack); break;
        }
    }

    @Override
    public void update() {
        // Simple burn tick for visual feedback
        if (isBurning()) {
            burnTime--;
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) new net.minecraftforge.items.wrapper.CombinedInvWrapper(inputSlot, fuelSlot, outputSlot);
        }
        return super.getCapability(capability, facing);
    }
}
