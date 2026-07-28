package com.brilliafy.magicstorage;

import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = ModInfo.MOD_ID, name = ModInfo.MOD_NAME, version = ModInfo.MOD_VERSION,
     dependencies = "required-after:forge@[14.23.5.2847,);after:jei")
public class MagicStorage {

    public static final String MODID = ModInfo.MOD_ID;

    @Mod.Instance(ModInfo.MOD_ID)
    public static MagicStorage instance;

    @SidedProxy(clientSide = ModInfo.CLIENT_PROXY, serverSide = ModInfo.COMMON_PROXY)
    public static CommonProxy proxy;

    public static final Logger LOGGER = LogManager.getLogger(ModInfo.MOD_ID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Magic Storage preInit");
        com.brilliafy.magicstorage.config.ModConfig.load(event);
        net.minecraftforge.common.ForgeChunkManager.setForcedChunkLoadingCallback(this, new com.brilliafy.magicstorage.tile.ChunkLoadCallback());
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Magic Storage init");
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("Magic Storage postInit");
        proxy.postInit(event);
    }
}
