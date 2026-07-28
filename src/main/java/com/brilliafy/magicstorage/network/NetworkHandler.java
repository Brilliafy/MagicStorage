package com.brilliafy.magicstorage.network;

import com.brilliafy.magicstorage.gui.IStorageContainer;
import com.brilliafy.magicstorage.reference.ModInfo;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;

public class NetworkHandler {

    public static SimpleNetworkWrapper INSTANCE;
    private static int packetId = 0;

    public static void init() {
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(ModInfo.MOD_ID);
        INSTANCE.registerMessage(com.brilliafy.magicstorage.network.RecipeMessage.class, com.brilliafy.magicstorage.network.RecipeMessage.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(RequestMessage.Handler.class, RequestMessage.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(InsertMessage.Handler.class, InsertMessage.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(SortMessage.Handler.class, SortMessage.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(ClearRecipeMessage.Handler.class, ClearRecipeMessage.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(StackRefreshClientMessage.Handler.class, StackRefreshClientMessage.class, packetId++, Side.CLIENT);
        INSTANCE.registerMessage(OpenGuiMessage.Handler.class, OpenGuiMessage.class, packetId++, Side.CLIENT);
        INSTANCE.registerMessage(StackResponseClientMessage.Handler.class, StackResponseClientMessage.class, packetId++, Side.CLIENT);
    }

    // === Helpers ===
    private static ItemStack readItemStack(ByteBuf buf) {
        if (!buf.readBoolean()) return ItemStack.EMPTY;
        short len = buf.readShort();
        if (len < 0) return ItemStack.EMPTY;
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        try {
            net.minecraft.nbt.NBTTagCompound tag = net.minecraft.nbt.CompressedStreamTools.readCompressed(new java.io.ByteArrayInputStream(bytes));
            ItemStack stack = new ItemStack(tag);
            stack.setCount(buf.readInt());
            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static void writeItemStack(ByteBuf buf, ItemStack stack) {
        if (stack.isEmpty()) {
            buf.writeBoolean(false);
            return;
        }
        buf.writeBoolean(true);
        net.minecraft.nbt.NBTTagCompound tag = stack.writeToNBT(new net.minecraft.nbt.NBTTagCompound());
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.CompressedStreamTools.writeCompressed(tag, baos);
            byte[] bytes = baos.toByteArray();
            buf.writeShort(bytes.length);
            buf.writeBytes(bytes);
            buf.writeInt(stack.getCount());
        } catch (Exception e) {
            buf.writeShort(-1);
        }
    }

    private static String readString(ByteBuf buf) {
        int len = buf.readInt();
        if (len < 0) return "";
        byte[] b = new byte[len];
        buf.readBytes(b);
        return new String(b, java.nio.charset.StandardCharsets.UTF_8);
    }
    private static void writeString(ByteBuf buf, String s) {
        byte[] b = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeInt(b.length);
        buf.writeBytes(b);
    }

    public static class RequestMessage implements IMessage {
        public int mouseButton, dim;
        public ItemStack stack = ItemStack.EMPTY;
        public boolean shift, ctrl;
        public RequestMessage() {}
        public RequestMessage(int mouseButton, ItemStack stack, boolean shift, boolean ctrl) {
            this.mouseButton = mouseButton; this.stack = stack; this.shift = shift; this.ctrl = ctrl;
        }
        @Override public void fromBytes(ByteBuf buf) {
            mouseButton = buf.readInt(); shift = buf.readBoolean(); ctrl = buf.readBoolean(); dim = buf.readInt();
            stack = readItemStack(buf);
        }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeInt(mouseButton); buf.writeBoolean(shift); buf.writeBoolean(ctrl); buf.writeInt(dim);
            writeItemStack(buf, stack);
        }
        public static class Handler implements IMessageHandler<RequestMessage, IMessage> {
            @Override public IMessage onMessage(RequestMessage msg, MessageContext ctx) {
                net.minecraft.entity.player.EntityPlayerMP player = ctx.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> {
                    if (player.openContainer instanceof com.brilliafy.magicstorage.gui.IStorageContainer) {
                        com.brilliafy.magicstorage.gui.IStorageContainer container = (com.brilliafy.magicstorage.gui.IStorageContainer) player.openContainer;
                        com.brilliafy.magicstorage.tile.TileStorageHeart heart = null;
                        com.brilliafy.magicstorage.tile.TileStorageUnit unit = null;
                        if (container instanceof com.brilliafy.magicstorage.container.ContainerMagicStorageBase) {
                            heart = ((com.brilliafy.magicstorage.container.ContainerMagicStorageBase) container).getTileMaster();
                        } else if (container instanceof com.brilliafy.magicstorage.container.ContainerStorageAccess) {
                            heart = ((com.brilliafy.magicstorage.container.ContainerStorageAccess) container).getAccessTile().findHeart();
                        } else if (container instanceof com.brilliafy.magicstorage.container.ContainerStorageUnit) {
                            unit = ((com.brilliafy.magicstorage.container.ContainerStorageUnit) container).getUnit();
                        }

                        // Empty stack signals a refresh request (from GUI init)
                        if (msg.stack.isEmpty()) {
                            if (heart != null) {
                                List<ItemStack> allItems = heart.getAllItems();
                                com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                                    new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(allItems, new ArrayList<>()),
                                    player);
                            } else if (unit != null) {
                                List<ItemStack> items = new ArrayList<>();
                                for (int i = 0; i < unit.getSlotCount(); i++) {
                                    ItemStack s = unit.getInventory().getStackInSlot(i);
                                    if (!s.isEmpty()) {
                                        boolean found = false;
                                        for (ItemStack existing : items) {
                                            if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(existing, s)) {
                                                existing.grow(s.getCount());
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) items.add(s.copy());
                                    }
                                }
                                com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                                    new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(items, new ArrayList<>()),
                                    player);
                            }
                            return;
                        }

                        if (heart == null && unit == null) return;

                        // Determine how many to extract
                        int sizeRequested;
                        if (msg.ctrl) {
                            sizeRequested = 1;
                        } else if (msg.mouseButton == 0) {  // left click
                            sizeRequested = msg.stack.getMaxStackSize();
                        } else {  // right click
                            sizeRequested = Math.max(1, msg.stack.getMaxStackSize() / 2);
                        }

                        ItemStack result;
                        if (heart != null) {
                            result = heart.extractItem(
                                s -> net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(s, msg.stack),
                                sizeRequested, false);
                            // Refresh crafting result (enchanting power depends on network items)
                            if (container instanceof com.brilliafy.magicstorage.container.ContainerMagicStorageBase) {
                                com.brilliafy.magicstorage.container.ContainerMagicStorageBase base = (com.brilliafy.magicstorage.container.ContainerMagicStorageBase) container;
                                base.onCraftMatrixChanged(base.getCraftMatrix());
                                // Force-send result slot to client
                                if (base.getResult() != null) {
                                    net.minecraft.item.ItemStack resultStack = base.getResult().getStackInSlot(0);
                                    player.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(base.windowId, 0, resultStack));
                                }
                            }
                        } else {
                            result = unit.extractItem(
                                s -> net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(s, msg.stack),
                                sizeRequested, false);
                        }

                        if (!result.isEmpty()) {
                            if (msg.shift) {
                                net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(player, result);
                            } else {
                                player.inventory.setItemStack(result);
                                com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                                    new com.brilliafy.magicstorage.network.NetworkHandler.StackResponseClientMessage(result),
                                    player);
                            }
                        }

                        // Sync updated item list back to client
                        if (heart != null) {
                            List<ItemStack> allItems = heart.getAllItems();
                            com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                                new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(allItems, new ArrayList<>()),
                                player);
                        } else if (unit != null) {
                            List<ItemStack> items = new ArrayList<>();
                            for (int i = 0; i < unit.getSlotCount(); i++) {
                                ItemStack s = unit.getInventory().getStackInSlot(i);
                                if (!s.isEmpty()) {
                                    boolean found = false;
                                    for (ItemStack existing : items) {
                                        if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(existing, s)) {
                                            existing.grow(s.getCount());
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) items.add(s.copy());
                                }
                            }
                            com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                                new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(items, new ArrayList<>()),
                                player);
                        }
                        player.openContainer.detectAndSendChanges();
                    }
                });
                return null;
            }
        }
    }

    public static class InsertMessage implements IMessage {
        public int dim, mouseButton;
        public InsertMessage() {}
        public InsertMessage(int dim, int mouseButton) { this.dim = dim; this.mouseButton = mouseButton; }
        @Override public void fromBytes(ByteBuf buf) { dim = buf.readInt(); mouseButton = buf.readInt(); }
        @Override public void toBytes(ByteBuf buf) { buf.writeInt(dim); buf.writeInt(mouseButton); }
        public static class Handler implements IMessageHandler<InsertMessage, IMessage> {
            @Override public IMessage onMessage(InsertMessage msg, MessageContext ctx) {
                net.minecraft.entity.player.EntityPlayerMP player = ctx.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> {
                    if (player.openContainer instanceof com.brilliafy.magicstorage.gui.IStorageContainer) {
                        com.brilliafy.magicstorage.gui.IStorageContainer container = (com.brilliafy.magicstorage.gui.IStorageContainer) player.openContainer;
                        com.brilliafy.magicstorage.tile.TileStorageHeart heart = null;
                        com.brilliafy.magicstorage.tile.TileStorageUnit unit = null;
                        if (container instanceof com.brilliafy.magicstorage.container.ContainerMagicStorageBase) {
                            heart = ((com.brilliafy.magicstorage.container.ContainerMagicStorageBase) container).getTileMaster();
                        } else if (container instanceof com.brilliafy.magicstorage.container.ContainerStorageAccess) {
                            heart = ((com.brilliafy.magicstorage.container.ContainerStorageAccess) container).getAccessTile().findHeart();
                        } else if (container instanceof com.brilliafy.magicstorage.container.ContainerStorageUnit) {
                            unit = ((com.brilliafy.magicstorage.container.ContainerStorageUnit) container).getUnit();
                        }
                        if (heart == null && unit == null) return;
                        ItemStack send = ItemStack.EMPTY;
                        ItemStack stackCarriedByMouse = player.inventory.getItemStack();
                        if (heart != null) {
                            if (msg.mouseButton == 0) {
                                // Left-click: insert entire held stack
                                ItemStack remainder = heart.insertItem(stackCarriedByMouse, false);
                                send = remainder;
                            } else if (msg.mouseButton == 1) {
                                // Right-click: insert 1 item (SSN pattern)
                                if (!stackCarriedByMouse.isEmpty()) {
                                    ItemStack oneItem = stackCarriedByMouse.copy();
                                    oneItem.setCount(1);
                                    ItemStack remainOne = heart.insertItem(oneItem, false);
                                    int consumed = remainOne.isEmpty() ? 1 : 0;
                                    int cursorCount = stackCarriedByMouse.getCount() - consumed;
                                    send = cursorCount > 0 ? net.minecraftforge.items.ItemHandlerHelper.copyStackWithSize(stackCarriedByMouse, cursorCount) : ItemStack.EMPTY;
                                }
                            }
                            // Refresh crafting result (enchanting power depends on network items)
                            if (container instanceof com.brilliafy.magicstorage.container.ContainerMagicStorageBase) {
                                com.brilliafy.magicstorage.container.ContainerMagicStorageBase base = (com.brilliafy.magicstorage.container.ContainerMagicStorageBase) container;
                                base.onCraftMatrixChanged(base.getCraftMatrix());
                                // Force-send result slot to client
                                if (base.getResult() != null) {
                                    net.minecraft.item.ItemStack resultStack = base.getResult().getStackInSlot(0);
                                    player.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(base.windowId, 0, resultStack));
                                }
                            }
                        } else {
                            // Unit path
                            net.minecraftforge.items.ItemStackHandler inv = unit.getInventory();
                            if (msg.mouseButton == 0) {
                                // Left-click: insert entire held stack
                                ItemStack toInsert = stackCarriedByMouse.copy();
                                int startCount = toInsert.getCount();
                                for (int i = 0; i < unit.getSlotCount() && !toInsert.isEmpty(); i++) {
                                    toInsert = inv.insertItem(i, toInsert, false);
                                }
                                int inserted = startCount - toInsert.getCount();
                                send = inserted > 0
                                    ? (inserted < stackCarriedByMouse.getCount() ? net.minecraftforge.items.ItemHandlerHelper.copyStackWithSize(stackCarriedByMouse, stackCarriedByMouse.getCount() - inserted) : ItemStack.EMPTY)
                                    : stackCarriedByMouse;
                            } else if (msg.mouseButton == 1) {
                                // Right-click: insert 1 item (SSN pattern)
                                if (!stackCarriedByMouse.isEmpty()) {
                                    ItemStack oneItem = stackCarriedByMouse.copy();
                                    oneItem.setCount(1);
                                    net.minecraftforge.items.ItemStackHandler uinv = unit.getInventory();
                                    for (int i = 0; i < unit.getSlotCount() && !oneItem.isEmpty(); i++) {
                                        oneItem = uinv.insertItem(i, oneItem, false);
                                    }
                                    int consumed = oneItem.isEmpty() ? 1 : 0;
                                    int cursorCount = stackCarriedByMouse.getCount() - consumed;
                                    send = cursorCount > 0 ? net.minecraftforge.items.ItemHandlerHelper.copyStackWithSize(stackCarriedByMouse, cursorCount) : ItemStack.EMPTY;
                                }
                            }
                        }
                        player.inventory.setItemStack(send);
                        com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                            new com.brilliafy.magicstorage.network.NetworkHandler.StackResponseClientMessage(send),
                            player);
                        player.openContainer.detectAndSendChanges();
                        if (heart != null) {
                            List<ItemStack> allItems = heart.getAllItems();
                            com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                                new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(allItems, new ArrayList<>()),
                                player);
                        } else if (unit != null) {
                            List<ItemStack> items = new ArrayList<>();
                            for (int i = 0; i < unit.getSlotCount(); i++) {
                                ItemStack s = unit.getInventory().getStackInSlot(i);
                                if (!s.isEmpty()) {
                                    boolean found = false;
                                    for (ItemStack existing : items) {
                                        if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(existing, s)) {
                                            existing.grow(s.getCount());
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) items.add(s.copy());
                                }
                            }
                            com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                                new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(items, new ArrayList<>()),
                                player);
                        }
                    }
                });
                return null;
            }
        }
    }

    public static class SortMessage implements IMessage {
        public int dim; public boolean down; public String sort;
        public SortMessage() {}
        public SortMessage(int dim, boolean down, String sort) { this.dim = dim; this.down = down; this.sort = sort; }
        @Override public void fromBytes(ByteBuf buf) { dim = buf.readInt(); down = buf.readBoolean(); sort = readString(buf); }
        @Override public void toBytes(ByteBuf buf) { buf.writeInt(dim); buf.writeBoolean(down); writeString(buf, sort); }
        public static class Handler implements IMessageHandler<SortMessage, IMessage> { @Override public IMessage onMessage(SortMessage msg, MessageContext ctx) { return null; } }
    }

    public static class ClearRecipeMessage implements IMessage {
        public ClearRecipeMessage() {}
        @Override public void fromBytes(ByteBuf buf) {}
        @Override public void toBytes(ByteBuf buf) {}
        public static class Handler implements IMessageHandler<ClearRecipeMessage, IMessage> {
            @Override public IMessage onMessage(ClearRecipeMessage msg, MessageContext ctx) {
                net.minecraft.entity.player.EntityPlayerMP player = ctx.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> {
                    if (player.openContainer instanceof com.brilliafy.magicstorage.container.ContainerMagicStorageBase) {
                        com.brilliafy.magicstorage.container.ContainerMagicStorageBase container =
                            (com.brilliafy.magicstorage.container.ContainerMagicStorageBase) player.openContainer;
                        net.minecraft.inventory.InventoryCrafting matrix = container.getCraftMatrix();
                        if (matrix == null) return;
                        com.brilliafy.magicstorage.tile.TileStorageHeart heart = container.getTileMaster();
                        for (int i = 0; i < matrix.getSizeInventory(); i++) {
                            net.minecraft.item.ItemStack s = matrix.getStackInSlot(i);
                            if (!s.isEmpty()) {
                                if (heart != null) {
                                    net.minecraft.item.ItemStack rem = heart.insertItem(s, false);
                                    if (!rem.isEmpty()) player.dropItem(rem, false);
                                } else {
                                    player.dropItem(s, false);
                                }
                                matrix.setInventorySlotContents(i, net.minecraft.item.ItemStack.EMPTY);
                            }
                        }
                        container.onCraftMatrixChanged(matrix);
                        container.detectAndSendChanges();
                        // Refresh items
                        if (heart != null) {
                            java.util.List<ItemStack> allItems = heart.getAllItems();
                            com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                                new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(allItems, new java.util.ArrayList<>()),
                                player);
                        }
                    }
                });
                return null;
            }
        }
    }

    public static class StackRefreshClientMessage implements IMessage {
        public List<ItemStack> stacks = new ArrayList<>();
        public List<ItemStack> craftableStacks = new ArrayList<>();
        public StackRefreshClientMessage() {}
        public StackRefreshClientMessage(List<ItemStack> stacks, List<ItemStack> craftable) {
            this.stacks = stacks; this.craftableStacks = craftable;
        }
        @Override public void fromBytes(ByteBuf buf) {
            int size = buf.readInt(); stacks = new ArrayList<>();
            for (int i = 0; i < size; i++) stacks.add(readItemStack(buf));
            size = buf.readInt(); craftableStacks = new ArrayList<>();
            for (int i = 0; i < size; i++) craftableStacks.add(readItemStack(buf));
        }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeInt(stacks.size());
            for (ItemStack s : stacks) writeItemStack(buf, s);
            buf.writeInt(craftableStacks.size());
            for (ItemStack s : craftableStacks) writeItemStack(buf, s);
        }
        public static class Handler implements IMessageHandler<StackRefreshClientMessage, IMessage> {
            @Override public IMessage onMessage(StackRefreshClientMessage msg, MessageContext ctx) {
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                    // SSN pattern: update GUI directly (currentScreen instanceof IStorageInventory)
                    if (net.minecraft.client.Minecraft.getMinecraft().currentScreen instanceof com.brilliafy.magicstorage.gui.IStorageInventory) {
                        com.brilliafy.magicstorage.gui.IStorageInventory gui = (com.brilliafy.magicstorage.gui.IStorageInventory) net.minecraft.client.Minecraft.getMinecraft().currentScreen;
                        gui.setStacks(msg.stacks);
                        gui.setCraftableStacks(msg.craftableStacks);
                    }
                    // Also update container for fallback
                    if (net.minecraft.client.Minecraft.getMinecraft().player.openContainer instanceof IStorageContainer) {
                        ((IStorageContainer)net.minecraft.client.Minecraft.getMinecraft().player.openContainer).setStacks(msg.stacks);
                        ((IStorageContainer)net.minecraft.client.Minecraft.getMinecraft().player.openContainer).setCraftableStacks(msg.craftableStacks);
                    }
                });
                return null;
            }
        }
    }

    /**
     * Server->Client: Updates the cursor item on the client.
     */
    public static class StackResponseClientMessage implements IMessage {
        public ItemStack stack = ItemStack.EMPTY;
        public StackResponseClientMessage() {}
        public StackResponseClientMessage(ItemStack stack) { this.stack = stack; }
        @Override public void fromBytes(ByteBuf buf) { stack = readItemStack(buf); }
        @Override public void toBytes(ByteBuf buf) { writeItemStack(buf, stack); }
        public static class Handler implements IMessageHandler<StackResponseClientMessage, IMessage> {
            @Override public IMessage onMessage(StackResponseClientMessage msg, MessageContext ctx) {
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                    net.minecraft.client.Minecraft.getMinecraft().player.inventory.setItemStack(msg.stack);
                });
                return null;
            }
        }
    }

    public static class OpenGuiMessage implements IMessage {
        public int x, y, z, guiId;
        public OpenGuiMessage() {}
        public OpenGuiMessage(int x, int y, int z, int guiId) { this.x = x; this.y = y; this.z = z; this.guiId = guiId; }
        @Override public void fromBytes(ByteBuf buf) { x = buf.readInt(); y = buf.readInt(); z = buf.readInt(); guiId = buf.readInt(); }
        @Override public void toBytes(ByteBuf buf) { buf.writeInt(x); buf.writeInt(y); buf.writeInt(z); buf.writeInt(guiId); }
        public static class Handler implements IMessageHandler<OpenGuiMessage, IMessage> {
            @Override public IMessage onMessage(OpenGuiMessage msg, MessageContext ctx) {
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                    net.minecraft.client.Minecraft.getMinecraft().player.openGui(
                        com.brilliafy.magicstorage.MagicStorage.instance, msg.guiId,
                        net.minecraft.client.Minecraft.getMinecraft().player.world,
                        msg.x, msg.y, msg.z);
                });
                return null;
            }
        }
    }
}
