package com.brilliafy.magicstorage.reference;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ModBlocksRef {
    public static Block STORAGE_HEART;
    public static Block STORAGE_ACCESS;
    public static Block CRAFTING_ACCESS;
    public static Block STORAGE_UNIT;
    public static Block REMOTE_ACCESS;
    public static Block HELL_BRICK;

    public static ItemStack storageHeart() {
        return new ItemStack(STORAGE_HEART);
    }

    public static ItemStack storageAccess() {
        return new ItemStack(STORAGE_ACCESS);
    }

    public static ItemStack craftingAccess() {
        return new ItemStack(CRAFTING_ACCESS);
    }

    public static ItemStack storageUnit() {
        return new ItemStack(STORAGE_UNIT);
    }

    public static ItemStack remoteAccess() {
        return new ItemStack(REMOTE_ACCESS);
    }

    public static ItemStack hellBrick() {
        return new ItemStack(HELL_BRICK);
    }
}
