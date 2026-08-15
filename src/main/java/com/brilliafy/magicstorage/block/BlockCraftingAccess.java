package com.brilliafy.magicstorage.block;

import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.tile.TileCraftingAccess;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class BlockCraftingAccess extends Block implements ITileEntityProvider {

    public BlockCraftingAccess() {
        super(Material.IRON);
        setRegistryName(ModInfo.MOD_ID, "crafting_access");
        setTranslationKey(ModInfo.MOD_ID + ".crafting_access");
        setHardness(5.0F); setResistance(10.0F);
        setCreativeTab(com.brilliafy.magicstorage.init.MagicStorageTab.TAB);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileCraftingAccess();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                     EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                     float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return true;
        if (!worldIn.isRemote) {
            TileCraftingAccess ca = (TileCraftingAccess) worldIn.getTileEntity(pos);
            if (ca != null) {
                TileStorageHeart heart = ca.findHeart();
                if (!com.brilliafy.magicstorage.util.ReskillableCraftingHelper.checkNetworkRequirements(playerIn, heart, ItemStack.EMPTY, this)) {
                    return true;
                }
                if (heart != null && heart.hasCraftingTable()) {
                    playerIn.openGui(com.brilliafy.magicstorage.MagicStorage.instance,
                        1, worldIn, pos.getX(), pos.getY(), pos.getZ()); // GUI ID 1 = CRAFTING_ACCESS
                } else {
                    playerIn.sendMessage(new TextComponentString("§c[Magic Storage] No crafting table found in the Storage Heart! Place a crafting table in the heart's inventory."));
                }
            }
        }
        return true;
    }
}
