package com.brilliafy.magicstorage.proxy;

import com.brilliafy.magicstorage.gui.GuiHandler;
import com.brilliafy.magicstorage.MagicStorage;
import com.brilliafy.magicstorage.network.NetworkHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        NetworkHandler.init();
    }

    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(MagicStorage.instance, new GuiHandler());
        net.minecraftforge.fml.common.registry.GameRegistry.registerWorldGenerator(new com.brilliafy.magicstorage.world.WorldGenOres(), 10);
        net.minecraftforge.fml.common.registry.GameRegistry.addSmelting(com.brilliafy.magicstorage.reference.ModBlocksRef.HELLSTONE_ORE, new net.minecraft.item.ItemStack(com.brilliafy.magicstorage.reference.ModItems.FRAGMENT_HELLSTONE), 0.7F);
        net.minecraftforge.fml.common.registry.GameRegistry.addSmelting(com.brilliafy.magicstorage.reference.ModBlocksRef.LUMINITE_ORE, new net.minecraft.item.ItemStack(com.brilliafy.magicstorage.reference.ModItems.FRAGMENT_LUMINITE), 1.5F);

        net.minecraftforge.oredict.OreDictionary.registerOre("oreHellstone", com.brilliafy.magicstorage.reference.ModBlocksRef.HELLSTONE_ORE);
        net.minecraftforge.oredict.OreDictionary.registerOre("oreLuminite", com.brilliafy.magicstorage.reference.ModBlocksRef.LUMINITE_ORE);
        net.minecraftforge.oredict.OreDictionary.registerOre("ingotDemonite", com.brilliafy.magicstorage.reference.ModItems.INGOT_DEMONITE);
        net.minecraftforge.oredict.OreDictionary.registerOre("ingotCrimtane", com.brilliafy.magicstorage.reference.ModItems.INGOT_CRIMTANE);
        net.minecraftforge.oredict.OreDictionary.registerOre("ingotHellstone", com.brilliafy.magicstorage.reference.ModItems.BAR_HELLSTONE);
        net.minecraftforge.oredict.OreDictionary.registerOre("ingotHallowed", com.brilliafy.magicstorage.reference.ModItems.INGOT_HALLOWED);
        net.minecraftforge.oredict.OreDictionary.registerOre("ingotChlorophyte", com.brilliafy.magicstorage.reference.ModItems.BAR_CHLOROPHYTE);
        net.minecraftforge.oredict.OreDictionary.registerOre("ingotLuminite", com.brilliafy.magicstorage.reference.ModItems.BAR_LUMINITE);
    }

    public void postInit(FMLPostInitializationEvent event) {
    }
}
