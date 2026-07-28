package com.brilliafy.magicstorage.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Anvil crafting using reflection on vanilla ContainerRepair.
 * Grid: [item] at slot 0, [secondary item / name tag] at slot 4.
 * Uses vanilla anvil logic for cost, repairs, renaming, and enchant combining.
 */
public class AnvilCraftingHelper {

    private static Method repairOutputMethod;
    private static Field inputSlotsField;
    private static Field stackResultField;
    private static Field maximumCostField;

    static {
        try {
            repairOutputMethod = findMethod(ContainerRepair.class,
                new String[]{"updateRepairOutput", "func_82848_d"});
            inputSlotsField = findField(ContainerRepair.class,
                new String[]{"inputSlots", "field_82854_h", "field_82853_g"});
            stackResultField = findField(ContainerRepair.class,
                new String[]{"outputSlot", "stackResult", "field_82852_f"});
            maximumCostField = findField(ContainerRepair.class,
                new String[]{"maximumCost", "field_82854_e"});
            // Fallback: discover inputSlots by type (IInventory) if name lookup failed
            if (inputSlotsField == null) {
                inputSlotsField = findFieldByType(ContainerRepair.class, IInventory.class, InventoryCraftResult.class);
            }
            com.brilliafy.magicstorage.MagicStorage.LOGGER.info("[MagicStorage] Anvil reflection init: method=" + (repairOutputMethod != null) + " inputSlots=" + (inputSlotsField != null) + " stackResult=" + (stackResultField != null) + " maxCost=" + (maximumCostField != null));
            // Diagnostic: dump all ContainerRepair fields
            if (inputSlotsField == null) {
                com.brilliafy.magicstorage.MagicStorage.LOGGER.warn("[MagicStorage] inputSlots still null — dumping all ContainerRepair fields:");
                for (java.lang.reflect.Field f : ContainerRepair.class.getDeclaredFields()) {
                    com.brilliafy.magicstorage.MagicStorage.LOGGER.warn("  field: " + f.getName() + " type=" + f.getType().getName());
                }
            }
        } catch (Exception e) {
            com.brilliafy.magicstorage.MagicStorage.LOGGER.warn("[MagicStorage] Anvil reflection init failed", e);
        }
    }

    private static Method findMethod(Class<?> clazz, String[] names, Class<?>... params) {
        for (String name : names) {
            try { Method m = clazz.getDeclaredMethod(name, params); m.setAccessible(true); return m; }
            catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    private static Field findField(Class<?> clazz, String[] names) {
        for (String name : names) {
            try { Field f = clazz.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    /** Find first declared field of the given type, excluding excludedTypes — resilience against coremod renames */
    private static Field findFieldByType(Class<?> clazz, Class<?> type, Class<?>... excludedTypes) {
        for (Field f : clazz.getDeclaredFields()) {
            if (type.isAssignableFrom(f.getType())) {
                boolean excluded = false;
                for (Class<?> ex : excludedTypes) {
                    if (ex.isAssignableFrom(f.getType())) { excluded = true; break; }
                }
                if (!excluded) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        return null;
    }

    public static boolean isAnvil(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.ANVIL);
    }

    public static boolean canCraft(ItemStack slot0, ItemStack slot4) {
        if (slot0.isEmpty() || slot4.isEmpty()) return false;
        ItemStack anvilStack = new ItemStack(net.minecraft.init.Blocks.ANVIL);
        if (slot0.getItem() == anvilStack.getItem() || slot4.getItem() == anvilStack.getItem()) return false;
        // Name tag renaming
        if (slot4.getItem() == Items.NAME_TAG && slot4.hasDisplayName()) return true;
        // Same-item repair
        if (slot0.getItem() == slot4.getItem() && slot0.getItem().isRepairable()) return true;
        // Enchant combining: right item must have enchantments applicable to left
        Map<Enchantment, Integer> rightEnchants = EnchantmentHelper.getEnchantments(slot4);
        if (!rightEnchants.isEmpty()) {
            for (Enchantment ench : rightEnchants.keySet()) {
                if (ench != null && canApply(ench, slot0)) return true;
            }
        }
        return false;
    }

    /** Compute result and cost using vanilla ContainerRepair via reflection */
    public static AnvilResult computeResult(ItemStack left, ItemStack right, net.minecraft.entity.player.EntityPlayer player) {
        if (left.isEmpty() || right.isEmpty()) return null;
        if (repairOutputMethod == null || inputSlotsField == null || player == null) {
            com.brilliafy.magicstorage.MagicStorage.LOGGER.warn("[MagicStorage] Anvil: cannot compute — method=" + (repairOutputMethod != null) + " inputSlots=" + (inputSlotsField != null) + " player=" + (player != null));
            return null;
        }
        try {
            ContainerRepair repair = new ContainerRepair(player.inventory, player.world, player.getPosition(), player);
            // Set input slots via reflection
            IInventory inputSlots = (IInventory) inputSlotsField.get(repair);
            if (inputSlots == null) {
                com.brilliafy.magicstorage.MagicStorage.LOGGER.warn("[MagicStorage] Anvil: inputSlots instance is null");
                return null;
            }
            inputSlots.setInventorySlotContents(0, left.copy());
            inputSlots.setInventorySlotContents(1, right.copy());

            // Do NOT call onContainerClosed — it calls clearContainer() which empties inputSlots!
            // Instead, just clear the listener list so updateRepairOutput's detectAndSendChanges
            // doesn't send packets to the player's real container.
            try {
                Field listenersField = Container.class.getDeclaredField("listeners");
                listenersField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<?> listeners = (java.util.List<?>) listenersField.get(repair);
                if (listeners != null) listeners.clear();
            } catch (Exception ignored) {}

            // Call updateRepairOutput — inputSlots still populated, no listener to receive packets
            repairOutputMethod.invoke(repair);

            // Read cost
            int cost = maximumCostField.getInt(repair);

            // Read result
            ItemStack resultStack = ItemStack.EMPTY;
            InventoryCraftResult stackResult = (InventoryCraftResult) stackResultField.get(repair);
            if (stackResult != null) {
                resultStack = stackResult.getStackInSlot(0).copy();
            }

            if (!resultStack.isEmpty() && cost > 0) {
                return new AnvilResult(resultStack, cost);
            }
            // updateRepairOutput returned nothing — this should not happen with valid inputs
            com.brilliafy.magicstorage.MagicStorage.LOGGER.warn("[MagicStorage] Anvil: updateRepairOutput produced empty result (cost=" + cost + ")");
        } catch (Exception e) {
            com.brilliafy.magicstorage.MagicStorage.LOGGER.warn("[MagicStorage] Anvil reflection failed", e);
        }
        return null;
    }
    


    public static void consumeIngredients(ItemStack[] matrix) {
        if (!matrix[0].isEmpty()) matrix[0].shrink(1);
        if (!matrix[4].isEmpty()) matrix[4].shrink(1);
    }

    public static int getXpCost(AnvilResult result) { return result != null ? result.cost : 0; }

    public static boolean hasEnoughXp(net.minecraft.entity.player.EntityPlayer player, int cost) {
        return player.isCreative() || player.isSpectator() || player.experienceLevel >= cost;
    }

    public static void consumeXp(net.minecraft.entity.player.EntityPlayer player, int cost) {
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

    private static boolean canApply(Enchantment ench, ItemStack stack) {
        try {
            Method m = findMethod(Enchantment.class, new String[]{"canApply", "func_92089_a"}, ItemStack.class);
            if (m != null) return (boolean) m.invoke(ench, stack);
        } catch (Exception e) {}
        return ench.type != null && ench.type.canEnchantItem(stack.getItem());
    }

    private static boolean canApplyTogether(Enchantment a, Enchantment b) {
        try {
            Method m = findMethod(Enchantment.class, new String[]{"canApplyTogether", "func_77326_a"}, Enchantment.class);
            if (m != null) return (boolean) m.invoke(a, b);
        } catch (Exception e) {}
        return true;
    }

    /** Generate JEI display recipes */
    public static List<AnvilRecipeDisplay> generateAllRecipes() {
        List<AnvilRecipeDisplay> recipes = new ArrayList<>();
        try {
            recipes.add(new AnvilRecipeDisplay(
                new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD),
                new ItemStack(net.minecraft.init.Items.ENCHANTED_BOOK),
                new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD)));
            recipes.add(new AnvilRecipeDisplay(
                new ItemStack(net.minecraft.init.Items.DIAMOND_PICKAXE),
                new ItemStack(net.minecraft.init.Items.ENCHANTED_BOOK),
                new ItemStack(net.minecraft.init.Items.DIAMOND_PICKAXE)));
            ItemStack nameTag = new ItemStack(Items.NAME_TAG);
            nameTag.setStackDisplayName("§fAny Name");
            recipes.add(new AnvilRecipeDisplay(
                new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD),
                nameTag,
                new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD)));
        } catch (Exception e) {}
        return recipes;
    }

    public static class AnvilResult {
        public final ItemStack stack;
        public final int cost;
        public AnvilResult(ItemStack stack, int cost) { this.stack = stack; this.cost = cost; }
    }

    public static class AnvilRecipeDisplay {
        public final ItemStack input;
        public final ItemStack secondary;
        public final ItemStack output;
        public AnvilRecipeDisplay(ItemStack i, ItemStack s, ItemStack o) { input = i; secondary = s; output = o; }
    }
}
