package com.brilliafy.magicstorage.inventory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

/**
 * Wraps a backing ItemStackHandler and exposes only a sliding window of slots.
 * Implements IItemHandlerModifiable so SlotItemHandler can write to it.
 */
public class ScrollableItemHandler implements IItemHandlerModifiable {

    private final ItemStackHandler backing;
    private final int totalSize;
    private int scrollOffset = 0;
    private final int windowSize;

    public ScrollableItemHandler(ItemStackHandler backing, int windowSize) {
        this.backing = backing;
        this.totalSize = backing.getSlots();
        this.windowSize = Math.min(windowSize, totalSize);
    }

    public void setScrollOffset(int offset) {
        int max = Math.max(0, totalSize - windowSize);
        this.scrollOffset = Math.max(0, Math.min(max, offset));
    }

    public int getScrollOffset() { return scrollOffset; }
    public int getWindowSize() { return windowSize; }
    public int getTotalSize() { return totalSize; }

    @Override
    public int getSlots() { return Math.min(windowSize, totalSize); }

    private int mapSlot(int visibleSlot) {
        return Math.min(visibleSlot + scrollOffset, totalSize - 1);
    }

    @Override @Nonnull
    public ItemStack getStackInSlot(int slot) {
        if (slot >= windowSize) return ItemStack.EMPTY;
        return backing.getStackInSlot(mapSlot(slot));
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        if (slot >= windowSize) return;
        backing.setStackInSlot(mapSlot(slot), stack);
    }

    @Override @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (slot >= windowSize) return stack;
        return backing.insertItem(mapSlot(slot), stack, simulate);
    }

    @Override @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot >= windowSize) return ItemStack.EMPTY;
        return backing.extractItem(mapSlot(slot), amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot >= windowSize) return 0;
        return backing.getSlotLimit(mapSlot(slot));
    }
}
