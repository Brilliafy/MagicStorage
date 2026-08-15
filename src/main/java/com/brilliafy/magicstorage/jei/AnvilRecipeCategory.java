package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.reference.ModBlocksRef;
import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.util.AnvilCraftingHelper;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        public AnvilJEIRecipe(ItemStack input, ItemStack book, ItemStack output) {
            this(Collections.singletonList(input), Collections.singletonList(book), Collections.singletonList(output));
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inputs = new ArrayList<>();
            inputs.add(leftInputs);
            inputs.add(rightInputs);
            ingredients.setInputLists(ItemStack.class, inputs);
            ingredients.setOutputLists(ItemStack.class, Collections.singletonList(outputs));
        }
    }

    public static List<AnvilJEIRecipe> generateAllRecipes() {
        List<AnvilJEIRecipe> recipes = new ArrayList<>();

        // 1. Book + Book combining recipes (only when lvl < maxLevel)
        for (Enchantment e : Enchantment.REGISTRY) {
            if (e == null) continue;
            for (int lvl = 1; lvl < e.getMaxLevel(); lvl++) {
                ItemStack bookLeft = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantedBook.addEnchantment(bookLeft, new EnchantmentData(e, lvl));

                ItemStack bookRight = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantedBook.addEnchantment(bookRight, new EnchantmentData(e, lvl));

                ItemStack bookOut = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantedBook.addEnchantment(bookOut, new EnchantmentData(e, lvl + 1));

                recipes.add(new AnvilJEIRecipe(bookLeft, bookRight, bookOut));
            }
        }

        // 2. Item + Book and Enchanted Item + Book recipes
        for (Enchantment e : Enchantment.REGISTRY) {
            if (e == null) continue;
            for (int lvl = 1; lvl <= e.getMaxLevel(); lvl++) {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantedBook.addEnchantment(book, new EnchantmentData(e, lvl));

                for (Item item : Item.REGISTRY) {
                    if (item == null) continue;
                    ItemStack base = new ItemStack(item);
                    if (base.isEmpty()) continue;
                    if (base.isItemStackDamageable() || item instanceof ItemSword || item instanceof ItemTool || item instanceof ItemArmor || item instanceof ItemBow || item instanceof ItemShield) {
                        if (e.canApply(base) || (e.type != null && e.type.canEnchantItem(item))) {
                            ItemStack out = base.copy();
                            out.addEnchantment(e, lvl);
                            recipes.add(new AnvilJEIRecipe(base, book, out));

                            // Existing enchanted variant with lower level combining with higher book
                            if (lvl > 1) {
                                for (int lowerLvl = 1; lowerLvl < lvl; lowerLvl++) {
                                    ItemStack exist = base.copy();
                                    exist.addEnchantment(e, lowerLvl);
                                    ItemStack comb = base.copy();
                                    comb.addEnchantment(e, lvl); // Replaces lower with higher level cleanly
                                    recipes.add(new AnvilJEIRecipe(exist, book, comb));
                                }
                            }

                            // Same level item combining with book -> upgrades to lvl + 1
                            if (lvl < e.getMaxLevel()) {
                                ItemStack existSame = base.copy();
                                existSame.addEnchantment(e, lvl);
                                ItemStack combUp = base.copy();
                                combUp.addEnchantment(e, lvl + 1);
                                recipes.add(new AnvilJEIRecipe(existSame, book, combUp));
                            }
                        }
                    }
                }
            }
        }

        // 3. Item + Same Item Repair & Merge recipes
        for (Item item : Item.REGISTRY) {
            if (item == null) continue;
            ItemStack base = new ItemStack(item);
            if (base.isEmpty()) continue;
            if (base.isItemStackDamageable() || item instanceof ItemSword || item instanceof ItemTool || item instanceof ItemArmor || item instanceof ItemBow || item instanceof ItemShield) {
                // Combine same item repair
                ItemStack damaged1 = base.copy();
                damaged1.setItemDamage(Math.max(1, damaged1.getMaxDamage() / 2));
                ItemStack damaged2 = base.copy();
                damaged2.setItemDamage(Math.max(1, damaged2.getMaxDamage() / 3));
                ItemStack repaired = base.copy();
                repaired.setItemDamage(0);

                recipes.add(new AnvilJEIRecipe(damaged1, damaged2, repaired));
            }
        }

        return recipes;
    }
}
