package com.brilliafy.magicstorage.container;

import com.brilliafy.magicstorage.gui.InventoryCraftingNetwork;
import com.brilliafy.magicstorage.network.NetworkHandler;
import com.brilliafy.magicstorage.tile.TileCraftingAccess;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ContainerCraftingAccess extends ContainerMagicStorageBase {

    private TileCraftingAccess tile;

    public ContainerCraftingAccess(final TileCraftingAccess tile, final InventoryPlayer playerInv) {
        this.tile = tile;
        this.playerInv = playerInv;
        this.result = new InventoryCraftResult();
        this.matrix = new InventoryCraftingNetwork(this, 3, 3);
        // Log network status on open
        TileStorageHeart heart = tile.findHeart();
        if (heart != null && !playerInv.player.world.isRemote) {
            heart.refreshNetwork();
            // Sync items to client
            // Sync the heart's tile entity (inventory with stations) to the client
            if (!playerInv.player.world.isRemote) {
                ((net.minecraft.world.WorldServer) playerInv.player.world).getPlayerChunkMap().markBlockForUpdate(heart.getPos());
            }
            List<ItemStack> allItems = heart.getAllItems();
            NetworkHandler.INSTANCE.sendTo(
                new NetworkHandler.StackRefreshClientMessage(allItems, new ArrayList<>()),
                (EntityPlayerMP) playerInv.player);
        }
        bindGrid();       // slot 0=result, 1-9=matrix
        bindPlayerInvo(playerInv); // slots 10-45
        onCraftMatrixChanged(matrix);
        if (playerInv.player.world.isRemote) return;
        com.brilliafy.magicstorage.MagicStorage.LOGGER.info("[MagicStorage] CA open: heartFound=" + (heart != null) + " heartPos=" + (tile != null ? tile.getHeartPos() : "null") + " hasEnchantTable=" + (heart != null ? heart.hasEnchantingTable() : "N/A"));
    }

    @Override
    public TileStorageHeart getTileMaster() { return tile != null ? tile.findHeart() : null; }

    @Override
    public void slotChanged() {
        onCraftMatrixChanged(matrix);
        if (!playerInv.player.world.isRemote && playerInv.player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            net.minecraft.entity.player.EntityPlayerMP mp = (net.minecraft.entity.player.EntityPlayerMP) playerInv.player;
            net.minecraft.item.ItemStack resultStack = result != null ? result.getStackInSlot(0) : net.minecraft.item.ItemStack.EMPTY;
            // Send result directly via connection — same as vanilla slotChangedCraftingGrid
            mp.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(this.windowId, 0, resultStack));
            TileStorageHeart master = getTileMaster();
            if (master != null) {
                List<ItemStack> allItems = master.getAllItems();
                NetworkHandler.INSTANCE.sendTo(
                    new NetworkHandler.StackRefreshClientMessage(allItems, new ArrayList<>()),
                    mp
                );
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile != null && tile.getWorld().getTileEntity(tile.getPos()) == tile
            && playerIn.getDistanceSq(tile.getPos().add(0.5, 0.5, 0.5)) <= 64.0;
    }
    public TileCraftingAccess getTileCraftingAccess() { return tile; }
    @Override public boolean isRequest() { return true; }
    @Override public net.minecraftforge.items.IItemHandler getItemHandler() { return null; }
}
