package com.brilliafy.magicstorage.tile;

import com.brilliafy.magicstorage.MagicStorage;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
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

    public static boolean isAllowedStation(ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.item.Item item = stack.getItem();
        if (item == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.CRAFTING_TABLE)) return true;
        if (item == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.FURNACE)) return com.brilliafy.magicstorage.config.ModConfig.enableFurnace;
        if (item == net.minecraft.init.Items.BREWING_STAND || item == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.BREWING_STAND)) return com.brilliafy.magicstorage.config.ModConfig.enableBrewingStand;
        if (item == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.ANVIL)) return com.brilliafy.magicstorage.config.ModConfig.enableAnvil;
        if (item == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.ENCHANTING_TABLE)) return com.brilliafy.magicstorage.config.ModConfig.enableEnchantingTable;
        if (item.getRegistryName() != null) {
            String reg = item.getRegistryName().toString();
            if ("rustic:condenser".equals(reg) || "rustic:condenser_advanced".equals(reg) ||
                "rustic:retort".equals(reg) || "rustic:retort_advanced".equals(reg)) {
                return com.brilliafy.magicstorage.config.ModConfig.enableRusticAlchemy;
            }
            if ("rustic:brewing_barrel".equals(reg)) return com.brilliafy.magicstorage.config.ModConfig.enableRusticBrewing;
            if ("rustic:crushing_tub".equals(reg)) return com.brilliafy.magicstorage.config.ModConfig.enableRusticCrushing;
            if ("disenchanter:disenchantmenttable".equals(reg)) return com.brilliafy.magicstorage.config.ModConfig.enableDisenchanterTable;
            if ("bountifulbaubles:reforger".equals(reg)) return com.brilliafy.magicstorage.config.ModConfig.enableBountifulBaublesReforger;
            if ("qualitytools:reforging_station".equals(reg)) return com.brilliafy.magicstorage.config.ModConfig.enableQualityToolsReforger;
        }
        return false;
    }

    private boolean cachedHasBrewingStand = false;
    private boolean cachedHasAnvil = false;
    private boolean cachedHasFurnace = false;
    private boolean cachedHasEnchantingTable = false;
    private boolean cachedHasCraftingTable = false;
    private boolean cachedHasRusticSimpleCondenser = false;
    private boolean cachedHasRusticAdvancedCondenser = false;
    private boolean cachedHasRusticBrewingBarrel = false;
    private boolean cachedHasRusticCrushingTub = false;
    private boolean cachedHasDisenchanterTable = false;
    private boolean cachedDisenchanterVoiding = false;
    private boolean cachedDisenchanterBulk = false;
    private boolean cachedHasBountifulBaublesReforger = false;
    private boolean cachedHasQualityToolsReforger = false;
    private boolean stationsDirty = true;
    private List<ItemStack> cachedAllItems = null;
    private int cachedEnchantPower = -1;
    private List<TileStorageUnit> cachedUnitTiles = null;

    public List<TileStorageUnit> getValidUnits() {
        if (cachedUnitTiles == null || cachedUnitTiles.size() != connectedUnits.size()) {
            List<TileStorageUnit> list = new ArrayList<>(connectedUnits.size());
            for (BlockPos p : connectedUnits) {
                if (world != null && world.isBlockLoaded(p)) {
                    TileEntity te = world.getTileEntity(p);
                    if (te instanceof TileStorageUnit && !te.isInvalid()) {
                        list.add((TileStorageUnit) te);
                    }
                }
            }
            cachedUnitTiles = list;
        }
        return cachedUnitTiles;
    }

    public void updateStationFlags() {
        cachedHasBrewingStand = false;
        cachedHasAnvil = false;
        cachedHasFurnace = false;
        cachedHasEnchantingTable = false;
        cachedHasCraftingTable = false;
        cachedHasRusticSimpleCondenser = false;
        cachedHasRusticAdvancedCondenser = false;
        cachedHasRusticBrewingBarrel = false;
        cachedHasRusticCrushingTub = false;
        cachedHasDisenchanterTable = false;
        cachedDisenchanterVoiding = false;
        cachedDisenchanterBulk = false;
        cachedHasBountifulBaublesReforger = false;
        cachedHasQualityToolsReforger = false;

        boolean rusticLoaded = net.minecraftforge.fml.common.Loader.isModLoaded("rustic");
        boolean disenchanterLoaded = net.minecraftforge.fml.common.Loader.isModLoaded("disenchanter");
        boolean baublesLoaded = net.minecraftforge.fml.common.Loader.isModLoaded("bountifulbaubles");
        boolean qualityToolsLoaded = net.minecraftforge.fml.common.Loader.isModLoaded("qualitytools");

        int retorts = 0;
        int condensers = 0;
        int advRetorts = 0;
        int advCondensers = 0;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            net.minecraft.item.Item item = stack.getItem();
            if (item == null) continue;

            if (item == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.CRAFTING_TABLE)) {
                cachedHasCraftingTable = true;
            } else if (item == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.FURNACE)) {
                if (com.brilliafy.magicstorage.config.ModConfig.enableFurnace) cachedHasFurnace = true;
            } else if (item == net.minecraft.init.Items.BREWING_STAND || item == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.BREWING_STAND)) {
                if (com.brilliafy.magicstorage.config.ModConfig.enableBrewingStand) cachedHasBrewingStand = true;
            } else if (item == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.ANVIL)) {
                if (com.brilliafy.magicstorage.config.ModConfig.enableAnvil) cachedHasAnvil = true;
            } else if (item == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.ENCHANTING_TABLE)) {
                if (com.brilliafy.magicstorage.config.ModConfig.enableEnchantingTable) cachedHasEnchantingTable = true;
            } else if (item.getRegistryName() != null) {
                String reg = item.getRegistryName().toString();
                if (rusticLoaded) {
                    if ("rustic:retort".equals(reg)) retorts += stack.getCount();
                    else if ("rustic:condenser".equals(reg)) condensers += stack.getCount();
                    else if ("rustic:retort_advanced".equals(reg)) advRetorts += stack.getCount();
                    else if ("rustic:condenser_advanced".equals(reg)) advCondensers += stack.getCount();
                    else if ("rustic:brewing_barrel".equals(reg)) {
                        if (com.brilliafy.magicstorage.config.ModConfig.enableRusticBrewing) cachedHasRusticBrewingBarrel = true;
                    } else if ("rustic:crushing_tub".equals(reg)) {
                        if (com.brilliafy.magicstorage.config.ModConfig.enableRusticCrushing) cachedHasRusticCrushingTub = true;
                    }
                }
                if (disenchanterLoaded && "disenchanter:disenchantmenttable".equals(reg)) {
                    if (com.brilliafy.magicstorage.config.ModConfig.enableDisenchanterTable) {
                        cachedHasDisenchanterTable = true;
                        int meta = stack.getMetadata();
                        if ((meta & 4) != 0) cachedDisenchanterVoiding = true;
                        if ((meta & 2) != 0) cachedDisenchanterBulk = true;
                    }
                } else if (baublesLoaded && "bountifulbaubles:reforger".equals(reg)) {
                    if (com.brilliafy.magicstorage.config.ModConfig.enableBountifulBaublesReforger) cachedHasBountifulBaublesReforger = true;
                } else if (qualityToolsLoaded && "qualitytools:reforging_station".equals(reg)) {
                    if (com.brilliafy.magicstorage.config.ModConfig.enableQualityToolsReforger) cachedHasQualityToolsReforger = true;
                }
            }
        }

        if (com.brilliafy.magicstorage.config.ModConfig.enableRusticAlchemy && rusticLoaded) {
            boolean hasBasic = retorts >= 3 && condensers >= 1;
            boolean hasAdv = advRetorts >= 3 && advCondensers >= 1;
            cachedHasRusticSimpleCondenser = hasBasic || hasAdv;
            cachedHasRusticAdvancedCondenser = (advRetorts >= 3 && advCondensers >= 1) || (retorts >= 3 && advCondensers >= 1);
        }
        stationsDirty = false;
    }

    private final ItemStackHandler inventory = new ItemStackHandler(20) {
        @Override
        public boolean isItemValid(int slot, @javax.annotation.Nonnull ItemStack stack) {
            return isAllowedStation(stack);
        }
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
        @Override
        protected void onContentsChanged(int slot) {
            updateStationFlags();
            TileStorageHeart.this.markDirty();
        }
    };

    public ItemStackHandler getInventory() { return inventory; }

    public void markContentsDirty() {
        cachedAllItems = null;
        cachedEnchantPower = -1;
        if (contentsDirtyTick < 0) contentsDirtyTick = tickCounter;
    }

    public int getEnchantPower(net.minecraft.world.World worldIn) {
        if (cachedEnchantPower >= 0) return cachedEnchantPower;
        cachedEnchantPower = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getPowerFromItems(getAllItems(), worldIn);
        return cachedEnchantPower;
    }

    public boolean hasBrewingStand() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableBrewingStand) return false;
        if (stationsDirty) updateStationFlags();
        return cachedHasBrewingStand;
    }

    public boolean hasAnvil() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableAnvil) return false;
        if (stationsDirty) updateStationFlags();
        return cachedHasAnvil;
    }

    public boolean hasFurnace() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableFurnace) return false;
        if (stationsDirty) updateStationFlags();
        return cachedHasFurnace;
    }

    public boolean hasEnchantingTable() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableEnchantingTable) return false;
        if (stationsDirty) updateStationFlags();
        return cachedHasEnchantingTable;
    }

    public boolean hasCraftingTable() {
        if (stationsDirty) updateStationFlags();
        return cachedHasCraftingTable;
    }

    public boolean hasRusticSimpleCondenser() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableRusticAlchemy || !net.minecraftforge.fml.common.Loader.isModLoaded("rustic")) return false;
        if (stationsDirty) updateStationFlags();
        return cachedHasRusticSimpleCondenser;
    }

    public boolean hasRusticAdvancedCondenser() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableRusticAlchemy || !net.minecraftforge.fml.common.Loader.isModLoaded("rustic")) return false;
        if (stationsDirty) updateStationFlags();
        return cachedHasRusticAdvancedCondenser;
    }

    public boolean hasRusticBrewingBarrel() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableRusticBrewing || !net.minecraftforge.fml.common.Loader.isModLoaded("rustic")) return false;
        if (stationsDirty) updateStationFlags();
        return cachedHasRusticBrewingBarrel;
    }

    public boolean hasRusticCrushingTub() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableRusticCrushing || !net.minecraftforge.fml.common.Loader.isModLoaded("rustic")) return false;
        if (stationsDirty) updateStationFlags();
        return cachedHasRusticCrushingTub;
    }

    public boolean hasDisenchanterTable() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableDisenchanterTable || !net.minecraftforge.fml.common.Loader.isModLoaded("disenchanter")) return false;
        if (stationsDirty) updateStationFlags();
        return cachedHasDisenchanterTable;
    }

    public boolean isDisenchanterVoiding() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableDisenchanterTable || !net.minecraftforge.fml.common.Loader.isModLoaded("disenchanter")) return false;
        if (stationsDirty) updateStationFlags();
        return cachedDisenchanterVoiding;
    }

    public boolean isDisenchanterBulk() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableDisenchanterTable || !net.minecraftforge.fml.common.Loader.isModLoaded("disenchanter")) return false;
        if (stationsDirty) updateStationFlags();
        return cachedDisenchanterBulk;
    }

    public boolean hasBountifulBaublesReforger() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableBountifulBaublesReforger || !net.minecraftforge.fml.common.Loader.isModLoaded("bountifulbaubles")) return false;
        if (stationsDirty) updateStationFlags();
        return cachedHasBountifulBaublesReforger;
    }

    public boolean hasQualityToolsReforger() {
        if (!com.brilliafy.magicstorage.config.ModConfig.enableQualityToolsReforger || !net.minecraftforge.fml.common.Loader.isModLoaded("qualitytools")) return false;
        if (stationsDirty) updateStationFlags();
        return cachedHasQualityToolsReforger;
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

        if (com.brilliafy.magicstorage.config.ModConfig.softAutoSortEnabled && connectedUnits.size() >= 2 && tickCounter % (com.brilliafy.magicstorage.config.ModConfig.softAutoSortIntervalSeconds * 20) == 0) {
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
            try {
                ForgeChunkManager.releaseTicket(chunkTicket);
            } catch (Throwable t) {
                MagicStorage.LOGGER.warn("Failed to release chunk ticket: " + t.getMessage());
            } finally {
                chunkTicket = null;
            }
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
        releaseChunkTicket();
        try {
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
        } catch (Throwable t) {
            MagicStorage.LOGGER.warn("Failed to update forced chunks for Heart at " + pos + ": " + t.getMessage());
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
                    if (h == this) {
                        com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(networkRefreshMsg, player);
                        // Refresh enchanting/anvil result when network items change
                        ((com.brilliafy.magicstorage.container.ContainerMagicStorageBase) container).onCraftMatrixChanged(
                            ((com.brilliafy.magicstorage.container.ContainerMagicStorageBase) container).getCraftMatrix());
                    }
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
        Set<BlockPos> newUnits = new LinkedHashSet<>();
        Set<BlockPos> newAccessPoints = new LinkedHashSet<>();

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
                    newUnits.add(neighbor);
                    ((TileStorageUnit) te).setHeart(pos);
                    queue.add(neighbor);
                } else if (te instanceof TileStorageAccess) {
                    newAccessPoints.add(neighbor);
                    ((TileStorageAccess) te).setHeartPos(pos);
                    queue.add(neighbor);
                } else if (te instanceof TileCraftingAccess) {
                    newAccessPoints.add(neighbor);
                    ((TileCraftingAccess) te).setHeartPos(pos);
                    queue.add(neighbor);
                } else if (te instanceof TileRemoteAccess) {
                    newAccessPoints.add(neighbor);
                    ((TileRemoteAccess) te).setLinkedHeartPos(pos);
                    queue.add(neighbor);
                } else if (te instanceof TileStorageHeart && !neighbor.equals(pos)) {
                    queue.add(neighbor);
                }
            }
        }

        boolean topologyChanged = !newUnits.equals(connectedUnits) || !newAccessPoints.equals(connectedAccessPoints);
        if (topologyChanged || chunkTicket == null) {
            connectedUnits.clear();
            connectedUnits.addAll(newUnits);
            connectedAccessPoints.clear();
            connectedAccessPoints.addAll(newAccessPoints);
            cachedAllItems = null;
            cachedUnitTiles = null;
            cachedEnchantPower = -1;
            markDirty();
            // Keep chunks loaded so remote access works across dimensions
            updateForcedChunks();
        }
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
        cachedAllItems = null;
        cachedUnitTiles = null;
        cachedEnchantPower = -1;
        // Release chunkloading ticket so the chunks can unload
        releaseChunkTicket();
    }

    public int getConnectedUnits() { return connectedUnits.size(); }
    public Set<BlockPos> getConnectedUnitPositions() {
        if (needsRefresh || connectedUnits.isEmpty()) {
            refreshNetwork();
            needsRefresh = false;
        }
        return Collections.unmodifiableSet(connectedUnits);
    }

    private static class ItemStackKey {
        final Item item;
        final int metadata;
        final NBTTagCompound nbt;

        ItemStackKey(ItemStack stack) {
            this.item = stack.getItem();
            this.metadata = stack.getMetadata();
            this.nbt = com.brilliafy.magicstorage.util.ItemMatchHelper.getComparableNBT(stack);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ItemStackKey)) return false;
            ItemStackKey other = (ItemStackKey) o;
            if (this.item != other.item || this.metadata != other.metadata) return false;
            return java.util.Objects.equals(this.nbt, other.nbt);
        }

        @Override
        public int hashCode() {
            int hash = System.identityHashCode(item);
            hash = 31 * hash + metadata;
            if (nbt != null) hash = 31 * hash + nbt.hashCode();
            return hash;
        }
    }

    public List<ItemStack> getAllItems() {
        if (cachedAllItems != null) {
            return new ArrayList<>(cachedAllItems);
        }
        Map<ItemStackKey, ItemStack> merged = new LinkedHashMap<>();
        List<TileStorageUnit> units = getValidUnits();
        for (TileStorageUnit unit : units) {
            if (unit.isEmpty()) continue;
            for (int i = 0; i < unit.getSlotCount(); i++) {
                ItemStack s = unit.getInventory().getStackInSlot(i);
                if (!s.isEmpty()) {
                    ItemStackKey key = new ItemStackKey(s);
                    ItemStack existing = merged.get(key);
                    if (existing != null) {
                        existing.grow(s.getCount());
                    } else {
                        merged.put(key, s.copy());
                    }
                }
            }
        }
        cachedAllItems = new ArrayList<>(merged.values());
        return new ArrayList<>(cachedAllItems);
    }

    public void autosort() {
        if (world.isRemote || connectedUnits.size() < 2) return;

        List<TileStorageUnit> validUnits = getValidUnits();
        if (validUnits.size() < 2) return;

        List<TileStorageUnit> sortedUnits = new ArrayList<>(validUnits);
        sortedUnits.sort((a, b) -> {
            int cmp = Double.compare(a.getPos().distanceSq(pos), b.getPos().distanceSq(pos));
            if (cmp != 0) return cmp;
            return a.getPos().toLong() < b.getPos().toLong() ? -1 : (a.getPos().toLong() > b.getPos().toLong() ? 1 : 0);
        });

        Map<BlockPos, TileStorageUnit> unitMap = new LinkedHashMap<>();
        for (TileStorageUnit unit : sortedUnits) {
            unitMap.put(unit.getPos(), unit);
        }

        Map<ItemStackKey, List<SlotEntry>> itemMap = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, TileStorageUnit> uEntry : unitMap.entrySet()) {
            BlockPos up = uEntry.getKey();
            TileStorageUnit unit = uEntry.getValue();
            if (unit.isEmpty()) continue;
            for (int i = 0; i < unit.getSlotCount(); i++) {
                ItemStack s = unit.getInventory().getStackInSlot(i);
                if (!s.isEmpty()) {
                    ItemStackKey key = new ItemStackKey(s);
                    itemMap.computeIfAbsent(key, k -> new ArrayList<>()).add(new SlotEntry(up, i, s.getCount()));
                }
            }
        }

        boolean anyMoved = false;
        for (Map.Entry<ItemStackKey, List<SlotEntry>> entry : itemMap.entrySet()) {
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
                                    anyMoved = true;
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
                        anyMoved = true;
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
                                anyMoved = true;
                                srcStack.shrink(moved);
                                if (srcStack.isEmpty()) srcUnit.getInventory().setStackInSlot(src.slot, ItemStack.EMPTY);
                                nr -= moved;
                            }
                        }
                    }
                }
            }
        }
        if (anyMoved) {
            markContentsDirty();
        }
    }

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remainder = stack.copy();
        List<TileStorageUnit> units = getValidUnits();

        // Pass 1: Try units that already contain this item type
        for (TileStorageUnit unit : units) {
            if (unit.containsItem(remainder.getItem())) {
                remainder = unit.insertItem(remainder, simulate);
                if (remainder.isEmpty()) {
                    if (!simulate) markContentsDirty();
                    return ItemStack.EMPTY;
                }
            }
        }

        // Pass 2: Try units that have empty slots
        for (TileStorageUnit unit : units) {
            if (unit.hasEmptySlot()) {
                remainder = unit.insertItem(remainder, simulate);
                if (remainder.isEmpty()) {
                    if (!simulate) markContentsDirty();
                    return ItemStack.EMPTY;
                }
            }
        }

        if (!simulate && remainder.getCount() < stack.getCount()) {
            markContentsDirty();
        }
        return remainder;
    }

    public ItemStack extractItem(java.util.function.Predicate<ItemStack> matcher, int maxCount, boolean simulate) {
        if (maxCount <= 0) return ItemStack.EMPTY;
        ItemStack extracted = ItemStack.EMPTY;
        int needed = maxCount;
        List<TileStorageUnit> units = getValidUnits();

        for (TileStorageUnit unit : units) {
            if (unit.isEmpty()) continue;
            for (int i = 0; i < unit.getSlotCount(); i++) {
                ItemStack s = unit.getInventory().getStackInSlot(i);
                if (!s.isEmpty() && matcher.test(s)) {
                    if (extracted.isEmpty() || com.brilliafy.magicstorage.util.ItemMatchHelper.matchesStorageItem(extracted, s)) {
                        int toExtract = Math.min(needed, s.getCount());
                        if (extracted.isEmpty()) {
                            extracted = s.copy();
                            extracted.setCount(toExtract);
                        } else {
                            extracted.grow(toExtract);
                        }
                        needed -= toExtract;
                        if (!simulate) {
                            s.shrink(toExtract);
                            unit.getInventory().setStackInSlot(i, s.isEmpty() ? ItemStack.EMPTY : s);
                        }
                        if (needed <= 0) {
                            if (!simulate) markContentsDirty();
                            return extracted;
                        }
                    }
                }
            }
        }
        if (!simulate && !extracted.isEmpty()) {
            markContentsDirty();
        }
        return extracted;
    }

    @Nullable
    public BlockPos findNearestUnit() {
        return connectedUnits.isEmpty() ? null : connectedUnits.iterator().next();
    }

    private int brewingCraftCounter = 0;
    public int getBrewingCraftCounter() { return brewingCraftCounter; }
    public void incrementBrewingCraftCounter() { brewingCraftCounter++; markDirty(); }

    private int reforgeCraftCounter = 0;
    public int getReforgeCraftCounter() { return reforgeCraftCounter; }
    public void incrementReforgeCraftCounter() { reforgeCraftCounter++; markDirty(); }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("brewingCraftCounter", brewingCraftCounter);
        compound.setInteger("reforgeCraftCounter", reforgeCraftCounter);
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
        if (compound.hasKey("Inventory")) {
            inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
            updateStationFlags();
        }
        if (compound.hasKey("brewingCraftCounter")) brewingCraftCounter = compound.getInteger("brewingCraftCounter");
        if (compound.hasKey("reforgeCraftCounter")) reforgeCraftCounter = compound.getInteger("reforgeCraftCounter");
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
