package com.brilliafy.magicstorage.item;

import com.brilliafy.magicstorage.block.BlockStorageUnit;
import com.brilliafy.magicstorage.init.MagicStorageTab;
import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.tile.TileStorageUnit;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class ItemUpgrade extends Item {

    private final int targetTier;

    public ItemUpgrade(String name, int targetTier) {
        setRegistryName(new ResourceLocation(ModInfo.MOD_ID, name));
        setTranslationKey(ModInfo.MOD_ID + "." + name);
        setCreativeTab(MagicStorageTab.TAB);
        setMaxStackSize(1);
        this.targetTier = targetTier;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return net.minecraft.util.text.TextFormatting.GOLD + super.getItemStackDisplayName(stack);
    }

    public int getTargetTier() {
        return targetTier;
    }

    @Override
    public void addInformation(ItemStack stack, net.minecraft.world.World playerIn, java.util.List<String> tooltip, net.minecraft.client.util.ITooltipFlag advanced) {
        if (targetTier >= 0 && targetTier < com.brilliafy.magicstorage.tile.TileStorageUnit.TIER_NAMES.length) {
            tooltip.add(net.minecraft.util.text.TextFormatting.GOLD + "Upgrade to " + com.brilliafy.magicstorage.tile.TileStorageUnit.TIER_NAMES[targetTier]);
            if (targetTier > 0) {
                tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Requires: " + com.brilliafy.magicstorage.tile.TileStorageUnit.TIER_NAMES[targetTier - 1] + " Storage Unit");
            }
            int slots = com.brilliafy.magicstorage.tile.TileStorageUnit.TIER_SLOT_COUNTS[targetTier];
            tooltip.add(net.minecraft.util.text.TextFormatting.GRAY + "Capacity: " + slots + " slots (" + (slots / 9) + " rows)");
        }
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
                                       EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) return EnumActionResult.SUCCESS;

        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileStorageUnit)) {
            return EnumActionResult.PASS;
        }

        TileStorageUnit unit = (TileStorageUnit) te;

        if (!unit.canUpgradeTo(targetTier)) {
            player.sendMessage(new TextComponentString(
                "Cannot upgrade to " + TileStorageUnit.TIER_NAMES[targetTier] + " from " + unit.getTierName()));
            return EnumActionResult.FAIL;
        }
        // Save items BEFORE the upgrade (setBlockState may recreate the tile entity)
        int oldSlots = unit.getSlotCount();
        net.minecraftforge.items.ItemStackHandler oldInv = unit.getInventory();
        java.util.List<ItemStack> savedItems = new java.util.ArrayList<>();
        for (int i = 0; i < oldSlots; i++) {
            savedItems.add(oldInv.getStackInSlot(i).copy());
        }

        // Upgrade the unit
        unit.setTier(targetTier);

        // Update block metadata to match new tier (this updates the texture!)
        IBlockState newState = worldIn.getBlockState(pos)
            .withProperty(BlockStorageUnit.VARIANT, BlockStorageUnit.StorageUnitType.values()[targetTier]);
        worldIn.setBlockState(pos, newState, 2);

        // Get the tile entity again (setBlockState may have recreated it)
        TileEntity newTe = worldIn.getTileEntity(pos);
        if (newTe instanceof TileStorageUnit) {
            TileStorageUnit newUnit = (TileStorageUnit) newTe;
            int newSlots = newUnit.getSlotCount();
            net.minecraftforge.items.ItemStackHandler newInv = newUnit.getInventory();
            for (int i = 0; i < Math.min(oldSlots, newSlots) && i < savedItems.size(); i++) {
                ItemStack saved = savedItems.get(i);
                if (!saved.isEmpty()) {
                    newInv.setStackInSlot(i, saved);
                }
            }
        }

        // Play equip sound (Ender eye placement sound)
        worldIn.playSound(null, pos, net.minecraft.init.SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);

        // Consume upgrade item
        if (!player.isCreative()) {
            ItemStack held = player.getHeldItem(hand);
            held.shrink(1);
            player.inventory.markDirty();
            if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                ((net.minecraft.entity.player.EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
        }

        int finalSlots = (newTe instanceof TileStorageUnit) ? ((TileStorageUnit) newTe).getSlotCount() : unit.getSlotCount();

        player.sendMessage(new net.minecraft.util.text.TextComponentString(
            "Upgraded to " + TileStorageUnit.TIER_NAMES[targetTier] + " (" + finalSlots + " slots)"));

        return EnumActionResult.SUCCESS;
    }
}
