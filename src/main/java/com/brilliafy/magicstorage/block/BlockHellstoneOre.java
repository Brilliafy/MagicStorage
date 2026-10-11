package com.brilliafy.magicstorage.block;

import com.brilliafy.magicstorage.init.MagicStorageTab;
import com.brilliafy.magicstorage.reference.ModInfo;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

public class BlockHellstoneOre extends Block {

    public BlockHellstoneOre() {
        super(Material.ROCK);
        setRegistryName(ModInfo.MOD_ID, "hellstone_ore");
        setTranslationKey(ModInfo.MOD_ID + ".hellstone_ore");
        setHardness(4.0F);
        setResistance(15.0F);
        setHarvestLevel("pickaxe", 2); // Iron pickaxe or better
        setLightLevel(0.35F);
        setSoundType(SoundType.STONE);
        setCreativeTab(MagicStorageTab.TAB);
    }
}
