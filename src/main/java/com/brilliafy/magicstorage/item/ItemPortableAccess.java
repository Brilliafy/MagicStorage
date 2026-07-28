package com.brilliafy.magicstorage.item;

import com.brilliafy.magicstorage.tile.TileRemoteAccess;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.List;

public class ItemPortableAccess extends Item {

    public static final String[] TIER_NAMES = {"PreHM", "HM", "Ultimate"};
    public static final int[] RANGES = {200, -1, -1};
    public static final boolean[] CROSS_DIM = {false, false, true};

    private final boolean isCrafting;

    public ItemPortableAccess(boolean isCrafting) {
        this.isCrafting = isCrafting;
        setMaxStackSize(1);
        setHasSubtypes(true);
        setTranslationKey("portable_access");
        setCreativeTab(com.brilliafy.magicstorage.init.MagicStorageTab.TAB);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return net.minecraft.util.text.TextFormatting.GOLD + super.getItemStackDisplayName(stack);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (tab == com.brilliafy.magicstorage.init.MagicStorageTab.TAB || tab == CreativeTabs.SEARCH) {
            for (int i = 0; i < 3; i++) {
                ItemStack stack = new ItemStack(this, 1, i);
                items.add(stack);
            }
        }
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return super.getTranslationKey() + "_" + TIER_NAMES[Math.min(stack.getMetadata(), 2)];
    }

    @Override
    public void addInformation(ItemStack stack, World playerIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag advanced) {
        if (stack.hasTagCompound() && stack.getTagCompound().getBoolean("bound")) {
            int x = stack.getTagCompound().getInteger("x");
            int y = stack.getTagCompound().getInteger("y");
            int z = stack.getTagCompound().getInteger("z");
            tooltip.add(net.minecraft.util.text.TextFormatting.GREEN + "Bound to: " + x + ", " + y + ", " + z);
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Dimension: " + stack.getTagCompound().getInteger("dim"));
        } else {
            tooltip.add(net.minecraft.util.text.TextFormatting.RED + "Unlinked");
            tooltip.add(net.minecraft.util.text.TextFormatting.DARK_GRAY + "Sneak+click on a Remote Access block to bind");
        }
        String range;
        switch (Math.min(stack.getMetadata(), 2)) {
            case 0: range = "200 blocks (same dim)"; break;
            case 1: range = "Unlimited (same dim)"; break;
            case 2: range = "Cross-dimensional"; break;
            default: range = "Unknown"; break;
        }
        tooltip.add(net.minecraft.util.text.TextFormatting.GRAY + "Range: " + range);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        int meta = Math.min(stack.getMetadata(), 2);
        if (!stack.hasTagCompound() || !stack.getTagCompound().getBoolean("bound")) {
            player.sendMessage(new TextComponentString("Remote not linked. Sneak+click on a Remote Access block to link."));
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        int slot = hand == EnumHand.OFF_HAND ? player.inventory.getSizeInventory() - 1 : player.inventory.currentItem;
        if (tryOpenGui(world, player, stack, slot, meta))
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    private boolean tryOpenGui(World world, EntityPlayer player, ItemStack remote, int slot, int meta) {
        if (!remote.hasTagCompound()) return false;
        int x = remote.getTagCompound().getInteger("x");
        int y = remote.getTagCompound().getInteger("y");
        int z = remote.getTagCompound().getInteger("z");
        int itemDim = remote.getTagCompound().getInteger("dim");

        // Validate sort defaults (like SSN)
        if (!remote.getTagCompound().hasKey("sort")) {
            remote.getTagCompound().setString("sort", "NAME");
        }

        BlockPos targetPos = new BlockPos(x, y, z);
        WorldServer targetWorld = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(itemDim);
        if (targetWorld == null) {
            player.sendMessage(new TextComponentString("Target dimension not loaded"));
            return false;
        }

        // Load chunk before checking tile (like SSN)
        if (!targetWorld.getChunk(targetPos).isLoaded()) {
            targetWorld.getChunk(targetPos);
        }

        // Check the remote access block still exists
        if (!(targetWorld.getTileEntity(targetPos) instanceof TileRemoteAccess)) {
            player.sendMessage(new TextComponentString("Remote Access block missing — place it back at the linked position"));
            return false;
        }
        TileRemoteAccess remoteTile = (TileRemoteAccess) targetWorld.getTileEntity(targetPos);
        // Check the remote access block has at least one heart in its network
        if (remoteTile.getLinkedHeartPos() == null || !(targetWorld.getTileEntity(remoteTile.getLinkedHeartPos()) instanceof TileStorageHeart)) {
            player.sendMessage(new TextComponentString("Remote is unlinked from the network — connect a Storage Heart to the network chain"));
            return false;
        }

        boolean sameDim = (itemDim == world.provider.getDimension());
        boolean inRange = player.getDistance(x, y, z) <= RANGES[meta];
        boolean canOpen = false;

        switch (meta) {
            case 0: canOpen = sameDim && inRange; break;
            case 1: canOpen = sameDim; break;
            case 2: canOpen = true; break;
        }

        if (!canOpen) {
            if (meta == 0 && !sameDim)
                player.sendMessage(new TextComponentString("PreHM remote only works in the same dimension"));
            else if (meta == 1 && !sameDim)
                player.sendMessage(new TextComponentString("HM remote only works in the same dimension"));
            else
                player.sendMessage(new TextComponentString("Remote out of range"));
            return false;
        }

        int guiId = isCrafting ? 5 : 6;
        player.openGui(com.brilliafy.magicstorage.MagicStorage.instance, guiId, world, slot, y, meta);
        return true;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                       EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return EnumActionResult.SUCCESS;
        ItemStack stack = player.getHeldItem(hand);
        if (world.getTileEntity(pos) instanceof TileRemoteAccess) {
            TileRemoteAccess remoteTile = (TileRemoteAccess) world.getTileEntity(pos);
            if (remoteTile.getLinkedHeartPos() == null) {
                player.sendMessage(new TextComponentString("Remote Access has no Storage Heart connected!"));
                return EnumActionResult.SUCCESS;
            }
            if (!stack.hasTagCompound()) stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
            stack.getTagCompound().setInteger("x", pos.getX());
            stack.getTagCompound().setInteger("y", pos.getY());
            stack.getTagCompound().setInteger("z", pos.getZ());
            stack.getTagCompound().setInteger("dim", world.provider.getDimension());
            stack.getTagCompound().setBoolean("bound", true);
            stack.getTagCompound().setString("sort", "NAME");
            player.sendMessage(new TextComponentString("Linked to Remote Access @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    /** SSN-style cross-dim heart lookup via the remote access block */
    public static TileStorageHeart getHeart(ItemStack stack, World playerWorld) {
        if (stack.isEmpty() || !stack.hasTagCompound() || FMLCommonHandler.instance().getMinecraftServerInstance() == null)
            return null;
        if (!stack.getTagCompound().getBoolean("bound")) return null;
        BlockPos pos = new BlockPos(
            stack.getTagCompound().getInteger("x"),
            stack.getTagCompound().getInteger("y"),
            stack.getTagCompound().getInteger("z"));
        WorldServer world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(stack.getTagCompound().getInteger("dim"));
        if (world == null) return null;
        if (world.getTileEntity(pos) instanceof TileRemoteAccess) {
            TileRemoteAccess remoteTile = (TileRemoteAccess) world.getTileEntity(pos);
            BlockPos heartPos = remoteTile.getLinkedHeartPos();
            if (heartPos != null && world.getTileEntity(heartPos) instanceof TileStorageHeart)
                return (TileStorageHeart) world.getTileEntity(heartPos);
        }
        return null;
    }
}
