package com.brilliafy.magicstorage.item;

import com.brilliafy.magicstorage.tile.TileStorageUnit;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class ItemBlockStorageUnit extends ItemBlock {

    public ItemBlockStorageUnit(net.minecraft.block.Block block) {
        super(block);
        setRegistryName(block.getRegistryName());
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return net.minecraft.util.text.TextFormatting.GOLD + super.getItemStackDisplayName(stack);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world,
                                 BlockPos pos, EnumFacing side, float hitX, float hitY,
                                 float hitZ, IBlockState newState) {
        if (!world.setBlockState(pos, newState, 3)) return false;
        net.minecraft.tileentity.TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileStorageUnit) {
            ((TileStorageUnit) te).setTier(stack.getMetadata());
        }
        return true;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return super.getTranslationKey() + "_" + stack.getMetadata();
    }

    @Override
    public void addInformation(ItemStack stack, World playerIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag advanced) {
        int meta = stack.getMetadata();
        if (meta >= 0 && meta < TileStorageUnit.TIER_SLOT_COUNTS.length) {
            int slots = TileStorageUnit.TIER_SLOT_COUNTS[meta];
            int rows = slots / 9;
            tooltip.add(net.minecraft.util.text.TextFormatting.GOLD + TileStorageUnit.TIER_NAMES[meta] + net.minecraft.util.text.TextFormatting.GRAY + " - " + slots + " slots (" + rows + " rows)");
        }
        tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Place adjacent to a Storage Heart to join the network");
        tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Right-click to access inventory");
        tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Can be upgraded with upgrade items");
    }
}
