package com.brilliafy.magicstorage.inventory;

import com.brilliafy.magicstorage.tile.TileStorageUnit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Virtual inventory backed by multiple Storage Units in the network.
 * Items are mapped from flat indices to specific unit positions and slots.
 */
public class NetworkStorageInventory extends InventoryBasic {

    private final List<SlotRef> slotRefs = new ArrayList<>();
    private final World world;
    private final List<BlockPos> unitPositions;

    public NetworkStorageInventory(World world, List<BlockPos> unitPositions) {
        super("Network Storage", false, countTotalSlots(world, unitPositions));
        this.world = world;
        this.unitPositions = unitPositions;
        rebuildSlots();
    }

    private static int countTotalSlots(World world, List<BlockPos> unitPositions) {
        int count = 0;
        for (BlockPos pos : unitPositions) {
            net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileStorageUnit) {
                count += ((TileStorageUnit) te).getInventory().getSlots();
            }
        }
        return count;
    }

    public void rebuildSlots() {
        slotRefs.clear();
        for (BlockPos unitPos : unitPositions) {
            net.minecraft.tileentity.TileEntity te = world.getTileEntity(unitPos);
            if (te instanceof TileStorageUnit) {
                TileStorageUnit unit = (TileStorageUnit) te;
                for (int i = 0; i < unit.getInventory().getSlots(); i++) {
                    slotRefs.add(new SlotRef(unitPos, i));
                }
            }
        }
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index < 0 || index >= slotRefs.size()) return ItemStack.EMPTY;
        SlotRef ref = slotRefs.get(index);
        TileStorageUnit unit = getUnit(ref.unitPos);
        if (unit == null) return ItemStack.EMPTY;
        return unit.getInventory().getStackInSlot(ref.slotIndex);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (index < 0 || index >= slotRefs.size()) return ItemStack.EMPTY;
        SlotRef ref = slotRefs.get(index);
        TileStorageUnit unit = getUnit(ref.unitPos);
        if (unit == null) return ItemStack.EMPTY;
        return unit.getInventory().extractItem(ref.slotIndex, count, false);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (index < 0 || index >= slotRefs.size()) return ItemStack.EMPTY;
        SlotRef ref = slotRefs.get(index);
        TileStorageUnit unit = getUnit(ref.unitPos);
        if (unit == null) return ItemStack.EMPTY;
        return unit.getInventory().extractItem(ref.slotIndex, 64, false);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 0 || index >= slotRefs.size()) return;
        SlotRef ref = slotRefs.get(index);
        TileStorageUnit unit = getUnit(ref.unitPos);
        if (unit == null) return;
        unit.getInventory().insertItem(ref.slotIndex, stack, false);
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getSizeInventory() {
        return slotRefs.size();
    }

    @Override
    public boolean isEmpty() {
        for (SlotRef ref : slotRefs) {
            TileStorageUnit unit = getUnit(ref.unitPos);
            if (unit != null && !unit.getInventory().getStackInSlot(ref.slotIndex).isEmpty()) {
                return false;
            }
        }
        return true;
    }
    /**
     * Insert an item into a specific slot. Returns the remainder.
     */
    public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
        if (index < 0 || index >= slotRefs.size()) return stack;
        SlotRef ref = slotRefs.get(index);
        TileStorageUnit unit = getUnit(ref.unitPos);
        if (unit == null) return stack;
        return unit.getInventory().insertItem(ref.slotIndex, stack, simulate);
    }

    public SlotRef getSlotRef(int index) {
        if (index < 0 || index >= slotRefs.size()) return null;
        return slotRefs.get(index);
    }

    public int getSlotCount() {
        return slotRefs.size();
    }

    private TileStorageUnit getUnit(BlockPos pos) {
        if (world == null) return null;
        net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileStorageUnit) return (TileStorageUnit) te;
        return null;
    }

    public static class SlotRef {
        public final BlockPos unitPos;
        public final int slotIndex;

        public SlotRef(BlockPos unitPos, int slotIndex) {
            this.unitPos = unitPos;
            this.slotIndex = slotIndex;
        }
    }
}
