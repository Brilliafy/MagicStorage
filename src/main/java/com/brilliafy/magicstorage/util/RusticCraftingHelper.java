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

    public static boolean isCoal(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.COAL;
    }

    public static boolean isWaterBucket(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET;
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
     * Slot 8 (m[7]) = Coal
     * Slot 9 (m[8]) = Water Bucket
     * Slots 5 & 6 (m[4], m[5]) = Must be empty
     * Slots 1, 2, 3, 4, 7 (m[0], m[1], m[2], m[3], m[6]) = Contains 1 Glass Bottle + materials
     */
    private static AlchemyGrid parseAlchemyGrid(ItemStack[] m) {
        AlchemyGrid grid = new AlchemyGrid();
        if (!m[4].isEmpty() || !m[5].isEmpty()) return grid; // slots 5, 6 must be empty
        if (!isCoal(m[7])) return grid; // slot 8 must be coal
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
        AlchemyGrid grid = parseAlchemyGrid(m);
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
        AlchemyGrid grid = parseAlchemyGrid(m);
        if (!grid.valid) return;

        if (grid.bottleSlot != -1 && !m[grid.bottleSlot].isEmpty()) {
            m[grid.bottleSlot].shrink(1);
        }
        for (int slot : grid.ingredientSlots) {
            if (!m[slot].isEmpty()) m[slot].shrink(1);
        }

        Random r = (rng != null) ? rng : rand;
        // 12.5% chance for water bucket to become empty bucket
        if (r.nextFloat() < 0.125f) {
            m[8] = new ItemStack(Items.BUCKET);
        }
        // 20% chance for coal to be consumed
        if (r.nextFloat() < 0.20f && !m[7].isEmpty()) {
            m[7].shrink(1);
        }
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
        AlchemyGrid grid = parseAlchemyGrid(m);
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
        AlchemyGrid grid = parseAlchemyGrid(m);
        if (!grid.valid) return;

        if (grid.bottleSlot != -1 && !m[grid.bottleSlot].isEmpty()) {
            m[grid.bottleSlot].shrink(1);
        }
        for (int slot : grid.ingredientSlots) {
            if (!m[slot].isEmpty()) m[slot].shrink(1);
        }

        Random r = (rng != null) ? rng : rand;
        // 12.5% chance for water bucket to become empty bucket
        if (r.nextFloat() < 0.125f) {
            m[8] = new ItemStack(Items.BUCKET);
        }
        // 20% chance for coal to be consumed
        if (r.nextFloat() < 0.20f && !m[7].isEmpty()) {
            m[7].shrink(1);
        }
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
        FluidStack outFluid = matchedRecipe.getResult(inputFluid, templateFluid);
        if (outFluid == null || outFluid.getFluid() == null) {
            outFluid = matchedRecipe.getResult(inputFluid);
        }
        if (outFluid == null || outFluid.getFluid() == null) return ItemStack.EMPTY;

        // Seed deterministic random generator using heart position and heart's craft counter.
        // Swapping bottles or changing stack count does NOT change the roll.
        long posLong = (heart != null && heart.getPos() != null) ? heart.getPos().toLong() : 0L;
        int craftCounter = (heart != null) ? heart.getBrewingCraftCounter() : 0;
        long seed = 31L * posLong + craftCounter;
        Random deterministicRand = new Random(seed);

        // Quality calculation if template is present (m[3])
        float templateQuality = -1.0f;
        if (templateFluid != null && templateFluid.getFluid() != null && templateFluid.tag != null && templateFluid.tag.hasKey(FluidBooze.QUALITY_NBT_KEY)) {
            templateQuality = templateFluid.tag.getFloat(FluidBooze.QUALITY_NBT_KEY);
        } else if (!m[3].isEmpty() && m[3].hasTagCompound()) {
            NBTTagCompound mainTag = m[3].getTagCompound();
            if (mainTag.hasKey("Fluid")) {
                NBTTagCompound fTag = mainTag.getCompoundTag("Fluid");
                if (fTag.hasKey("Tag") && fTag.getCompoundTag("Tag").hasKey(FluidBooze.QUALITY_NBT_KEY)) {
                    templateQuality = fTag.getCompoundTag("Tag").getFloat(FluidBooze.QUALITY_NBT_KEY);
                }
            }
        }

        if (templateQuality >= 0.0f) {
            // Normal distribution around template quality with +- 0.04 range (mean 0)
            double diff = deterministicRand.nextGaussian() * 0.02;
            diff = Math.max(-0.04, Math.min(0.04, diff));
            float newQuality = (float) Math.max(0.0, Math.min(1.0, templateQuality + diff));
            if (outFluid.tag == null) outFluid.tag = new NBTTagCompound();
            outFluid.tag.setFloat(FluidBooze.QUALITY_NBT_KEY, newQuality);
        } else if (outFluid.getFluid() instanceof FluidBooze) {
            // Exact Rustic formula for brewing without template:
            float baseQuality;
            if (inputFluid.getFluid() == ModFluids.GOLDEN_APPLE_JUICE) {
                // Ambrosia / Golden Apple Juice special formula from Rustic Recipes.java
                int r = (deterministicRand.nextInt(4) == 0) ? deterministicRand.nextInt(26) : (deterministicRand.nextInt(12) + 14);
                baseQuality = ((49 + r) / 100F);
            } else {
                // Standard booze formula from Rustic BrewingBarrelRecipe.java: ((5 + rand.nextInt(71)) / 100F)
                baseQuality = ((5 + deterministicRand.nextInt(71)) / 100F);
            }
            if (outFluid.tag == null) outFluid.tag = new NBTTagCompound();
            outFluid.tag.setFloat(FluidBooze.QUALITY_NBT_KEY, baseQuality);
        }

        ItemFluidBottle bottleItem = (ItemFluidBottle) ModItems.FLUID_BOTTLE;
        ItemStack resBottle = bottleItem.getFilledBottle(outFluid.getFluid());
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
        Random r = (rng != null) ? rng : rand;
        // Cumulative blaze powder consumption: 6.25% per bottle (1/16 per bottle)
        if (!m[7].isEmpty()) {
            float chance = count * 0.0625f;
            int toConsume = (int) chance;
            if (r.nextFloat() < (chance - toConsume)) {
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
    // m[0] = 4 items of crushable ingredient (Slot 1, count >= 4)
    // m[4] = 1 Glass bottle (Slot 5)
    // m[1], m[2], m[3], m[5], m[6], m[7], m[8] must be empty (Slots 2, 3, 4, 6, 7, 8, 9)
    // ==========================================

    public static boolean canCraftCrushing(ItemStack[] m) {
        if (!Loader.isModLoaded("rustic")) return false;
        if (!m[1].isEmpty() || !m[2].isEmpty() || !m[3].isEmpty() || !m[5].isEmpty() || !m[6].isEmpty() || !m[7].isEmpty() || !m[8].isEmpty()) return false;
        if (m[0].isEmpty() || m[0].getCount() < 4) return false;
        if (m[4].isEmpty() || m[4].getItem() != Items.GLASS_BOTTLE) return false;

        return !computeCrushingResult(m).isEmpty();
    }

    public static ItemStack computeCrushingResult(ItemStack[] m) {
        if (!Loader.isModLoaded("rustic")) return ItemStack.EMPTY;
        for (ICrushingTubRecipe recipe : Recipes.crushingTubRecipes) {
            if (recipe.matches(m[0])) {
                FluidStack fluidOut = recipe.getResult();
                if (fluidOut != null && fluidOut.getFluid() != null) {
                    ItemFluidBottle bottleItem = (ItemFluidBottle) ModItems.FLUID_BOTTLE;
                    return bottleItem.getFilledBottle(fluidOut.getFluid());
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static void consumeCrushingIngredients(ItemStack[] m) {
        if (!m[0].isEmpty()) m[0].shrink(4);
        if (!m[4].isEmpty()) m[4].shrink(1);
    }
}
