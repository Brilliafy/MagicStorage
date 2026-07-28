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
    }

    public void postInit(FMLPostInitializationEvent event) {
    }
}
