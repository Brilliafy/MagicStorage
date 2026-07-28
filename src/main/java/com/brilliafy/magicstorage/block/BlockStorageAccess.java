package com.brilliafy.magicstorage.block;

import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.tile.TileStorageAccess;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockStorageAccess extends Block implements ITileEntityProvider {

    public BlockStorageAccess() {
        super(Material.IRON);
        setRegistryName(ModInfo.MOD_ID, "storage_access");
        setTranslationKey(ModInfo.MOD_ID + ".storage_access");
        setHardness(5.0F); setResistance(10.0F);
        setCreativeTab(com.brilliafy.magicstorage.init.MagicStorageTab.TAB);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileStorageAccess();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                     EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                     float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            playerIn.openGui(com.brilliafy.magicstorage.MagicStorage.instance,
                0, worldIn, pos.getX(), pos.getY(), pos.getZ()); // GUI ID 0 = STORAGE_ACCESS
        }
        return true;
    }
}
