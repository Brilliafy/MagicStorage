package com.brilliafy.magicstorage.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public class TileRemoteAccess extends TileEntity implements ITickable {

    @Nullable
    private BlockPos linkedHeartPos = null;

    public TileRemoteAccess() {
    }

    @Nullable
    public BlockPos getLinkedHeartPos() {
        return linkedHeartPos;
    }

    public void setLinkedHeartPos(@Nullable BlockPos pos) {
        this.linkedHeartPos = pos;
        markDirty();
    }

    @Override
    public void update() {
        if (world.isRemote) return;
        // Heart's BFS handles linking. Just check if link is still valid.
        if (linkedHeartPos != null && !(world.getTileEntity(linkedHeartPos) instanceof TileStorageHeart)) {
            setLinkedHeartPos(null);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (linkedHeartPos != null) {
            compound.setLong("LinkedHeart", linkedHeartPos.toLong());
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("LinkedHeart")) {
            linkedHeartPos = BlockPos.fromLong(compound.getLong("LinkedHeart"));
        }
    }
}
