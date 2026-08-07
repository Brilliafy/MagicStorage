package com.brilliafy.magicstorage.network;

import com.brilliafy.magicstorage.item.ItemPortableAccess;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Sent from client when the "Open Remote" keybind is pressed.
 * Finds the first remote in the player's inventory and opens its GUI.
 */
public class OpenRemoteKeyMessage implements IMessage {

    public OpenRemoteKeyMessage() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<OpenRemoteKeyMessage, IMessage> {
        @Override
        public IMessage onMessage(OpenRemoteKeyMessage msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                boolean opened = false;
                for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                    ItemStack stack = player.inventory.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof ItemPortableAccess) {
                        ItemPortableAccess remoteItem = (ItemPortableAccess) stack.getItem();
                        if (remoteItem.tryOpenGui(player.world, player, stack, i, Math.min(stack.getMetadata(), 2))) {
                            opened = true;
                            break;
                        }
                    }
                }
                if (!opened) {
                    player.sendMessage(new TextComponentString("§c[Magic Storage] No bound remote access item found in inventory!"));
                }
            });
            return null;
        }
    }
}
