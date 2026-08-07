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
 * Nametag rename is intentionally blocked.
 */
public class AnvilCraftingHelper {

    private static Method repairOutputMethod;
    private static Field inputSlotsField;
    private static Field stackResultField;
    private static Field maximumCostField;
    private static Field materialCostField;

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
            materialCostField = findField(ContainerRepair.class,
                new String[]{"materialCost", "field_82856_l"});
            if (inputSlotsField == null) {
                inputSlotsField = findFieldByType(ContainerRepair.class, IInventory.class, InventoryCraftResult.class);
            }
            com.brilliafy.magicstorage.MagicStorage.LOGGER.info("[MagicStorage] Anvil reflection: method=" + (repairOutputMethod != null)
                + " inputSlots=" + (inputSlotsField != null)
                + " stackResult=" + (stackResultField != null)
                + " maxCost=" + (maximumCostField != null)
                + " matCost=" + (materialCostField != null));
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

    public static boolean canCraft(ItemStack slot0, ItemStack slot4, EntityPlayer player) {
        if (slot0.isEmpty() || slot4.isEmpty()) return false;
        if (isAnvil(slot0) || isAnvil(slot4)) return false;
        if (slot4.getItem() == Items.NAME_TAG) return false;
        AnvilResult ar = computeResult(slot0, slot4, player);
        return ar != null;
    }

    public static boolean isAnvilGrid(ItemStack[] m) {
        if (m == null || m.length < 9) return false;
        if (m[0].isEmpty() || m[4].isEmpty()) return false;
        for (int i = 0; i < 9; i++) {
            if (i != 0 && i != 4 && !m[i].isEmpty()) return false;
        }
        return true;
    }

    /**
     * Compute result, XP cost, AND material cost via vanilla ContainerRepair reflection.
     * materialCost is the exact number of right-item units vanilla consumes.
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

            try {
                Field listenersField = Container.class.getDeclaredField("listeners");
                listenersField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<?> listeners = (java.util.List<?>) listenersField.get(repair);
                if (listeners != null) listeners.clear();
            } catch (Exception ignored) {}

            repairOutputMethod.invoke(repair);

            int cost = maximumCostField.getInt(repair);
            int matCost = materialCostField != null ? materialCostField.getInt(repair) : 1;
                // materialCost is only set by the repair branch; enchant/rename defaults to 0
                if (matCost <= 0) matCost = 1;

            ItemStack resultStack = ItemStack.EMPTY;
            InventoryCraftResult stackResult = (InventoryCraftResult) stackResultField.get(repair);
            if (stackResult != null) {
                resultStack = stackResult.getStackInSlot(0).copy();
            }

            if (!resultStack.isEmpty() && cost >= 0) {
                if (left.hasDisplayName()) {
                    resultStack.setStackDisplayName(left.getDisplayName());
                }
                // Strip any lore from the result
                NBTTagCompound tag = resultStack.getTagCompound();
                if (tag != null && tag.hasKey("display", 10)) {
                    NBTTagCompound display = tag.getCompoundTag("display");
                    display.removeTag("Lore");
                    if (display.getSize() == 0) tag.removeTag("display");
                }
                return new AnvilResult(resultStack, cost, matCost);
            }
        } catch (Exception e) {
            com.brilliafy.magicstorage.MagicStorage.LOGGER.warn("[MagicStorage] Anvil reflection failed", e);
        }
        return null;
    }

    // ===================== Display / Tooltip =====================

    private static Map<Enchantment, Integer> getAllEnchants(ItemStack stack) {
        Map<Enchantment, Integer> enchants = new LinkedHashMap<>();
        if (stack.getItem() == Items.ENCHANTED_BOOK && stack.hasTagCompound()) {
            NBTTagList stored = stack.getTagCompound().getTagList("StoredEnchantments", 10);
            for (int i = 0; i < stored.tagCount(); i++) {
                NBTTagCompound tag = stored.getCompoundTagAt(i);
                Enchantment ench = Enchantment.getEnchantmentByID(tag.getShort("id"));
                if (ench != null) enchants.put(ench, (int) tag.getShort("lvl"));
            }
        } else {
            Map<Enchantment, Integer> normal = EnchantmentHelper.getEnchantments(stack);
            if (!normal.isEmpty()) enchants.putAll(normal);
        }
        return enchants;
    }

    public static ItemStack buildDisplayStack(ItemStack leftInput, ItemStack rightInput, ItemStack anvilResult, int xpCost) {
        ItemStack display = anvilResult.copy();
        display.setCount(1);
        NBTTagCompound rootTag = display.hasTagCompound() ? display.getTagCompound().copy() : new NBTTagCompound();

        if (leftInput.hasDisplayName()) {
            display.setStackDisplayName(leftInput.getDisplayName());
        }

        NBTTagList lore = new NBTTagList();
        boolean isEnchantedBook = display.getItem() == Items.ENCHANTED_BOOK;
        if (!isEnchantedBook) {
            Map<Enchantment, Integer> resultEnchants = getAllEnchants(display);
            for (Map.Entry<Enchantment, Integer> entry : resultEnchants.entrySet()) {
                lore.appendTag(new NBTTagString("\u00A77" + entry.getKey().getTranslatedName(entry.getValue())));
            }
            if (!resultEnchants.isEmpty()) {
                lore.appendTag(new NBTTagString(""));
            }
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

    /** Overload with custom error message (e.g. insufficient XP) */
    public static ItemStack buildDisplayStack(ItemStack leftInput, ItemStack rightInput, ItemStack anvilResult, int xpCost, String errorMessage) {
        ItemStack display = buildDisplayStack(leftInput, rightInput, anvilResult, xpCost);
        NBTTagCompound rootTag = display.getTagCompound();
        if (rootTag != null && rootTag.hasKey("display", 10)) {
            NBTTagCompound displayTag = rootTag.getCompoundTag("display");
            NBTTagList lore = displayTag.hasKey("Lore", 9) ? displayTag.getTagList("Lore", 8) : new NBTTagList();
            // Make the cost line red instead of yellow
            NBTTagList newLore = new NBTTagList();
            for (int i = 0; i < lore.tagCount(); i++) {
                String line = lore.getStringTagAt(i);
                if (line.contains("Level cost:")) {
                    newLore.appendTag(new NBTTagString(line.replace("\u00A7e", "\u00A7c")));
                } else {
                    newLore.appendTag(new NBTTagString(line));
                }
            }
            // Append red insufficient XP line after cost
            newLore.appendTag(new NBTTagString("")); // blank line before error
            newLore.appendTag(new NBTTagString("\u00A7c\u2716 Insufficient XP"));
            displayTag.setTag("Lore", newLore);
        }
        return display;
    }

    // ===================== Consuming =====================

    /**
     * Consume ingredients using vanilla's materialCost (read from ContainerRepair).
     */
    public static void consumeIngredients(ItemStack[] matrix, AnvilResult anvilResult) {
        if (matrix[0].isEmpty() || matrix[4].isEmpty()) return;

        // Use vanilla's materialCost — it already calculated exactly how many to consume
        int toConsume = Math.min(anvilResult.materialCost, matrix[4].getCount());

        matrix[0].shrink(1);
        matrix[4].shrink(toConsume);
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
        public final int materialCost;
        public AnvilResult(ItemStack stack, int cost, int materialCost) {
            this.stack = stack; this.cost = cost; this.materialCost = materialCost;
        }
    }

    public static class AnvilRecipeDisplay {
        public final ItemStack input;
        public final ItemStack secondary;
        public final ItemStack output;
        public AnvilRecipeDisplay(ItemStack input, ItemStack secondary, ItemStack output) {
            this.input = input; this.secondary = secondary; this.output = output;
        }
    }

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
        } catch (Exception e) {}
        return recipes;
    }
}
