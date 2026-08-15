package com.brilliafy.magicstorage.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemMatchHelper {

    public static NBTTagCompound getComparableNBT(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound().copy();

        // Strip client-side tooltip mutations (e.g. SpartanWeaponry tooltip enchant check)
        tag.removeTag("enchChecked");
        tag.removeTag("enchantmentsInvalid");

        // Strip random instance UUIDs (e.g. SpartanWeaponry throwing weapons assigning random UUIDs)
        tag.removeTag("UUID");
        tag.removeTag("UUIDMost");
        tag.removeTag("UUIDLeast");

        if (tag.getKeySet().isEmpty()) return null;
        return tag;
    }

    public static boolean matchesStorageItem(ItemStack stored, ItemStack requested) {
        if (stored == null || requested == null || stored.isEmpty() || requested.isEmpty()) return false;
        if (stored.getItem() != requested.getItem()) return false;
        if (stored.getMetadata() != requested.getMetadata()) return false;
        if (stored.isItemStackDamageable() && stored.getItemDamage() != requested.getItemDamage()) return false;

        NBTTagCompound tagA = getComparableNBT(stored);
        NBTTagCompound tagB = getComparableNBT(requested);

        if (tagA == null && tagB == null) return true;
        if (tagA == null || tagB == null) return false;
        return tagA.equals(tagB);
    }
}
