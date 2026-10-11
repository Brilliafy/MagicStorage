package com.brilliafy.magicstorage.reference;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ModItems {
    public static Item STORAGE_COMPONENT;
    public static Item UPGRADE_IRON;
    public static Item UPGRADE_GOLD;
    public static Item UPGRADE_DIAMOND;
    public static Item UPGRADE_EMERALD;
    public static Item UPGRADE_OBSIDIAN;
    public static Item UPGRADE_NETHER_STAR;
    public static Item UPGRADE_DEMONITE;

    public static Item UPGRADE_CRIMTANE;
    public static Item UPGRADE_HELLSTONE;
    public static Item UPGRADE_HALLOWED;
    public static Item UPGRADE_BLUE_CHLOROPHYTE;
    public static Item UPGRADE_LUMINITE;
    public static Item UPGRADE_TERRA;

    public static Item INGOT_DEMONITE;
    public static Item INGOT_CRIMTANE;
    public static Item BAR_HELLSTONE;
    public static Item INGOT_HALLOWED;
    public static Item BAR_CHLOROPHYTE;
    public static Item BAR_LUMINITE;
    public static Item TERRA_SINGULARITY;

    public static ItemStack storageComponent(int count) {
        return new ItemStack(STORAGE_COMPONENT, count);
    }

    public static ItemStack upgradeIron(int count) {
        return new ItemStack(UPGRADE_IRON, count);
    }

    public static ItemStack upgradeGold(int count) {
        return new ItemStack(UPGRADE_GOLD, count);
    }

    public static ItemStack upgradeDiamond(int count) {
        return new ItemStack(UPGRADE_DIAMOND, count);
    }

    public static ItemStack upgradeEmerald(int count) {
        return new ItemStack(UPGRADE_EMERALD, count);
    }

    public static ItemStack upgradeObsidian(int count) {
        return new ItemStack(UPGRADE_OBSIDIAN, count);
    }

    public static ItemStack upgradeNetherStar(int count) {
        return new ItemStack(UPGRADE_NETHER_STAR, count);
    }
}
