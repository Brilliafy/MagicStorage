package com.brilliafy.magicstorage.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class TileProxyBrewingStand extends TileEntityBrewingStand {

    private final ItemStackHandler inventory = new ItemStackHandler(5);

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) { return true; }

    @Override
    public int getInventoryStackLimit() { return 64; }

    @Override
    public boolean isUsableByPlayer(net.minecraft.entity.player.EntityPlayer player) { return true; }

    @Override
    public String getName() { return "container.brewing"; }

    @Override
    public boolean hasCustomName() { return false; }

    @Override
    public int getSizeInventory() { return 5; }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index < 5 ? inventory.getStackInSlot(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return index < 5 ? inventory.extractItem(index, count, false) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return decrStackSize(index, 64);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 5) inventory.setStackInSlot(index, stack);
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
            return (T) inventory;
        }
        return super.getCapability(capability, facing);
    }
}
