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
        registry.register(ModBlocksRef.STORAGE_HEART);
        registry.register(ModBlocksRef.STORAGE_ACCESS);
        registry.register(ModBlocksRef.CRAFTING_ACCESS);
        registry.register(ModBlocksRef.STORAGE_UNIT);
        registry.register(ModBlocksRef.REMOTE_ACCESS);

        GameRegistry.registerTileEntity(TileStorageHeart.class, ModInfo.MOD_ID + ":storage_heart");
        GameRegistry.registerTileEntity(TileStorageAccess.class, ModInfo.MOD_ID + ":storage_access");
        GameRegistry.registerTileEntity(TileCraftingAccess.class, ModInfo.MOD_ID + ":crafting_access");
        registry.register(ModBlocksRef.HELL_BRICK);
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

        // Portable remotes
        PORTABLE_ACCESS.setRegistryName(ModInfo.MOD_ID, "portable_access");
        PORTABLE_ACCESS.setTranslationKey(ModInfo.MOD_ID + ".portable_access");
        registry.register(PORTABLE_ACCESS);

        PORTABLE_CRAFTING_ACCESS.setRegistryName(ModInfo.MOD_ID, "portable_crafting_access");
        PORTABLE_CRAFTING_ACCESS.setTranslationKey(ModInfo.MOD_ID + ".portable_crafting_access");
        registry.register(PORTABLE_CRAFTING_ACCESS);
    }

    private static Item registerItem(IForgeRegistry<Item> registry, String name) {
        Item item = new Item().setRegistryName(ModInfo.MOD_ID, name)
            .setTranslationKey(ModInfo.MOD_ID + "." + name)
            .setCreativeTab(com.brilliafy.magicstorage.init.MagicStorageTab.TAB);
        registry.register(item);
        return item;
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

        // Items
        registerItemModel(ModItems.STORAGE_COMPONENT);
        registerBlockModel(ModBlocksRef.HELL_BRICK);
        registerItemModel(ModItems.UPGRADE_IRON);
        registerItemModel(ModItems.UPGRADE_GOLD);
        registerItemModel(ModItems.UPGRADE_DIAMOND);
        registerItemModel(ModItems.UPGRADE_EMERALD);
        registerItemModel(ModItems.UPGRADE_OBSIDIAN);
        registerItemModel(ModItems.UPGRADE_NETHER_STAR);
        registerItemModel(ModItems.UPGRADE_DEMONITE);

        // Portable access - 3 variants each, use inventory variant with full path
        for (int i = 0; i < 3; i++) {
            String tier = i == 0 ? "prehm" : (i == 1 ? "hm" : "ultimate");
            ModelLoader.setCustomModelResourceLocation(PORTABLE_ACCESS, i,
                new ModelResourceLocation(new net.minecraft.util.ResourceLocation(ModInfo.MOD_ID, "portable_access_" + tier), "inventory"));
            ModelLoader.setCustomModelResourceLocation(PORTABLE_CRAFTING_ACCESS, i,
                new ModelResourceLocation(new net.minecraft.util.ResourceLocation(ModInfo.MOD_ID, "portable_crafting_access_" + tier), "inventory"));
        }
    }
    
    private static String getVariantName(int id) {
        return BlockStorageUnit.StorageUnitType.byId(id).getName();
    }

    private static String getTierName(int id) {
        return id == 0 ? "prehm" : (id == 1 ? "hm" : "ultimate");
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
