package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.network.RecipeMessage;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MagicRecipeTransferHandler<C extends Container & com.brilliafy.magicstorage.gui.IStorageContainer> implements IRecipeTransferHandler<C> {

    private final Class<C> clazz;
    private final String categoryUid;

    public static final List<ItemStack> COMMON_FUELS = new ArrayList<>();
    static {
        COMMON_FUELS.add(new ItemStack(Items.COAL, 1, 0)); // Coal
        COMMON_FUELS.add(new ItemStack(Items.COAL, 1, 1)); // Charcoal
        COMMON_FUELS.add(new ItemStack(Blocks.COAL_BLOCK));
        COMMON_FUELS.add(new ItemStack(Items.BLAZE_ROD));
        COMMON_FUELS.add(new ItemStack(Items.LAVA_BUCKET));
        COMMON_FUELS.add(new ItemStack(Blocks.PLANKS, 1, OreDictionary.WILDCARD_VALUE));
        COMMON_FUELS.add(new ItemStack(Blocks.LOG, 1, OreDictionary.WILDCARD_VALUE));
        COMMON_FUELS.add(new ItemStack(Blocks.LOG2, 1, OreDictionary.WILDCARD_VALUE));
        COMMON_FUELS.add(new ItemStack(Items.STICK));
    }

    public MagicRecipeTransferHandler(Class<C> clazz, String categoryUid) {
        this.clazz = clazz;
        this.categoryUid = categoryUid;
    }

    @Override
    public Class<C> getContainerClass() {
        return clazz;
    }

    @Override
    public IRecipeTransferError transferRecipe(Container container, IRecipeLayout recipeLayout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            NBTTagCompound nbt = recipeToTag(container, recipeLayout, categoryUid, maxTransfer);
            com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendToServer(new RecipeMessage(nbt, maxTransfer));
        }
        return null;
    }

    private static void putCandidates(NBTTagCompound nbt, int slotIndex, List<ItemStack> candidates) {
        if (candidates == null || candidates.isEmpty()) return;
        NBTTagList invList = nbt.hasKey("s" + slotIndex) ? nbt.getTagList("s" + slotIndex, 10) : new NBTTagList();
        int max = (slotIndex == 7 || slotIndex == 4) ? Math.min(candidates.size(), 300) : Math.min(candidates.size(), 30);
        for (int i = 0; i < max; i++) {
            ItemStack stack = candidates.get(i);
            if (!stack.isEmpty()) {
                NBTTagCompound tag = new NBTTagCompound();
                stack.writeToNBT(tag);
                invList.appendTag(tag);
            }
        }
        nbt.setTag("s" + slotIndex, invList);
    }

    private static void putStack(NBTTagCompound nbt, int slotIndex, ItemStack stack) {
        if (stack.isEmpty()) return;
        NBTTagList invList = nbt.hasKey("s" + slotIndex) ? nbt.getTagList("s" + slotIndex, 10) : new NBTTagList();
        NBTTagCompound tag = new NBTTagCompound();
        stack.writeToNBT(tag);
        invList.appendTag(tag);
        nbt.setTag("s" + slotIndex, invList);
    }

    private static List<ItemStack> prioritizeFocused(List<ItemStack> candidates, ItemStack focused) {
        if (candidates == null) candidates = new ArrayList<>();
        if (focused == null || focused.isEmpty()) return candidates;

        List<ItemStack> result = new ArrayList<>();
        boolean matches = false;
        for (ItemStack cand : candidates) {
            if (cand.getItem() == focused.getItem()) {
                matches = true;
                break;
            }
        }
        if (matches || candidates.isEmpty()) {
            result.add(focused.copy());
        }
        for (ItemStack cand : candidates) {
            if (!ItemStack.areItemStacksEqual(cand, focused)) {
                result.add(cand);
            }
        }
        return result;
    }

    private static NBTTagCompound recipeToTag(Container container, IRecipeLayout recipeLayout, String categoryUid, boolean maxTransfer) {
        NBTTagCompound nbt = new NBTTagCompound();
        Map<Integer, ? extends IGuiIngredient<ItemStack>> inputs = recipeLayout.getItemStacks().getGuiIngredients();

        ItemStack focusedStack = ItemStack.EMPTY;
        if (recipeLayout != null && recipeLayout.getFocus() != null && recipeLayout.getFocus().getValue() instanceof ItemStack) {
            focusedStack = ((ItemStack) recipeLayout.getFocus().getValue()).copy();
        }

        if (VanillaRecipeCategoryUid.CRAFTING.equals(categoryUid)) {
            for (Slot slot : container.inventorySlots) {
                if (slot.inventory instanceof InventoryCrafting) {
                    IGuiIngredient<ItemStack> ingredient = inputs.get(slot.getSlotIndex() + 1);
                    if (ingredient == null) continue;
                    List<ItemStack> possibleItems = ingredient.getAllIngredients();
                    putCandidates(nbt, slot.getSlotIndex(), prioritizeFocused(possibleItems, focusedStack));
                }
            }
            return nbt;
        }

        if (VanillaRecipeCategoryUid.SMELTING.equals(categoryUid) || SmeltingRecipeCategory.UID.equals(categoryUid)) {
            nbt.setInteger("fuelSlot", 4);
            nbt.setInteger("reqBurnTicks", 200);
            nbt.setBoolean("isSmelting", true);

            // Fuel in center slot 4 (take any fuel item!)
            List<ItemStack> fuelCandidates = new ArrayList<>();
            IGuiIngredient<ItemStack> fuelIng = inputs.get(1);
            if (fuelIng != null && fuelIng.getAllIngredients() != null && !fuelIng.getAllIngredients().isEmpty()) {
                fuelCandidates.addAll(fuelIng.getAllIngredients());
            }
            fuelCandidates.addAll(COMMON_FUELS);
            putCandidates(nbt, 4, fuelCandidates);

            // Smeltable input item
            IGuiIngredient<ItemStack> inputIng = inputs.get(0);
            if (inputIng != null && inputIng.getAllIngredients() != null && !inputIng.getAllIngredients().isEmpty()) {
                List<ItemStack> inputCandidates = prioritizeFocused(inputIng.getAllIngredients(), focusedStack);
                if (maxTransfer) {
                    // Put in all 8 surrounding slots: 0, 1, 2, 3, 5, 6, 7, 8
                    int[] surrounding = {0, 1, 2, 3, 5, 6, 7, 8};
                    for (int s : surrounding) {
                        putCandidates(nbt, s, inputCandidates);
                    }
                } else {
                    putCandidates(nbt, 0, inputCandidates);
                }
            }
            return nbt;
        }

        if (VanillaRecipeCategoryUid.BREWING.equals(categoryUid) || BrewingRecipeCategory.UID.equals(categoryUid)) {
            // Blaze Powder in slot 0
            putStack(nbt, 0, new ItemStack(Items.BLAZE_POWDER));

            List<ItemStack> bottleCandidates = null;
            List<ItemStack> ingredientCandidates = null;

            if (BrewingRecipeCategory.UID.equals(categoryUid)) {
                if (inputs.get(1) != null) bottleCandidates = inputs.get(1).getAllIngredients();
                if (inputs.get(2) != null) ingredientCandidates = inputs.get(2).getAllIngredients();
            } else {
                // Vanilla JEI Brewing category: slot 3 is ingredient, slots 0, 1, 2 are potion bottles
                if (inputs.get(3) != null) {
                    ingredientCandidates = inputs.get(3).getAllIngredients();
                } else if (inputs.get(1) != null) {
                    ingredientCandidates = inputs.get(1).getAllIngredients();
                }
                if (inputs.get(0) != null) {
                    bottleCandidates = inputs.get(0).getAllIngredients();
                } else if (inputs.get(1) != null && inputs.get(3) != null) {
                    bottleCandidates = inputs.get(1).getAllIngredients();
                }
            }

            if (ingredientCandidates != null && !ingredientCandidates.isEmpty()) {
                putCandidates(nbt, 1, prioritizeFocused(ingredientCandidates, focusedStack));
            }
            if (bottleCandidates != null && !bottleCandidates.isEmpty()) {
                List<ItemStack> prioritizedBottles = prioritizeFocused(bottleCandidates, focusedStack);
                putCandidates(nbt, 3, prioritizedBottles);
                putCandidates(nbt, 4, prioritizedBottles);
                putCandidates(nbt, 5, prioritizedBottles);
            }
            return nbt;
        }

        if (VanillaRecipeCategoryUid.ANVIL.equals(categoryUid) || AnvilRecipeCategory.UID.equals(categoryUid)) {
            IGuiIngredient<ItemStack> leftIng = inputs.get(0);
            IGuiIngredient<ItemStack> rightIng = inputs.get(1);

            if (leftIng != null && leftIng.getAllIngredients() != null) {
                putCandidates(nbt, 0, prioritizeFocused(leftIng.getAllIngredients(), focusedStack));
            }
            if (rightIng != null && rightIng.getAllIngredients() != null) {
                putCandidates(nbt, 4, prioritizeFocused(rightIng.getAllIngredients(), focusedStack));
            }
            return nbt;
        }

        if (EnchantingRecipeCategory.UID.equals(categoryUid)) {
            IGuiIngredient<ItemStack> itemIng = inputs.get(0);
            if (itemIng != null && itemIng.getAllIngredients() != null) {
                putCandidates(nbt, 0, prioritizeFocused(itemIng.getAllIngredients(), focusedStack));
            }
            ItemStack lapis = new ItemStack(Items.DYE, 1, 4);
            putStack(nbt, 3, lapis);
            putStack(nbt, 4, lapis);
            putStack(nbt, 5, lapis);
            return nbt;
        }

        if ("rustic.alchemy_simple".equals(categoryUid) || "rustic.alchemy_advanced".equals(categoryUid) ||
            RusticSimpleAlchemyRecipeCategory.UID.equals(categoryUid) || RusticAdvancedAlchemyRecipeCategory.UID.equals(categoryUid)) {
            boolean isAdvanced = "rustic.alchemy_advanced".equals(categoryUid) || RusticAdvancedAlchemyRecipeCategory.UID.equals(categoryUid);
            int reqTicks = isAdvanced ? 300 : 400;

            nbt.setInteger("fuelSlot", 7);
            nbt.setInteger("reqBurnTicks", reqTicks);

            putStack(nbt, 0, new ItemStack(Items.GLASS_BOTTLE));
            putCandidates(nbt, 7, getSortedFuelCandidates(reqTicks));
            putStack(nbt, 8, new ItemStack(Items.WATER_BUCKET));

            int[] alchemySlots = {1, 2, 3, 6};
            int slotIdx = 0;
            for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : inputs.entrySet()) {
                IGuiIngredient<ItemStack> ing = entry.getValue();
                if (ing.isInput() && ing.getAllIngredients() != null && !ing.getAllIngredients().isEmpty()) {
                    ItemStack sample = ing.getAllIngredients().get(0);
                    if (sample.getItem() != Items.GLASS_BOTTLE && sample.getItem() != Items.WATER_BUCKET && net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(sample) <= 0) {
                        if (slotIdx < alchemySlots.length) {
                            putCandidates(nbt, alchemySlots[slotIdx], prioritizeFocused(ing.getAllIngredients(), focusedStack));
                            slotIdx++;
                        }
                    }
                }
            }
            return nbt;
        }

        if ("rustic.brewing".equals(categoryUid) || RusticBrewingRecipeCategory.UID.equals(categoryUid)) {
            putStack(nbt, 7, new ItemStack(Items.BLAZE_POWDER));
            if (RusticBrewingRecipeCategory.UID.equals(categoryUid)) {
                if (inputs.size() >= 5) {
                    // 5-slot layout: Slot 0 = modifier booze, Slot 1 = input juice
                    IGuiIngredient<ItemStack> modIng = inputs.get(0);
                    IGuiIngredient<ItemStack> juiceIng = inputs.get(1);
                    if (modIng != null && modIng.getAllIngredients() != null) {
                        putCandidates(nbt, 3, prioritizeFocused(modIng.getAllIngredients(), focusedStack));
                    }
                    if (juiceIng != null && juiceIng.getAllIngredients() != null) {
                        putCandidates(nbt, 4, prioritizeFocused(juiceIng.getAllIngredients(), focusedStack));
                    }
                } else {
                    // 4-slot layout: Slot 0 = input juice
                    IGuiIngredient<ItemStack> juiceIng = inputs.get(0);
                    if (juiceIng != null && juiceIng.getAllIngredients() != null) {
                        putCandidates(nbt, 4, prioritizeFocused(juiceIng.getAllIngredients(), focusedStack));
                    }
                }
            } else {
                for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : inputs.entrySet()) {
                    if (entry.getValue().isInput() && entry.getValue().getAllIngredients() != null && !entry.getValue().getAllIngredients().isEmpty()) {
                        putCandidates(nbt, 4, prioritizeFocused(entry.getValue().getAllIngredients(), focusedStack));
                        break;
                    }
                }
                if (recipeLayout.getFluidStacks() != null) {
                    Map<Integer, ? extends IGuiIngredient<net.minecraftforge.fluids.FluidStack>> fluids = recipeLayout.getFluidStacks().getGuiIngredients();
                    if (fluids.get(0) != null && fluids.get(0).getDisplayedIngredient() != null) {
                        net.minecraftforge.fluids.FluidStack fluidIn = fluids.get(0).getDisplayedIngredient();
                        if (fluidIn != null && fluidIn.getFluid() != null && Loader.isModLoaded("rustic")) {
                            try {
                                ItemStack bottle = ((rustic.common.items.ItemFluidBottle) rustic.common.items.ModItems.FLUID_BOTTLE).getFilledBottle(fluidIn.getFluid());
                                putStack(nbt, 4, !focusedStack.isEmpty() && focusedStack.getItem() == bottle.getItem() ? focusedStack : bottle);
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }
            return nbt;
        }

        if ("rustic.crushing_tub".equals(categoryUid) || RusticCrushingRecipeCategory.UID.equals(categoryUid)) {
            IGuiIngredient<ItemStack> crushIng = inputs.get(0);
            if (crushIng != null && crushIng.getAllIngredients() != null) {
                List<ItemStack> list4 = new ArrayList<>();
                for (ItemStack ing : crushIng.getAllIngredients()) {
                    if (ing != null && !ing.isEmpty()) {
                        ItemStack copy = ing.copy();
                        copy.setCount(4);
                        list4.add(copy);
                    }
                }
                putCandidates(nbt, 0, prioritizeFocused(list4, focusedStack));
            }
            IGuiIngredient<ItemStack> bottleIng = inputs.size() > 1 ? inputs.get(1) : null;
            if (bottleIng != null && bottleIng.getDisplayedIngredient() != null) {
                putStack(nbt, 4, bottleIng.getDisplayedIngredient());
            } else {
                putStack(nbt, 4, new ItemStack(Items.GLASS_BOTTLE));
            }
            return nbt;
        }

        if (DisenchanterRecipeCategory.UID.equals(categoryUid)) {
            IGuiIngredient<ItemStack> itemIng = inputs.get(0);
            if (itemIng != null && itemIng.getAllIngredients() != null) {
                putCandidates(nbt, 4, prioritizeFocused(itemIng.getAllIngredients(), focusedStack));
            }
            putStack(nbt, 2, new ItemStack(Items.BOOK));
            return nbt;
        }

        if (BountifulBaublesRecipeCategory.UID.equals(categoryUid)) {
            IGuiIngredient<ItemStack> baubleIng = inputs.get(0);
            if (baubleIng != null && baubleIng.getAllIngredients() != null) {
                putCandidates(nbt, 8, prioritizeFocused(baubleIng.getAllIngredients(), focusedStack));
            }
            return nbt;
        }

        if (QualityToolsRecipeCategory.UID.equals(categoryUid)) {
            IGuiIngredient<ItemStack> toolIng = inputs.get(0);
            IGuiIngredient<ItemStack> matIng = inputs.get(1);
            if (toolIng != null && toolIng.getAllIngredients() != null) {
                putCandidates(nbt, 4, prioritizeFocused(toolIng.getAllIngredients(), focusedStack));
            }
            if (matIng != null && matIng.getAllIngredients() != null) {
                putCandidates(nbt, 8, prioritizeFocused(matIng.getAllIngredients(), focusedStack));
            }
            return nbt;
        }

        return nbt;
    }

    public static List<ItemStack> getSortedFuelCandidates(int reqTicks) {
        List<ItemStack> allCandidateFuels = new ArrayList<>();
        for (Item item : Item.REGISTRY) {
            if (item == null) continue;
            net.minecraft.util.NonNullList<ItemStack> subItems = net.minecraft.util.NonNullList.create();
            try {
                item.getSubItems(net.minecraft.creativetab.CreativeTabs.SEARCH, subItems);
            } catch (Throwable t) {
                try {
                    item.getSubItems(item.getCreativeTab(), subItems);
                } catch (Throwable t2) {
                    subItems.add(new ItemStack(item));
                }
            }
            if (subItems.isEmpty()) {
                subItems.add(new ItemStack(item));
            }
            for (ItemStack sub : subItems) {
                if (sub.isEmpty()) continue;
                int bt = net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(sub);
                if (bt > 0) {
                    allCandidateFuels.add(sub);
                }
            }
        }

        List<ItemStack> fuels = new ArrayList<>();
        // Priority 1: Coal (meta 0)
        fuels.add(new ItemStack(Items.COAL, 1, 0));
        // Priority 2: Charcoal (meta 1)
        fuels.add(new ItemStack(Items.COAL, 1, 1));

        // Priority 3: All other fuels sorted descending by burn time
        List<ItemStack> otherFuels = new ArrayList<>();
        for (ItemStack f : allCandidateFuels) {
            if (f.getItem() == Items.COAL && (f.getMetadata() == 0 || f.getMetadata() == 1)) continue;
            otherFuels.add(f);
        }
        otherFuels.sort((a, b) -> {
            int btA = net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(a);
            int btB = net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(b);
            return Integer.compare(btB, btA);
        });
        fuels.addAll(otherFuels);

        // Adjust stack count for minimum required fuel ticks
        for (ItemStack f : fuels) {
            int bt = net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(f);
            if (bt > 0) {
                int count = (int) Math.ceil((double) reqTicks / (double) bt);
                f.setCount(Math.max(1, count));
            }
        }
        return fuels;
    }
}
