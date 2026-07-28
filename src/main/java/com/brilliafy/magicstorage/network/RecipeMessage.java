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

    public RecipeMessage() {}
    public RecipeMessage(NBTTagCompound nbt) { this.nbt = nbt; }

    @Override
    public void fromBytes(ByteBuf buf) { nbt = ByteBufUtils.readTag(buf); }

    @Override
    public void toBytes(ByteBuf buf) { ByteBufUtils.writeTag(buf, nbt); }

    @Override
    public IMessage onMessage(final RecipeMessage message, final MessageContext ctx) {
        ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
            if (ctx.getServerHandler().player.openContainer instanceof ContainerMagicStorageBase) {
                ContainerMagicStorageBase container = (ContainerMagicStorageBase) ctx.getServerHandler().player.openContainer;
                TileStorageHeart heart = container.getTileMaster();
                if (heart == null) return;

                // Clear existing grid
                for (int i = 0; i < 9; i++) {
                    ItemStack s = container.getCraftMatrix().getStackInSlot(i);
                    if (!s.isEmpty()) {
                        ItemStack remainder = heart.insertItem(s, false);
                        if (!remainder.isEmpty()) ctx.getServerHandler().player.dropItem(remainder, false);
                    }
                }

                // Fill from recipe data
                for (int slot = 0; slot < 9; slot++) {
                    if (message.nbt.hasKey("s" + slot)) {
                        NBTTagList invList = message.nbt.getTagList("s" + slot, 10);
                        for (int i = 0; i < invList.tagCount(); i++) {
                            NBTTagCompound stackTag = invList.getCompoundTagAt(i);
                            ItemStack stack = new ItemStack(stackTag);
                            if (!stack.isEmpty()) {
                                // Try to extract from network
                                ItemStack extracted = heart.extractItem(
                                    s -> net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(s, stack),
                                    stack.getCount(), false);
                                if (!extracted.isEmpty()) {
                                    container.getCraftMatrix().setInventorySlotContents(slot, extracted);
                                    break;
                                }
                            }
                        }
                    }
                }
                container.onCraftMatrixChanged(container.getCraftMatrix());
                container.detectAndSendChanges();
            }
        });
        return null;
    }
}
