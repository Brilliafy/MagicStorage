package com.brilliafy.magicstorage.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * Storage Access TileEntity. When right-clicked, opens the network storage GUI.
 * Must be connected to a Storage Heart via the network.
 */
public class TileStorageAccess extends TileEntity {

    @Nullable
    private BlockPos heartPos = null;

    public TileStorageAccess() {
    }

    @Nullable
    public BlockPos getHeartPos() {
        return heartPos;
    }

    public void setHeartPos(@Nullable BlockPos heartPos) {
        this.heartPos = heartPos;
        markDirty();
    }

    /**
     * Called when the network changes (units added/removed).
     */
    public void onNetworkChanged() {
        markDirty();
    }

    /**
     * Try to find the Storage Heart by searching nearby for one.
     */
    @Nullable
    public TileStorageHeart findHeart() {
        if (heartPos != null) {
            TileEntity te = world.getTileEntity(heartPos);
            if (te instanceof TileStorageHeart) {
                return (TileStorageHeart) te;
            }
        }
        return null;
    }

    /**
     * Get connected unit positions from the heart.
     */
    public java.util.Set<BlockPos> getConnectedUnitPositions() {
        TileStorageHeart heart = findHeart();
        if (heart != null) {
            return heart.getConnectedUnitPositions();
        }
        return java.util.Collections.emptySet();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (heartPos != null) {
            compound.setLong("HeartPos", heartPos.toLong());
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("HeartPos")) {
            heartPos = BlockPos.fromLong(compound.getLong("HeartPos"));
        }
    }
}
