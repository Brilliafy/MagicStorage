package com.brilliafy.magicstorage.block;

import com.brilliafy.magicstorage.reference.ModInfo;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockHellBrick extends Block {

    public BlockHellBrick() {
        super(Material.ROCK);
        setRegistryName(ModInfo.MOD_ID, "hell_brick");
        setTranslationKey(ModInfo.MOD_ID + ".hell_brick");
        setHardness(2.0F);
        setResistance(10.0F);
        setSoundType(SoundType.STONE);
        setCreativeTab(com.brilliafy.magicstorage.init.MagicStorageTab.TAB);
    }

    @Override
    public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn) {
        if (entityIn instanceof EntityLivingBase && !entityIn.isImmuneToFire()) {
            EntityLivingBase living = (EntityLivingBase) entityIn;
            if (!living.isPotionActive(MobEffects.FIRE_RESISTANCE)) {
                living.setFire(3);
            }
        }
    }
}
