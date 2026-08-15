package com.brilliafy.magicstorage.init;

import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.reference.ModBlocksRef;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class MagicStorageTab extends CreativeTabs {

    public static final MagicStorageTab TAB = new MagicStorageTab();

    public MagicStorageTab() {
        super(ModInfo.MOD_ID);
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ModBlocksRef.CRAFTING_ACCESS);
    }
}
