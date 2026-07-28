package com.brilliafy.magicstorage.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.List;

public class ItemBlockStorage extends ItemBlock {

    public ItemBlockStorage(Block block) {
        super(block);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (block instanceof BlockStorageHeart)
            return net.minecraft.util.text.TextFormatting.DARK_RED + super.getItemStackDisplayName(stack);
        if (block instanceof BlockStorageAccess)
            return net.minecraft.util.text.TextFormatting.DARK_GREEN + super.getItemStackDisplayName(stack);
        if (block instanceof BlockCraftingAccess)
            return net.minecraft.util.text.TextFormatting.DARK_AQUA + super.getItemStackDisplayName(stack);
        if (block instanceof BlockRemoteAccess)
            return net.minecraft.util.text.TextFormatting.DARK_PURPLE + super.getItemStackDisplayName(stack);
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public void addInformation(ItemStack stack, World playerIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag advanced) {
        if (block instanceof BlockStorageHeart) {
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_RED + "" + net.minecraft.util.text.TextFormatting.BOLD + "Network Core");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "The heart of your storage network");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Place Storage Units and Access points adjacent");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Required for all other Magic Storage blocks");
        } else if (block instanceof BlockStorageAccess) {
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GREEN + "" + net.minecraft.util.text.TextFormatting.BOLD + "Network Access Terminal");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Full inventory access with search & sort");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "View items from all connected Storage Units");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Requires a Storage Heart in the network");
        } else if (block instanceof BlockCraftingAccess) {
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_AQUA + "" + net.minecraft.util.text.TextFormatting.BOLD + "Crafting Access Terminal");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Inventory access with built-in crafting grid");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Craft directly from your Storage Units");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Requires a Storage Heart in the network");
        } else if (block instanceof BlockRemoteAccess) {
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_PURPLE + "" + net.minecraft.util.text.TextFormatting.BOLD + "Remote Access Point");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Link point for Portable Access items");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Sneak+right-click with a Portable Access to bind");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Portable remotes work from anywhere (tier dependant)");
        }
    }
}
