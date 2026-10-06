package com.brilliafy.magicstorage.tile;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.*;

public class TileCraftingAccess extends TileEntity implements ITickable {

    public static class Station {
        public final String name;
        public final Block block;
        public BlockPos offset;
        public Station(String name, Block block) { this.name = name; this.block = block; }
    }

    public static final Station[] ALL_STATIONS = {
        new Station("crafting_table", Blocks.CRAFTING_TABLE),
        new Station("furnace", Blocks.FURNACE),
        new Station("brewing_stand", Blocks.BREWING_STAND),
        new Station("anvil", Blocks.ANVIL),
        new Station("enchanting_table", Blocks.ENCHANTING_TABLE),
        new Station("rustic_simple_condenser", Block.getBlockFromName("rustic:condenser")),
        new Station("rustic_advanced_condenser", Block.getBlockFromName("rustic:condenser_advanced")),
        new Station("rustic_brewing_barrel", Block.getBlockFromName("rustic:brewing_barrel")),
        new Station("rustic_crushing_tub", Block.getBlockFromName("rustic:crushing_tub")),
        new Station("disenchanter_table", Block.getBlockFromName("disenchanter:disenchantmenttable")),
        new Station("bountiful_reforger", Block.getBlockFromName("bountifulbaubles:reforger")),
        new Station("quality_reforger", Block.getBlockFromName("qualitytools:reforging_station")),
    };

    @Nullable
    private BlockPos heartPos = null;
    private int currentStationIndex = 0;
    private final Set<String> foundStations = new LinkedHashSet<>();
    private int tickCounter = 0;

    public TileCraftingAccess() {}

    @Nullable public BlockPos getHeartPos() { return heartPos; }
    public void setHeartPos(@Nullable BlockPos heartPos) { this.heartPos = heartPos; markDirty(); }

    private List<Station> cachedValidStations = Collections.singletonList(ALL_STATIONS[0]);

    public int getCurrentStationIndex() { return currentStationIndex; }

    public void setCurrentStationIndex(int index) {
        List<Station> valid = getValidStations();
        if (index >= 0 && index < valid.size()) currentStationIndex = index;
    }

    public Station getCurrentStation() {
        List<Station> valid = getValidStations();
        if (currentStationIndex >= 0 && currentStationIndex < valid.size())
            return valid.get(currentStationIndex);
        return ALL_STATIONS[0];
    }

    public int getStationCount() { return getValidStations().size(); }

    public List<Station> getValidStations() {
        return cachedValidStations;
    }

    @Override
    public void update() {
        if (tickCounter++ % 20 == 0 && world != null && !world.isRemote) {
            TileStorageHeart heart = findHeart();
            // Detect stations from the heart's inventory
            Set<String> newStations = new HashSet<>();
            newStations.add("crafting_table"); // always available if CA opens
            if (heart != null) {
                if (heart.hasFurnace()) newStations.add("furnace");
                if (heart.hasBrewingStand()) newStations.add("brewing_stand");
                if (heart.hasAnvil()) newStations.add("anvil");
                if (heart.hasEnchantingTable()) newStations.add("enchanting_table");
                if (heart.hasRusticSimpleCondenser()) newStations.add("rustic_simple_condenser");
                if (heart.hasRusticAdvancedCondenser()) newStations.add("rustic_advanced_condenser");
                if (heart.hasRusticBrewingBarrel()) newStations.add("rustic_brewing_barrel");
                if (heart.hasRusticCrushingTub()) newStations.add("rustic_crushing_tub");
                if (heart.hasDisenchanterTable()) newStations.add("disenchanter_table");
                if (heart.hasBountifulBaublesReforger()) newStations.add("bountiful_reforger");
                if (heart.hasQualityToolsReforger()) newStations.add("quality_reforger");
            }
            if (!newStations.equals(foundStations)) {
                foundStations.clear();
                foundStations.addAll(newStations);
                List<Station> list = new ArrayList<>();
                for (Station s : ALL_STATIONS) {
                    if (foundStations.contains(s.name) || s.name.equals("crafting_table"))
                        list.add(s);
                }
                if (list.isEmpty()) list.add(ALL_STATIONS[0]);
                cachedValidStations = Collections.unmodifiableList(list);
            }
        }
    }

    @Nullable
    public TileStorageHeart findHeart() {
        if (heartPos != null && world != null && world.isBlockLoaded(heartPos)) {
            TileEntity te = world.getTileEntity(heartPos);
            if (te instanceof TileStorageHeart) return (TileStorageHeart) te;
        }
        if (world != null) {
            for (EnumFacing dir : EnumFacing.VALUES) {
                BlockPos neighbor = pos.offset(dir);
                if (world.isBlockLoaded(neighbor)) {
                    TileEntity te = world.getTileEntity(neighbor);
                    if (te instanceof TileStorageHeart) {
                        heartPos = te.getPos();
                        markDirty();
                        return (TileStorageHeart) te;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (heartPos != null) {
            compound.setInteger("heartX", heartPos.getX());
            compound.setInteger("heartY", heartPos.getY());
            compound.setInteger("heartZ", heartPos.getZ());
        }
        compound.setInteger("stationIndex", currentStationIndex);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("heartX")) {
            heartPos = new BlockPos(compound.getInteger("heartX"), compound.getInteger("heartY"), compound.getInteger("heartZ"));
        }
        if (compound.hasKey("stationIndex")) currentStationIndex = compound.getInteger("stationIndex");
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
