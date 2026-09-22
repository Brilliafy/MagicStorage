package com.brilliafy.magicstorage.util;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemMatchHelper {

    public static boolean isPotionItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() == Items.POTIONITEM
            || stack.getItem() == Items.SPLASH_POTION
            || stack.getItem() == Items.LINGERING_POTION
            || stack.getItem() == Items.TIPPED_ARROW;
    }

    public static boolean matchesPotion(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return false;
        if (a.getItem() != b.getItem()) return false;

        String potA = a.hasTagCompound() ? a.getTagCompound().getString("Potion") : "";
        String potB = b.hasTagCompound() ? b.getTagCompound().getString("Potion") : "";

        boolean isWaterA = potA.isEmpty() || potA.equals("minecraft:empty") || potA.equals("minecraft:water");
        boolean isWaterB = potB.isEmpty() || potB.equals("minecraft:empty") || potB.equals("minecraft:water");

        if (isWaterA || isWaterB) {
            if (isWaterA != isWaterB) return false;
        } else if (!potA.equals(potB)) {
            return false;
        }

        // Custom potion effects check
        boolean hasCustomA = a.hasTagCompound() && a.getTagCompound().hasKey("CustomPotionEffects");
        boolean hasCustomB = b.hasTagCompound() && b.getTagCompound().hasKey("CustomPotionEffects");
        if (hasCustomA != hasCustomB) return false;
        if (hasCustomA) {
            if (!a.getTagCompound().getTagList("CustomPotionEffects", 10).equals(b.getTagCompound().getTagList("CustomPotionEffects", 10))) {
                return false;
            }
        }

        return true;
    }

    public static NBTTagCompound getComparableNBT(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound().copy();

        // Strip client-side tooltip mutations (e.g. SpartanWeaponry tooltip enchant check, Bountiful Baubles shields)
        tag.removeTag("enchChecked");
        tag.removeTag("enchantmentsInvalid");
        tag.removeTag("HideFlags");

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

        if (isPotionItem(stored) || isPotionItem(requested)) {
            return matchesPotion(stored, requested);
        }

        NBTTagCompound tagA = getComparableNBT(stored);
        NBTTagCompound tagB = getComparableNBT(requested);

        if (tagA == null && tagB == null) return true;
        if (tagA == null || tagB == null) return false;
        return tagA.equals(tagB);
    }
}
