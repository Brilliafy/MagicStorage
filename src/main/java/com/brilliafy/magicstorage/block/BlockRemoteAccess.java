package com.brilliafy.magicstorage.block;

import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.tile.TileRemoteAccess;
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

public class BlockRemoteAccess extends Block implements ITileEntityProvider {

    public BlockRemoteAccess() {
        super(Material.IRON);
        setRegistryName(ModInfo.MOD_ID, "remote_access");
        setTranslationKey(ModInfo.MOD_ID + ".remote_access");
        setHardness(5.0F); setResistance(10.0F);
        setCreativeTab(com.brilliafy.magicstorage.init.MagicStorageTab.TAB);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileRemoteAccess();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                     EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                     float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileRemoteAccess) {
                TileRemoteAccess remote = (TileRemoteAccess) te;
                if (remote.getLinkedHeartPos() != null) {
                    playerIn.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "Remote Access is linked to a Storage Heart. Use a Remote item to access storage."));
                } else {
                    playerIn.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "Remote Access not linked! Shift+right-click with a Remote to link it."));
                }
            }
        }
        return true;
    }
}
