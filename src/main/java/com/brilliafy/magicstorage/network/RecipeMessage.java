package com.brilliafy.magicstorage.network;

import com.brilliafy.magicstorage.container.ContainerMagicStorageBase;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles JEI recipe transfer - fills the crafting grid from network items.
 */
public class RecipeMessage implements IMessage, IMessageHandler<RecipeMessage, IMessage> {

    private NBTTagCompound nbt;
    private boolean maxTransfer;

    public RecipeMessage() {}
    public RecipeMessage(NBTTagCompound nbt, boolean maxTransfer) {
        this.nbt = nbt;
        this.maxTransfer = maxTransfer;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        nbt = ByteBufUtils.readTag(buf);
        maxTransfer = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, nbt);
        buf.writeBoolean(maxTransfer);
    }

    @Override
    public IMessage onMessage(final RecipeMessage message, final MessageContext ctx) {
        net.minecraft.entity.player.EntityPlayerMP player = ctx.getServerHandler().player;
        player.getServerWorld().addScheduledTask(() -> {
            if (player.openContainer instanceof ContainerMagicStorageBase) {
                ContainerMagicStorageBase container = (ContainerMagicStorageBase) player.openContainer;
                TileStorageHeart heart = container.getTileMaster();

                // 1. Clear existing grid items: Network -> Player Inventory -> Floor
                for (int i = 0; i < 9; i++) {
                    ItemStack s = container.getCraftMatrix().getStackInSlot(i);
                    if (!s.isEmpty()) {
                        container.getCraftMatrix().setInventorySlotContents(i, ItemStack.EMPTY);
                        ItemStack remainder = heart != null ? heart.insertItem(s, false) : s;
                        if (!remainder.isEmpty()) {
                            boolean added = player.inventory.addItemStackToInventory(remainder);
                            if (!added || !remainder.isEmpty()) {
                                player.dropItem(remainder, false);
                            }
                        }
                    }
                }

                if (heart == null) return;

                // 2. Group recipe slots by candidate ingredient for equal stack distribution
                class IngredientGroup {
                    final ItemStack candidate;
                    final List<Integer> slots = new ArrayList<>();
                    IngredientGroup(ItemStack candidate) { this.candidate = candidate; }
                }

                List<IngredientGroup> groups = new ArrayList<>();
                for (int slot = 0; slot < 9; slot++) {
                    if (message.nbt.hasKey("s" + slot)) {
                        NBTTagList invList = message.nbt.getTagList("s" + slot, 10);
                        if (invList.tagCount() > 0) {
                            NBTTagCompound stackTag = invList.getCompoundTagAt(0);
                            ItemStack candidate = new ItemStack(stackTag);
                            if (!candidate.isEmpty()) {
                                IngredientGroup targetGroup = null;
                                for (IngredientGroup g : groups) {
                                    if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(g.candidate, candidate)) {
                                        targetGroup = g;
                                        break;
                                    }
                                }
                                if (targetGroup == null) {
                                    targetGroup = new IngredientGroup(candidate);
                                    groups.add(targetGroup);
                                }
                                targetGroup.slots.add(slot);
                            }
                        }
                    }
                }

                // Fill each group with equal stack distribution (Priority 1: Network, Priority 2: Player Inventory)
                for (IngredientGroup group : groups) {
                    int slotCount = group.slots.size();
                    int targetPerSlot = message.maxTransfer ? group.candidate.getMaxStackSize() : 1;
                    int totalTargetForGroup = targetPerSlot * slotCount;

                    // Priority 1: Extract total needed from Storage Network
                    ItemStack networkExtracted = heart.extractItem(
                        s -> net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(s, group.candidate),
                        totalTargetForGroup, false);
                    int totalExtracted = networkExtracted.isEmpty() ? 0 : networkExtracted.getCount();

                    // Priority 2: Extract remaining needed from Player Inventory
                    if (totalExtracted < totalTargetForGroup) {
                        int neededFromPlayer = totalTargetForGroup - totalExtracted;
                        for (int slotIdx = 0; slotIdx < player.inventory.mainInventory.size(); slotIdx++) {
                            ItemStack invStack = player.inventory.mainInventory.get(slotIdx);
                            if (!invStack.isEmpty() && net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(invStack, group.candidate)) {
                                int take = Math.min(neededFromPlayer, invStack.getCount());
                                invStack.shrink(take);
                                totalExtracted += take;
                                neededFromPlayer -= take;
                                if (neededFromPlayer <= 0) break;
                            }
                        }
                    }

                    if (totalExtracted > 0) {
                        int basePerSlot = totalExtracted / slotCount;
                        int remainder = totalExtracted % slotCount;

                        for (int i = 0; i < slotCount; i++) {
                            int slotIndex = group.slots.get(i);
                            int countForSlot = basePerSlot + (i < remainder ? 1 : 0);
                            if (countForSlot > 0) {
                                ItemStack slotStack = group.candidate.copy();
                                slotStack.setCount(countForSlot);
                                container.getCraftMatrix().setInventorySlotContents(slotIndex, slotStack);
                            }
                        }
                    }
                }
                container.onCraftMatrixChanged(container.getCraftMatrix());
                container.detectAndSendChanges();
                // Send refresh message so client UI updates network item counts
                List<ItemStack> allItems = heart.getAllItems();
                com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                    new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(allItems, new ArrayList<>()),
                    player);
            }
        });
        return null;
    }
}
