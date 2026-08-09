package com.brilliafy.magicstorage.util;

import baubles.api.cap.BaublesCapabilities;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import cursedflames.bountifulbaubles.baubleeffect.EnumBaubleModifier;
import cursedflames.bountifulbaubles.util.XpUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fml.common.Loader;

import java.util.Random;

public class BountifulBaublesCraftingHelper {

    public static boolean isBauble(ItemStack stack) {
        if (stack.isEmpty() || stack.getMaxStackSize() > 1) return false;
        return stack.hasCapability(BaublesCapabilities.CAPABILITY_ITEM_BAUBLE, null);
    }

    public static boolean isBaubleReforgeGrid(ItemStack[] m) {
        if (m == null || m.length < 9) return false;
        if (m[8].isEmpty()) return false;
        for (int i = 0; i < 8; i++) {
            if (!m[i].isEmpty()) return false;
        }
        return true;
    }

    public static boolean canCraft(ItemStack bauble) {
        if (!Loader.isModLoaded("bountifulbaubles")) return false;
        return isBauble(bauble);
    }

    public static class BaubleReforgeResult {
        public final ItemStack displayStack;
        public final ItemStack actualReforgedStack;
        public final int xpCostPoints;
        public final int levelCost;
        public final boolean hasEnoughXp;

        public BaubleReforgeResult(ItemStack displayStack, ItemStack actualReforgedStack, int xpCostPoints, int levelCost, boolean hasEnoughXp) {
            this.displayStack = displayStack;
            this.actualReforgedStack = actualReforgedStack;
            this.xpCostPoints = xpCostPoints;
            this.levelCost = levelCost;
            this.hasEnoughXp = hasEnoughXp;
        }
    }

    private static EnumBaubleModifier getDeterministicModifier(Random rand) {
        EnumBaubleModifier[] values = EnumBaubleModifier.values();
        int totalWeight = 0;
        for (EnumBaubleModifier mod : values) {
            totalWeight += mod.weight;
        }
        int roll = rand.nextInt(totalWeight);
        int currentWeight = 0;
        for (EnumBaubleModifier mod : values) {
            currentWeight += mod.weight;
            if (roll < currentWeight) return mod;
        }
        return EnumBaubleModifier.NONE;
    }

    public static BaubleReforgeResult computeResult(ItemStack bauble, EntityPlayer player, TileStorageHeart heart) {
        if (!canCraft(bauble)) return null;

        ItemStack baubleCopy = bauble.copy();
        if (!baubleCopy.hasTagCompound()) {
            baubleCopy.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound tag = baubleCopy.getTagCompound();

        long posLong = (heart != null && heart.getPos() != null) ? heart.getPos().toLong() : 0L;
        int craftCounter = (heart != null) ? heart.getBrewingCraftCounter() : 0;
        long seed = 31L * posLong + craftCounter;
        Random deterministicRand = new Random(seed);

        int xpCost = tag.hasKey("reforgeCost") ? tag.getInteger("reforgeCost") : (deterministicRand.nextInt(320 - 80 + 1) + 80);

        EnumBaubleModifier mod = getDeterministicModifier(deterministicRand);
        mod.addTo(baubleCopy);
        baubleCopy.getTagCompound().setInteger("reforgeCost", deterministicRand.nextInt(320 - 80 + 1) + 80);

        int levelCost = XpUtil.getLevelForExperience(xpCost);
        if (levelCost <= 0) levelCost = 1;

        int playerXp = (player != null) ? XpUtil.getPlayerXP(player) : 0;
        boolean creative = (player != null) && (player.isCreative() || player.isSpectator());
        boolean hasEnoughXp = creative || (playerXp >= xpCost);

        // Build display stack with level cost lore and optional insufficient XP text
        ItemStack displayStack = baubleCopy.copy();
        displayStack.setCount(1);

        NBTTagCompound displayRoot = displayStack.hasTagCompound() ? displayStack.getTagCompound().copy() : new NBTTagCompound();
        NBTTagList lore = new NBTTagList();

        if (hasEnoughXp) {
            lore.appendTag(new NBTTagString("\u00A7e\u00A7l\u00A7nLevel cost: " + levelCost));
        } else {
            lore.appendTag(new NBTTagString("\u00A7c\u00A7l\u00A7nLevel cost: " + levelCost));
            lore.appendTag(new NBTTagString(""));
            lore.appendTag(new NBTTagString("\u00A7c\u2716 Insufficient XP"));
        }

        NBTTagCompound displayTag = displayRoot.getCompoundTag("display");
        if (displayTag == null) displayTag = new NBTTagCompound();
        displayTag.setTag("Lore", lore);
        displayRoot.setTag("display", displayTag);
        displayRoot.setInteger("HideFlags", 1);
        displayStack.setTagCompound(displayRoot);

        return new BaubleReforgeResult(displayStack, baubleCopy, xpCost, levelCost, hasEnoughXp);
    }

    public static void consumeIngredients(ItemStack[] matrix, BaubleReforgeResult result, EntityPlayer player, TileStorageHeart heart) {
        if (matrix[8].isEmpty() || result == null) return;

        if (player != null && !player.isCreative() && !player.isSpectator()) {
            XpUtil.addPlayerXP(player, -result.xpCostPoints);
        }

        matrix[8] = ItemStack.EMPTY;

        if (heart != null) {
            heart.incrementBrewingCraftCounter();
        }
    }
}
