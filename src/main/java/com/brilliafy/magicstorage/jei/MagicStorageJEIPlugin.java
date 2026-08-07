package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.container.ContainerCraftingAccess;
import com.brilliafy.magicstorage.container.ContainerPortableAccess;
import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.reference.ModBlocksRef;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.item.ItemStack;

@JEIPlugin
public class MagicStorageJEIPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        JeiHooks.setJeiLoaded(true);

        // Vanilla crafting catalysts
        registry.addRecipeCatalyst(new ItemStack(ModBlocksRef.CRAFTING_ACCESS), VanillaRecipeCategoryUid.CRAFTING);

        // Recipe transfer for request table (like SSN's ContainerRequest)
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(
            new MagicRecipeTransferHandler<>(ContainerCraftingAccess.class),
            VanillaRecipeCategoryUid.CRAFTING);

        // Recipe transfer for remote (like SSN's ContainerRemote)
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(
            new MagicRecipeTransferHandler<>(ContainerPortableAccess.class),
            VanillaRecipeCategoryUid.CRAFTING);

        // Custom categories — only CRAFTING_ACCESS is the catalyst, not STORAGE_ACCESS
        registry.addRecipeCategories(new BrewingRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        registry.addRecipeCatalyst(new ItemStack(ModBlocksRef.CRAFTING_ACCESS), BrewingRecipeCategory.UID);
        registry.addRecipes(BrewingRecipeCategory.generateAllRecipes(), BrewingRecipeCategory.UID);

        registry.addRecipeCategories(new AnvilRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        registry.addRecipeCatalyst(new ItemStack(ModBlocksRef.CRAFTING_ACCESS), AnvilRecipeCategory.UID);
        registry.addRecipes(AnvilRecipeCategory.generateAllRecipes(), AnvilRecipeCategory.UID);

        registry.addRecipeCategories(new SmeltingRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        registry.addRecipeCatalyst(new ItemStack(ModBlocksRef.CRAFTING_ACCESS), SmeltingRecipeCategory.UID);
        registry.addRecipes(SmeltingRecipeCategory.generateAllRecipes(), SmeltingRecipeCategory.UID);

        registry.addRecipeCategories(new EnchantingRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        registry.addRecipeCatalyst(new ItemStack(ModBlocksRef.CRAFTING_ACCESS), EnchantingRecipeCategory.UID);
        registry.addRecipes(EnchantingRecipeCategory.generateAllRecipes(), EnchantingRecipeCategory.UID);

        // GUI handlers so JEI recognises hovered stacks in Storage Access / Crafting Access
        registry.addAdvancedGuiHandlers(new StorageGuiHandler(), new CraftingAccessGuiHandler());

    }
}
