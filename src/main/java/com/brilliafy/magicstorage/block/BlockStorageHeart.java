package com.brilliafy.magicstorage.block;

import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

public class BlockStorageHeart extends Block implements ITileEntityProvider {

    public BlockStorageHeart() {
        super(Material.IRON);
        setRegistryName(ModInfo.MOD_ID, "storage_heart");
        setTranslationKey(ModInfo.MOD_ID + ".storage_heart");
        setHardness(5.0F);
        setResistance(10.0F);
        setCreativeTab(com.brilliafy.magicstorage.init.MagicStorageTab.TAB);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileStorageHeart();
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        super.onBlockAdded(worldIn, pos, state);
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileStorageHeart) {
                ((TileStorageHeart) te).markNeedsRefresh();
            }
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileStorageHeart) {
            ((TileStorageHeart) te).markNeedsRefresh();
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                     EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                     float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileStorageHeart) {
                playerIn.openGui(com.brilliafy.magicstorage.MagicStorage.instance,
                    4, worldIn, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileStorageHeart) {
            TileStorageHeart heart = (TileStorageHeart) te;
            heart.disconnectNetwork();
            // Drop the heart's 20-slot inventory (stations)
            ItemStackHandler inv = heart.getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
        super.breakBlock(worldIn, pos, state);
    }
}
