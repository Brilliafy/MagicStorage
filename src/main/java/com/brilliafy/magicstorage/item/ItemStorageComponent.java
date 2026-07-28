package com.brilliafy.magicstorage.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import com.brilliafy.magicstorage.reference.ModInfo;
import java.util.List;
public class ItemStorageComponent extends Item {
    public ItemStorageComponent() {
        setRegistryName(ModInfo.MOD_ID, "storage_component");
        setTranslationKey(ModInfo.MOD_ID + ".storage_component");
        setCreativeTab(com.brilliafy.magicstorage.init.MagicStorageTab.TAB);
    }

    @Override
    public void addInformation(ItemStack stack, net.minecraft.world.World playerIn, java.util.List<String> tooltip, net.minecraft.client.util.ITooltipFlag advanced) {
        tooltip.add(net.minecraft.util.text.TextFormatting.WHITE + "Crafting component for Magic Storage");
        tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Used to craft Storage Units, Hearts, and Access points");
    }
}
