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
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EnchantingRecipeCategory implements IRecipeCategory<EnchantingRecipeCategory.EnchantingJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".enchanting";
    private final IDrawable background;
    private final IDrawable icon;

    public EnchantingRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(160, 50);
        icon = helper.createDrawableIngredient(new ItemStack(ModBlocksRef.CRAFTING_ACCESS));
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return "Magic Storage Enchanting"; }
    @Override public String getModName() { return ModInfo.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, EnchantingJEIRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        gui.init(0, true, 10, 15);   // enchantable item / book
        gui.init(1, true, 45, 15);   // lapis lazuli
        gui.init(2, false, 80, 15);  // required station: enchanting table
        gui.init(3, false, 125, 15); // output enchanted item

        gui.set(0, ingredients.getInputs(ItemStack.class).get(0));
        gui.set(1, ingredients.getInputs(ItemStack.class).get(1));
        gui.set(2, new ItemStack(Blocks.ENCHANTING_TABLE));
        gui.set(3, ingredients.getOutputs(ItemStack.class).get(0));

        gui.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (slotIndex == 2) {
                tooltip.add(TextFormatting.GOLD + "Requires Enchanting Table in Storage Heart");
                tooltip.add(TextFormatting.GRAY + "Place an Enchanting Table inside the Storage Heart to craft.");
            }
        });
    }

    public static class EnchantingJEIRecipe implements IRecipeWrapper {
        private final List<ItemStack> inputs;
        private final ItemStack lapis;
        private final List<ItemStack> outputs;

        public EnchantingJEIRecipe(List<ItemStack> inputs, ItemStack lapis, List<ItemStack> outputs) {
            this.inputs = inputs;
            this.lapis = lapis;
            this.outputs = outputs;
        }

        public EnchantingJEIRecipe(ItemStack singleInput, ItemStack lapis, ItemStack singleOutput) {
            this(Collections.singletonList(singleInput), lapis, Collections.singletonList(singleOutput));
        }

        public List<ItemStack> getInputs() { return inputs; }
        public ItemStack getLapis() { return lapis; }
        public List<ItemStack> getOutputs() { return outputs; }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inLists = new ArrayList<>();
            inLists.add(inputs);
            inLists.add(Collections.singletonList(lapis));
            ingredients.setInputLists(ItemStack.class, inLists);
            ingredients.setOutputLists(ItemStack.class, Collections.singletonList(outputs));
        }

        public boolean matchesInput(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            for (ItemStack in : inputs) {
                if (in.getItem() == stack.getItem() && in.getMetadata() == stack.getMetadata()) {
                    return true;
                }
            }
            return false;
        }

        public boolean matchesOutput(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            for (ItemStack out : outputs) {
                if (out.getItem() == stack.getItem()) {
                    if (out.getItem() == Items.ENCHANTED_BOOK) {
                        net.minecraft.nbt.NBTTagList outEnch = net.minecraft.item.ItemEnchantedBook.getEnchantments(out);
                        net.minecraft.nbt.NBTTagList stackEnch = net.minecraft.item.ItemEnchantedBook.getEnchantments(stack);
                        if (isEnchSubset(outEnch, stackEnch)) return true;
                    } else if (stack.isItemEnchanted()) {
                        net.minecraft.nbt.NBTTagList outEnch = out.getEnchantmentTagList();
                        net.minecraft.nbt.NBTTagList stackEnch = stack.getEnchantmentTagList();
                        if (isEnchSubset(outEnch, stackEnch)) return true;
                    }
                }
            }
            return false;
        }

        private static boolean isEnchSubset(net.minecraft.nbt.NBTTagList req, net.minecraft.nbt.NBTTagList actual) {
            if (req == null || req.isEmpty()) return true;
            if (actual == null || actual.isEmpty()) return false;
            for (int i = 0; i < req.tagCount(); i++) {
                short reqId = req.getCompoundTagAt(i).getShort("id");
                short reqLvl = req.getCompoundTagAt(i).getShort("lvl");
                boolean found = false;
                for (int j = 0; j < actual.tagCount(); j++) {
                    if (actual.getCompoundTagAt(j).getShort("id") == reqId && actual.getCompoundTagAt(j).getShort("lvl") == reqLvl) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
    }

    // ===================== Dynamic Classification & Helpers =====================

    public static boolean isEnchantingInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() == Items.DYE && stack.getMetadata() == 4) return true; // Lapis Lazuli
        if (stack.getItem() == Items.BOOK) return true;
        if (AnvilRecipeCategory.isEquipment(stack) && !stack.isItemEnchanted()) return true;
        if (stack.getItem() == Item.getItemFromBlock(ModBlocksRef.CRAFTING_ACCESS)) return true;
        return false;
    }

    public static boolean isEnchantingOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() == Items.ENCHANTED_BOOK) return true;
        if (AnvilRecipeCategory.isEquipment(stack) && stack.isItemEnchanted()) return true;
        return false;
    }

    // ===================== Baseline Recipes =====================

    public static List<EnchantingJEIRecipe> generateBaselineRecipes() {
        List<EnchantingJEIRecipe> recipes = new ArrayList<>();
        ItemStack lapis = new ItemStack(Items.DYE, 1, 4);
        ItemStack regularBook = new ItemStack(Items.BOOK);

        for (Enchantment e : Enchantment.REGISTRY) {
            if (e == null) continue;
            for (int lvl = 1; lvl <= e.getMaxLevel(); lvl++) {
                ItemStack outputBook = AnvilRecipeCategory.createEnchantedBook(e, lvl);
                recipes.add(new EnchantingJEIRecipe(regularBook, lapis, outputBook));
            }
        }

        // Representative equipment enchants
        if (net.minecraft.init.Enchantments.SHARPNESS != null) {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            recipes.add(new EnchantingJEIRecipe(sword, lapis, AnvilRecipeCategory.withEnchantment(sword, net.minecraft.init.Enchantments.SHARPNESS, 1)));
        }
        if (net.minecraft.init.Enchantments.EFFICIENCY != null) {
            ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
            recipes.add(new EnchantingJEIRecipe(pick, lapis, AnvilRecipeCategory.withEnchantment(pick, net.minecraft.init.Enchantments.EFFICIENCY, 1)));
        }

        return recipes;
    }

    public static List<EnchantingJEIRecipe> generateAllRecipes() {
        return generateBaselineRecipes();
    }

    // ===================== Caching =====================

    private static ItemStack lastInputFocus = ItemStack.EMPTY;
    private static List<EnchantingJEIRecipe> lastInputCache = null;

    private static ItemStack lastOutputFocus = ItemStack.EMPTY;
    private static List<EnchantingJEIRecipe> lastOutputCache = null;

    public static synchronized List<EnchantingJEIRecipe> getCachedRecipesForInput(ItemStack input, List<EnchantingJEIRecipe> baseline) {
        if (input == null || input.isEmpty()) return Collections.emptyList();
        if (!lastInputFocus.isEmpty() && ItemStack.areItemStacksEqual(lastInputFocus, input) && ItemStack.areItemStackTagsEqual(lastInputFocus, input)) {
            return lastInputCache;
        }
        List<EnchantingJEIRecipe> result = getRecipesForInput(input, baseline);
        lastInputFocus = input.copy();
        lastInputCache = result;
        return result;
    }

    public static synchronized List<EnchantingJEIRecipe> getCachedRecipesForOutput(ItemStack output) {
        if (output == null || output.isEmpty()) return Collections.emptyList();
        if (!lastOutputFocus.isEmpty() && ItemStack.areItemStacksEqual(lastOutputFocus, output) && ItemStack.areItemStackTagsEqual(lastOutputFocus, output)) {
            return lastOutputCache;
        }
        List<EnchantingJEIRecipe> result = getRecipesForOutput(output);
        lastOutputFocus = output.copy();
        lastOutputCache = result;
        return result;
    }

    // ===================== Dynamic Recipe Calculations =====================

    public static List<EnchantingJEIRecipe> getRecipesForInput(ItemStack input, List<EnchantingJEIRecipe> baseline) {
        if (input == null || input.isEmpty()) return Collections.emptyList();
        ItemStack lapis = new ItemStack(Items.DYE, 1, 4);

        // 1. If input is Lapis Lazuli: return baseline
        if (input.getItem() == Items.DYE && input.getMetadata() == 4) {
            return baseline;
        }

        // 2. If input is Book:
        if (input.getItem() == Items.BOOK) {
            List<EnchantingJEIRecipe> recipes = new ArrayList<>();
            for (Enchantment e : Enchantment.REGISTRY) {
                if (e == null) continue;
                for (int lvl = 1; lvl <= e.getMaxLevel(); lvl++) {
                    ItemStack outputBook = AnvilRecipeCategory.createEnchantedBook(e, lvl);
                    recipes.add(new EnchantingJEIRecipe(input.copy(), lapis, outputBook));
                }
            }
            return recipes;
        }

        // 3. If input is unenchanted equipment:
        if (AnvilRecipeCategory.isEquipment(input) && !input.isItemEnchanted()) {
            List<EnchantingJEIRecipe> recipes = new ArrayList<>();
            for (Enchantment e : Enchantment.REGISTRY) {
                if (e == null) continue;
                if (AnvilRecipeCategory.canEnchant(e, input)) {
                    for (int lvl = 1; lvl <= e.getMaxLevel(); lvl++) {
                        ItemStack out = AnvilRecipeCategory.withEnchantment(input, e, lvl);
                        recipes.add(new EnchantingJEIRecipe(input.copy(), lapis, out));
                    }
                }
            }
            return recipes;
        }

        return Collections.emptyList();
    }

    public static List<EnchantingJEIRecipe> getRecipesForOutput(ItemStack output) {
        if (output == null || output.isEmpty()) return Collections.emptyList();
        ItemStack lapis = new ItemStack(Items.DYE, 1, 4);

        // 1. If output is Enchanted Book:
        if (output.getItem() == Items.ENCHANTED_BOOK) {
            Map<Enchantment, Integer> enchs = AnvilRecipeCategory.getEnchantments(output);
            if (enchs.isEmpty()) return Collections.emptyList();

            List<EnchantingJEIRecipe> recipes = new ArrayList<>();
            ItemStack regularBook = new ItemStack(Items.BOOK);
            for (Map.Entry<Enchantment, Integer> entry : enchs.entrySet()) {
                ItemStack outBook = AnvilRecipeCategory.createEnchantedBook(entry.getKey(), entry.getValue());
                recipes.add(new EnchantingJEIRecipe(regularBook, lapis, outBook));
            }
            return recipes;
        }

        // 2. If output is enchanted equipment:
        if (AnvilRecipeCategory.isEquipment(output) && output.isItemEnchanted()) {
            List<EnchantingJEIRecipe> recipes = new ArrayList<>();
            Map<Enchantment, Integer> enchs = AnvilRecipeCategory.getEnchantments(output);
            for (Map.Entry<Enchantment, Integer> entry : enchs.entrySet()) {
                ItemStack baseWithout = AnvilRecipeCategory.withoutEnchantment(output, entry.getKey());
                recipes.add(new EnchantingJEIRecipe(baseWithout, lapis, output.copy()));
            }
            return recipes;
        }

        return Collections.emptyList();
    }
}
