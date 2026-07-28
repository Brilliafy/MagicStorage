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
    };

    @Nullable
    private BlockPos heartPos = null;
    private int currentStationIndex = 0;
    private final Set<String> foundStations = new LinkedHashSet<>();
    private int tickCounter = 0;

    public TileCraftingAccess() {}

    @Nullable public BlockPos getHeartPos() { return heartPos; }
    public void setHeartPos(@Nullable BlockPos heartPos) { this.heartPos = heartPos; markDirty(); }

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
        List<Station> list = new ArrayList<>();
        for (Station s : ALL_STATIONS) {
            if (foundStations.contains(s.name) || s.name.equals("crafting_table"))
                list.add(s);
        }
        if (list.isEmpty()) list.add(ALL_STATIONS[0]);
        return list;
    }

    @Override
    public void update() {
        if (tickCounter++ % 20 == 0 && world != null && !world.isRemote) {
            TileStorageHeart heart = findHeart();
            if (heart == null) {
                // Try to find a heart adjacent
                for (EnumFacing dir : EnumFacing.VALUES) {
                    TileEntity te = world.getTileEntity(pos.offset(dir));
                    if (te instanceof TileStorageHeart) {
                        heartPos = te.getPos();
                        break;
                    }
                }
            }
            // Detect stations from the heart's inventory
            foundStations.clear();
            foundStations.add("crafting_table"); // always available if CA opens
            if (heart != null) {
                if (heart.hasFurnace()) foundStations.add("furnace");
                if (heart.hasBrewingStand()) foundStations.add("brewing_stand");
                if (heart.hasAnvil()) foundStations.add("anvil");
                if (heart.hasEnchantingTable()) foundStations.add("enchanting_table");
            }
        }
    }

    @Nullable
    public TileStorageHeart findHeart() {
        if (heartPos != null && world != null) {
            world.getChunk(heartPos);
            TileEntity te = world.getTileEntity(heartPos);
            if (te instanceof TileStorageHeart) return (TileStorageHeart) te;
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
}
