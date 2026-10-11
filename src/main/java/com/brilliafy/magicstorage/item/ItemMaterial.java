package com.brilliafy.magicstorage.item;

import com.brilliafy.magicstorage.init.MagicStorageTab;
import com.brilliafy.magicstorage.reference.ModInfo;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class ItemMaterial extends Item {

    private final TextFormatting nameColor;
    private final String[] tooltips;

    public ItemMaterial(String name, TextFormatting nameColor, String... tooltips) {
        setRegistryName(ModInfo.MOD_ID, name);
        setTranslationKey(ModInfo.MOD_ID + "." + name);
        setCreativeTab(MagicStorageTab.TAB);
        this.nameColor = nameColor;
        this.tooltips = tooltips;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (nameColor != null) {
            return nameColor + super.getItemStackDisplayName(stack);
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (tooltips != null) {
            for (String t : tooltips) {
                tooltip.add(t);
            }
        }
    }
}
