package com.brilliafy.magicstorage.init;

import com.brilliafy.magicstorage.block.*;
import com.brilliafy.magicstorage.crafting.*;
import com.brilliafy.magicstorage.item.*;
import com.brilliafy.magicstorage.reference.ModBlocksRef;
import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.reference.ModItems;
import com.brilliafy.magicstorage.tile.*;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid = ModInfo.MOD_ID)
public class RegistryHandler {

    public static final ItemPortableAccess PORTABLE_ACCESS = new ItemPortableAccess(false);
    public static final ItemPortableAccess PORTABLE_CRAFTING_ACCESS = new ItemPortableAccess(true);

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();

        ModBlocksRef.STORAGE_HEART = new BlockStorageHeart();
        ModBlocksRef.STORAGE_ACCESS = new BlockStorageAccess();
        ModBlocksRef.CRAFTING_ACCESS = new BlockCraftingAccess();
        ModBlocksRef.STORAGE_UNIT = new BlockStorageUnit();
        ModBlocksRef.REMOTE_ACCESS = new BlockRemoteAccess();
        ModBlocksRef.HELL_BRICK = new BlockHellBrick();
        ModBlocksRef.HELLSTONE_ORE = new BlockHellstoneOre();
        ModBlocksRef.LUMINITE_ORE = new BlockLuminiteOre();

        registry.register(ModBlocksRef.STORAGE_HEART);
        registry.register(ModBlocksRef.STORAGE_ACCESS);
        registry.register(ModBlocksRef.CRAFTING_ACCESS);
        registry.register(ModBlocksRef.STORAGE_UNIT);
        registry.register(ModBlocksRef.REMOTE_ACCESS);
        registry.register(ModBlocksRef.HELL_BRICK);
        registry.register(ModBlocksRef.HELLSTONE_ORE);
        registry.register(ModBlocksRef.LUMINITE_ORE);

        GameRegistry.registerTileEntity(TileStorageHeart.class, ModInfo.MOD_ID + ":storage_heart");
        GameRegistry.registerTileEntity(TileStorageAccess.class, ModInfo.MOD_ID + ":storage_access");
        GameRegistry.registerTileEntity(TileCraftingAccess.class, ModInfo.MOD_ID + ":crafting_access");
        GameRegistry.registerTileEntity(TileStorageUnit.class, ModInfo.MOD_ID + ":storage_unit");
        GameRegistry.registerTileEntity(TileRemoteAccess.class, ModInfo.MOD_ID + ":remote_access");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();

        registry.register(new ItemBlockStorage(ModBlocksRef.STORAGE_HEART)
            .setRegistryName(ModBlocksRef.STORAGE_HEART.getRegistryName()));
        registry.register(new ItemBlockStorage(ModBlocksRef.STORAGE_ACCESS)
            .setRegistryName(ModBlocksRef.STORAGE_ACCESS.getRegistryName()));
        registry.register(new ItemBlockStorage(ModBlocksRef.CRAFTING_ACCESS)
            .setRegistryName(ModBlocksRef.CRAFTING_ACCESS.getRegistryName()));
        registry.register(new ItemBlockStorageUnit(ModBlocksRef.STORAGE_UNIT));
        registry.register(new ItemBlockStorage(ModBlocksRef.REMOTE_ACCESS)
            .setRegistryName(ModBlocksRef.REMOTE_ACCESS.getRegistryName()));
        registry.register(new ItemBlock(ModBlocksRef.HELL_BRICK) {
            @Override
            public String getItemStackDisplayName(ItemStack stack) {
                return TextFormatting.RED + super.getItemStackDisplayName(stack);
            }
        }.setRegistryName(ModBlocksRef.HELL_BRICK.getRegistryName()));

        registry.register(new ItemBlock(ModBlocksRef.HELLSTONE_ORE)
            .setRegistryName(ModBlocksRef.HELLSTONE_ORE.getRegistryName()));
        registry.register(new ItemBlock(ModBlocksRef.LUMINITE_ORE)
            .setRegistryName(ModBlocksRef.LUMINITE_ORE.getRegistryName()));

        ModItems.STORAGE_COMPONENT = new ItemStorageComponent();
        registry.register(ModItems.STORAGE_COMPONENT);

        ModItems.UPGRADE_IRON = new ItemUpgrade("upgrade_crimtane", 1);
        registry.register(ModItems.UPGRADE_IRON);
        ModItems.UPGRADE_GOLD = new ItemUpgrade("upgrade_hellstone", 3);
        registry.register(ModItems.UPGRADE_GOLD);
        ModItems.UPGRADE_DIAMOND = new ItemUpgrade("upgrade_hallowed", 4);
        registry.register(ModItems.UPGRADE_DIAMOND);
        ModItems.UPGRADE_EMERALD = new ItemUpgrade("upgrade_blue_chlorophyte", 5);
        registry.register(ModItems.UPGRADE_EMERALD);
        ModItems.UPGRADE_OBSIDIAN = new ItemUpgrade("upgrade_luminite", 6);
        registry.register(ModItems.UPGRADE_OBSIDIAN);
        ModItems.UPGRADE_NETHER_STAR = new ItemUpgrade("upgrade_terra", 7);
        registry.register(ModItems.UPGRADE_NETHER_STAR);
        ModItems.UPGRADE_DEMONITE = new ItemUpgrade("upgrade_demonite", 2);
        registry.register(ModItems.UPGRADE_DEMONITE);

        ModItems.UPGRADE_CRIMTANE = ModItems.UPGRADE_IRON;
        ModItems.UPGRADE_HELLSTONE = ModItems.UPGRADE_GOLD;
        ModItems.UPGRADE_HALLOWED = ModItems.UPGRADE_DIAMOND;
        ModItems.UPGRADE_BLUE_CHLOROPHYTE = ModItems.UPGRADE_EMERALD;
        ModItems.UPGRADE_LUMINITE = ModItems.UPGRADE_OBSIDIAN;
        ModItems.UPGRADE_TERRA = ModItems.UPGRADE_NETHER_STAR;

        // Custom Progression Materials
        ModItems.INGOT_DEMONITE = new ItemMaterial("ingot_demonite", TextFormatting.DARK_PURPLE,
            TextFormatting.GRAY + "Shadow-infused metal",
            TextFormatting.DARK_GRAY + "Used for Demonite Storage Upgrades");
        registry.register(ModItems.INGOT_DEMONITE);

        ModItems.INGOT_CRIMTANE = new ItemMaterial("ingot_crimtane", TextFormatting.RED,
            TextFormatting.GRAY + "Flesh-carved metal",
            TextFormatting.DARK_GRAY + "Used for Crimtane Storage Upgrades");
        registry.register(ModItems.INGOT_CRIMTANE);

        ModItems.BAR_HELLSTONE = new ItemMaterial("bar_hellstone", TextFormatting.DARK_RED,
            TextFormatting.GOLD + "Smelting Station: Storage Heart with Furnace",
            TextFormatting.GRAY + "Forged from Nether Hellstone Ore and Obsidian",
            TextFormatting.DARK_GRAY + "Used for Hellstone Storage Upgrades");
        registry.register(ModItems.BAR_HELLSTONE);

        ModItems.INGOT_HALLOWED = new ItemMaterial("ingot_hallowed", TextFormatting.YELLOW,
            TextFormatting.GRAY + "Sacred metal radiating holy energy",
            TextFormatting.DARK_GRAY + "Used for Hallowed Storage Upgrades");
        registry.register(ModItems.INGOT_HALLOWED);

        ModItems.BAR_CHLOROPHYTE = new ItemMaterial("bar_chlorophyte", TextFormatting.GREEN,
            TextFormatting.GRAY + "Living botanical alloy",
            TextFormatting.DARK_GRAY + "Used for Blue Chlorophyte Storage Upgrades");
        registry.register(ModItems.BAR_CHLOROPHYTE);

        ModItems.BAR_LUMINITE = new ItemMaterial("bar_luminite", TextFormatting.AQUA,
            TextFormatting.GOLD + "Smelting Station: Storage Heart with Furnace",
            TextFormatting.GRAY + "Refined from celestial End Luminite Ore",
            TextFormatting.DARK_GRAY + "Used for Luminite Storage Upgrades");
        registry.register(ModItems.BAR_LUMINITE);

        ModItems.TERRA_SINGULARITY = new ItemMaterial("terra_singularity", TextFormatting.LIGHT_PURPLE,
            TextFormatting.GOLD + "The ultimate synthesis of all tiers",
            TextFormatting.DARK_PURPLE + "Harnesses infinite cosmic storage power",
            TextFormatting.DARK_GRAY + "Used for Terra Storage Upgrades and Ultimate Remotes");
        registry.register(ModItems.TERRA_SINGULARITY);

        // Portable remotes
        PORTABLE_ACCESS.setRegistryName(ModInfo.MOD_ID, "portable_access");
        PORTABLE_ACCESS.setTranslationKey(ModInfo.MOD_ID + ".portable_access");
        registry.register(PORTABLE_ACCESS);

        PORTABLE_CRAFTING_ACCESS.setRegistryName(ModInfo.MOD_ID, "portable_crafting_access");
        PORTABLE_CRAFTING_ACCESS.setTranslationKey(ModInfo.MOD_ID + ".portable_crafting_access");
        registry.register(PORTABLE_CRAFTING_ACCESS);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        IForgeRegistry<IRecipe> registry = event.getRegistry();

        // 1. Heart Furnace Recipes (Smelted in Storage Heart equipped with Furnace)
        Ingredient oreH = Ingredient.fromItem(Item.getItemFromBlock(ModBlocksRef.HELLSTONE_ORE));
        Ingredient obs = Ingredient.fromItem(Item.getItemFromBlock(Blocks.OBSIDIAN));
        ShapelessRecipes innerHellstone = makeShapeless(new ResourceLocation(ModInfo.MOD_ID, "bar_hellstone"),
            new ItemStack(ModItems.BAR_HELLSTONE), oreH, oreH, oreH, obs);
        registry.register(new RecipeHeartFurnace(innerHellstone.getRegistryName(), innerHellstone));

        Ingredient oreL = Ingredient.fromItem(Item.getItemFromBlock(ModBlocksRef.LUMINITE_ORE));
        ShapelessRecipes innerLuminite = makeShapeless(new ResourceLocation(ModInfo.MOD_ID, "bar_luminite"),
            new ItemStack(ModItems.BAR_LUMINITE), oreL, oreL, oreL, oreL);
        registry.register(new RecipeHeartFurnace(innerLuminite.getRegistryName(), innerLuminite));

        // 2. Portable Access Conversion to Crafting Remote (shapeless with Crafting Table, preserves NBT link!)
        registry.register(new RecipeRemoteConversion(new ResourceLocation(ModInfo.MOD_ID, "remote_conversion_prehm"), 0));
        registry.register(new RecipeRemoteConversion(new ResourceLocation(ModInfo.MOD_ID, "remote_conversion_hm"), 1));
        registry.register(new RecipeRemoteConversion(new ResourceLocation(ModInfo.MOD_ID, "remote_conversion_ultimate"), 2));

        // 3. Remote Tier Upgrades (preserves NBT link!)
        Ingredient shulker = Ingredient.fromItem(Items.SHULKER_SHELL);
        Ingredient star = Ingredient.fromItem(Items.NETHER_STAR);
        Ingredient chorus = Ingredient.fromItem(Items.CHORUS_FRUIT_POPPED);
        Ingredient remote0 = Ingredient.fromStacks(new ItemStack(PORTABLE_ACCESS, 1, 0));
        Ingredient dBlock = Ingredient.fromItem(Item.getItemFromBlock(Blocks.DIAMOND_BLOCK));
        Ingredient eye = Ingredient.fromItem(Items.ENDER_EYE);

        ShapedRecipes hmInner = make3x3(new ResourceLocation(ModInfo.MOD_ID, "portable_access_hm"),
            new ItemStack(PORTABLE_ACCESS, 1, 1),
            shulker, star, shulker,
            chorus, remote0, chorus,
            dBlock, eye, dBlock);
        registry.register(new RecipeRemoteUpgrade(hmInner.getRegistryName(), hmInner));

        Ingredient craftRemote0 = Ingredient.fromStacks(new ItemStack(PORTABLE_CRAFTING_ACCESS, 1, 0));
        ShapedRecipes hmCraftInner = make3x3(new ResourceLocation(ModInfo.MOD_ID, "portable_crafting_access_hm"),
            new ItemStack(PORTABLE_CRAFTING_ACCESS, 1, 1),
            shulker, star, shulker,
            chorus, craftRemote0, chorus,
            dBlock, eye, dBlock);
        registry.register(new RecipeRemoteUpgrade(hmCraftInner.getRegistryName(), hmCraftInner));

        Ingredient breath = Ingredient.fromItem(Items.DRAGON_BREATH);
        Ingredient beacon = Ingredient.fromItem(Item.getItemFromBlock(Blocks.BEACON));
        Ingredient crystal = Ingredient.fromItem(Items.END_CRYSTAL);
        Ingredient remote1 = Ingredient.fromStacks(new ItemStack(PORTABLE_ACCESS, 1, 1));
        Ingredient barL = Ingredient.fromItem(ModItems.BAR_LUMINITE);
        Ingredient sing = Ingredient.fromItem(ModItems.TERRA_SINGULARITY);

        ShapedRecipes ultInner = make3x3(new ResourceLocation(ModInfo.MOD_ID, "portable_access_ultimate"),
            new ItemStack(PORTABLE_ACCESS, 1, 2),
            breath, beacon, breath,
            crystal, remote1, crystal,
            barL, sing, barL);
        registry.register(new RecipeRemoteUpgrade(ultInner.getRegistryName(), ultInner));

        Ingredient craftRemote1 = Ingredient.fromStacks(new ItemStack(PORTABLE_CRAFTING_ACCESS, 1, 1));
        ShapedRecipes ultCraftInner = make3x3(new ResourceLocation(ModInfo.MOD_ID, "portable_crafting_access_ultimate"),
            new ItemStack(PORTABLE_CRAFTING_ACCESS, 1, 2),
            breath, beacon, breath,
            crystal, craftRemote1, crystal,
            barL, sing, barL);
        registry.register(new RecipeRemoteUpgrade(ultCraftInner.getRegistryName(), ultCraftInner));
    }

    private static ShapedRecipes make3x3(ResourceLocation name, ItemStack result,
                                          Ingredient i0, Ingredient i1, Ingredient i2,
                                          Ingredient i3, Ingredient i4, Ingredient i5,
                                          Ingredient i6, Ingredient i7, Ingredient i8) {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(i0); list.add(i1); list.add(i2);
        list.add(i3); list.add(i4); list.add(i5);
        list.add(i6); list.add(i7); list.add(i8);
        ShapedRecipes r = new ShapedRecipes("", 3, 3, list, result);
        r.setRegistryName(name);
        return r;
    }

    private static ShapelessRecipes makeShapeless(ResourceLocation name, ItemStack result, Ingredient... ingredients) {
        NonNullList<Ingredient> list = NonNullList.create();
        for (Ingredient ing : ingredients) {
            list.add(ing);
        }
        ShapelessRecipes r = new ShapelessRecipes("", result, list);
        r.setRegistryName(name);
        return r;
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        // Blocks
        registerBlockModel(ModBlocksRef.STORAGE_HEART);
        registerBlockModel(ModBlocksRef.STORAGE_ACCESS);
        registerBlockModel(ModBlocksRef.CRAFTING_ACCESS);

        // Storage Unit has variants
        for (int i = 0; i < 8; i++) {
            ModelLoader.setCustomModelResourceLocation(
                Item.getItemFromBlock(ModBlocksRef.STORAGE_UNIT), i,
                new ModelResourceLocation(ModBlocksRef.STORAGE_UNIT.getRegistryName(), "variant=" + getVariantName(i))
            );
        }

        registerBlockModel(ModBlocksRef.REMOTE_ACCESS);
        registerBlockModel(ModBlocksRef.HELL_BRICK);
        registerBlockModel(ModBlocksRef.HELLSTONE_ORE);
        registerBlockModel(ModBlocksRef.LUMINITE_ORE);

        // Items
        registerItemModel(ModItems.STORAGE_COMPONENT);
        registerItemModel(ModItems.UPGRADE_IRON);
        registerItemModel(ModItems.UPGRADE_GOLD);
        registerItemModel(ModItems.UPGRADE_DIAMOND);
        registerItemModel(ModItems.UPGRADE_EMERALD);
        registerItemModel(ModItems.UPGRADE_OBSIDIAN);
        registerItemModel(ModItems.UPGRADE_NETHER_STAR);
        registerItemModel(ModItems.UPGRADE_DEMONITE);

        registerItemModel(ModItems.INGOT_DEMONITE);
        registerItemModel(ModItems.INGOT_CRIMTANE);
        registerItemModel(ModItems.BAR_HELLSTONE);
        registerItemModel(ModItems.INGOT_HALLOWED);
        registerItemModel(ModItems.BAR_CHLOROPHYTE);
        registerItemModel(ModItems.BAR_LUMINITE);
        registerItemModel(ModItems.TERRA_SINGULARITY);

        // Portable access - 3 variants each, use inventory variant with full path
        for (int i = 0; i < 3; i++) {
            String tier = i == 0 ? "prehm" : (i == 1 ? "hm" : "ultimate");
            ModelLoader.setCustomModelResourceLocation(PORTABLE_ACCESS, i,
                new ModelResourceLocation(new ResourceLocation(ModInfo.MOD_ID, "portable_access_" + tier), "inventory"));
            ModelLoader.setCustomModelResourceLocation(PORTABLE_CRAFTING_ACCESS, i,
                new ModelResourceLocation(new ResourceLocation(ModInfo.MOD_ID, "portable_crafting_access_" + tier), "inventory"));
        }
    }

    private static String getVariantName(int id) {
        return BlockStorageUnit.StorageUnitType.byId(id).getName();
    }

    private static void registerBlockModel(Block block) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation(block.getRegistryName(), "inventory"));
    }

    private static void registerItemModel(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
            new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
