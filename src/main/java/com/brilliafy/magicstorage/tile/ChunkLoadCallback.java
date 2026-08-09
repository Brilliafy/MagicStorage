package com.brilliafy.magicstorage.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

import java.util.List;

/**
 * Handles ForgeChunkManager callbacks for chunk loading.
 * When a world loads saved tickets, this re-registers forced chunks.
 */
public class ChunkLoadCallback implements ForgeChunkManager.LoadingCallback {

    /**
     * Called when the world loads and tickets are restored from save data.
     * We stored the heart's position in ticket.modData, so we look up the heart
     * and re-force its network chunks.
     */
    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        for (ForgeChunkManager.Ticket ticket : tickets) {
            try {
                NBTTagCompound data = ticket.getModData();
                if (data.hasKey("heartX")) {
                    BlockPos heartPos = new BlockPos(
                        data.getInteger("heartX"),
                        data.getInteger("heartY"),
                        data.getInteger("heartZ")
                    );
                    if (world.isBlockLoaded(heartPos)) {
                        net.minecraft.tileentity.TileEntity te = world.getTileEntity(heartPos);
                        if (te instanceof TileStorageHeart) {
                            ((TileStorageHeart) te).setTicket(ticket);
                            ((TileStorageHeart) te).updateForcedChunks();
                        }
                    }
                }
            } catch (Throwable t) {
                com.brilliafy.magicstorage.MagicStorage.LOGGER.warn("Failed to load chunk ticket: " + t.getMessage());
            }
        }
    }
}
