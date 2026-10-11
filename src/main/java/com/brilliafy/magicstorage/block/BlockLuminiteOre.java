package com.brilliafy.magicstorage.block;

import com.brilliafy.magicstorage.init.MagicStorageTab;
import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.reference.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

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

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return ModItems.FRAGMENT_LUMINITE;
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        if (fortune > 0) {
            int bonus = random.nextInt(fortune + 2) - 1;
            if (bonus < 0) {
                bonus = 0;
            }
            return this.quantityDropped(random) * (bonus + 1);
        } else {
            return this.quantityDropped(random);
        }
    }

    @Override
    public int getExpDrop(IBlockState state, IBlockAccess world, BlockPos pos, int fortune) {
        Random rand = world instanceof World ? ((World) world).rand : new Random();
        return MathHelper.getInt(rand, 3, 7);
    }
}
