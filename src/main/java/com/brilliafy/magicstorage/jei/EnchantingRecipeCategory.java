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
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public static List<EnchantingJEIRecipe> generateAllRecipes() {
        List<EnchantingJEIRecipe> recipes = new ArrayList<>();
        ItemStack lapis = new ItemStack(Items.DYE, 1, 4);

        for (net.minecraft.enchantment.Enchantment e : net.minecraft.enchantment.Enchantment.REGISTRY) {
            if (e == null) continue;
            for (int lvl = 1; lvl <= e.getMaxLevel(); lvl++) {
                // 1. Book -> Enchanted Book with this enchantment
                ItemStack outputBook = new ItemStack(Items.ENCHANTED_BOOK);
                net.minecraft.item.ItemEnchantedBook.addEnchantment(outputBook, new net.minecraft.enchantment.EnchantmentData(e, lvl));
                recipes.add(new EnchantingJEIRecipe(new ItemStack(Items.BOOK), lapis, outputBook));

                // 2. All equipment that can receive this enchantment (1-to-1 recipes)
                for (Item item : Item.REGISTRY) {
                    if (item == null) continue;
                    ItemStack st = new ItemStack(item);
                    if (st.isEmpty()) continue;
                    if (st.isItemStackDamageable() || item instanceof net.minecraft.item.ItemSword || item instanceof net.minecraft.item.ItemTool || item instanceof net.minecraft.item.ItemArmor || item instanceof net.minecraft.item.ItemBow) {
                        if (e.canApply(st) || e.canApplyAtEnchantingTable(st) || (e.type != null && e.type.canEnchantItem(item))) {
                            ItemStack out = st.copy();
                            out.addEnchantment(e, lvl);
                            recipes.add(new EnchantingJEIRecipe(st, lapis, out));
                        }
                    }
                }
            }
        }

        return recipes;
    }
}
