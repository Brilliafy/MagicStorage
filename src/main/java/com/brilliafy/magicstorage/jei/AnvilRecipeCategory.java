package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.reference.ModBlocksRef;
import com.brilliafy.magicstorage.reference.ModInfo;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnvilRecipeCategory implements IRecipeCategory<AnvilRecipeCategory.AnvilJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".anvil";
    private final IDrawable background;
    private final IDrawable icon;

    public AnvilRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(160, 50);
        icon = helper.createDrawableIngredient(new ItemStack(ModBlocksRef.CRAFTING_ACCESS));
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return "Magic Storage Anvil"; }
    @Override public String getModName() { return ModInfo.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, AnvilJEIRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        gui.init(0, true, 10, 15);   // input item
        gui.init(1, true, 45, 15);   // secondary item (book or material or 2nd item)
        gui.init(2, false, 80, 15);  // required station: anvil
        gui.init(3, false, 125, 15); // output item

        gui.set(0, ingredients.getInputs(ItemStack.class).get(0));
        gui.set(1, ingredients.getInputs(ItemStack.class).get(1));
        gui.set(2, new ItemStack(Blocks.ANVIL));
        gui.set(3, ingredients.getOutputs(ItemStack.class).get(0));

        gui.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (slotIndex == 2) {
                tooltip.add(TextFormatting.GOLD + "Requires Anvil in Storage Heart");
                tooltip.add(TextFormatting.GRAY + "Place an Anvil inside the Storage Heart to craft.");
            }
        });
    }

    public static class AnvilJEIRecipe implements IRecipeWrapper {
        private final List<ItemStack> leftInputs;
        private final List<ItemStack> rightInputs;
        private final List<ItemStack> outputs;

        public AnvilJEIRecipe(List<ItemStack> leftInputs, List<ItemStack> rightInputs, List<ItemStack> outputs) {
            this.leftInputs = leftInputs;
            this.rightInputs = rightInputs;
            this.outputs = outputs;
        }

        public AnvilJEIRecipe(ItemStack input, ItemStack secondary, ItemStack output) {
            this(Collections.singletonList(input), Collections.singletonList(secondary), Collections.singletonList(output));
        }

        public List<ItemStack> getLeftInputs() { return leftInputs; }
        public List<ItemStack> getRightInputs() { return rightInputs; }
        public List<ItemStack> getOutputs() { return outputs; }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inputs = new ArrayList<>();
            inputs.add(leftInputs);
            inputs.add(rightInputs);
            ingredients.setInputLists(ItemStack.class, inputs);
            ingredients.setOutputLists(ItemStack.class, Collections.singletonList(outputs));
        }
    }

    // ===================== Helpers & Dynamic Resolution =====================

    private static List<ItemStack> equipmentPrototypes = null;

    public static List<ItemStack> getEquipmentPrototypes() {
        if (equipmentPrototypes == null) {
            List<ItemStack> list = new ArrayList<>();
            for (Item item : Item.REGISTRY) {
                if (item != null) {
                    ItemStack st = new ItemStack(item);
                    if (isEquipment(st)) {
                        list.add(st);
                    }
                }
            }
            equipmentPrototypes = Collections.unmodifiableList(list);
        }
        return equipmentPrototypes;
    }

    public static boolean isEquipment(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item == null) return false;
        return stack.isItemStackDamageable() ||
               item instanceof ItemSword ||
               item instanceof ItemTool ||
               item instanceof ItemArmor ||
               item instanceof ItemBow ||
               item instanceof ItemShield ||
               item instanceof net.minecraft.item.ItemShears ||
               item instanceof net.minecraft.item.ItemFishingRod ||
               item instanceof net.minecraft.item.ItemFlintAndSteel ||
               item instanceof net.minecraft.item.ItemHoe ||
               item instanceof net.minecraft.item.ItemElytra;
    }

    public static boolean canEnchant(Enchantment e, ItemStack stack) {
        if (e == null || stack == null || stack.isEmpty()) return false;
        try {
            if (e.canApply(stack)) return true;
        } catch (Throwable ignored) {}
        try {
            if (e.canApplyAtEnchantingTable(stack)) return true;
        } catch (Throwable ignored) {}
        try {
            if (e.type != null && e.type.canEnchantItem(stack.getItem())) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static Map<Enchantment, Integer> getEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> enchs = new LinkedHashMap<>();
        if (stack == null || stack.isEmpty()) return enchs;
        if (stack.getItem() == Items.ENCHANTED_BOOK) {
            NBTTagList stored = ItemEnchantedBook.getEnchantments(stack);
            if (stored != null) {
                for (int i = 0; i < stored.tagCount(); i++) {
                    NBTTagCompound tag = stored.getCompoundTagAt(i);
                    Enchantment ench = Enchantment.getEnchantmentByID(tag.getShort("id"));
                    if (ench != null) {
                        enchs.put(ench, (int) tag.getShort("lvl"));
                    }
                }
            }
        } else {
            Map<Enchantment, Integer> normal = EnchantmentHelper.getEnchantments(stack);
            if (normal != null && !normal.isEmpty()) {
                enchs.putAll(normal);
            }
        }
        return enchs;
    }

    public static ItemStack createEnchantedBook(Enchantment e, int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantedBook.addEnchantment(book, new EnchantmentData(e, level));
        return book;
    }

    public static ItemStack withEnchantment(ItemStack stack, Enchantment e, int level) {
        ItemStack copy = stack.copy();
        Map<Enchantment, Integer> enchs = getEnchantments(copy);
        enchs.put(e, level);
        EnchantmentHelper.setEnchantments(enchs, copy);
        return copy;
    }

    public static ItemStack withoutEnchantment(ItemStack stack, Enchantment e) {
        ItemStack copy = stack.copy();
        Map<Enchantment, Integer> enchs = getEnchantments(copy);
        enchs.remove(e);
        EnchantmentHelper.setEnchantments(enchs, copy);
        return copy;
    }

    public static boolean isAnvilInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() == Items.ENCHANTED_BOOK) return true;
        if (isEquipment(stack)) return true;
        if (stack.getItem() == Item.getItemFromBlock(ModBlocksRef.CRAFTING_ACCESS)) return true;
        return false;
    }

    public static boolean isAnvilOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() == Items.ENCHANTED_BOOK) {
            Map<Enchantment, Integer> enchs = getEnchantments(stack);
            for (int lvl : enchs.values()) {
                if (lvl > 1) return true;
            }
            return false;
        }
        if (stack.isItemEnchanted()) return true;
        if (isEquipment(stack)) return true;
        return false;
    }

    // ===================== Caching =====================

    private static ItemStack lastInputFocus = ItemStack.EMPTY;
    private static List<AnvilJEIRecipe> lastInputCache = null;

    private static ItemStack lastOutputFocus = ItemStack.EMPTY;
    private static List<AnvilJEIRecipe> lastOutputCache = null;

    public static synchronized List<AnvilJEIRecipe> getCachedRecipesForInput(ItemStack input, List<AnvilJEIRecipe> baseline) {
        if (input == null || input.isEmpty()) return Collections.emptyList();
        if (!lastInputFocus.isEmpty() && ItemStack.areItemStacksEqual(lastInputFocus, input) && ItemStack.areItemStackTagsEqual(lastInputFocus, input)) {
            return lastInputCache;
        }
        List<AnvilJEIRecipe> result = getRecipesForInput(input, baseline);
        lastInputFocus = input.copy();
        lastInputCache = result;
        return result;
    }

    public static synchronized List<AnvilJEIRecipe> getCachedRecipesForOutput(ItemStack output) {
        if (output == null || output.isEmpty()) return Collections.emptyList();
        if (!lastOutputFocus.isEmpty() && ItemStack.areItemStacksEqual(lastOutputFocus, output) && ItemStack.areItemStackTagsEqual(lastOutputFocus, output)) {
            return lastOutputCache;
        }
        List<AnvilJEIRecipe> result = getRecipesForOutput(output);
        lastOutputFocus = output.copy();
        lastOutputCache = result;
        return result;
    }

    // ===================== Dynamic Recipe Calculations =====================

    public static List<AnvilJEIRecipe> getRecipesForInput(ItemStack input, List<AnvilJEIRecipe> baseline) {
        if (input == null || input.isEmpty()) return Collections.emptyList();

        // 1. If focus is Enchanted Book:
        if (input.getItem() == Items.ENCHANTED_BOOK) {
            Map<Enchantment, Integer> enchs = getEnchantments(input);
            if (enchs.isEmpty()) return Collections.emptyList();

            List<AnvilJEIRecipe> recipes = new ArrayList<>();
            for (Map.Entry<Enchantment, Integer> entry : enchs.entrySet()) {
                Enchantment e = entry.getKey();
                int lvl = entry.getValue();

                // Book + Same Book combining (if lvl < maxLevel)
                if (lvl < e.getMaxLevel()) {
                    ItemStack bookLeft = input.copy(); bookLeft.setCount(1);
                    ItemStack bookRight = input.copy(); bookRight.setCount(1);
                    ItemStack bookOut = createEnchantedBook(e, lvl + 1);
                    recipes.add(new AnvilJEIRecipe(bookLeft, bookRight, bookOut));
                }

                // Equipment that can receive this enchantment
                for (ItemStack base : getEquipmentPrototypes()) {
                    if (canEnchant(e, base)) {
                        // Base unenchanted + Book -> Base with (e, lvl)
                        ItemStack out = withEnchantment(base, e, lvl);
                        recipes.add(new AnvilJEIRecipe(base.copy(), input.copy(), out));

                        // If lvl > 1: Item with (lvl - 1) + Book -> Base with (e, lvl)
                        if (lvl > 1) {
                            ItemStack lower = withEnchantment(base, e, lvl - 1);
                            recipes.add(new AnvilJEIRecipe(lower, input.copy(), out));
                        }

                        // If lvl < maxLevel: Item with (lvl) + Book -> Base with (e, lvl + 1)
                        if (lvl < e.getMaxLevel()) {
                            ItemStack same = withEnchantment(base, e, lvl);
                            ItemStack combUp = withEnchantment(base, e, lvl + 1);
                            recipes.add(new AnvilJEIRecipe(same, input.copy(), combUp));
                        }
                    }
                }
            }
            return recipes;
        }

        // 2. If focus is Equipment:
        if (isEquipment(input)) {
            List<AnvilJEIRecipe> recipes = new ArrayList<>();

            // Repair recipe: Damaged + Damaged -> Repaired
            ItemStack damaged1 = input.copy();
            damaged1.setItemDamage(Math.max(1, damaged1.getMaxDamage() / 2));
            ItemStack damaged2 = input.copy();
            damaged2.setItemDamage(Math.max(1, damaged2.getMaxDamage() / 3));
            ItemStack repaired = input.copy();
            repaired.setItemDamage(0);
            recipes.add(new AnvilJEIRecipe(damaged1, damaged2, repaired));

            // Enchanting with books
            Map<Enchantment, Integer> currentEnchs = getEnchantments(input);

            for (Enchantment e : Enchantment.REGISTRY) {
                if (e == null) continue;
                if (canEnchant(e, input)) {
                    // Check compatibility with existing enchantments on this tool
                    boolean compatible = true;
                    for (Enchantment cur : currentEnchs.keySet()) {
                        if (cur != e && !cur.isCompatibleWith(e)) {
                            compatible = false;
                            break;
                        }
                    }
                    if (!compatible) continue;

                    int currentLvl = currentEnchs.getOrDefault(e, 0);
                    if (currentLvl == 0) {
                        for (int lvl = 1; lvl <= e.getMaxLevel(); lvl++) {
                            ItemStack book = createEnchantedBook(e, lvl);
                            ItemStack out = withEnchantment(input, e, lvl);
                            recipes.add(new AnvilJEIRecipe(input.copy(), book, out));
                        }
                    } else {
                        // Upgrade existing enchantment with same level book -> lvl + 1
                        if (currentLvl < e.getMaxLevel()) {
                            ItemStack book = createEnchantedBook(e, currentLvl);
                            ItemStack out = withEnchantment(input, e, currentLvl + 1);
                            recipes.add(new AnvilJEIRecipe(input.copy(), book, out));
                        }
                        // Upgrade with higher level book
                        for (int lvl = currentLvl + 1; lvl <= e.getMaxLevel(); lvl++) {
                            ItemStack book = createEnchantedBook(e, lvl);
                            ItemStack out = withEnchantment(input, e, lvl);
                            recipes.add(new AnvilJEIRecipe(input.copy(), book, out));
                        }
                    }
                }
            }
            return recipes;
        }

        return Collections.emptyList();
    }

    public static List<AnvilJEIRecipe> getRecipesForOutput(ItemStack output) {
        if (output == null || output.isEmpty()) return Collections.emptyList();

        // 1. If output is Enchanted Book:
        if (output.getItem() == Items.ENCHANTED_BOOK) {
            Map<Enchantment, Integer> enchs = getEnchantments(output);
            if (enchs.isEmpty()) return Collections.emptyList();

            List<AnvilJEIRecipe> recipes = new ArrayList<>();
            for (Map.Entry<Enchantment, Integer> entry : enchs.entrySet()) {
                Enchantment e = entry.getKey();
                int lvl = entry.getValue();

                if (lvl > 1) {
                    ItemStack bookLeft = createEnchantedBook(e, lvl - 1);
                    ItemStack bookRight = createEnchantedBook(e, lvl - 1);
                    recipes.add(new AnvilJEIRecipe(bookLeft, bookRight, output.copy()));
                }
            }
            return recipes;
        }

        // 2. If output is Equipment:
        if (isEquipment(output)) {
            List<AnvilJEIRecipe> recipes = new ArrayList<>();

            // Repair recipe: Damaged + Damaged -> Repaired (this output with damage 0)
            ItemStack damaged1 = output.copy();
            damaged1.setItemDamage(Math.max(1, damaged1.getMaxDamage() / 2));
            ItemStack damaged2 = output.copy();
            damaged2.setItemDamage(Math.max(1, damaged2.getMaxDamage() / 3));
            ItemStack repOut = output.copy();
            repOut.setItemDamage(0);
            recipes.add(new AnvilJEIRecipe(damaged1, damaged2, repOut));

            // If enchanted: recipes that craft this enchantment combination
            Map<Enchantment, Integer> enchs = getEnchantments(output);
            for (Map.Entry<Enchantment, Integer> entry : enchs.entrySet()) {
                Enchantment e = entry.getKey();
                int lvl = entry.getValue();

                // 2a. Base without this enchantment + Book(lvl) -> output
                ItemStack baseWithout = withoutEnchantment(output, e);
                ItemStack book = createEnchantedBook(e, lvl);
                recipes.add(new AnvilJEIRecipe(baseWithout, book, output.copy()));

                // 2b. Same item with (lvl - 1) + Book(lvl - 1) -> output (combining two lvl-1 to reach lvl)
                if (lvl > 1) {
                    ItemStack lowerItem = withEnchantment(baseWithout, e, lvl - 1);
                    ItemStack lowerBook = createEnchantedBook(e, lvl - 1);
                    recipes.add(new AnvilJEIRecipe(lowerItem, lowerBook, output.copy()));
                }

                // 2c. Same item with lower level + Book(lvl) -> output (replaces lower with lvl)
                if (lvl > 2) {
                    for (int lowerLvl = 1; lowerLvl < lvl - 1; lowerLvl++) {
                        ItemStack lowerItem = withEnchantment(baseWithout, e, lowerLvl);
                        recipes.add(new AnvilJEIRecipe(lowerItem, book, output.copy()));
                    }
                }
            }
            return recipes;
        }

        return Collections.emptyList();
    }

    // ===================== Baseline Recipes =====================

    public static List<AnvilJEIRecipe> generateBaselineRecipes() {
        List<AnvilJEIRecipe> recipes = new ArrayList<>();

        // 1. Book + Book combining recipes for all registered enchantments
        for (Enchantment e : Enchantment.REGISTRY) {
            if (e == null) continue;
            for (int lvl = 1; lvl < e.getMaxLevel(); lvl++) {
                ItemStack bookLeft = createEnchantedBook(e, lvl);
                ItemStack bookRight = createEnchantedBook(e, lvl);
                ItemStack bookOut = createEnchantedBook(e, lvl + 1);
                recipes.add(new AnvilJEIRecipe(bookLeft, bookRight, bookOut));
            }
        }

        // 2. Sample repairs for common representative items
        Item[] sampleItems = {
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD,
            Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.BOW, Items.SHIELD,
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS
        };

        for (Item item : sampleItems) {
            if (item == null) continue;
            ItemStack base = new ItemStack(item);
            ItemStack d1 = base.copy(); d1.setItemDamage(Math.max(1, d1.getMaxDamage() / 2));
            ItemStack d2 = base.copy(); d2.setItemDamage(Math.max(1, d2.getMaxDamage() / 3));
            ItemStack rep = base.copy(); rep.setItemDamage(0);
            recipes.add(new AnvilJEIRecipe(d1, d2, rep));
        }

        // 3. Sample tool + book enchanting
        if (net.minecraft.init.Enchantments.SHARPNESS != null) {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            ItemStack book = createEnchantedBook(net.minecraft.init.Enchantments.SHARPNESS, 5);
            recipes.add(new AnvilJEIRecipe(sword, book, withEnchantment(sword, net.minecraft.init.Enchantments.SHARPNESS, 5)));
        }
        if (net.minecraft.init.Enchantments.EFFICIENCY != null) {
            ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
            ItemStack book = createEnchantedBook(net.minecraft.init.Enchantments.EFFICIENCY, 5);
            recipes.add(new AnvilJEIRecipe(pick, book, withEnchantment(pick, net.minecraft.init.Enchantments.EFFICIENCY, 5)));
        }
        if (net.minecraft.init.Enchantments.INFINITY != null) {
            ItemStack bow = new ItemStack(Items.BOW);
            ItemStack book = createEnchantedBook(net.minecraft.init.Enchantments.INFINITY, 1);
            recipes.add(new AnvilJEIRecipe(bow, book, withEnchantment(bow, net.minecraft.init.Enchantments.INFINITY, 1)));
        }

        return recipes;
    }

    public static List<AnvilJEIRecipe> generateAllRecipes() {
        return generateBaselineRecipes();
    }
}
