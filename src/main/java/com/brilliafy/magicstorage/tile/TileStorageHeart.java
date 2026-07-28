package com.brilliafy.magicstorage.tile;

import com.brilliafy.magicstorage.MagicStorage;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.*;

public class TileStorageHeart extends TileEntity implements ITickable {

    // Chunkloading ticket — force-loads this heart's network so remote access works across dimensions
    private transient ForgeChunkManager.Ticket chunkTicket = null;

    private final Set<BlockPos> connectedUnits = new LinkedHashSet<>();
    private final Set<BlockPos> connectedAccessPoints = new LinkedHashSet<>();
    private boolean needsRefresh = true;
    private int tickCounter = 0;
    private int contentsDirtyTick = -1;

    private final ItemStackHandler inventory = new ItemStackHandler(20);

    public ItemStackHandler getInventory() { return inventory; }

    public void markContentsDirty() {
        if (contentsDirtyTick < 0) contentsDirtyTick = tickCounter;
    }

    public boolean hasBrewingStand() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                // Compare by registry name — Item.getItemFromBlock and Block.getBlockFromItem
                // both fail for brewing stands in some Forge versions
                if ("minecraft:brewing_stand".equals(stack.getItem().getRegistryName().toString())) return true;
            }
        }
        return false;
    }

    public boolean hasAnvil() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && "minecraft:anvil".equals(stack.getItem().getRegistryName().toString())) return true;
        }
        return false;
    }

    public boolean hasFurnace() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && "minecraft:furnace".equals(stack.getItem().getRegistryName().toString())) return true;
        }
        return false;
    }

    public boolean hasEnchantingTable() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && "minecraft:enchanting_table".equals(stack.getItem().getRegistryName().toString())) return true;
        }
        return false;
    }

    public boolean hasCraftingTable() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && "minecraft:crafting_table".equals(stack.getItem().getRegistryName().toString())) return true;
        }
        return false;
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
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void update() {
        if (world.isRemote) return;
        tickCounter++;
        int interval = connectedUnits.isEmpty() ? 20 : 80;
        if (tickCounter == 1 || tickCounter % interval == 0) needsRefresh = true;
        if (needsRefresh) {
            refreshNetwork();
            needsRefresh = false;
        }

        if (contentsDirtyTick >= 0 && tickCounter - contentsDirtyTick >= 10) {
            contentsDirtyTick = -1;
            refreshOpenGUIs();
        }

        if (connectedUnits.size() >= 2 && tickCounter % 1200 == 0) {
            autosort();
        }
    }

    @Override
    public void onLoad() {
        if (world != null && !world.isRemote) {
            markNeedsRefresh();
            requestChunkTicket();
        }
    }

    @Override
    public void onChunkUnload() {
        releaseChunkTicket();
    }

    @Override
    public void invalidate() {
        releaseChunkTicket();
        super.invalidate();
    }

    // ======== Chunkloading ========

    public void setTicket(ForgeChunkManager.Ticket ticket) {
        this.chunkTicket = ticket;
    }

    private void requestChunkTicket() {
        if (world == null || world.isRemote) return;
        if (chunkTicket != null) return; // already handled by updateForcedChunks
        updateForcedChunks();
    }

    private void releaseChunkTicket() {
        if (chunkTicket != null) {
            ForgeChunkManager.releaseTicket(chunkTicket);
            chunkTicket = null;
        }
    }

    /**
     * Force-load this heart's chunk and all connected storage unit chunks.
     * Automatically releases old ticket and gets a fresh one, so removed units
     * are no longer force-loaded.
     * Call after refreshNetwork() discovers new connected units.
     */
    public void updateForcedChunks() {
        if (world == null || world.isRemote) return;
        // Release old ticket to clear previously forced chunks
        if (chunkTicket != null) {
            ForgeChunkManager.releaseTicket(chunkTicket);
            chunkTicket = null;
        }
        // Request fresh ticket
        chunkTicket = ForgeChunkManager.requestTicket(MagicStorage.instance, world, ForgeChunkManager.Type.NORMAL);
        if (chunkTicket == null) return;
        // Store heart position so callback can re-register on world load
        chunkTicket.getModData().setInteger("heartX", pos.getX());
        chunkTicket.getModData().setInteger("heartY", pos.getY());
        chunkTicket.getModData().setInteger("heartZ", pos.getZ());
        // Force the heart's own chunk
        net.minecraft.util.math.ChunkPos heartChunk = new net.minecraft.util.math.ChunkPos(pos);
        ForgeChunkManager.forceChunk(chunkTicket, heartChunk);
        // Force chunks of all connected storage units
        for (BlockPos unitPos : connectedUnits) {
            net.minecraft.util.math.ChunkPos unitChunk = new net.minecraft.util.math.ChunkPos(unitPos);
            ForgeChunkManager.forceChunk(chunkTicket, unitChunk);
        }
        // Force chunks of all connected access points
        for (BlockPos accessPos : connectedAccessPoints) {
            net.minecraft.util.math.ChunkPos accessChunk = new net.minecraft.util.math.ChunkPos(accessPos);
            ForgeChunkManager.forceChunk(chunkTicket, accessChunk);
        }
    }

    public void refreshOpenGUIs() {
        if (world == null || world.isRemote) return;
        List<ItemStack> allItems = getAllItems();
        com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage networkRefreshMsg =
            new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(allItems, new ArrayList<>());
        for (net.minecraft.entity.player.EntityPlayerMP player : net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers()) {
            if (player.openContainer instanceof com.brilliafy.magicstorage.gui.IStorageContainer) {
                com.brilliafy.magicstorage.gui.IStorageContainer container = (com.brilliafy.magicstorage.gui.IStorageContainer) player.openContainer;
                if (container instanceof com.brilliafy.magicstorage.container.ContainerMagicStorageBase) {
                    TileStorageHeart h = ((com.brilliafy.magicstorage.container.ContainerMagicStorageBase) container).getTileMaster();
                    if (h == this) com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(networkRefreshMsg, player);
                } else if (container instanceof com.brilliafy.magicstorage.container.ContainerStorageAccess) {
                    com.brilliafy.magicstorage.container.ContainerStorageAccess ca = (com.brilliafy.magicstorage.container.ContainerStorageAccess) container;
                    if (ca.getAccessTile() != null) {
                        TileStorageHeart h = ca.getAccessTile().findHeart();
                        if (h == this) com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(networkRefreshMsg, player);
                    }
                } else if (container instanceof com.brilliafy.magicstorage.container.ContainerStorageUnit) {
                    TileStorageUnit su = ((com.brilliafy.magicstorage.container.ContainerStorageUnit) container).getUnit();
                    if (su != null) {
                        List<ItemStack> unitItems = new ArrayList<>();
                        for (int i = 0; i < su.getSlotCount(); i++) {
                            ItemStack s = su.getInventory().getStackInSlot(i);
                            if (!s.isEmpty()) {
                                boolean found = false;
                                for (ItemStack existing : unitItems) {
                                    if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(existing, s)) {
                                        existing.grow(s.getCount());
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) unitItems.add(s.copy());
                            }
                        }
                        com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                            new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(unitItems, new ArrayList<>()), player);
                    }
                }
            }
        }
    }

    public void markNeedsRefresh() { needsRefresh = true; }

    public void refreshNetwork() {
        connectedUnits.clear();
        connectedAccessPoints.clear();

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(pos);
        visited.add(pos);

        final int MAX_DISTANCE = 32;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (current.distanceSq(pos) > MAX_DISTANCE * MAX_DISTANCE) continue;

            for (EnumFacing dir : EnumFacing.VALUES) {
                BlockPos neighbor = current.offset(dir);
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);
                if (!world.isBlockLoaded(neighbor)) continue;

                TileEntity te = world.getTileEntity(neighbor);
                if (te instanceof TileStorageUnit) {
                    connectedUnits.add(neighbor);
                    ((TileStorageUnit) te).setHeart(pos);
                    queue.add(neighbor);
                } else if (te instanceof TileStorageAccess) {
                    connectedAccessPoints.add(neighbor);
                    ((TileStorageAccess) te).setHeartPos(pos);
                    queue.add(neighbor);
                } else if (te instanceof TileCraftingAccess) {
                    connectedAccessPoints.add(neighbor);
                    ((TileCraftingAccess) te).setHeartPos(pos);
                    queue.add(neighbor);
                } else if (te instanceof TileRemoteAccess) {
                    connectedAccessPoints.add(neighbor);
                    ((TileRemoteAccess) te).setLinkedHeartPos(pos);
                    queue.add(neighbor);
                } else if (te instanceof TileStorageHeart && !neighbor.equals(pos)) {
                    queue.add(neighbor);
                }
            }
        }
        markDirty();
        // Keep chunks loaded so remote access works across dimensions
        updateForcedChunks();
    }

    public void disconnectNetwork() {
        for (BlockPos p : connectedUnits) {
            TileEntity te = world.getTileEntity(p);
            if (te instanceof TileStorageUnit) ((TileStorageUnit) te).setHeart(null);
        }
        for (BlockPos p : connectedAccessPoints) {
            TileEntity te = world.getTileEntity(p);
            if (te instanceof TileStorageAccess) ((TileStorageAccess) te).setHeartPos(null);
            if (te instanceof TileCraftingAccess) ((TileCraftingAccess) te).setHeartPos(null);
        }
        connectedUnits.clear();
        connectedAccessPoints.clear();
        // Release chunkloading ticket so the chunks can unload
        releaseChunkTicket();
    }

    public int getConnectedUnits() { return connectedUnits.size(); }
    public Set<BlockPos> getConnectedUnitPositions() { return Collections.unmodifiableSet(connectedUnits); }

    public List<ItemStack> getAllItems() {
        Map<ItemStack, Integer> merged = new LinkedHashMap<>();
        for (BlockPos p : connectedUnits) {
            TileEntity te = world.getTileEntity(p);
            if (te instanceof TileStorageUnit) {
                TileStorageUnit unit = (TileStorageUnit) te;
                for (int i = 0; i < unit.getSlotCount(); i++) {
                    ItemStack s = unit.getInventory().getStackInSlot(i);
                    if (!s.isEmpty()) {
                        boolean found = false;
                        for (Map.Entry<ItemStack, Integer> entry : merged.entrySet()) {
                            if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(entry.getKey(), s)) {
                                entry.setValue(entry.getValue() + s.getCount());
                                found = true;
                                break;
                            }
                        }
                        if (!found) merged.put(s.copy(), s.getCount());
                    }
                }
            }
        }
        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<ItemStack, Integer> entry : merged.entrySet()) {
            ItemStack stack = entry.getKey();
            stack.setCount(entry.getValue());
            result.add(stack);
        }
        return result;
    }

    public void autosort() {
        if (world.isRemote || connectedUnits.size() < 2) return;

        List<BlockPos> sortedUnits = new ArrayList<>(connectedUnits);
        sortedUnits.sort((a, b) -> {
            int cmp = Double.compare(a.distanceSq(pos), b.distanceSq(pos));
            if (cmp != 0) return cmp;
            return a.toLong() < b.toLong() ? -1 : (a.toLong() > b.toLong() ? 1 : 0);
        });

        Map<BlockPos, TileStorageUnit> unitMap = new LinkedHashMap<>();
        for (BlockPos p : sortedUnits) {
            TileEntity te = world.getTileEntity(p);
            if (te instanceof TileStorageUnit) unitMap.put(p, (TileStorageUnit) te);
        }
        if (unitMap.size() < 2) return;

        Map<String, List<SlotEntry>> itemMap = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, TileStorageUnit> uEntry : unitMap.entrySet()) {
            BlockPos up = uEntry.getKey();
            TileStorageUnit unit = uEntry.getValue();
            for (int i = 0; i < unit.getSlotCount(); i++) {
                ItemStack s = unit.getInventory().getStackInSlot(i);
                if (!s.isEmpty()) {
                    String key = s.getItem().getRegistryName() + "@" + s.getMetadata();
                    if (s.hasTagCompound()) key += "#" + s.getTagCompound().toString();
                    itemMap.computeIfAbsent(key, k -> new ArrayList<>()).add(new SlotEntry(up, i, s.getCount()));
                }
            }
        }

        for (Map.Entry<String, List<SlotEntry>> entry : itemMap.entrySet()) {
            List<SlotEntry> slots = entry.getValue();
            if (slots.size() < 2) continue;
            slots.sort((a, b) -> {
                int cmp = Double.compare(a.unitPos.distanceSq(pos), b.unitPos.distanceSq(pos));
                if (cmp != 0) return cmp;
                return Integer.compare(a.slot, b.slot);
            });

            SlotEntry nearest = slots.get(0);
            TileStorageUnit nearestUnit = unitMap.get(nearest.unitPos);
            if (nearestUnit == null) continue;

            ItemStack nearestStack = nearestUnit.getInventory().getStackInSlot(nearest.slot);
            if (nearestStack.isEmpty()) continue;
            int maxStack = nearestStack.getMaxStackSize();
            int roomInTarget = maxStack - nearestStack.getCount();

            if (roomInTarget <= 0) {
                for (int si = 1; si < slots.size(); si++) {
                    SlotEntry alt = slots.get(si);
                    TileStorageUnit altUnit = unitMap.get(alt.unitPos);
                    if (altUnit == null) continue;
                    ItemStack altStack = altUnit.getInventory().getStackInSlot(alt.slot);
                    if (altStack.isEmpty()) continue;
                    int altRoom = altStack.getMaxStackSize() - altStack.getCount();
                    if (altRoom > 0) {
                        for (int sj = slots.size() - 1; sj > si; sj--) {
                            if (roomInTarget >= altRoom) break;
                            SlotEntry src = slots.get(sj);
                            TileStorageUnit srcUnit = unitMap.get(src.unitPos);
                            if (srcUnit == null) continue;
                            ItemStack srcStack = srcUnit.getInventory().getStackInSlot(src.slot);
                            if (srcStack.isEmpty()) continue;
                            int canTake = Math.min(altRoom - roomInTarget, srcStack.getCount());
                            if (canTake > 0) {
                                ItemStack toInsert = srcStack.copy();
                                toInsert.setCount(canTake);
                                ItemStack remainder = altUnit.getInventory().insertItem(alt.slot, toInsert, false);
                                int moved = canTake - remainder.getCount();
                                if (moved > 0) {
                                    srcStack.shrink(moved);
                                    if (srcStack.isEmpty()) srcUnit.getInventory().setStackInSlot(src.slot, ItemStack.EMPTY);
                                    roomInTarget += moved;
                                }
                            }
                        }
                    }
                }
                continue;
            }

            for (int si = slots.size() - 1; si > 0 && roomInTarget > 0; si--) {
                SlotEntry src = slots.get(si);
                if (src.unitPos.equals(nearest.unitPos) && src.slot == nearest.slot) continue;
                TileStorageUnit srcUnit = unitMap.get(src.unitPos);
                if (srcUnit == null) continue;
                ItemStack srcStack = srcUnit.getInventory().getStackInSlot(src.slot);
                if (srcStack.isEmpty()) continue;
                int canTake = Math.min(roomInTarget, srcStack.getCount());
                if (canTake > 0) {
                    ItemStack toInsert = srcStack.copy();
                    toInsert.setCount(canTake);
                    ItemStack remainder = nearestUnit.getInventory().insertItem(nearest.slot, toInsert, false);
                    int moved = canTake - remainder.getCount();
                    if (moved > 0) {
                        srcStack.shrink(moved);
                        if (srcStack.isEmpty()) srcUnit.getInventory().setStackInSlot(src.slot, ItemStack.EMPTY);
                        roomInTarget -= moved;
                    }
                }
            }

            if (roomInTarget <= 0) {
                for (int si = 1; si < slots.size() && roomInTarget <= 0; si++) {
                    SlotEntry nextNearest = slots.get(si);
                    TileStorageUnit nextUnit = unitMap.get(nextNearest.unitPos);
                    if (nextUnit == null) continue;
                    ItemStack ns = nextUnit.getInventory().getStackInSlot(nextNearest.slot);
                    if (ns.isEmpty()) continue;
                    int nr = ns.getMaxStackSize() - ns.getCount();
                    if (nr <= 0) continue;
                    for (int sj = slots.size() - 1; sj > si && nr > 0; sj--) {
                        SlotEntry src = slots.get(sj);
                        if (src.unitPos.equals(nextNearest.unitPos) && src.slot == nextNearest.slot) continue;
                        TileStorageUnit srcUnit = unitMap.get(src.unitPos);
                        if (srcUnit == null) continue;
                        ItemStack srcStack = srcUnit.getInventory().getStackInSlot(src.slot);
                        if (srcStack.isEmpty()) continue;
                        int canTake = Math.min(nr, srcStack.getCount());
                        if (canTake > 0) {
                            ItemStack toInsert = srcStack.copy();
                            toInsert.setCount(canTake);
                            ItemStack remainder = nextUnit.getInventory().insertItem(nextNearest.slot, toInsert, false);
                            int moved = canTake - remainder.getCount();
                            if (moved > 0) {
                                srcStack.shrink(moved);
                                if (srcStack.isEmpty()) srcUnit.getInventory().setStackInSlot(src.slot, ItemStack.EMPTY);
                                nr -= moved;
                            }
                        }
                    }
                }
            }
        }
        refreshOpenGUIs();
    }

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        ItemStack remainder = stack.copy();
        for (BlockPos p : connectedUnits) {
            TileEntity te = world.getTileEntity(p);
            if (te instanceof TileStorageUnit) {
                TileStorageUnit unit = (TileStorageUnit) te;
                remainder = unit.insertItem(remainder, simulate);
                if (remainder.isEmpty()) return ItemStack.EMPTY;
            }
        }
        if (!remainder.isEmpty()) {
            remainder = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(inventory, remainder, simulate);
        }
        return remainder;
    }

    public ItemStack extractItem(java.util.function.Predicate<ItemStack> matcher, int maxCount, boolean simulate) {
        for (BlockPos p : connectedUnits) {
            TileEntity te = world.getTileEntity(p);
            if (te instanceof TileStorageUnit) {
                TileStorageUnit unit = (TileStorageUnit) te;
                for (int i = 0; i < unit.getSlotCount(); i++) {
                    ItemStack s = unit.getInventory().getStackInSlot(i);
                    if (!s.isEmpty() && matcher.test(s)) {
                        int toExtract = Math.min(maxCount, s.getCount());
                        ItemStack extracted = s.copy();
                        extracted.setCount(toExtract);
                        if (!simulate) {
                            s.shrink(toExtract);
                            unit.getInventory().setStackInSlot(i, s);
                        }
                        return extracted;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    public BlockPos findNearestUnit() {
        return connectedUnits.isEmpty() ? null : connectedUnits.iterator().next();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        NBTTagList units = new NBTTagList();
        for (BlockPos p : connectedUnits) {
            NBTTagCompound c = new NBTTagCompound();
            c.setLong("Pos", p.toLong());
            units.appendTag(c);
        }
        compound.setTag("ConnectedUnits", units);
        NBTTagList access = new NBTTagList();
        for (BlockPos p : connectedAccessPoints) {
            NBTTagCompound c = new NBTTagCompound();
            c.setLong("Pos", p.toLong());
            access.appendTag(c);
        }
        compound.setTag("ConnectedAccess", access);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        connectedUnits.clear();
        NBTTagList units = compound.getTagList("ConnectedUnits", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < units.tagCount(); i++) {
            connectedUnits.add(BlockPos.fromLong(units.getCompoundTagAt(i).getLong("Pos")));
        }
        connectedAccessPoints.clear();
        NBTTagList access = compound.getTagList("ConnectedAccess", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < access.tagCount(); i++) {
            connectedAccessPoints.add(BlockPos.fromLong(access.getCompoundTagAt(i).getLong("Pos")));
        }
    }

    public static class ItemStackWithPos {
        public final ItemStack stack;
        public final BlockPos pos;
        public final int slot;
        public ItemStackWithPos(ItemStack stack, BlockPos pos, int slot) {
            this.stack = stack; this.pos = pos; this.slot = slot;
        }
    }

    private static class SlotEntry {
        final BlockPos unitPos;
        final int slot;
        final int count;
        SlotEntry(BlockPos unitPos, int slot, int count) {
            this.unitPos = unitPos; this.slot = slot; this.count = count;
        }
    }
}
