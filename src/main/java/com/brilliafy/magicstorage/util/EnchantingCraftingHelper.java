package com.brilliafy.magicstorage.util;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

/**
 * Enchanting table simulation using ONLY public static methods.
 * No field reflection needed — EnchantmentHelper.calcItemStackEnchantability
 * and buildEnchantmentList are public static.
 * Bookshelf power is counted from network inventory items using
 * Block.getEnchantPowerBonus (reflected). ForgeEventFactory hook also
 * reflected for mod compatibility.
 */
public class EnchantingCraftingHelper {

    private static Method blockEnchantPowerBonusMethod;
    private static Method forgeLevelSetMethod;

    static {
        try {
            blockEnchantPowerBonusMethod = Block.class.getMethod("getEnchantPowerBonus",
                World.class, BlockPos.class);
        } catch (Exception ignored) {}

        try {
            forgeLevelSetMethod = ForgeEventFactory.class.getMethod("onEnchantmentLevelSet",
                World.class, BlockPos.class, int.class, int.class, ItemStack.class, int.class);
        } catch (Exception ignored) {}
    }

    // ============================================================
    //  Result type
    // ============================================================

    public static class EnchantResult {
        public final int enchantLevel;
        public final int xpCost;
        public final List<EnchantmentData> enchantments;
        public final EnchantmentData clue;
        public final ItemStack displayStack;
        public final int xpSeed;

        public EnchantResult(int enchantLevel, int xpCost, List<EnchantmentData> enchantments,
                             EnchantmentData clue, ItemStack displayStack, int xpSeed) {
            this.enchantLevel = enchantLevel;
            this.xpCost = xpCost;
            this.enchantments = enchantments;
            this.clue = clue;
            this.displayStack = displayStack;
            this.xpSeed = xpSeed;
        }
    }

    // ============================================================
    //  Bookshelf power from network inventory
    // ============================================================

    public static int getPowerFromItems(List<ItemStack> networkItems, World world) {
        float power = 0;
        for (ItemStack stack : networkItems) {
            if (stack.isEmpty()) continue;
            Block block = Block.getBlockFromItem(stack.getItem());
            if (block == null || block == Blocks.AIR) continue;
            try {
                float bonus = 1.0f;
                if (blockEnchantPowerBonusMethod != null) {
                    bonus = (float) blockEnchantPowerBonusMethod.invoke(block, world, BlockPos.ORIGIN);
                } else if (block == Blocks.BOOKSHELF) {
                    bonus = 1.0f;
                } else {
                    continue;
                }
                if (bonus > 0) power += bonus * stack.getCount();
            } catch (Exception ignored) {}
        }
        return (int) power;
    }

    public static int getPowerFromHeart(
            com.brilliafy.magicstorage.tile.TileStorageHeart heart, World world) {
        if (heart == null) return 0;
        return getPowerFromItems(heart.getAllItems(), world);
    }

    // ============================================================
    //  Core simulation
    // ============================================================

    public static EnchantResult simulateEnchant(ItemStack item, EntityPlayer player,
                                                 int power, int slot) {
        if (item.isEmpty() || !item.isItemEnchantable()) return null;

        int xpSeed = player.getXPSeed();
        Random rng = new Random();
        rng.setSeed(xpSeed);

        // --- Levels for all 3 slots (same RNG seed, matching vanilla) ---
        int[] levels = new int[3];
        for (int i = 0; i < 3; i++) {
            levels[i] = EnchantmentHelper.calcItemStackEnchantability(rng, i, power, item);
            if (levels[i] < i + 1) levels[i] = 0;

            if (forgeLevelSetMethod != null) {
                try {
                    levels[i] = (int) forgeLevelSetMethod.invoke(null,
                        player.world, BlockPos.ORIGIN, i, power, item, levels[i]);
                } catch (Exception ignored) {}
            }
        }

        int enchantLevel = levels[slot];
        if (enchantLevel <= 0) return null;

        // --- Enchantment list for this slot ---
        rng.setSeed(xpSeed + slot);
        List<EnchantmentData> list = EnchantmentHelper.buildEnchantmentList(
            rng, item, enchantLevel, false);
        if (list == null || list.isEmpty()) return null;

        if (item.getItem() == Items.BOOK && list.size() > 1) {
            list.remove(rng.nextInt(list.size()));
        }

        EnchantmentData clue = list.get(0);
        int xpCost = slot + 1;  // Vanilla: each slot costs slot+1 levels (1, 2, or 3)
        ItemStack displayStack = buildDisplayStack(item, clue, xpCost, enchantLevel);

        return new EnchantResult(enchantLevel, xpCost, list, clue, displayStack, xpSeed);
    }

    // ============================================================
    //  Apply
    // ============================================================

    public static ItemStack applyEnchantList(ItemStack item, List<EnchantmentData> list) {
        if (item.isEmpty() || list == null || list.isEmpty()) return item.copy();
        boolean isBook = item.getItem() == Items.BOOK;
        ItemStack result = isBook ? new ItemStack(Items.ENCHANTED_BOOK) : item.copy();
        result.setCount(1);

        // Clear any fake display enchants/lore from the preview stack
        if (!isBook) {
            net.minecraft.nbt.NBTTagCompound tag = result.getTagCompound();
            if (tag != null) {
                tag.removeTag("ench");
                tag.removeTag("HideFlags");
                if (tag.hasKey("display")) {
                    net.minecraft.nbt.NBTTagCompound display = tag.getCompoundTag("display");
                    display.removeTag("Lore");
                    if (display.getSize() == 0) {
                        tag.removeTag("display");
                    }
                }
            }
        }

        for (EnchantmentData ed : list) {
            if (isBook) ItemEnchantedBook.addEnchantment(result, ed);
            else result.addEnchantment(ed.enchantment, ed.enchantmentLevel);
        }
        return result;
    }

    /** Reproduce exact enchants from a prior simulation */
    public static ItemStack recreateEnchants(ItemStack originalItem,
                                               int slot, int level, int xpSeed) {
        if (originalItem.isEmpty()) return ItemStack.EMPTY;
        Random rng = new Random();
        rng.setSeed(xpSeed + slot);
        List<EnchantmentData> list = EnchantmentHelper.buildEnchantmentList(
            rng, originalItem, level, false);
        if (list == null || list.isEmpty()) return originalItem.copy();

        if (originalItem.getItem() == Items.BOOK && list.size() > 1) {
            list.remove(rng.nextInt(list.size()));
        }
        return applyEnchantList(originalItem, list);
    }

    // ============================================================
    //  Display
    // ============================================================

    private static ItemStack buildDisplayStack(ItemStack item, EnchantmentData clue, int xpCost, int enchantLevel) {
        ItemStack r = item.copy();
        r.setCount(1);
        net.minecraft.nbt.NBTTagCompound rootTag = r.hasTagCompound()
            ? r.getTagCompound() : new net.minecraft.nbt.NBTTagCompound();

        net.minecraft.nbt.NBTTagList enchList = rootTag.getTagList("ench", 10);
        if (enchList == null || enchList.tagCount() == 0) {
            enchList = new net.minecraft.nbt.NBTTagList();
            net.minecraft.nbt.NBTTagCompound fakeEnch = new net.minecraft.nbt.NBTTagCompound();
            fakeEnch.setShort("id", (short) 0);
            fakeEnch.setShort("lvl", (short) 1);
            enchList.appendTag(fakeEnch);
            rootTag.setTag("ench", enchList);
        }

        net.minecraft.nbt.NBTTagList lore = new net.minecraft.nbt.NBTTagList();
        if (clue != null) {
            lore.appendTag(new net.minecraft.nbt.NBTTagString(
                "§7" + clue.enchantment.getTranslatedName(clue.enchantmentLevel) + " ?"));
        }
        lore.appendTag(new net.minecraft.nbt.NBTTagString("§e§l§nEnchanting Level: " + enchantLevel));
        lore.appendTag(new net.minecraft.nbt.NBTTagString(""));
        lore.appendTag(new net.minecraft.nbt.NBTTagString("§aCost: " + xpCost + " Levels"));

        net.minecraft.nbt.NBTTagCompound display = rootTag.getCompoundTag("display");
        if (display == null) display = new net.minecraft.nbt.NBTTagCompound();
        display.setTag("Lore", lore);
        rootTag.setTag("display", display);
        rootTag.setInteger("HideFlags", 1);
        r.setTagCompound(rootTag);
        return r;
    }

    // ============================================================
    //  Utilities
    // ============================================================

    public static boolean isEnchantTable(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.ENCHANTING_TABLE);
    }

    public static boolean isLapis(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.DYE && stack.getMetadata() == 4;
    }

    public static boolean canCraft(ItemStack itemSlot, ItemStack slot3, ItemStack slot4, ItemStack slot5) {
        if (itemSlot.isEmpty() || !itemSlot.isItemEnchantable()) return false;
        return isLapis(slot3) || isLapis(slot4) || isLapis(slot5);
    }

    public static int getEnchantTier(ItemStack slot3, ItemStack slot4, ItemStack slot5) {
        int count = 0;
        if (isLapis(slot3)) count++;
        if (isLapis(slot4)) count++;
        if (isLapis(slot5)) count++;
        return count; // 0, 1, 2, or 3
    }

    public static void consumeIngredients(ItemStack[] matrix) {
        if (!matrix[0].isEmpty()) matrix[0].shrink(1);
        if (!matrix[3].isEmpty()) matrix[3].shrink(1);
        if (!matrix[4].isEmpty()) matrix[4].shrink(1);
        if (!matrix[5].isEmpty()) matrix[5].shrink(1);
    }

    public static boolean hasEnoughXp(EntityPlayer player, int cost) {
        return player.isCreative() || player.isSpectator() || player.experienceLevel >= cost;
    }

    public static void consumeXp(EntityPlayer player, int cost) {
        if (!player.isCreative() && !player.isSpectator()) {
            player.addExperienceLevel(-cost);
            if (player.experienceLevel < 0) {
                player.experienceLevel = 0;
                player.experience = 0.0F;
                player.experienceTotal = 0;
            }
            if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                ((net.minecraft.entity.player.EntityPlayerMP) player).connection.sendPacket(
                    new net.minecraft.network.play.server.SPacketSetExperience(
                        player.experience, player.experienceTotal, player.experienceLevel));
            }
        }
    }
}
