package com.brilliafy.magicstorage.block;

import com.brilliafy.magicstorage.init.MagicStorageTab;
import com.brilliafy.magicstorage.reference.ModInfo;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

public class BlockLuminiteOre extends Block {

    public BlockLuminiteOre() {
        super(Material.ROCK);
        setRegistryName(ModInfo.MOD_ID, "luminite_ore");
        setTranslationKey(ModInfo.MOD_ID + ".luminite_ore");
        setHardness(5.0F);
        setResistance(20.0F);
        setHarvestLevel("pickaxe", 3); // Diamond pickaxe or better
        setLightLevel(0.5F);
        setSoundType(SoundType.STONE);
        setCreativeTab(MagicStorageTab.TAB);
    }
}
