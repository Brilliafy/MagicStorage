package com.brilliafy.magicstorage.init;

import com.brilliafy.magicstorage.block.*;
import com.brilliafy.magicstorage.item.*;
import com.brilliafy.magicstorage.reference.ModBlocksRef;
import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.reference.ModItems;
import com.brilliafy.magicstorage.tile.*;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
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
            TextFormatting.DARK_GRAY + "Used for Demonite Storage Upgrades & Shadow Diamonds");
        registry.register(ModItems.INGOT_DEMONITE);

        ModItems.INGOT_CRIMTANE = new ItemMaterial("ingot_crimtane", TextFormatting.RED,
            TextFormatting.GRAY + "Flesh-carved metal",
            TextFormatting.DARK_GRAY + "Used for Crimtane Storage Upgrades & Shadow Diamonds");
        registry.register(ModItems.INGOT_CRIMTANE);

        ModItems.BAR_HELLSTONE = new ItemMaterial("bar_hellstone", TextFormatting.DARK_RED,
            TextFormatting.GOLD + "Smelting Station: Storage Heart with Furnace",
            TextFormatting.GRAY + "Forged from Hellstone Fragments, Obsidian, and Blaze Powder",
            TextFormatting.DARK_GRAY + "Used for Hellstone Storage Upgrades");
        registry.register(ModItems.BAR_HELLSTONE);

        ModItems.INGOT_HALLOWED = new ItemMaterial("ingot_hallowed", TextFormatting.YELLOW,
            TextFormatting.GRAY + "Sacred metal radiating holy energy",
            TextFormatting.DARK_GRAY + "Used for Hallowed Storage Upgrades & Advanced Remotes");
        registry.register(ModItems.INGOT_HALLOWED);

        ModItems.BAR_CHLOROPHYTE = new ItemMaterial("bar_chlorophyte", TextFormatting.GREEN,
            TextFormatting.GRAY + "Living botanical alloy",
            TextFormatting.DARK_GRAY + "Used for Blue Chlorophyte Storage Upgrades");
        registry.register(ModItems.BAR_CHLOROPHYTE);

        ModItems.BAR_LUMINITE = new ItemMaterial("bar_luminite", TextFormatting.AQUA,
            TextFormatting.GOLD + "Smelting Station: Storage Heart with Furnace",
            TextFormatting.GRAY + "Refined from celestial End Luminite Fragments",
            TextFormatting.DARK_GRAY + "Used for Luminite Storage Upgrades & Radiant Jewels");
        registry.register(ModItems.BAR_LUMINITE);

        ModItems.FRAGMENT_HELLSTONE = new ItemMaterial("fragment_hellstone", TextFormatting.GOLD,
            TextFormatting.GRAY + "A fiery molten shard extracted from Hellstone Ore",
            TextFormatting.DARK_GRAY + "Used to forge Hellstone Bars (Affected by Fortune)");
        registry.register(ModItems.FRAGMENT_HELLSTONE);

        ModItems.FRAGMENT_LUMINITE = new ItemMaterial("fragment_luminite", TextFormatting.AQUA,
            TextFormatting.GRAY + "A celestial crystal shard extracted from Luminite Ore",
            TextFormatting.DARK_GRAY + "Used to forge Luminite Bars (Affected by Fortune)");
        registry.register(ModItems.FRAGMENT_LUMINITE);

        ModItems.SHADOW_DIAMOND = new ItemMaterial("shadow_diamond", TextFormatting.DARK_PURPLE,
            TextFormatting.GRAY + "A dark, abyssal diamond infused with shadow magic",
            TextFormatting.DARK_GRAY + "Used to craft Basic Portable Access Remotes");
        registry.register(ModItems.SHADOW_DIAMOND);

        ModItems.RADIANT_JEWEL = new ItemMaterial("radiant_jewel", TextFormatting.AQUA,
            TextFormatting.GOLD + "A celestial prism radiating stellar energy",
            TextFormatting.DARK_GRAY + "Used to craft Ultimate Portable Access Remotes");
        registry.register(ModItems.RADIANT_JEWEL);

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
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.craftMatrix == null || event.crafting.isEmpty()) return;
        if (event.crafting.getItem() instanceof ItemPortableAccess) {
            for (int i = 0; i < event.craftMatrix.getSizeInventory(); i++) {
                ItemStack stack = event.craftMatrix.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemPortableAccess && stack.hasTagCompound()) {
                    event.crafting.setTagCompound(stack.getTagCompound().copy());
                    break;
                }
            }
        }
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
        registerItemModel(ModItems.FRAGMENT_HELLSTONE);
        registerItemModel(ModItems.FRAGMENT_LUMINITE);
        registerItemModel(ModItems.SHADOW_DIAMOND);
        registerItemModel(ModItems.RADIANT_JEWEL);
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
