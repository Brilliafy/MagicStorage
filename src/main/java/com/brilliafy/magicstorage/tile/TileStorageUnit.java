package com.brilliafy.magicstorage.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class TileStorageUnit extends TileEntity {

    public static final int[] TIER_SLOT_COUNTS = {40, 80, 80, 120, 160, 220, 300, 600};
    public static final String[] TIER_NAMES = {"Basic", "Crimtane", "Demonite", "Hellstone", "Hallowed", "Blue Chlorophyte", "Luminite", "Terra"};

    private int tier = 0;
    // ALWAYS allocate max size to prevent client/server desync
    private ItemStackHandler inventory = createHandler(TIER_SLOT_COUNTS[TIER_SLOT_COUNTS.length - 1]);
    @Nullable
    private BlockPos heartPos = null;

    public TileStorageUnit() {
    }

    private ItemStackHandler createHandler(int size) {
        return new ItemStackHandler(size) {
            @Override
            public int getSlots() {
                return getSlotCount();
            }
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot < getSlotCount();
            }
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot >= getSlotCount() || stack.isEmpty()) return stack;
                return super.insertItem(slot, stack, simulate);
            }
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot >= getSlotCount()) return ItemStack.EMPTY;
                return super.extractItem(slot, amount, simulate);
            }
            @Override
            public int getSlotLimit(int slot) {
                return slot < getSlotCount() ? super.getSlotLimit(slot) : 0;
            }
            @Override
            protected void onContentsChanged(int slot) {
                markDirty();
                // Notify connected heart that contents changed (for GUI auto-refresh)
                if (world != null && !world.isRemote && heartPos != null) {
                    TileEntity te = world.getTileEntity(heartPos);
                    if (te instanceof com.brilliafy.magicstorage.tile.TileStorageHeart) {
                        ((com.brilliafy.magicstorage.tile.TileStorageHeart) te).markContentsDirty();
                    }
                }
            }
        };
    }

    public int getTier() { return tier; }

    public void setTier(int tier) {
        if (tier < 0 || tier >= TIER_SLOT_COUNTS.length) return;
        // Just update the tier — the handler already has enough capacity (max size = 315)
        // getSlots() returns getSlotCount() which reads this.tier, so it auto-adjusts
        this.tier = tier;
        markDirty();
        if (world != null && !world.isRemote) {
            ((net.minecraft.world.WorldServer) world).getPlayerChunkMap().markBlockForUpdate(pos);
        }
    }

    public int getSlotCount() {
        return TIER_SLOT_COUNTS[tier];
    }

    public String getTierName() {
        return TIER_NAMES[tier];
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public void setHeart(@Nullable BlockPos heartPos) {
        this.heartPos = heartPos;
        markDirty();
    }

    @Nullable
    public BlockPos getHeartPos() {
        return heartPos;
    }

    @Nullable
    public TileStorageHeart findHeart() {
        if (world != null && heartPos != null) {
            TileEntity te = world.getTileEntity(heartPos);
            if (te instanceof TileStorageHeart) {
                return (TileStorageHeart) te;
            }
        }
        return null;
    }

    public ItemStack extractItem(java.util.function.Predicate<ItemStack> matcher, int maxCount, boolean simulate) {
        ItemStackHandler inv = getInventory();
        int remaining = maxCount;
        ItemStack result = ItemStack.EMPTY;
        for (int i = 0; i < getSlotCount() && remaining > 0; i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (!s.isEmpty() && matcher.test(s)) {
                int toExtract = Math.min(remaining, s.getCount());
                ItemStack extracted = s.copy();
                extracted.setCount(toExtract);
                if (result.isEmpty()) {
                    result = extracted;
                } else {
                    result.grow(toExtract);
                }
                if (!simulate) {
                    s.shrink(toExtract);
                    inv.setStackInSlot(i, s);
                }
                remaining -= toExtract;
            }
        }
        return result;
    }

    public boolean canUpgradeTo(int newTier) {
        // Allow upgrading to any higher tier (skip Demonite index 2 in the main chain)
        return newTier > tier && newTier < TIER_SLOT_COUNTS.length;
    }
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        return net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(getInventory(), stack, simulate);
    }

    public void dropContents(net.minecraft.world.World world, BlockPos pos) {
        net.minecraftforge.items.ItemStackHandler inv = getInventory();
        for (int i = 0; i < getSlotCount(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (!s.isEmpty()) {
                net.minecraft.inventory.InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), s);
            }
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(getInventory());
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Tier", tier);
        compound.setTag("Inventory", inventory.serializeNBT());
        if (heartPos != null) {
            compound.setLong("HeartPos", heartPos.toLong());
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        tier = compound.getInteger("Tier");
        if (compound.hasKey("Inventory")) {
            inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        }
        if (compound.hasKey("HeartPos")) {
            heartPos = BlockPos.fromLong(compound.getLong("HeartPos"));
        } else {
            heartPos = null;
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public net.minecraft.network.play.server.SPacketUpdateTileEntity getUpdatePacket() {
        return new net.minecraft.network.play.server.SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, net.minecraft.network.play.server.SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}
