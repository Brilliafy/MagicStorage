package com.brilliafy.magicstorage.util;

import codersafterdark.reskillable.api.data.PlayerData;
import codersafterdark.reskillable.api.data.PlayerDataHandler;
import codersafterdark.reskillable.api.data.RequirementHolder;
import codersafterdark.reskillable.api.requirement.Requirement;
import codersafterdark.reskillable.api.requirement.SkillRequirement;
import codersafterdark.reskillable.base.LevelLockHandler;
import com.brilliafy.magicstorage.reference.ModBlocksRef;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.List;

public class ReskillableCraftingHelper {

    public static boolean hasSkillForBlock(EntityPlayer player, Block block) {
        if (!Loader.isModLoaded("reskillable")) return true;
        if (player == null || block == null) return true;
        if (player.isCreative() || player.isSpectator()) return true;

        try {
            return ReskillableBridge.canUse(player, new ItemStack(block));
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean hasSkillForStack(EntityPlayer player, ItemStack stack) {
        if (!Loader.isModLoaded("reskillable")) return true;
        if (player == null || stack.isEmpty()) return true;
        if (player.isCreative() || player.isSpectator()) return true;

        try {
            return ReskillableBridge.canUse(player, stack);
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean checkStackRequirement(EntityPlayer player, ItemStack stack) {
        if (!Loader.isModLoaded("reskillable")) return true;
        if (player == null || stack.isEmpty() || player.isCreative() || player.isSpectator()) return true;

        try {
            if (!hasSkillForStack(player, stack)) {
                ReskillableBridge.enforce(player, stack);
                return false;
            }
        } catch (Throwable ignored) {}
        return true;
    }

    /**
     * Checks if all network components (held remote, access block, storage heart,
     * station items inside the heart, and connected storage units) satisfy player skill requirements.
     * If any requirement is NOT met, triggers Reskillable's locked item prompt and sends a chat error.
     *
     * @return true if player meets all requirements, false if blocked.
     */
    public static boolean checkNetworkRequirements(EntityPlayer player, TileStorageHeart heart, ItemStack remoteStack, Block accessBlock) {
        if (!Loader.isModLoaded("reskillable")) return true;
        if (player == null || player.isCreative() || player.isSpectator()) return true;

        try {
            // 1. Check held remote
            if (remoteStack != null && !remoteStack.isEmpty()) {
                if (!hasSkillForStack(player, remoteStack)) {
                    ReskillableBridge.enforce(player, remoteStack);
                    return false;
                }
            }

            // 2. Check access block
            if (accessBlock != null) {
                ItemStack accessStack = new ItemStack(accessBlock);
                if (!hasSkillForStack(player, accessStack)) {
                    ReskillableBridge.enforce(player, accessStack);
                    return false;
                }
            }

            // 3. Check heart and connected network components
            if (heart != null) {
                ItemStack heartStack = new ItemStack(ModBlocksRef.STORAGE_HEART);
                if (!hasSkillForStack(player, heartStack)) {
                    ReskillableBridge.enforce(player, heartStack);
                    return false;
                }

                // Check all connected storage units
                if (heart.getWorld() != null) {
                    for (BlockPos unitPos : heart.getConnectedUnitPositions()) {
                        if (heart.getWorld().isBlockLoaded(unitPos)) {
                            IBlockState state = heart.getWorld().getBlockState(unitPos);
                            net.minecraft.tileentity.TileEntity te = heart.getWorld().getTileEntity(unitPos);
                            int meta = (te instanceof com.brilliafy.magicstorage.tile.TileStorageUnit)
                                ? ((com.brilliafy.magicstorage.tile.TileStorageUnit) te).getTier()
                                : state.getBlock().getMetaFromState(state);
                            ItemStack unitStack = new ItemStack(state.getBlock(), 1, meta);
                            if (!unitStack.isEmpty() && !hasSkillForStack(player, unitStack)) {
                                ReskillableBridge.enforce(player, unitStack);
                                return false;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return true;
    }

    public static ItemStack applySkillLockTooltip(ItemStack original) {
        if (original.isEmpty()) return original;
        ItemStack display = original.copy();
        NBTTagCompound rootTag = display.hasTagCompound() ? display.getTagCompound().copy() : new NBTTagCompound();
        NBTTagCompound displayTag = rootTag.getCompoundTag("display");
        if (displayTag == null) displayTag = new NBTTagCompound();

        NBTTagList lore = displayTag.hasKey("Lore", 9) ? displayTag.getTagList("Lore", 8).copy() : new NBTTagList();
        lore.appendTag(new NBTTagString(""));
        lore.appendTag(new NBTTagString("\u00A7c\u00A7l\u2716    Insufficient Skill Level"));

        displayTag.setTag("Lore", lore);
        rootTag.setTag("display", displayTag);
        display.setTagCompound(rootTag);
        return display;
    }

    private static class ReskillableBridge {
        private static boolean canUse(EntityPlayer player, ItemStack stack) {
            if (stack == null || stack.isEmpty()) return true;
            return LevelLockHandler.canPlayerUseItem(player, stack);
        }

        private static void enforce(EntityPlayer player, ItemStack stack) {
            LevelLockHandler.tellPlayer(player, stack, "reskillable.misc.locked.block_use");
            RequirementHolder holder = LevelLockHandler.getSkillLock(stack);
            PlayerData data = PlayerDataHandler.get(player);
            List<String> unachieved = new ArrayList<>();
            for (Requirement req : holder.getRequirements()) {
                if (req != null && !req.achievedByPlayer(player)) {
                    if (req instanceof SkillRequirement) {
                        SkillRequirement sr = (SkillRequirement) req;
                        unachieved.add(sr.getSkill().getName() + " " + sr.getLevel());
                    } else if (data != null) {
                        String s = req.getToolTip(data).replaceAll("\u00A7[0-9a-fk-or]", "").trim();
                        if (!s.isEmpty()) unachieved.add(s);
                    }
                }
            }
            String reqStr = unachieved.isEmpty() ? "required skill(s)" : String.join(", ", unachieved);
            player.sendMessage(new TextComponentString(
                "\u00A7c[Magic Storage] Network locked! Component (\u00A7e" + stack.getDisplayName() + "\u00A7c) requires \u00A7e" + reqStr + "\u00A7c in order to use the network."));
        }
    }
}
