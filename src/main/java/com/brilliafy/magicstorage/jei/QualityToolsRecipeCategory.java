package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.reference.ModBlocksRef;
import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.util.QualityToolsCraftingHelper;
import com.tmtravlr.qualitytools.config.ConfigLoader;
import com.tmtravlr.qualitytools.config.CustomMaterial;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QualityToolsRecipeCategory implements IRecipeCategory<QualityToolsRecipeCategory.QualityToolsJEIRecipe> {

    public static final String UID = ModInfo.MOD_ID + ".quality_reforge";
    private final IDrawable background;
    private final IDrawable icon;

    public QualityToolsRecipeCategory(IGuiHelper helper) {
        background = helper.createBlankDrawable(160, 50);
        icon = helper.createDrawableIngredient(new ItemStack(ModBlocksRef.CRAFTING_ACCESS));
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return "Magic Storage Tool Reforging"; }
    @Override public String getModName() { return ModInfo.MOD_NAME; }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayout layout, QualityToolsJEIRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup gui = layout.getItemStacks();
        gui.init(0, true, 10, 15);   // tool
        gui.init(1, true, 45, 15);   // material
        gui.init(2, false, 80, 15);  // required station: reforging station
        gui.init(3, false, 125, 15); // output reforged tool

        gui.set(0, ingredients.getInputs(ItemStack.class).get(0));
        gui.set(1, ingredients.getInputs(ItemStack.class).get(1));

        Block reforgerBlock = Block.getBlockFromName("qualitytools:reforging_station");
        ItemStack stationStack = (reforgerBlock != null) ? new ItemStack(reforgerBlock) : new ItemStack(net.minecraft.init.Blocks.ANVIL);
        gui.set(2, stationStack);
        gui.set(3, ingredients.getOutputs(ItemStack.class).get(0));

        gui.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (slotIndex == 2) {
                tooltip.add(TextFormatting.GOLD + "Requires Reforging Station in Storage Heart");
                tooltip.add(TextFormatting.GRAY + "Place a Reforging Station inside the Storage Heart to craft.");
            }
        });
    }

    public static class QualityToolsJEIRecipe implements IRecipeWrapper {
        private final ItemStack tool;
        private final ItemStack material;
        private final ItemStack output;

        public QualityToolsJEIRecipe(ItemStack tool, ItemStack material, ItemStack output) {
            this.tool = tool;
            this.material = material;
            this.output = output;
        }

        public ItemStack getTool() {
            return tool;
        }

        public ItemStack getMaterial() {
            return material;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inputs = new ArrayList<>();
            inputs.add(Collections.singletonList(tool));
            inputs.add(Collections.singletonList(material));
            ingredients.setInputLists(ItemStack.class, inputs);
            ingredients.setOutput(ItemStack.class, output);
        }
    }

    public static List<QualityToolsJEIRecipe> generateAllRecipes() {
        List<QualityToolsJEIRecipe> recipes = new ArrayList<>();
        if (!Loader.isModLoaded("qualitytools")) return recipes;

        QualityToolsCraftingHelper.ensureQualityToolsConfigLoaded();

        List<ItemStack> candidateMaterials = new ArrayList<>();
        Set<String> addedMaterialKeys = new HashSet<>();

        java.util.function.Consumer<ItemStack> addCandidate = (s) -> {
            if (s != null && !s.isEmpty()) {
                String key = s.getItem().getRegistryName() + "@" + s.getMetadata();
                if (s.hasTagCompound()) key += "#" + s.getTagCompound().toString();
                if (addedMaterialKeys.add(key)) {
                    ItemStack c = s.copy();
                    c.setCount(1);
                    candidateMaterials.add(c);
                }
            }
        };

        // 1. All materials from QualityTools config
        if (ConfigLoader.customReforgeMaterials != null) {
            for (CustomMaterial mat : ConfigLoader.customReforgeMaterials.values()) {
                if (mat != null) {
                    if (mat.item != null) {
                        ItemStack s = new ItemStack(mat.item, 1, mat.meta == Short.MAX_VALUE ? 0 : mat.meta);
                        if (mat.tag != null) s.setTagCompound(mat.tag.copy());
                        addCandidate.accept(s);
                    }
                    if (mat.oreDict != null) {
                        for (ItemStack ore : OreDictionary.getOres(mat.oreDict)) {
                            addCandidate.accept(ore);
                        }
                    }
                }
            }
        }

        // 2. Standard & common repair materials
        addCandidate.accept(new ItemStack(Items.DIAMOND));
        addCandidate.accept(new ItemStack(Items.IRON_INGOT));
        addCandidate.accept(new ItemStack(Items.GOLD_INGOT));
        addCandidate.accept(new ItemStack(Items.EMERALD));
        addCandidate.accept(new ItemStack(Items.LEATHER));
        addCandidate.accept(new ItemStack(Items.STRING));
        addCandidate.accept(new ItemStack(Items.PRISMARINE_SHARD));
        addCandidate.accept(new ItemStack(Items.GLOWSTONE_DUST));
        addCandidate.accept(new ItemStack(Items.REDSTONE));
        addCandidate.accept(new ItemStack(Items.BLAZE_ROD));
        addCandidate.accept(new ItemStack(Items.QUARTZ));
        addCandidate.accept(new ItemStack(Items.ENDER_PEARL));
        addCandidate.accept(new ItemStack(net.minecraft.init.Blocks.PLANKS));
        addCandidate.accept(new ItemStack(net.minecraft.init.Blocks.COBBLESTONE));
        addCandidate.accept(new ItemStack(net.minecraft.init.Blocks.OBSIDIAN));

        // Bountiful Baubles Spectral Silt
        Item silt = Item.getByNameOrId("bountifulbaubles:spectralsilt");
        if (silt != null) addCandidate.accept(new ItemStack(silt));

        // Common OreDictionary repair materials
        for (String oreName : OreDictionary.getOreNames()) {
            if (oreName.startsWith("ingot") || oreName.startsWith("gem") || oreName.startsWith("dust") ||
                oreName.startsWith("leather") || oreName.startsWith("plank") || oreName.startsWith("stone") ||
                oreName.startsWith("cobblestone") || oreName.startsWith("scale") || oreName.startsWith("chitin") ||
                oreName.startsWith("fang") || oreName.startsWith("horn") || oreName.startsWith("bone") ||
                oreName.startsWith("material") || oreName.startsWith("nugget") || oreName.startsWith("shard") ||
                oreName.startsWith("crystal") || oreName.startsWith("cloth") || oreName.startsWith("fabric") ||
                oreName.startsWith("wool")) {
                for (ItemStack ore : OreDictionary.getOres(oreName)) {
                    addCandidate.accept(ore);
                }
            }
        }

        // Universal reforge item (Nether Star)
        ItemStack universalItem = null;
        if (ConfigLoader.universalReforgeItem != null) {
            CustomMaterial u = ConfigLoader.universalReforgeItem;
            if (u.item != null) {
                universalItem = new ItemStack(u.item, 1, u.meta == Short.MAX_VALUE ? 0 : u.meta);
                if (u.tag != null) universalItem.setTagCompound(u.tag.copy());
            } else if (u.oreDict != null) {
                List<ItemStack> ores = OreDictionary.getOres(u.oreDict);
                if (!ores.isEmpty()) universalItem = ores.get(0).copy();
            }
        }
        if (universalItem == null || universalItem.isEmpty()) {
            universalItem = new ItemStack(Items.NETHER_STAR);
        }
        addCandidate.accept(universalItem);

        // 3. Scan tools in registry and build recipes
        for (Item item : Item.REGISTRY) {
            if (item == null) continue;

            NonNullList<ItemStack> subItems = NonNullList.create();
            try {
                item.getSubItems(CreativeTabs.SEARCH, subItems);
            } catch (Throwable t) {
                subItems.add(new ItemStack(item));
            }
            if (subItems.isEmpty()) {
                subItems.add(new ItemStack(item));
            }

            for (ItemStack tool : subItems) {
                if (tool.isEmpty() || !QualityToolsCraftingHelper.isReforgeable(tool)) continue;

                List<ItemStack> specificMaterials = new ArrayList<>();
                boolean matchesUniversal = false;

                for (ItemStack mat : candidateMaterials) {
                    if (isUniversal(mat, universalItem)) {
                        if (QualityToolsCraftingHelper.canCraft(tool, mat)) {
                            matchesUniversal = true;
                        }
                    } else if (QualityToolsCraftingHelper.canCraft(tool, mat)) {
                        specificMaterials.add(mat);
                    }
                }

                // Add static recipe for each specific material (still and distinct!)
                for (ItemStack mat : specificMaterials) {
                    recipes.add(new QualityToolsJEIRecipe(tool, mat, tool.copy()));
                }

                // Add static recipe for universal material (Nether Star)
                if (matchesUniversal) {
                    recipes.add(new QualityToolsJEIRecipe(tool, universalItem, tool.copy()));
                }
            }
        }

        return recipes;
    }

    private static boolean isUniversal(ItemStack mat, ItemStack universalItem) {
        if (mat == null || mat.isEmpty()) return false;
        if (mat.getItem() == Items.NETHER_STAR) return true;
        if (universalItem != null && !universalItem.isEmpty()) {
            return mat.getItem() == universalItem.getItem() && (universalItem.getMetadata() == Short.MAX_VALUE || mat.getMetadata() == universalItem.getMetadata());
        }
        return false;
    }
}
