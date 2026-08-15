package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.container.ContainerCraftingAccess;
import com.brilliafy.magicstorage.container.ContainerPortableAccess;
import com.brilliafy.magicstorage.reference.ModBlocksRef;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferRegistry;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

import mezz.jei.api.ISubtypeRegistry;

@JEIPlugin
public class MagicStorageJEIPlugin implements IModPlugin {

    @Override
    public void registerItemSubtypes(ISubtypeRegistry subtypeRegistry) {
        // Rustic fluid bottles & elixirs: map by FluidName / Potion name so any quality (e.g. 0.73, 0.75, 1.0) matches the recipe
        net.minecraft.item.Item fluidBottle = net.minecraft.item.Item.getByNameOrId("rustic:fluid_bottle");
        if (fluidBottle != null && !subtypeRegistry.hasSubtypeInterpreter(new ItemStack(fluidBottle))) {
            subtypeRegistry.registerSubtypeInterpreter(fluidBottle, stack -> {
                if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Fluid", 10)) {
                    String fn = stack.getTagCompound().getCompoundTag("Fluid").getString("FluidName");
                    return fn.isEmpty() ? ISubtypeRegistry.ISubtypeInterpreter.NONE : fn;
                }
                return ISubtypeRegistry.ISubtypeInterpreter.NONE;
            });
        }
        net.minecraft.item.Item elixir = net.minecraft.item.Item.getByNameOrId("rustic:elixir");
        if (elixir != null && !subtypeRegistry.hasSubtypeInterpreter(new ItemStack(elixir))) {
            subtypeRegistry.registerSubtypeInterpreter(elixir, stack -> {
                if (stack.hasTagCompound() && stack.getTagCompound().hasKey("Elixir", 10)) {
                    String pn = stack.getTagCompound().getCompoundTag("Elixir").getString("Effect");
                    return pn.isEmpty() ? ISubtypeRegistry.ISubtypeInterpreter.NONE : pn;
                }
                return ISubtypeRegistry.ISubtypeInterpreter.NONE;
            });
        }

        for (net.minecraft.item.Item item : net.minecraft.item.Item.REGISTRY) {
            if (item != null && (item.isDamageable() || item instanceof net.minecraft.item.ItemSword || item instanceof net.minecraft.item.ItemTool || item instanceof net.minecraft.item.ItemArmor || item instanceof net.minecraft.item.ItemBow || item instanceof net.minecraft.item.ItemShield || item instanceof net.minecraft.item.ItemEnchantedBook)) {
                if (!subtypeRegistry.hasSubtypeInterpreter(new ItemStack(item))) {
                    subtypeRegistry.registerSubtypeInterpreter(item, stack -> {
                        if (stack.isItemEnchanted() || stack.getItem() == net.minecraft.init.Items.ENCHANTED_BOOK) {
                            net.minecraft.nbt.NBTTagList tagList = stack.getEnchantmentTagList();
                            return (tagList != null && !tagList.isEmpty()) ? tagList.toString() : "enchanted";
                        }
                        return ISubtypeRegistry.ISubtypeInterpreter.NONE;
                    });
                }
            }
        }
    }

    @Override
    public void register(IModRegistry registry) {
        JeiHooks.setJeiLoaded(true);

        ItemStack craftingAccessStack = new ItemStack(ModBlocksRef.CRAFTING_ACCESS);
        IRecipeTransferRegistry transferRegistry = registry.getRecipeTransferRegistry();

        // 1. Vanilla Crafting
        registry.addRecipeCatalyst(craftingAccessStack, VanillaRecipeCategoryUid.CRAFTING);
        registerTransfer(transferRegistry, VanillaRecipeCategoryUid.CRAFTING);

        // 2. Smelting (Vanilla & Magic Storage)
        registerTransfer(transferRegistry, VanillaRecipeCategoryUid.SMELTING);

        registry.addRecipeCategories(new SmeltingRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        registry.addRecipeCatalyst(craftingAccessStack, SmeltingRecipeCategory.UID);
        registry.addRecipes(SmeltingRecipeCategory.generateAllRecipes(), SmeltingRecipeCategory.UID);
        registerTransfer(transferRegistry, SmeltingRecipeCategory.UID);

        // 3. Brewing (Vanilla & Magic Storage)
        registerTransfer(transferRegistry, VanillaRecipeCategoryUid.BREWING);

        registry.addRecipeCategories(new BrewingRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        registry.addRecipeCatalyst(craftingAccessStack, BrewingRecipeCategory.UID);
        registry.addRecipes(BrewingRecipeCategory.generateAllRecipes(), BrewingRecipeCategory.UID);
        registerTransfer(transferRegistry, BrewingRecipeCategory.UID);

        // 4. Anvil (Vanilla & Magic Storage)
        registerTransfer(transferRegistry, VanillaRecipeCategoryUid.ANVIL);

        registry.addRecipeCategories(new AnvilRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        registry.addRecipeCatalyst(craftingAccessStack, AnvilRecipeCategory.UID);
        registry.addRecipes(AnvilRecipeCategory.generateAllRecipes(), AnvilRecipeCategory.UID);
        registerTransfer(transferRegistry, AnvilRecipeCategory.UID);

        // 5. Enchanting, Rustic Brewing & Crushing (Custom RecipeRegistryPlugin handles dynamic lookups & filtering)
        registry.addRecipeCategories(new EnchantingRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
        registry.addRecipeCatalyst(craftingAccessStack, EnchantingRecipeCategory.UID);
        registerTransfer(transferRegistry, EnchantingRecipeCategory.UID);

        java.util.List<RusticBrewingRecipeCategory.RusticBrewingJEIRecipe> brewingRecipes = java.util.Collections.emptyList();
        java.util.List<RusticCrushingRecipeCategory.RusticCrushingJEIRecipe> crushingRecipes = java.util.Collections.emptyList();
        if (Loader.isModLoaded("rustic")) {
            brewingRecipes = RusticBrewingRecipeCategory.generateAllRecipes();
            crushingRecipes = RusticCrushingRecipeCategory.generateAllRecipes();
        }

        java.util.List<DisenchanterRecipeCategory.DisenchanterJEIRecipe> disenchanterRecipes = java.util.Collections.emptyList();
        if (Loader.isModLoaded("disenchanter")) {
            disenchanterRecipes = DisenchanterRecipeCategory.generateAllRecipes();
        }

        java.util.List<QualityToolsRecipeCategory.QualityToolsJEIRecipe> qualityToolsRecipes = java.util.Collections.emptyList();
        if (Loader.isModLoaded("qualitytools")) {
            qualityToolsRecipes = QualityToolsRecipeCategory.generateAllRecipes();
        }

        java.util.List<BountifulBaublesRecipeCategory.BaubleReforgeJEIRecipe> baubleRecipes = java.util.Collections.emptyList();
        if (Loader.isModLoaded("bountifulbaubles")) {
            baubleRecipes = BountifulBaublesRecipeCategory.generateAllRecipes();
        }

        registry.addRecipeRegistryPlugin(new MagicStorageJEIRegistryPlugin(
            EnchantingRecipeCategory.generateAllRecipes(),
            brewingRecipes,
            crushingRecipes,
            disenchanterRecipes
        ));

        // 6. Rustic Integration
        if (Loader.isModLoaded("rustic")) {
            // Magic Storage Custom Tabs
            registry.addRecipeCategories(new RusticBrewingRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
            registry.addRecipeCatalyst(craftingAccessStack, RusticBrewingRecipeCategory.UID);
            registerTransfer(transferRegistry, RusticBrewingRecipeCategory.UID);

            registry.addRecipeCategories(new RusticCrushingRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
            registry.addRecipeCatalyst(craftingAccessStack, RusticCrushingRecipeCategory.UID);
            registerTransfer(transferRegistry, RusticCrushingRecipeCategory.UID);

            registry.addRecipeCategories(new RusticSimpleAlchemyRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
            registry.addRecipeCatalyst(craftingAccessStack, RusticSimpleAlchemyRecipeCategory.UID);
            registry.addRecipes(RusticSimpleAlchemyRecipeCategory.generateAllRecipes(), RusticSimpleAlchemyRecipeCategory.UID);
            registerTransfer(transferRegistry, RusticSimpleAlchemyRecipeCategory.UID);

            registry.addRecipeCategories(new RusticAdvancedAlchemyRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
            registry.addRecipeCatalyst(craftingAccessStack, RusticAdvancedAlchemyRecipeCategory.UID);
            registry.addRecipes(RusticAdvancedAlchemyRecipeCategory.generateAllRecipes(), RusticAdvancedAlchemyRecipeCategory.UID);
            registerTransfer(transferRegistry, RusticAdvancedAlchemyRecipeCategory.UID);

            // Rustic's native tabs transfer support
            registerTransfer(transferRegistry, "rustic.alchemy_simple");
            registerTransfer(transferRegistry, "rustic.alchemy_advanced");
            registerTransfer(transferRegistry, "rustic.brewing");
            registerTransfer(transferRegistry, "rustic.crushing_tub");
        }

        // 7. Disenchanter Integration
        if (Loader.isModLoaded("disenchanter")) {
            registry.addRecipeCategories(new DisenchanterRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
            registry.addRecipeCatalyst(craftingAccessStack, DisenchanterRecipeCategory.UID);
            registry.addRecipes(DisenchanterRecipeCategory.generateAllRecipes(), DisenchanterRecipeCategory.UID);
            registerTransfer(transferRegistry, DisenchanterRecipeCategory.UID);
        }

        // 8. Bountiful Baubles Integration
        if (Loader.isModLoaded("bountifulbaubles")) {
            registry.addRecipeCategories(new BountifulBaublesRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
            registry.addRecipeCatalyst(craftingAccessStack, BountifulBaublesRecipeCategory.UID);
            registry.addRecipes(baubleRecipes, BountifulBaublesRecipeCategory.UID);
            registerTransfer(transferRegistry, BountifulBaublesRecipeCategory.UID);
        }

        // 9. Quality Tools Integration
        if (Loader.isModLoaded("qualitytools")) {
            registry.addRecipeCategories(new QualityToolsRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
            registry.addRecipeCatalyst(craftingAccessStack, QualityToolsRecipeCategory.UID);
            registry.addRecipes(qualityToolsRecipes, QualityToolsRecipeCategory.UID);
            registerTransfer(transferRegistry, QualityToolsRecipeCategory.UID);
        }

        // GUI handlers so JEI recognises hovered stacks in Storage Access / Crafting Access
        registry.addAdvancedGuiHandlers(new StorageGuiHandler(), new CraftingAccessGuiHandler());
    }

    private static void registerTransfer(IRecipeTransferRegistry registry, String categoryUid) {
        registry.addRecipeTransferHandler(new MagicRecipeTransferHandler<>(ContainerCraftingAccess.class, categoryUid), categoryUid);
        registry.addRecipeTransferHandler(new MagicRecipeTransferHandler<>(ContainerPortableAccess.class, categoryUid), categoryUid);
    }
}
