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
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DisenchanterRecipeCategory implements IRecipeCategory<DisenchanterRecipeCategory.DisenchanterJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".disenchanting";
    private final IDrawable background;
    private final IDrawable icon;

    public DisenchanterRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(160, 50);
        icon = helper.createDrawableIngredient(new ItemStack(ModBlocksRef.CRAFTING_ACCESS));
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return "Magic Storage Disenchanting"; }
    @Override public String getModName() { return ModInfo.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, DisenchanterJEIRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        gui.init(0, true, 10, 15);   // enchanted item / book
        gui.init(1, true, 45, 15);   // regular book
        gui.init(2, false, 80, 15);  // required station: disenchantment table
        gui.init(3, false, 125, 15); // output enchanted book

        gui.set(0, ingredients.getInputs(ItemStack.class).get(0));
        gui.set(1, ingredients.getInputs(ItemStack.class).get(1));

        Block tableBlock = Block.getBlockFromName("disenchanter:disenchantmenttable");
        ItemStack stationStack = (tableBlock != null) ? new ItemStack(tableBlock) : new ItemStack(net.minecraft.init.Blocks.ENCHANTING_TABLE);
        gui.set(2, stationStack);
        gui.set(3, ingredients.getOutputs(ItemStack.class).get(0));

        gui.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (slotIndex == 2) {
                tooltip.add(TextFormatting.GOLD + "Requires Disenchantment Table in Storage Heart");
                tooltip.add(TextFormatting.GRAY + "Place a Disenchantment Table inside the Storage Heart to craft.");
            }
        });
    }

    public static class DisenchanterJEIRecipe implements IRecipeWrapper {
        private final List<ItemStack> enchantedItems;
        private final ItemStack book;
        private final ItemStack outputBook;

        public DisenchanterJEIRecipe(List<ItemStack> enchantedItems, ItemStack book, ItemStack outputBook) {
            this.enchantedItems = enchantedItems;
            this.book = book;
            this.outputBook = outputBook;
        }

        public DisenchanterJEIRecipe(ItemStack singleItem, ItemStack book, ItemStack outputBook) {
            this(Collections.singletonList(singleItem), book, outputBook);
        }

        public ItemStack getOutputBook() {
            return outputBook;
        }

        public List<ItemStack> getEnchantedItems() {
            return enchantedItems;
        }

        public ItemStack getBook() {
            return book;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inputs = new ArrayList<>();
            inputs.add(enchantedItems);
            inputs.add(Collections.singletonList(book));
            ingredients.setInputLists(ItemStack.class, inputs);
            ingredients.setOutput(ItemStack.class, outputBook);
        }
    }

    public static boolean isCurrentTableBulk() {
        try {
            if (!Loader.isModLoaded("disenchanter")) return false;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc != null && mc.player != null && mc.player.openContainer instanceof com.brilliafy.magicstorage.container.ContainerMagicStorageBase) {
                com.brilliafy.magicstorage.tile.TileStorageHeart heart = ((com.brilliafy.magicstorage.container.ContainerMagicStorageBase) mc.player.openContainer).getTileMaster();
                if (heart != null) {
                    return heart.isDisenchanterBulk();
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static ItemStack createOutputBook(ItemStack source, boolean isBulk) {
        if (source.isEmpty()) return ItemStack.EMPTY;
        ItemStack outputBook = new ItemStack(Items.ENCHANTED_BOOK);
        java.util.Map<Enchantment, Integer> enchants = net.minecraft.enchantment.EnchantmentHelper.getEnchantments(source);
        if (enchants.isEmpty()) return ItemStack.EMPTY;

        if (isBulk) {
            for (java.util.Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                    ItemEnchantedBook.addEnchantment(outputBook, new EnchantmentData(entry.getKey(), entry.getValue()));
                }
            }
        } else {
            java.util.Map.Entry<Enchantment, Integer> first = enchants.entrySet().iterator().next();
            if (first.getKey() != null && first.getValue() != null && first.getValue() > 0) {
                ItemEnchantedBook.addEnchantment(outputBook, new EnchantmentData(first.getKey(), first.getValue()));
            }
        }
        return outputBook;
    }

    public static List<DisenchanterJEIRecipe> generateAllRecipes() {
        List<DisenchanterJEIRecipe> recipes = new ArrayList<>();
        if (!Loader.isModLoaded("disenchanter")) return recipes;

        ItemStack regularBook = new ItemStack(Items.BOOK);

        for (Enchantment e : Enchantment.REGISTRY) {
            if (e == null) continue;
            for (int lvl = 1; lvl <= e.getMaxLevel(); lvl++) {
                // Register Enchanted Book -> Book + Enchanted Book
                ItemStack inputBook = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantedBook.addEnchantment(inputBook, new EnchantmentData(e, lvl));

                ItemStack outputBook = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantedBook.addEnchantment(outputBook, new EnchantmentData(e, lvl));

                recipes.add(new DisenchanterJEIRecipe(inputBook, regularBook, outputBook));

                // Register all enchantable items (swords, tools, armors, bows, fishing rods, etc.)
                List<ItemStack> candidates = new ArrayList<>();
                for (Item item : Item.REGISTRY) {
                    if (item == null) continue;
                    ItemStack st = new ItemStack(item);
                    if (st.isEmpty()) continue;
                    if (st.isItemStackDamageable() || item instanceof net.minecraft.item.ItemSword || item instanceof net.minecraft.item.ItemTool || item instanceof net.minecraft.item.ItemArmor || item instanceof net.minecraft.item.ItemBow) {
                        if (e.canApply(st) || e.canApplyAtEnchantingTable(st) || (e.type != null && e.type.canEnchantItem(item))) {
                            ItemStack ench = st.copy();
                            ench.addEnchantment(e, lvl);
                            candidates.add(ench);
                        }
                    }
                }
                if (!candidates.isEmpty()) {
                    recipes.add(new DisenchanterJEIRecipe(candidates, regularBook, outputBook.copy()));
                }
            }
        }

        return recipes;
    }
}
