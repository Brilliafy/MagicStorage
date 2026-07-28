package com.brilliafy.magicstorage.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Anvil crafting via reflection on vanilla ContainerRepair.
 * Delegates ALL logic to vanilla — handles repairs, renaming, enchant combining,
 * weapon combining, modded items, everything.
 *
 * Grid: slot 0 = left item, slot 4 = right item.
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
            if (inputSlotsField == null) {
                inputSlotsField = findFieldByType(ContainerRepair.class, IInventory.class, InventoryCraftResult.class);
            }
            com.brilliafy.magicstorage.MagicStorage.LOGGER.info("[MagicStorage] Anvil reflection: method=" + (repairOutputMethod != null)
                + " inputSlots=" + (inputSlotsField != null)
                + " stackResult=" + (stackResultField != null)
                + " maxCost=" + (maximumCostField != null));
        } catch (Exception e) {
            com.brilliafy.magicstorage.MagicStorage.LOGGER.warn("[MagicStorage] Anvil reflection init failed", e);
        }
    }

    // ===================== Reflection helpers =====================

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

    // ===================== Public API =====================

    public static boolean isAnvil(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.ANVIL);
    }

    /**
     * Quick check: does the vanilla anvil produce a result for these two items?
     */
    public static boolean canCraft(ItemStack slot0, ItemStack slot4, EntityPlayer player) {
        if (slot0.isEmpty() || slot4.isEmpty()) return false;
        if (isAnvil(slot0) || isAnvil(slot4)) return false;
        AnvilResult ar = computeResult(slot0, slot4, player);
        return ar != null;
    }

    /**
     * Compute result and XP cost via vanilla ContainerRepair reflection.
     */
    public static AnvilResult computeResult(ItemStack left, ItemStack right, EntityPlayer player) {
        if (left.isEmpty() || right.isEmpty()) return null;
        if (repairOutputMethod == null || inputSlotsField == null || player == null) return null;
        try {
            ContainerRepair repair = new ContainerRepair(
                player.inventory, player.world, player.getPosition(), player);

            IInventory inputSlots = (IInventory) inputSlotsField.get(repair);
            if (inputSlots == null) return null;
            inputSlots.setInventorySlotContents(0, left.copy());
            inputSlots.setInventorySlotContents(1, right.copy());

            // Clear listeners so updateRepairOutput doesn't send packets
            try {
                Field listenersField = Container.class.getDeclaredField("listeners");
                listenersField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<?> listeners = (java.util.List<?>) listenersField.get(repair);
                if (listeners != null) listeners.clear();
            } catch (Exception ignored) {}

            repairOutputMethod.invoke(repair);

            int cost = maximumCostField.getInt(repair);

            ItemStack resultStack = ItemStack.EMPTY;
            InventoryCraftResult stackResult = (InventoryCraftResult) stackResultField.get(repair);
            if (stackResult != null) {
                resultStack = stackResult.getStackInSlot(0).copy();
            }

            if (!resultStack.isEmpty() && cost > 0) {
                return new AnvilResult(resultStack, cost);
            }
        } catch (Exception e) {
            com.brilliafy.magicstorage.MagicStorage.LOGGER.warn("[MagicStorage] Anvil reflection failed", e);
        }
        return null;
    }

    // ===================== Display / Tooltip =====================

    /**
     * Get enchantments from an item — handles both normal enchants and StoredEnchantments (books).
     */
    private static Map<Enchantment, Integer> getAllEnchants(ItemStack stack) {
        Map<Enchantment, Integer> enchants = new LinkedHashMap<>();
        Map<Enchantment, Integer> normal = EnchantmentHelper.getEnchantments(stack);
        if (!normal.isEmpty()) enchants.putAll(normal);
        // Enchanted books store in StoredEnchantments
        if (stack.getItem() == Items.ENCHANTED_BOOK && stack.hasTagCompound()) {
            NBTTagList stored = stack.getTagCompound().getTagList("StoredEnchantments", 10);
            for (int i = 0; i < stored.tagCount(); i++) {
                NBTTagCompound tag = stored.getCompoundTagAt(i);
                Enchantment ench = Enchantment.getEnchantmentByID(tag.getShort("id"));
                if (ench != null) enchants.put(ench, (int) tag.getShort("lvl"));
            }
        }
        return enchants;
    }

    /**
     * Build a display stack with enchantment diff + "Level cost: X" lore for tooltip.
     * @param leftInput  the original left item (before anvil)
     * @param rightInput the original right item (before anvil)
     * @param anvilResult the vanilla anvil result
     * @param xpCost     the XP level cost
     */
    public static ItemStack buildDisplayStack(ItemStack leftInput, ItemStack rightInput, ItemStack anvilResult, int xpCost) {
        ItemStack display = anvilResult.copy();
        display.setCount(1);
        NBTTagCompound rootTag = display.hasTagCompound() ? display.getTagCompound().copy() : new NBTTagCompound();

        // Only add a fake ench tag if the item actually has enchantments
        // (avoids enchanted glint on non-enchanted repair results like iron sword + iron)
        NBTTagList enchList = rootTag.getTagList("ench", 10);
        NBTTagList storedEnchList = null;
        boolean hasRealEnchants = (enchList != null && enchList.tagCount() > 0);
        if (!hasRealEnchants && display.getItem() == Items.ENCHANTED_BOOK) {
            storedEnchList = rootTag.getTagList("StoredEnchantments", 10);
            hasRealEnchants = storedEnchList != null && storedEnchList.tagCount() > 0;
        }
        // Only add fake ench if there are real enchants (to preserve existing glint)
        // Non-enchanted items stay glint-free

        // Build lore: show ALL enchantments on the result + cost
        NBTTagList lore = new NBTTagList();
        Map<Enchantment, Integer> resultEnchants = getAllEnchants(display);

        for (Map.Entry<Enchantment, Integer> entry : resultEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int level = entry.getValue();
            String name = ench.getTranslatedName(level);
            lore.appendTag(new NBTTagString("\u00A77" + name));
        }

        // Separator + cost
        if (!resultEnchants.isEmpty()) {
            lore.appendTag(new NBTTagString(""));
        }
        lore.appendTag(new NBTTagString("\u00A7e\u00A7l\u00A7nLevel cost: " + xpCost));

        NBTTagCompound displayTag = rootTag.getCompoundTag("display");
        if (displayTag == null) displayTag = new NBTTagCompound();
        displayTag.setTag("Lore", lore);
        rootTag.setTag("display", displayTag);
        rootTag.setInteger("HideFlags", 1);
        display.setTagCompound(rootTag);
        return display;
    }

    // ===================== Consuming =====================

    /**
     * Consume ingredients for anvil craft.
     * Left item: always 1 (the item being repaired/renamed/enchanted).
     * Right item: for repair, calculates how many materials are needed based on durability.
     *             For enchant/rename, consumes 1.
     */
    public static void consumeIngredients(ItemStack[] matrix, ItemStack leftInput, ItemStack rightInput) {
        if (!matrix[0].isEmpty()) matrix[0].shrink(1);
        if (matrix[4].isEmpty()) return;

        // Check if this is a repair (same item or repairable material) vs enchant/rename
        int materialsNeeded = calculateMaterialCount(leftInput, rightInput);
        int available = matrix[4].getCount();
        int toConsume = Math.min(materialsNeeded, available);
        matrix[4].shrink(toConsume);
    }

    /**
     * Calculate how many of the right item are needed for repair.
     * Each material repairs 25% of max durability (vanilla behavior).
     * For non-repair scenarios (enchant, rename), returns 1.
     */
    private static int calculateMaterialCount(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return 1;

        boolean isSameItem = left.getItem() == right.getItem();
        boolean isRepairMaterial = !isSameItem && right.getItem().isRepairable() && left.getItem().isRepairable();

        if (!isSameItem && !isRepairMaterial) {
            // Enchant combining or name tag rename — consume 1
            return 1;
        }

        // Repair: calculate based on durability
        int maxDamage = left.getMaxDamage();
        if (maxDamage <= 0) return 1;

        int currentDamage = left.getItemDamage();
        int damageToRepair = maxDamage - currentDamage;
        if (damageToRepair <= 0) return 1; // Already full durability

        // Each material repairs 25% of max durability (vanilla formula)
        int repairPerMaterial = maxDamage / 4;
        if (repairPerMaterial <= 0) repairPerMaterial = 1;

        int needed = (damageToRepair + repairPerMaterial - 1) / repairPerMaterial; // ceiling division
        return Math.max(needed, 1);
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

    // ===================== Data classes =====================

    public static class AnvilResult {
        public final ItemStack stack;
        public final int cost;
        public AnvilResult(ItemStack stack, int cost) { this.stack = stack; this.cost = cost; }
    }

    public static class AnvilRecipeDisplay {
        public final ItemStack input;
        public final ItemStack secondary;
        public final ItemStack output;
        public AnvilRecipeDisplay(ItemStack input, ItemStack secondary, ItemStack output) {
            this.input = input; this.secondary = secondary; this.output = output;
        }
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
            nameTag.setStackDisplayName("\u00A7fAny Name");
            recipes.add(new AnvilRecipeDisplay(
                new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD),
                nameTag,
                new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD)));
        } catch (Exception e) {}
        return recipes;
    }
}
