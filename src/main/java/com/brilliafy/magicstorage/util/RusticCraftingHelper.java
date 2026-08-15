package com.brilliafy.magicstorage.util;

import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.common.Loader;
import rustic.common.blocks.fluids.FluidBooze;
import rustic.common.blocks.fluids.ModFluids;
import rustic.common.crafting.*;
import rustic.common.items.ItemFluidBottle;
import rustic.common.items.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RusticCraftingHelper {

    private static final Random rand = new Random();

    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(stack) > 0;
    }

    public static boolean isWaterBucket(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET;
    }

    public static String getFluidName(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().hasKey("Fluid", 10)) {
            return stack.getTagCompound().getCompoundTag("Fluid").getString("FluidName");
        }
        return "";
    }

    public static ItemStack getFilledBottle(net.minecraftforge.fluids.Fluid fluid) {
        if (fluid == null) return ItemStack.EMPTY;
        if (fluid == FluidRegistry.WATER) {
            return net.minecraft.potion.PotionUtils.addPotionToItemStack(
                new ItemStack(Items.POTIONITEM), net.minecraft.init.PotionTypes.WATER);
        }
        net.minecraft.item.Item fluidBottleItem = net.minecraft.item.Item.getByNameOrId("rustic:fluid_bottle");
        if (fluidBottleItem == null) return ItemStack.EMPTY;

        ItemStack bottle = new ItemStack(fluidBottleItem);
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound fluidTag = new NBTTagCompound();
        fluidTag.setString("FluidName", fluid.getName());
        fluidTag.setInteger("Amount", 1000);
        if (fluid instanceof FluidBooze) {
            NBTTagCompound boozeTag = new NBTTagCompound();
            boozeTag.setFloat(FluidBooze.QUALITY_NBT_KEY, 0.75f);
            fluidTag.setTag("Tag", boozeTag);
        }
        tag.setTag("Fluid", fluidTag);
        bottle.setTagCompound(tag);
        return bottle;
    }

    private static void consumeFuel(ItemStack[] m, int reqTicks) {
        ItemStack fuel = m[7];
        if (fuel.isEmpty()) return;
        int singleBurnTime = net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(fuel);
        if (singleBurnTime <= 0) return;

        double fuelsNeeded = (double) reqTicks / (double) singleBurnTime;
        int wholeFuels = (int) Math.floor(fuelsNeeded);
        double chance = fuelsNeeded - wholeFuels;
        if (MagicStorageRandom.rollChance(chance)) {
            wholeFuels++;
        }
        if (wholeFuels > 0) {
            net.minecraft.item.Item item = fuel.getItem();
            ItemStack container = item.getContainerItem(fuel);
            fuel.shrink(Math.min(wholeFuels, fuel.getCount()));
            if (fuel.isEmpty() && !container.isEmpty()) {
                m[7] = container;
            }
        }
    }

    // Parsed alchemy grid containing bottle and ingredient slots
    private static class AlchemyGrid {
        int bottleSlot = -1;
        ItemStack bottleStack = ItemStack.EMPTY;
        List<Integer> ingredientSlots = new ArrayList<>();
        List<ItemStack> ingredientStacks = new ArrayList<>();
        boolean valid = false;
    }

    /**
     * Parses the 3x3 grid for alchemy recipes:
     * Slot 8 (m[7]) = Fuel (any furnace fuel with burn time > 0)
     * Slot 9 (m[8]) = Water Bucket
     * Slots 5 & 6 (m[4], m[5]) = Must be empty
     * Slots 1, 2, 3, 4, 7 (m[0], m[1], m[2], m[3], m[6]) = Contains 1 Glass Bottle + materials
     */
    private static AlchemyGrid parseAlchemyGrid(ItemStack[] m, int reqTicks) {
        AlchemyGrid grid = new AlchemyGrid();
        if (!m[4].isEmpty() || !m[5].isEmpty()) return grid; // slots 5, 6 must be empty
        if (m[7].isEmpty()) return grid;
        int singleBurn = net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(m[7]);
        if (singleBurn <= 0 || singleBurn * m[7].getCount() < reqTicks) return grid; // Must satisfy minimum total fuel requirement!
        if (!isWaterBucket(m[8])) return grid; // slot 9 must be water bucket

        int[] alchemySlots = {0, 1, 2, 3, 6};
        for (int s : alchemySlots) {
            ItemStack stack = m[s];
            if (!stack.isEmpty()) {
                if (stack.getItem() == Items.GLASS_BOTTLE && grid.bottleSlot == -1) {
                    grid.bottleSlot = s;
                    grid.bottleStack = stack;
                } else {
                    grid.ingredientSlots.add(s);
                    grid.ingredientStacks.add(stack);
                }
            }
        }
        if (grid.bottleSlot != -1 && !grid.ingredientStacks.isEmpty()) {
            grid.valid = true;
        }
        return grid;
    }

    // ==========================================
    // 1. SIMPLE ALCHEMIC CONDENSER
    // ==========================================

    public static boolean canCraftSimpleCondenser(ItemStack[] m) {
        if (!Loader.isModLoaded("rustic")) return false;
        return !computeSimpleCondenserResult(m).isEmpty();
    }

    public static ItemStack computeSimpleCondenserResult(ItemStack[] m) {
        if (!Loader.isModLoaded("rustic")) return ItemStack.EMPTY;
        AlchemyGrid grid = parseAlchemyGrid(m, 400);
        if (!grid.valid) return ItemStack.EMPTY;

        ItemStack[] inArray = grid.ingredientStacks.toArray(new ItemStack[0]);

        for (ICondenserRecipe recipe : Recipes.condenserRecipes) {
            if (recipe.isBasic() && recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, grid.bottleStack, inArray)) {
                return recipe.getResult().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    public static void consumeSimpleCondenserIngredients(ItemStack[] m, Random rng) {
        AlchemyGrid grid = parseAlchemyGrid(m, 400);
        if (!grid.valid) return;

        if (grid.bottleSlot != -1 && !m[grid.bottleSlot].isEmpty()) {
            m[grid.bottleSlot].shrink(1);
        }
        for (int slot : grid.ingredientSlots) {
            if (!m[slot].isEmpty()) m[slot].shrink(1);
        }

        // 12.5% chance for water bucket to become empty bucket
        if (MagicStorageRandom.rollChance(0.125)) {
            m[8] = new ItemStack(Items.BUCKET);
        }
        // Fuel consumption: Basic condenser takes 400 burn ticks
        consumeFuel(m, 400);
    }

    // ==========================================
    // 2. ADVANCED ALCHEMIC CONDENSER
    // ==========================================

    public static boolean canCraftAdvancedCondenser(ItemStack[] m) {
        if (!Loader.isModLoaded("rustic")) return false;
        return !computeAdvancedCondenserResult(m).isEmpty();
    }

    public static ItemStack computeAdvancedCondenserResult(ItemStack[] m) {
        if (!Loader.isModLoaded("rustic")) return ItemStack.EMPTY;
        AlchemyGrid grid = parseAlchemyGrid(m, 300);
        if (!grid.valid) return ItemStack.EMPTY;

        List<ItemStack> ingredients = grid.ingredientStacks;

        // Try matching with modifier = EMPTY (some advanced recipes use empty modifier)
        ItemStack[] inArray = ingredients.toArray(new ItemStack[0]);
        for (ICondenserRecipe recipe : Recipes.condenserRecipes) {
            if (recipe.isAdvanced() && recipe.matches(FluidRegistry.WATER, ItemStack.EMPTY, grid.bottleStack, inArray)) {
                return recipe.getResult().copy();
            }
        }

        // Try each ingredient stack as potential modifier
        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack modifier = ingredients.get(i);
            List<ItemStack> otherInputs = new ArrayList<>(ingredients);
            otherInputs.remove(i);
            ItemStack[] subArray = otherInputs.toArray(new ItemStack[0]);

            for (ICondenserRecipe recipe : Recipes.condenserRecipes) {
                if (recipe.isAdvanced() && recipe.matches(FluidRegistry.WATER, modifier, grid.bottleStack, subArray)) {
                    return recipe.getResult().copy();
                }
            }
        }

        return ItemStack.EMPTY;
    }

    public static void consumeAdvancedCondenserIngredients(ItemStack[] m, Random rng) {
        AlchemyGrid grid = parseAlchemyGrid(m, 300);
        if (!grid.valid) return;

        if (grid.bottleSlot != -1 && !m[grid.bottleSlot].isEmpty()) {
            m[grid.bottleSlot].shrink(1);
        }
        for (int slot : grid.ingredientSlots) {
            if (!m[slot].isEmpty()) m[slot].shrink(1);
        }

        // 12.5% chance for water bucket to become empty bucket
        if (MagicStorageRandom.rollChance(0.125)) {
            m[8] = new ItemStack(Items.BUCKET);
        }
        // Fuel consumption: Advanced condenser takes 300 burn ticks
        consumeFuel(m, 300);
    }

    // ==========================================
    // 3. BREWING (BREWING BARREL)
    // Slots (0-indexed):
    // m[3] = Template bottle (Slot 4, optional)
    // m[4] = Juice / bottle of whatever (Slot 5)
    // m[7] = Blaze Powder (Slot 8)
    // m[0], m[1], m[2], m[5], m[6], m[8] must be empty (Slots 1, 2, 3, 6, 7, 9)
    // ==========================================

    public static boolean canCraftBrewing(ItemStack[] m, TileStorageHeart heart) {
        if (!Loader.isModLoaded("rustic")) return false;
        if (!m[0].isEmpty() || !m[1].isEmpty() || !m[2].isEmpty() || !m[5].isEmpty() || !m[6].isEmpty() || !m[8].isEmpty()) return false;
        if (m[7].isEmpty() || m[7].getItem() != Items.BLAZE_POWDER) return false;
        if (m[4].isEmpty()) return false;

        return !computeBrewingResult(m, heart).isEmpty();
    }

    public static ItemStack computeBrewingResult(ItemStack[] m, TileStorageHeart heart) {
        if (!Loader.isModLoaded("rustic")) return ItemStack.EMPTY;
        FluidStack inputFluid = FluidUtil.getFluidContained(m[4]);
        if (inputFluid == null || inputFluid.getFluid() == null) return ItemStack.EMPTY;

        IBrewingBarrelRecipe matchedRecipe = null;
        for (IBrewingBarrelRecipe recipe : Recipes.brewingRecipes) {
            if (recipe.matches(inputFluid)) {
                matchedRecipe = recipe;
                break;
            }
        }
        if (matchedRecipe == null) return ItemStack.EMPTY;

        FluidStack templateFluid = !m[3].isEmpty() ? FluidUtil.getFluidContained(m[3]) : null;
        if (templateFluid != null && (templateFluid.tag == null || !templateFluid.tag.hasKey(FluidBooze.QUALITY_NBT_KEY))) {
            if (m[3].hasTagCompound() && m[3].getTagCompound().hasKey("Fluid")) {
                NBTTagCompound fTag = m[3].getTagCompound().getCompoundTag("Fluid");
                if (fTag.hasKey("Tag") && fTag.getCompoundTag("Tag").hasKey(FluidBooze.QUALITY_NBT_KEY)) {
                    if (templateFluid.tag == null) templateFluid.tag = new NBTTagCompound();
                    templateFluid.tag.setFloat(FluidBooze.QUALITY_NBT_KEY, fTag.getCompoundTag("Tag").getFloat(FluidBooze.QUALITY_NBT_KEY));
                }
            }
        }

        FluidStack outFluid = matchedRecipe.getResult(inputFluid, templateFluid);
        if (outFluid == null || outFluid.getFluid() == null) {
            outFluid = matchedRecipe.getResult(inputFluid);
        }
        if (outFluid == null || outFluid.getFluid() == null) return ItemStack.EMPTY;

        ItemStack resBottle = getFilledBottle(outFluid.getFluid());
        if (outFluid.tag != null && outFluid.tag.hasKey(FluidBooze.QUALITY_NBT_KEY)) {
            float q = outFluid.tag.getFloat(FluidBooze.QUALITY_NBT_KEY);
            NBTTagCompound tag = resBottle.getTagCompound();
            if (tag != null && tag.hasKey("Fluid")) {
                NBTTagCompound fluidTag = tag.getCompoundTag("Fluid");
                if (!fluidTag.hasKey("Tag")) fluidTag.setTag("Tag", new NBTTagCompound());
                fluidTag.getCompoundTag("Tag").setFloat(FluidBooze.QUALITY_NBT_KEY, q);
            }
        }

        // Crafting N bottles in one go matches the input juice bottle count
        int craftAmount = m[4].getCount();
        resBottle.setCount(craftAmount);

        return resBottle;
    }

    public static void consumeBrewingIngredients(ItemStack[] m, Random rng, TileStorageHeart heart) {
        int count = 1;
        if (!m[4].isEmpty()) {
            count = m[4].getCount();
            m[4].shrink(count); // Craft all input juice bottles in one go
        }
        // m[3] (template) is NOT consumed
        // Cumulative blaze powder consumption: 6.25% per bottle (1/16 per bottle)
        if (!m[7].isEmpty()) {
            double chance = count * 0.0625;
            int toConsume = (int) chance;
            if (MagicStorageRandom.rollChance(chance - toConsume)) {
                toConsume++;
            }
            if (toConsume > 0) {
                m[7].shrink(Math.min(toConsume, m[7].getCount()));
            }
        }
        // Increment craft counter on heart by 1 for this entire batch craft
        if (heart != null) {
            heart.incrementBrewingCraftCounter();
        }
    }

    // ==========================================
    // 4. CRUSHING (CRUSHING TUB)
    // Slots (0-indexed):
    // m[0] = crushable ingredient (Slot 1, count >= required amount)
    // m[4] = 1 Glass bottle (Slot 5)
    // m[1], m[2], m[3], m[5], m[6], m[7], m[8] must be empty (Slots 2, 3, 4, 6, 7, 8, 9)
    // ==========================================

    public static boolean isCrushable(ItemStack stack) {
        if (!Loader.isModLoaded("rustic") || stack == null || stack.isEmpty()) return false;
        for (ICrushingTubRecipe recipe : Recipes.crushingTubRecipes) {
            if (recipe.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    public static int getRequiredCrushingAmount(ItemStack stack) {
        if (!Loader.isModLoaded("rustic") || stack == null || stack.isEmpty()) return 4;
        for (ICrushingTubRecipe recipe : Recipes.crushingTubRecipes) {
            if (recipe.matches(stack)) {
                FluidStack fluidOut = recipe.getResult();
                if (fluidOut != null && fluidOut.amount > 0) {
                    return (int) Math.ceil(1000.0 / fluidOut.amount);
                }
            }
        }
        return 4;
    }

    public static boolean canCraftCrushing(ItemStack[] m) {
        if (!Loader.isModLoaded("rustic")) return false;
        if (!m[1].isEmpty() || !m[2].isEmpty() || !m[3].isEmpty() || !m[5].isEmpty() || !m[6].isEmpty() || !m[7].isEmpty() || !m[8].isEmpty()) return false;
        if (m[0].isEmpty()) return false;
        int req = getRequiredCrushingAmount(m[0]);
        if (m[0].getCount() < req) return false;
        if (m[4].isEmpty() || m[4].getItem() != Items.GLASS_BOTTLE) return false;

        return !computeCrushingResult(m).isEmpty();
    }

    public static ItemStack computeCrushingResult(ItemStack[] m) {
        if (!Loader.isModLoaded("rustic") || m == null || m.length < 5 || m[0].isEmpty()) return ItemStack.EMPTY;
        for (ICrushingTubRecipe recipe : Recipes.crushingTubRecipes) {
            if (recipe.matches(m[0])) {
                FluidStack fluidOut = recipe.getResult();
                if (fluidOut != null && fluidOut.getFluid() != null) {
                    return getFilledBottle(fluidOut.getFluid());
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static void consumeCrushingIngredients(ItemStack[] m) {
        if (m != null && m.length > 4) {
            if (!m[0].isEmpty()) {
                int req = getRequiredCrushingAmount(m[0]);
                m[0].shrink(req);
            }
            if (!m[4].isEmpty()) {
                m[4].shrink(1);
            }
        }
    }
}
