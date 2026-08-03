package com.brilliafy.magicstorage.util;

import de.impelon.disenchanter.DisenchantingProperties;
import de.impelon.disenchanter.DisenchantingUtils;
import de.impelon.disenchanter.block.TableVariant;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DisenchanterCraftingHelper {

    private static final Random rand = new Random();

    public static boolean hasEnchantments(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() == Items.ENCHANTED_BOOK) {
            return !EnchantmentHelper.getEnchantments(stack).isEmpty();
        }
        return stack.isItemEnchanted();
    }

    public static boolean canCraft(ItemStack target, ItemStack book) {
        if (!Loader.isModLoaded("disenchanter")) return false;
        if (target.isEmpty() || book.isEmpty()) return false;
        if (book.getItem() != Items.BOOK) return false;
        return hasEnchantments(target);
    }

    public static ItemStack computeResult(ItemStack target, ItemStack book, boolean isVoiding, boolean isBulk) {
        if (!canCraft(target, book)) return ItemStack.EMPTY;

        ItemStack sourceCopy = target.copy();
        sourceCopy.setCount(1);
        ItemStack targetBook = new ItemStack(Items.ENCHANTED_BOOK);

        List<TableVariant> variants = new ArrayList<>();
        if (isVoiding) variants.add(TableVariant.VOIDING);
        if (isBulk) variants.add(TableVariant.BULKDISENCHANTING);
        DisenchantingProperties props = new DisenchantingProperties(variants);

        boolean success = DisenchantingUtils.disenchant(sourceCopy, targetBook, false, 0, props, 1.0f, rand);
        if (success && !targetBook.isEmpty()) {
            return targetBook;
        }
        return ItemStack.EMPTY;
    }

    public static void consumeIngredients(ItemStack[] matrix, boolean isVoiding, boolean isBulk) {
        ItemStack source = matrix[4]; // Slot 5
        ItemStack book = matrix[2];   // Slot 3

        if (source.isEmpty() || book.isEmpty()) return;

        book.shrink(1); // consume 1 book

        if (isVoiding) {
            matrix[4] = ItemStack.EMPTY; // void target item completely
            return;
        }

        ItemStack targetBook = new ItemStack(Items.ENCHANTED_BOOK);
        List<TableVariant> variants = new ArrayList<>();
        if (isBulk) variants.add(TableVariant.BULKDISENCHANTING);
        DisenchantingProperties props = new DisenchantingProperties(variants);

        DisenchantingUtils.disenchant(source, targetBook, false, 0, props, 1.0f, rand);

        if (source.isItemStackDamageable() && source.getItemDamage() >= source.getMaxDamage()) {
            matrix[4] = ItemStack.EMPTY;
        } else if (source.getItem() == Items.ENCHANTED_BOOK && !hasEnchantments(source)) {
            matrix[4] = new ItemStack(Items.BOOK);
        }
    }
}
