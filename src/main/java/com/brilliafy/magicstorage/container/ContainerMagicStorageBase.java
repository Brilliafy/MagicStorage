package com.brilliafy.magicstorage.container;

import com.brilliafy.magicstorage.gui.IStorageContainer;
import com.brilliafy.magicstorage.gui.InventoryCraftingNetwork;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.util.ArrayList;
import java.util.List;

public abstract class ContainerMagicStorageBase extends Container implements IStorageContainer {

    protected InventoryPlayer playerInv;
    protected InventoryCraftResult result;
    protected InventoryCraftingNetwork matrix;
    protected boolean recipeLocked = false;
    protected boolean isSimple = false;
    protected boolean anvilResultLocked = false; // true when XP insufficient for anvil result
    protected boolean enchantResultLocked = false; // true when XP insufficient for enchant result
    protected List<ItemStack> cachedStacks = new ArrayList<>();
    protected List<ItemStack> cachedCraftableStacks = new ArrayList<>();

    public List<ItemStack> getCachedStacks() { return cachedStacks; }

    @Override
    public void setStacks(List<ItemStack> stacks) { this.cachedStacks = stacks; }

    @Override
    public void setCraftableStacks(List<ItemStack> stacks) { this.cachedCraftableStacks = stacks; }

    @Override
    public InventoryCrafting getCraftMatrix() { return this.matrix; }
    public abstract TileStorageHeart getTileMaster();
    public abstract void slotChanged();
    public abstract boolean isRequest();

    // SSN slot layout: 0=craftResult, 1-9=craftMatrix, 10-36=playerInv, 37-45=hotbar
    protected void bindPlayerInvo(final InventoryPlayer playerInv) {
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 174 + i * 18));
        for (int j = 0; j < 9; ++j)
            this.addSlotToContainer(new Slot(playerInv, j, 8 + j * 18, 232));
    }


    protected void bindGrid() {
        // 0 = craft result at SSN position x=101, y=128
        this.addSlotToContainer(new SlotCraftingNetwork(playerInv.player, matrix, result, 0, 101, 128));
        // 1-9 = 3x3 matrix at x=8, y=110 (SSN position)
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 3; ++j)
                this.addSlotToContainer(new Slot(matrix, j + i * 3, 8 + j * 18, 110 + i * 18));
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        // Only run recipe matching on the SERVER — the client doesn't have
        // the heart's station inventory synced yet, so hasEnchantingTable()
        // etc. return false and the result shows EMPTY.  The server sends
        // the correct result back via detectAndSendChanges().
        if (!playerInv.player.world.isRemote) {
            findMatchingRecipe(matrix);
        }
    }

    protected void findMatchingRecipe(InventoryCrafting craftMatrix) {
        if (recipeLocked) return;
        if (this.result == null) return;

        TileStorageHeart master = getTileMaster();
        if (master == null) { result.setInventorySlotContents(0, ItemStack.EMPTY); return; }

        ItemStack[] m = new ItemStack[9];
        for (int i = 0; i < 9; i++) m[i] = craftMatrix.getStackInSlot(i);

        // 1. Enchanting — simulate table, show tooltip hint
        enchantResultLocked = false;
        if (master.hasEnchantingTable() && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.canCraft(m[0], m[3], m[4], m[5])) {
            int slot = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getEnchantTier(m[3], m[4], m[5]) - 1;
            if (slot >= 0) {
                int power = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getPowerFromHeart(master, playerInv.player.world);
                com.brilliafy.magicstorage.util.EnchantingCraftingHelper.EnchantResult er =
                    com.brilliafy.magicstorage.util.EnchantingCraftingHelper.simulateEnchant(m[0], playerInv.player, power, slot);
                if (er != null) {
                    if (com.brilliafy.magicstorage.util.EnchantingCraftingHelper.hasEnoughXp(playerInv.player, Math.max(er.xpCost, er.enchantLevel))) {
                        result.setInventorySlotContents(0, er.displayStack);
                    } else {
                        enchantResultLocked = true;
                        result.setInventorySlotContents(0, com.brilliafy.magicstorage.util.EnchantingCraftingHelper.buildDisplayStackInsufficientXp(m[0], er.clue, er.xpCost, er.enchantLevel));
                    }
                    return;
                } else {
                    // Tier unavailable (not enough bookshelves for this slot)
                    enchantResultLocked = true;
                    result.setInventorySlotContents(0, com.brilliafy.magicstorage.util.EnchantingCraftingHelper.buildDisplayStackUnavailableTier(m[0]));
                    return;
                }
            }
        }
        // 2. Furnace
        if (master.hasFurnace() && com.brilliafy.magicstorage.util.SmeltingCraftingHelper.canCraft(m)) {
            ItemStack smelted = com.brilliafy.magicstorage.util.SmeltingCraftingHelper.computeResult(m);
            if (!smelted.isEmpty()) { result.setInventorySlotContents(0, smelted); return; }
        }
        // 3. Anvil — delegate to vanilla ContainerRepair via reflection
        anvilResultLocked = false;
        if (master.hasAnvil() && com.brilliafy.magicstorage.util.AnvilCraftingHelper.canCraft(m[0], m[4], playerInv.player)) {
            com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilResult ar = com.brilliafy.magicstorage.util.AnvilCraftingHelper.computeResult(m[0], m[4], playerInv.player);
            if (ar != null) {
                if (com.brilliafy.magicstorage.util.AnvilCraftingHelper.hasEnoughXp(playerInv.player, ar.cost)) {
                    result.setInventorySlotContents(0, com.brilliafy.magicstorage.util.AnvilCraftingHelper.buildDisplayStack(m[0], m[4], ar.stack, ar.cost));
                } else {
                    // Show result with red "Insufficient XP" tooltip, block taking
                    anvilResultLocked = true;
                    result.setInventorySlotContents(0, com.brilliafy.magicstorage.util.AnvilCraftingHelper.buildDisplayStack(m[0], m[4], ar.stack, ar.cost, "insufficient"));
                }
                return;
            }
        }
        // 4. Brewing
        if (master.hasBrewingStand() && com.brilliafy.magicstorage.util.PotionCraftingHelper.canCraft(m[0], m[1], m[3], m[4], m[5])) {
            List<ItemStack> results = com.brilliafy.magicstorage.util.PotionCraftingHelper.computeResult(m);
            if (!results.isEmpty()) { result.setInventorySlotContents(0, results.get(0)); return; }
        }
        // 5. Vanilla
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            if (recipe.matches(craftMatrix, playerInv.player.world)) {
                ItemStack output = recipe.getCraftingResult(craftMatrix);
                if (!output.isEmpty()) { result.setInventorySlotContents(0, output); return; }
            }
        }
        result.setInventorySlotContents(0, ItemStack.EMPTY);
    }

    protected void craftShift(EntityPlayer player, TileStorageHeart tile) {
        if (tile == null || matrix == null || result == null) return;
        if (result.getStackInSlot(0).isEmpty()) return;

        ItemStack output = result.getStackInSlot(0);
        ItemStack[] m = new ItemStack[9];
        for (int i = 0; i < 9; i++) m[i] = matrix.getStackInSlot(i);

        // Enchanting — simulate table and apply
        if (tile.hasEnchantingTable() && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.canCraft(m[0], m[3], m[4], m[5])) {
            int slot = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getEnchantTier(m[3], m[4], m[5]) - 1;
            if (slot >= 0) {
                int power = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getPowerFromHeart(tile, player.world);
                com.brilliafy.magicstorage.util.EnchantingCraftingHelper.EnchantResult er =
                    com.brilliafy.magicstorage.util.EnchantingCraftingHelper.simulateEnchant(m[0], player, power, slot);
                    if (er != null && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.hasEnoughXp(player, Math.max(er.xpCost, er.enchantLevel))) {
                    ItemStack enchanted = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.applyEnchantList(m[0], er.enchantments);
                    if (!player.inventory.addItemStackToInventory(enchanted)) player.dropItem(enchanted, false);
                    com.brilliafy.magicstorage.util.EnchantingCraftingHelper.consumeIngredients(m);
                    player.onEnchant(enchanted, er.xpCost);
                    if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                        ((net.minecraft.entity.player.EntityPlayerMP) player).connection.sendPacket(
                            new net.minecraft.network.play.server.SPacketSetExperience(
                                player.experience, player.experienceTotal, player.experienceLevel));
                    }
                    player.world.playSound(null, player.getPosition(), net.minecraft.init.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                    for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                    onCraftMatrixChanged(matrix);
                    detectAndSendChanges();
                    return;
                } else {
                    return;
                }
            }
        }

        // Furnace
        if (tile.hasFurnace() && com.brilliafy.magicstorage.util.SmeltingCraftingHelper.canCraft(m)) {
            int count = com.brilliafy.magicstorage.util.SmeltingCraftingHelper.getSmeltableCount(m);
            ItemStack smelted = com.brilliafy.magicstorage.util.SmeltingCraftingHelper.computeResult(m);
            if (!smelted.isEmpty()) {
                smelted.setCount(Math.min(count, smelted.getMaxStackSize()));
                if (!player.inventory.addItemStackToInventory(smelted)) player.dropItem(smelted, false);
                com.brilliafy.magicstorage.util.SmeltingCraftingHelper.consumeIngredients(m);
                player.world.playSound(null, player.getPosition(), net.minecraft.init.SoundEvents.BLOCK_FIRE_AMBIENT, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                onCraftMatrixChanged(matrix);
                detectAndSendChanges();
                return;
            }
        }
        
        if (tile.hasAnvil() && com.brilliafy.magicstorage.util.AnvilCraftingHelper.canCraft(m[0], m[4], player)) {
            com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilResult ar = com.brilliafy.magicstorage.util.AnvilCraftingHelper.computeResult(m[0], m[4], player);
            if (ar != null && com.brilliafy.magicstorage.util.AnvilCraftingHelper.hasEnoughXp(player, ar.cost)) {
                if (!player.inventory.addItemStackToInventory(ar.stack)) player.dropItem(ar.stack, false);
                com.brilliafy.magicstorage.util.AnvilCraftingHelper.consumeIngredients(m, ar);
                com.brilliafy.magicstorage.util.AnvilCraftingHelper.consumeXp(player, ar.cost);
                player.world.playSound(null, player.getPosition(), net.minecraft.init.SoundEvents.BLOCK_ANVIL_USE, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                for (int i = 0; i < 9; i++) matrix.setInventorySlotContents(i, m[i]);
                onCraftMatrixChanged(matrix);
                detectAndSendChanges();
                return;
            }
        }

        // Brewing
        if (tile.hasBrewingStand() && com.brilliafy.magicstorage.util.PotionCraftingHelper.canCraft(m[0], m[1], m[3], m[4], m[5])) {
            List<ItemStack> results = com.brilliafy.magicstorage.util.PotionCraftingHelper.computeResult(m);
            if (!results.isEmpty()) {
                for (ItemStack r : results) {
                    if (!player.inventory.addItemStackToInventory(r)) player.dropItem(r, false);
                }
                com.brilliafy.magicstorage.util.PotionCraftingHelper.consumeIngredients(m, player.getRNG());
                player.world.playSound(null, player.getPosition(), net.minecraft.init.SoundEvents.BLOCK_BREWING_STAND_BREW, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                onCraftMatrixChanged(matrix);
                detectAndSendChanges();
                return;
            }
        }

        // Regular shift-craft (SSN style)
        this.recipeLocked = true;
        ItemStack res = output.copy();
        if (!player.inventory.addItemStackToInventory(res)) player.dropItem(res, false);
        for (int i = 0; i < matrix.getSizeInventory(); i++) {
            ItemStack s = matrix.getStackInSlot(i);
            if (!s.isEmpty()) {
                tile.extractItem(s2 -> ItemHandlerHelper.canItemStacksStack(s2, s), 1, false);
                matrix.decrStackSize(i, 1);
            }
        }
        this.recipeLocked = false;
        onCraftMatrixChanged(matrix);
        detectAndSendChanges();
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        // Return crafting grid items to network when GUI closes (SSN behavior)
        if (!playerIn.world.isRemote && matrix != null && !isSimple) {
            TileStorageHeart master = getTileMaster();
            for (int i = 0; i < matrix.getSizeInventory(); i++) {
                ItemStack stack = matrix.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    if (master != null) {
                        ItemStack remainder = master.insertItem(stack, false);
                        if (!remainder.isEmpty()) {
                            playerIn.dropItem(remainder, false);
                        }
                    } else {
                        playerIn.dropItem(stack, false);
                    }
                    matrix.setInventorySlotContents(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int slotIndex) {
        if (playerIn.world.isRemote) return ItemStack.EMPTY;
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            TileStorageHeart tileMaster = getTileMaster();

            // Slot 0 = craft result -> craftShift
            if (slotIndex == 0 && !isSimple) {
                craftShift(playerIn, tileMaster);
                return ItemStack.EMPTY;
            }
            // Matrix slots (1-9) -> return to network
            else if (slotIndex >= 1 && slotIndex <= 9) {
                if (tileMaster != null) {
                    ItemStack remainder = tileMaster.insertItem(itemstack1, false);
                    slot.putStack(remainder);
                    detectAndSendChanges();
                    sendItemRefresh(playerIn, tileMaster);
                    return ItemStack.EMPTY;
                }
            }
            // Player inventory/hotbar -> insert into network
            else if (tileMaster != null) {
                ItemStack remainder = tileMaster.insertItem(itemstack1, false);
                slot.putStack(remainder);
                detectAndSendChanges();
                sendItemRefresh(playerIn, tileMaster);
                if (remainder.isEmpty()) return ItemStack.EMPTY;
                slot.onTake(playerIn, itemstack1);
                return ItemStack.EMPTY;
            }
        }

        return itemstack;
    }
    
    protected void sendItemRefresh(EntityPlayer player, TileStorageHeart master) {
        if (master != null && !player.world.isRemote) {
            List<ItemStack> allItems = master.getAllItems();
            com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(allItems, new ArrayList<>()),
                (EntityPlayerMP) player);
        }
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return slot.inventory != this.result && super.canMergeSlot(stack, slot);
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, net.minecraft.inventory.ClickType clickTypeIn, EntityPlayer player) {
        // Compute anvilResultLocked on client side (only set server-side by findMatchingRecipe)
        if (slotId == 0 && player.world.isRemote && matrix != null) {
            anvilResultLocked = false;
            ItemStack[] m = new ItemStack[9];
            for (int i = 0; i < 9; i++) m[i] = matrix.getStackInSlot(i);
            if (!m[0].isEmpty() && !m[4].isEmpty()) {
                com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilResult ar = com.brilliafy.magicstorage.util.AnvilCraftingHelper.computeResult(m[0], m[4], player);
                if (ar != null && !com.brilliafy.magicstorage.util.AnvilCraftingHelper.hasEnoughXp(player, ar.cost)) {
                    anvilResultLocked = true;
                }
            }
        }
        // Block ALL clicks on result slot when XP insufficient (anvil or enchanting)
        if (slotId == 0 && (anvilResultLocked || enchantResultLocked) && clickTypeIn != net.minecraft.inventory.ClickType.QUICK_CRAFT) {
            return net.minecraft.item.ItemStack.EMPTY;
        }

        // Handle hotkey on result slot: craft + move to specific hotbar slot
        if (clickTypeIn == net.minecraft.inventory.ClickType.SWAP && slotId == 0 && !player.world.isRemote) {
            if (anvilResultLocked || enchantResultLocked) return ItemStack.EMPTY; // Block when XP insufficient
            TileStorageHeart master = getTileMaster();
            if (master != null && dragType >= 0 && dragType < 9) {
                // Snapshot inventory before craft
                net.minecraft.item.ItemStack[] before = new net.minecraft.item.ItemStack[player.inventory.getSizeInventory()];
                for (int i = 0; i < before.length; i++) {
                    before[i] = player.inventory.getStackInSlot(i).copy();
                }
                
                // Craft the item (puts in first available inventory slot)
                this.craftShift(player, master);
                
                // Find the newly crafted item and move to target hotbar slot
                for (int i = 0; i < before.length; i++) {
                    net.minecraft.item.ItemStack now = player.inventory.getStackInSlot(i);
                    net.minecraft.item.ItemStack prev = before[i];
                    if (!net.minecraft.item.ItemStack.areItemStacksEqual(now, prev)) {
                        // This slot changed — move the delta to hotbar
                        int delta = now.getCount() - prev.getCount();
                        if (delta > 0 && !now.isEmpty()) {
                            // Copy the crafted item
                            net.minecraft.item.ItemStack crafted = now.splitStack(delta);
                            net.minecraft.item.ItemStack oldHotbar = player.inventory.getStackInSlot(dragType);
                            player.inventory.setInventorySlotContents(dragType, crafted);
                            if (!oldHotbar.isEmpty()) {
                                // Put displaced hotbar item back in inventory
                                if (!player.inventory.addItemStackToInventory(oldHotbar)) {
                                    player.dropItem(oldHotbar, false);
                                }
                            }
                            // Sync all changes to client (craftShift already called detectAndSendChanges,
                            // but hotbar/inventory changes happened after that)
                            this.detectAndSendChanges();
                            break;
                        }
                    }
                }
            }
            return net.minecraft.item.ItemStack.EMPTY;
        }

        // Capture matrix state before super processes the click
        ItemStack[] beforeMatrix = null;
        if (slotId >= 1 && slotId <= 9 && matrix != null) {
            beforeMatrix = new ItemStack[9];
            for (int i = 0; i < 9; i++) beforeMatrix[i] = matrix.getStackInSlot(i).copy();
        }

        ItemStack result = super.slotClick(slotId, dragType, clickTypeIn, player);

        // If a matrix slot changed (right-click to add items), force recipe re-check
        // and send result to client immediately
        if (beforeMatrix != null && matrix != null && !player.world.isRemote) {
            boolean changed = false;
            for (int i = 0; i < 9; i++) {
                if (!net.minecraft.item.ItemStack.areItemStacksEqual(beforeMatrix[i], matrix.getStackInSlot(i))) {
                    changed = true;
                    break;
                }
            }
            if (changed) {
                onCraftMatrixChanged(matrix);
                // Send result directly to client — don't rely on detectAndSendChanges
                // which may not detect the change in time
                if (player instanceof EntityPlayerMP && this.result != null) {
                    ItemStack resultStack = this.result.getStackInSlot(0);
                    ((EntityPlayerMP) player).connection.sendPacket(
                        new net.minecraft.network.play.server.SPacketSetSlot(this.windowId, 0, resultStack));
                }
            }
        }

        return result;
    }

    public final class SlotCraftingNetwork extends SlotCrafting {
        public SlotCraftingNetwork(EntityPlayer player, InventoryCrafting crafting, IInventory result, int slotIndex, int x, int y) {
            super(player, crafting, result, slotIndex, x, y);
        }

        @Override
        public boolean canTakeStack(EntityPlayer playerIn) {
            if (anvilResultLocked) return false;
            // Check XP on client — anvilResultLocked is only set server-side
            if (playerInv.player.world.isRemote) {
                TileStorageHeart master = getTileMaster();
                if (master == null || !master.hasAnvil()) return super.canTakeStack(playerIn);
                ItemStack[] m = new ItemStack[9];
                for (int i = 0; i < 9; i++) m[i] = matrix.getStackInSlot(i);
                com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilResult ar = com.brilliafy.magicstorage.util.AnvilCraftingHelper.computeResult(m[0], m[4], playerInv.player);
                if (ar != null && !com.brilliafy.magicstorage.util.AnvilCraftingHelper.hasEnoughXp(playerInv.player, ar.cost)) {
                    return false;
                }
            }
            return super.canTakeStack(playerIn);
        }

        @Override
        public ItemStack decrStackSize(int amount) {
            if (anvilResultLocked) return ItemStack.EMPTY;
            return super.decrStackSize(amount);
        }

        @Override
        public ItemStack onTake(EntityPlayer playerIn, ItemStack stack) {
            TileStorageHeart master = getTileMaster();
            
            ItemStack[] m = new ItemStack[9];
            for (int i = 0; i < 9; i++) m[i] = matrix.getStackInSlot(i);

            // Client side: for anvil, compute real result; for others, let server send via SPacketSetSlot
            if (playerIn.world.isRemote) {
                if (master != null && master.hasAnvil() && com.brilliafy.magicstorage.util.AnvilCraftingHelper.canCraft(m[0], m[4], playerIn)) {
                    com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilResult ar = com.brilliafy.magicstorage.util.AnvilCraftingHelper.computeResult(m[0], m[4], playerIn);
                    if (ar != null) {
                        if (com.brilliafy.magicstorage.util.AnvilCraftingHelper.hasEnoughXp(playerIn, ar.cost)) {
                            return ar.stack.copy();
                        }
                        playerIn.inventory.setItemStack(ItemStack.EMPTY);
                        return ItemStack.EMPTY;
                    }
                }
                // For furnace/enchanting/brewing: return stack as-is, server sends real result
                return stack;
            }
            
            // For custom recipes: handle everything ourselves, skip super.onTake
            // (vanilla onTake would decrStackSize on ALL slots, double-consuming our ingredients)
            if (master != null) {
                if (master.hasEnchantingTable() && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.canCraft(m[0], m[3], m[4], m[5])) {
                    int slot = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getEnchantTier(m[3], m[4], m[5]) - 1;
                    if (slot >= 0) {
                        int power = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getPowerFromHeart(master, playerIn.world);
                        com.brilliafy.magicstorage.util.EnchantingCraftingHelper.EnchantResult er =
                            com.brilliafy.magicstorage.util.EnchantingCraftingHelper.simulateEnchant(m[0], playerIn, power, slot);
                        if (er != null && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.hasEnoughXp(playerIn, Math.max(er.xpCost, er.enchantLevel))) {
                            stack.onCrafting(playerIn.world, playerIn, 1);
                            // Save item BEFORE consuming ingredients
                            ItemStack savedItem = m[0].copy();
                            com.brilliafy.magicstorage.util.EnchantingCraftingHelper.consumeIngredients(m);
                            // Apply real enchantments using the SAVED item (not m[0] which is now empty)
                            ItemStack realItem = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.applyEnchantList(savedItem, er.enchantments);
                            stack.setTagCompound(realItem.getTagCompound());
                            playerIn.onEnchant(stack, er.xpCost);
                            playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                            // Force client to update cursor immediately (fixes tooltip delay)
                            if (playerIn instanceof net.minecraft.entity.player.EntityPlayerMP) {
                                ((net.minecraft.entity.player.EntityPlayerMP) playerIn).connection.sendPacket(
                                    new net.minecraft.network.play.server.SPacketSetSlot(-1, -1, stack));
                                ((net.minecraft.entity.player.EntityPlayerMP) playerIn).connection.sendPacket(
                                    new net.minecraft.network.play.server.SPacketSetExperience(
                                        playerIn.experience, playerIn.experienceTotal, playerIn.experienceLevel));
                            }
                            for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                            onCraftMatrixChanged(matrix);
                            detectAndSendChanges();
                            return stack;
                        }
                    }
                    return ItemStack.EMPTY;
                }
                if (master.hasFurnace() && com.brilliafy.magicstorage.util.SmeltingCraftingHelper.canCraft(m)) {
                    stack.onCrafting(playerIn.world, playerIn, 1);
                    com.brilliafy.magicstorage.util.SmeltingCraftingHelper.consumeIngredients(m);
                    playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                    for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                    onCraftMatrixChanged(matrix);
                    detectAndSendChanges();
                    return stack;
                }
                if (master.hasAnvil() && com.brilliafy.magicstorage.util.AnvilCraftingHelper.canCraft(m[0], m[4], playerIn)) {
                    com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilResult ar = com.brilliafy.magicstorage.util.AnvilCraftingHelper.computeResult(m[0], m[4], playerIn);
                    if (ar != null && com.brilliafy.magicstorage.util.AnvilCraftingHelper.hasEnoughXp(playerIn, ar.cost)) {
                        stack.onCrafting(playerIn.world, playerIn, 1);
                        com.brilliafy.magicstorage.util.AnvilCraftingHelper.consumeIngredients(m, ar);
                        com.brilliafy.magicstorage.util.AnvilCraftingHelper.consumeXp(playerIn, ar.cost);
                        playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_ANVIL_USE, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                        // Modify the cursor item IN PLACE — vanilla ignores onTake return value
                        // and keeps the item from decrStackSize (the display stack with lore)
                        ItemStack realResult = ar.stack.copy();
                        stack.setTagCompound(realResult.getTagCompound());
                        stack.setCount(1);
                        for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                        onCraftMatrixChanged(matrix);
                        detectAndSendChanges();
                        // Sync to client
                        if (playerIn instanceof net.minecraft.entity.player.EntityPlayerMP) {
                            ((net.minecraft.entity.player.EntityPlayerMP) playerIn).connection.sendPacket(
                                new net.minecraft.network.play.server.SPacketSetSlot(-1, -1, stack));
                            ((net.minecraft.entity.player.EntityPlayerMP) playerIn).connection.sendPacket(
                                new net.minecraft.network.play.server.SPacketSetExperience(
                                    playerIn.experience, playerIn.experienceTotal, playerIn.experienceLevel));
                        }
                        return stack;
                    }
                    return ItemStack.EMPTY;
                }
                if (master.hasBrewingStand() && com.brilliafy.magicstorage.util.PotionCraftingHelper.canCraft(m[0], m[1], m[3], m[4], m[5])) {
                    List<ItemStack> results = com.brilliafy.magicstorage.util.PotionCraftingHelper.computeResult(m);
                    if (!results.isEmpty()) {
                        stack.onCrafting(playerIn.world, playerIn, 1);
                        com.brilliafy.magicstorage.util.PotionCraftingHelper.consumeIngredients(m, playerIn.getRNG());
                        playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_BREWING_STAND_BREW, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                        for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                        onCraftMatrixChanged(matrix);
                        detectAndSendChanges();
                        return stack;
                    }
                    return ItemStack.EMPTY;
                }
            }
            
            // Vanilla recipe: use super.onTake for proper remaining-items handling
            List<ItemStack> lis = new ArrayList<>();
            for (int i = 0; i < matrix.getSizeInventory(); i++)
                lis.add(matrix.getStackInSlot(i).copy());
            super.onTake(playerIn, stack);
            // SSN-style: auto-refill empty matrix slots from network
            for (int i = 0; i < matrix.getSizeInventory(); i++) {
                if (matrix.getStackInSlot(i).isEmpty() && master != null && !lis.get(i).isEmpty()) {
                    final int slot = i;
                    ItemStack req = master.extractItem(
                        s -> net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(s, lis.get(slot)),
                        1, false);
                    if (!req.isEmpty()) matrix.setInventorySlotContents(i, req);
                }
            }
            detectAndSendChanges();
            return stack;
        }
    }
}
