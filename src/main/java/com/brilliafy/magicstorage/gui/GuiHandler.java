/*
 * Portions of this file contain code adapted from Storage Network
 * by Lothrazar (https://github.com/Lothrazar/Storage-Network).
 *
 * Copyright (c) Lothrazar
 * Licensed under the MIT License.
 */
package com.brilliafy.magicstorage.gui;

import com.brilliafy.magicstorage.container.ContainerCraftingAccess;
import com.brilliafy.magicstorage.container.ContainerPortableAccess;
import com.brilliafy.magicstorage.container.ContainerStorageAccess;
import com.brilliafy.magicstorage.container.ContainerStorageHeart;
import com.brilliafy.magicstorage.container.ContainerStorageUnit;
import com.brilliafy.magicstorage.item.ItemPortableAccess;
import com.brilliafy.magicstorage.tile.TileCraftingAccess;
import com.brilliafy.magicstorage.tile.TileStorageAccess;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import com.brilliafy.magicstorage.tile.TileStorageUnit;
import com.brilliafy.magicstorage.tile.TileRemoteAccess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class GuiHandler implements IGuiHandler {

    // GUI IDs
    public static final int STORAGE_ACCESS = 0;      // Block -> Full request GUI
    public static final int CRAFTING_ACCESS = 1;      // Block -> Request table with crafting
    public static final int STORAGE_UNIT = 2;          // Block -> Scrollable chest
    public static final int REMOTE_ACCESS = 3;         // Block -> Remote GUI
    public static final int STORAGE_HEART = 4;         // Block -> 20-slot container
    public static final int CRAFTING_REMOTE = 5;       // Item (portable) -> Crafting request
    public static final int STORAGE_REMOTE = 6;        // Item (portable) -> Storage request

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        switch (id) {
            case STORAGE_ACCESS:
                if (te instanceof TileStorageAccess)
                    return new ContainerStorageAccess(player.inventory, (TileStorageAccess) te);
                break;
            case CRAFTING_ACCESS:
                if (te instanceof TileCraftingAccess)
                    return new ContainerCraftingAccess((TileCraftingAccess) te, player.inventory);
                break;
            case STORAGE_UNIT:
                if (te instanceof TileStorageUnit)
                    return new ContainerStorageUnit(player.inventory, (TileStorageUnit) te);
                break;
            case REMOTE_ACCESS:
                if (te instanceof TileRemoteAccess) {
                    TileRemoteAccess remoteTile = (TileRemoteAccess) te;
                    TileStorageHeart heart = null;
                    if (remoteTile.getLinkedHeartPos() != null && world.getTileEntity(remoteTile.getLinkedHeartPos()) instanceof TileStorageHeart) {
                        heart = (TileStorageHeart) world.getTileEntity(remoteTile.getLinkedHeartPos());
                    }
                    // Always return container — even with null heart, to match client slot layout
                    return new ContainerStorageAccess(player.inventory, heart);
                }
                break;
            case STORAGE_HEART:
                if (te instanceof TileStorageHeart)
                    return new ContainerStorageHeart(player.inventory, (TileStorageHeart) te);
                break;
            case STORAGE_REMOTE:
            case CRAFTING_REMOTE:
                // x parameter is the inventory slot of the remote item (passed from tryOpenGui)
                if (x >= 0 && x < player.inventory.getSizeInventory()) {
                    ItemStack stack = player.inventory.getStackInSlot(x);
                    if (stack.getItem() instanceof ItemPortableAccess) {
                        return new ContainerPortableAccess(player.inventory, stack, x, world);
                    }
                }
                // Fallback: scan for the item if slot is invalid
                for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                    ItemStack stack = player.inventory.getStackInSlot(i);
                    if (stack.getItem() instanceof ItemPortableAccess) {
                        return new ContainerPortableAccess(player.inventory, stack, i, world);
                    }
                }
                break;
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);

        switch (id) {
            case STORAGE_ACCESS:
                if (te instanceof TileStorageAccess)
                    return new GuiStorageAccess(player.inventory,
                        new ContainerStorageAccess(player.inventory, (TileStorageAccess) te));
                break;
            case CRAFTING_ACCESS:
                if (te instanceof TileCraftingAccess)
                    return new GuiCraftingAccessRequest(
                        new ContainerCraftingAccess((TileCraftingAccess) te, player.inventory),
                        (TileCraftingAccess) te);
                break;
            case STORAGE_UNIT:
                if (te instanceof TileStorageUnit)
                    return new GuiStorageAccess(player.inventory,
                        new ContainerStorageUnit(player.inventory, (TileStorageUnit) te));
                break;
            case REMOTE_ACCESS:
                if (te instanceof TileRemoteAccess) {
                    TileRemoteAccess remoteTile = (TileRemoteAccess) te;
                    TileStorageHeart heart = null;
                    if (remoteTile.getLinkedHeartPos() != null && world.getTileEntity(remoteTile.getLinkedHeartPos()) instanceof TileStorageHeart) {
                        heart = (TileStorageHeart) world.getTileEntity(remoteTile.getLinkedHeartPos());
                    }
                    // Always return a GUI — even with null heart, to match server's slot layout
                    return new GuiStorageAccess(player.inventory,
                        new ContainerStorageAccess(player.inventory, heart));
                }
                break;
            case STORAGE_HEART:
                if (te instanceof TileStorageHeart)
                    return new GuiStorageHeart(player.inventory, (TileStorageHeart) te);
                break;
            case STORAGE_REMOTE:
            case CRAFTING_REMOTE:
                // x parameter is the inventory slot of the remote item
                if (x >= 0 && x < player.inventory.getSizeInventory()) {
                    ItemStack stack = player.inventory.getStackInSlot(x);
                    if (stack.getItem() instanceof ItemPortableAccess) {
                        ContainerPortableAccess container = new ContainerPortableAccess(player.inventory, stack, x, world);
                        return new GuiPortableAccess(container);
                    }
                }
                // Fallback: scan for the item
                for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                    ItemStack stack = player.inventory.getStackInSlot(i);
                    if (stack.getItem() instanceof ItemPortableAccess) {
                        ContainerPortableAccess container = new ContainerPortableAccess(player.inventory, stack, i, world);
                        return new GuiPortableAccess(container);
                    }
                }
                break;
        }
        return null;
    }
}
