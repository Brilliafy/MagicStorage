package com.brilliafy.magicstorage.util;

import codersafterdark.reskillable.base.LevelLockHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fml.common.Loader;

public class ReskillableCraftingHelper {

    public static boolean hasSkillForBlock(EntityPlayer player, Block block) {
        if (!Loader.isModLoaded("reskillable")) return true;
        if (player == null || block == null) return true;
        if (player.isCreative() || player.isSpectator()) return true;

        try {
            return ReskillableBridge.canUse(player, new ItemStack(block));
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean hasSkillForStack(EntityPlayer player, ItemStack stack) {
        if (!Loader.isModLoaded("reskillable")) return true;
        if (player == null || stack.isEmpty()) return true;
        if (player.isCreative() || player.isSpectator()) return true;

        try {
            return ReskillableBridge.canUse(player, stack);
        } catch (Throwable t) {
            return true;
        }
    }

    public static ItemStack applySkillLockTooltip(ItemStack original) {
        if (original.isEmpty()) return original;
        ItemStack display = original.copy();
        NBTTagCompound rootTag = display.hasTagCompound() ? display.getTagCompound().copy() : new NBTTagCompound();
        NBTTagCompound displayTag = rootTag.getCompoundTag("display");
        if (displayTag == null) displayTag = new NBTTagCompound();

        NBTTagList lore = displayTag.hasKey("Lore", 9) ? displayTag.getTagList("Lore", 8).copy() : new NBTTagList();
        lore.appendTag(new NBTTagString(""));
        lore.appendTag(new NBTTagString("\u00A7c\u00A7l\u2716    Insufficient Skill Level"));

        displayTag.setTag("Lore", lore);
        rootTag.setTag("display", displayTag);
        display.setTagCompound(rootTag);
        return display;
    }

    private static class ReskillableBridge {
        private static boolean canUse(EntityPlayer player, ItemStack stack) {
            return LevelLockHandler.canPlayerUseItem(player, stack);
        }
    }
}
