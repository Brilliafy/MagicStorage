package com.brilliafy.magicstorage.container;

import com.brilliafy.magicstorage.gui.IStorageContainer;
import com.brilliafy.magicstorage.gui.InventoryCraftingNetwork;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import com.brilliafy.magicstorage.util.MagicStorageRandom;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.util.ArrayList;
import java.util.List;

public abstract class ContainerMagicStorageBase extends Container implements IStorageContainer {

    public void syncAllSlots(EntityPlayer playerIn) {
        if (playerIn instanceof EntityPlayerMP) {
            EntityPlayerMP mp = (EntityPlayerMP) playerIn;
            for (int i = 0; i < 9; i++) {
                mp.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(this.windowId, i + 1, matrix.getStackInSlot(i)));
            }
            ItemStack resultStack = result != null ? result.getStackInSlot(0) : ItemStack.EMPTY;
            mp.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(this.windowId, 0, resultStack));
            mp.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(-1, -1, mp.inventory.getItemStack()));
        }
    }

    protected InventoryPlayer playerInv;
    protected InventoryCraftResult result;
    protected InventoryCraftingNetwork matrix;
    protected boolean recipeLocked = false;
    protected boolean isSimple = false;
    protected boolean anvilResultLocked = false; // true when XP insufficient for anvil result
    protected boolean enchantResultLocked = false; // true when XP insufficient for enchant result
    protected boolean bountifulBaublesResultLocked = false;
    protected boolean reskillableResultLocked = false;
    protected boolean isShiftCrafting = false;
    protected int autofillMode = 2; // 2=FULL (Network & Inventory), 1=NETWORK_ONLY, 0=DISABLED
    protected List<ItemStack> cachedStacks = new ArrayList<>();
    protected List<ItemStack> cachedCraftableStacks = new ArrayList<>();

    public int getAutofillMode() { return autofillMode; }
    public void setAutofillMode(int mode) { this.autofillMode = mode; }
    public boolean isAutofillEnabled() { return autofillMode != 0; }
    public void setAutofillEnabled(boolean enabled) { this.autofillMode = enabled ? 2 : 0; }

    public List<ItemStack> getCachedStacks() { return cachedStacks; }

    @Override
    public void setStacks(List<ItemStack> stacks) { this.cachedStacks = stacks; }

    @Override
    public void setCraftableStacks(List<ItemStack> stacks) { this.cachedCraftableStacks = stacks; }

    @Override
    public InventoryCrafting getCraftMatrix() { return this.matrix; }
    public InventoryCraftResult getResult() { return this.result; }
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
        // 0 = craft result at position x=101, y=128
        this.addSlotToContainer(new SlotCraftingNetwork(playerInv.player, matrix, result, 0, 101, 128));
        // 1-9 = 3x3 matrix at x=8, y=110
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 3; ++j)
                this.addSlotToContainer(new Slot(matrix, j + i * 3, 8 + j * 18, 110 + i * 18));
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        if (!playerInv.player.world.isRemote) {
            findMatchingRecipe(matrix);
            if (playerInv.player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                net.minecraft.entity.player.EntityPlayerMP mp = (net.minecraft.entity.player.EntityPlayerMP) playerInv.player;
                ItemStack resultStack = result != null ? result.getStackInSlot(0) : ItemStack.EMPTY;
                mp.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(this.windowId, 0, resultStack));
            }
        }
    }

    protected void findMatchingRecipe(InventoryCrafting craftMatrix) {
        if (recipeLocked) return;
        if (this.result == null) return;

        this.anvilResultLocked = false;
        this.enchantResultLocked = false;
        this.bountifulBaublesResultLocked = false;
        this.reskillableResultLocked = false;

        TileStorageHeart master = getTileMaster();

        try {
            ItemStack[] m = new ItemStack[9];
            for (int i = 0; i < 9; i++) m[i] = craftMatrix.getStackInSlot(i);

            if (master != null) {
                // 1. Enchanting Table
                if (master.hasEnchantingTable() && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.isEnchantingGrid(m) && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.canCraft(m[0], m[3], m[4], m[5])) {
                    int slot = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getEnchantTier(m[3], m[4], m[5]) - 1;
                    if (slot >= 0) {
                        int power = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getPowerFromHeart(master, playerInv.player.world);
                        com.brilliafy.magicstorage.util.EnchantingCraftingHelper.EnchantResult er =
                            com.brilliafy.magicstorage.util.EnchantingCraftingHelper.simulateEnchant(m[0], playerInv.player, power, slot);
                        if (er != null) {
                            int reqXp = Math.max(er.xpCost, er.enchantLevel);
                            if (com.brilliafy.magicstorage.util.EnchantingCraftingHelper.hasEnoughXp(playerInv.player, reqXp)) {
                                result.setInventorySlotContents(0, er.displayStack);
                            } else {
                                enchantResultLocked = true;
                                result.setInventorySlotContents(0, com.brilliafy.magicstorage.util.EnchantingCraftingHelper.buildDisplayStackInsufficientXp(m[0], er.clue, er.xpCost, er.enchantLevel));
                            }
                            checkAndApplyReskillableLock(playerInv.player, Blocks.ENCHANTING_TABLE);
                            return;
                        }
                    }
                }
                // 2. Anvil — delegate to vanilla ContainerRepair via reflection
                if (master.hasAnvil() && com.brilliafy.magicstorage.util.AnvilCraftingHelper.isAnvilGrid(m) && com.brilliafy.magicstorage.util.AnvilCraftingHelper.canCraft(m[0], m[4], playerInv.player)) {
                    com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilResult ar = com.brilliafy.magicstorage.util.AnvilCraftingHelper.computeResult(m[0], m[4], playerInv.player);
                    if (ar != null) {
                        if (com.brilliafy.magicstorage.util.AnvilCraftingHelper.hasEnoughXp(playerInv.player, ar.cost)) {
                            result.setInventorySlotContents(0, com.brilliafy.magicstorage.util.AnvilCraftingHelper.buildDisplayStack(m[0], m[4], ar.stack, ar.cost));
                        } else {
                            // Show result with red "Insufficient XP" tooltip, block taking
                            anvilResultLocked = true;
                            result.setInventorySlotContents(0, com.brilliafy.magicstorage.util.AnvilCraftingHelper.buildDisplayStack(m[0], m[4], ar.stack, ar.cost, "insufficient"));
                        }
                        checkAndApplyReskillableLock(playerInv.player, Blocks.ANVIL);
                        return;
                    }
                }
                // 3. Brewing
                if (master.hasBrewingStand() && com.brilliafy.magicstorage.util.PotionCraftingHelper.isPotionGrid(m) && com.brilliafy.magicstorage.util.PotionCraftingHelper.canCraft(m[0], m[1], m[3], m[4], m[5])) {
                    List<ItemStack> results = com.brilliafy.magicstorage.util.PotionCraftingHelper.computeResult(m);
                    if (!results.isEmpty()) {
                        result.setInventorySlotContents(0, results.get(0));
                        checkAndApplyReskillableLock(playerInv.player, Blocks.BREWING_STAND);
                        return;
                    }
                }
                // 4. Rustic Advanced Alchemy
                if (master.hasRusticAdvancedCondenser() && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftAdvancedCondenser(m)) {
                    ItemStack res = com.brilliafy.magicstorage.util.RusticCraftingHelper.computeAdvancedCondenserResult(m);
                    if (!res.isEmpty()) {
                        result.setInventorySlotContents(0, res);
                        checkAndApplyReskillableLock(playerInv.player, Block.getBlockFromName("rustic:condenser_advanced"), Block.getBlockFromName("rustic:retort_advanced"));
                        return;
                    }
                }
                // 5. Rustic Simple Alchemy
                if (master.hasRusticSimpleCondenser() && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftSimpleCondenser(m)) {
                    ItemStack res = com.brilliafy.magicstorage.util.RusticCraftingHelper.computeSimpleCondenserResult(m);
                    if (!res.isEmpty()) {
                        result.setInventorySlotContents(0, res);
                        checkAndApplyReskillableLock(playerInv.player, Block.getBlockFromName("rustic:condenser"), Block.getBlockFromName("rustic:retort"));
                        return;
                    }
                }
                // 6. Rustic Brewing Barrel
                if (master.hasRusticBrewingBarrel() && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftBrewing(m, master)) {
                    ItemStack res = com.brilliafy.magicstorage.util.RusticCraftingHelper.computeBrewingResult(m, master);
                    if (!res.isEmpty()) {
                        result.setInventorySlotContents(0, res);
                        checkAndApplyReskillableLock(playerInv.player, Block.getBlockFromName("rustic:brewing_barrel"));
                        return;
                    }
                }
                // 7. Rustic Crushing Tub
                if (master.hasRusticCrushingTub() && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftCrushing(m)) {
                    ItemStack res = com.brilliafy.magicstorage.util.RusticCraftingHelper.computeCrushingResult(m);
                    if (!res.isEmpty()) {
                        result.setInventorySlotContents(0, res);
                        checkAndApplyReskillableLock(playerInv.player, Block.getBlockFromName("rustic:crushing_tub"));
                        return;
                    }
                }
                // 8. Disenchanter
                if (master.hasDisenchanterTable() && com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.isDisenchanterGrid(m) && com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.canCraft(m[4], m[2])) {
                    boolean isVoiding = master.isDisenchanterVoiding();
                    boolean isBulk = master.isDisenchanterBulk();
                    ItemStack res = com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.computeResult(m[4], m[2], isVoiding, isBulk);
                    if (!res.isEmpty()) {
                        result.setInventorySlotContents(0, res);
                        checkAndApplyReskillableLock(playerInv.player, Block.getBlockFromName("disenchanter:disenchantmenttable"));
                        return;
                    }
                }
                // 9. Bountiful Baubles Reforge
                if (master.hasBountifulBaublesReforger() && com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.isBaubleReforgeGrid(m) && com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.canCraft(m[8])) {
                    com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.BaubleReforgeResult bbr =
                        com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.computeResult(m[8], playerInv.player, master);
                    if (bbr != null) {
                        result.setInventorySlotContents(0, bbr.displayStack);
                        if (!bbr.hasEnoughXp) {
                            bountifulBaublesResultLocked = true;
                        }
                        checkAndApplyReskillableLock(playerInv.player, Block.getBlockFromName("bountifulbaubles:reforger"));
                        return;
                    }
                }
                // 10. Quality Tools Reforge
                if (master.hasQualityToolsReforger() && com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.isQualityToolsGrid(m) && com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.canCraft(m[4], m[8])) {
                    ItemStack res = com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.computeResult(m[4], m[8], master);
                    if (!res.isEmpty()) {
                        result.setInventorySlotContents(0, res);
                        checkAndApplyReskillableLock(playerInv.player, Block.getBlockFromName("qualitytools:reforging_station"));
                        return;
                    }
                }
            }

            // 11. Normal Crafting Table recipes
            IRecipe recipe = CraftingManager.findMatchingRecipe(craftMatrix, playerInv.player.world);
            if (recipe != null) {
                ItemStack output = recipe.getCraftingResult(craftMatrix);
                if (!output.isEmpty()) {
                    result.setInventorySlotContents(0, output);
                    return;
                }
            }

            // 12. Furnace Smelting (if no normal crafting recipe matched)
            if (master != null && master.hasFurnace() && com.brilliafy.magicstorage.util.SmeltingCraftingHelper.canCraft(m)) {
                ItemStack smelted = com.brilliafy.magicstorage.util.SmeltingCraftingHelper.computeResult(m);
                if (!smelted.isEmpty()) {
                    result.setInventorySlotContents(0, smelted);
                    checkAndApplyReskillableLock(playerInv.player, Blocks.FURNACE);
                    return;
                }
            }
        } catch (Throwable t) {
            com.brilliafy.magicstorage.MagicStorage.LOGGER.error("Error matching recipe in ContainerMagicStorageBase", t);
        }
        result.setInventorySlotContents(0, ItemStack.EMPTY);
    }

    private boolean checkAndApplyReskillableLock(EntityPlayer player, Block... stationBlocks) {
        reskillableResultLocked = false;
        for (Block b : stationBlocks) {
            if (b != null && !com.brilliafy.magicstorage.util.ReskillableCraftingHelper.hasSkillForBlock(player, b)) {
                reskillableResultLocked = true;
                ItemStack current = result.getStackInSlot(0);
                if (!current.isEmpty()) {
                    result.setInventorySlotContents(0, com.brilliafy.magicstorage.util.ReskillableCraftingHelper.applySkillLockTooltip(current));
                }
                return false;
            }
        }
        return true;
    }

    private boolean canPlayerInventoryAccept(EntityPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        int needed = stack.getCount();
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack invStack = player.inventory.mainInventory.get(i);
            if (invStack.isEmpty()) {
                return true;
            } else if (ItemHandlerHelper.canItemStacksStack(invStack, stack)) {
                int space = invStack.getMaxStackSize() - invStack.getCount();
                if (space >= needed) return true;
                needed -= space;
            }
        }
        return needed <= 0;
    }

    private boolean doSingleCraftFromMatrixOnly(EntityPlayer player, TileStorageHeart tile, ItemStack[] m) {
        if (tile != null) {
            // Custom station crafts
            if (tile.hasEnchantingTable() && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.isEnchantingGrid(m) && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.canCraft(m[0], m[3], m[4], m[5])) {
                int slot = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getEnchantTier(m[3], m[4], m[5]) - 1;
                if (slot >= 0) {
                    int power = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getPowerFromHeart(tile, player.world);
                    com.brilliafy.magicstorage.util.EnchantingCraftingHelper.EnchantResult er =
                        com.brilliafy.magicstorage.util.EnchantingCraftingHelper.simulateEnchant(m[0], player, power, slot);
                    if (er != null && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.hasEnoughXp(player, Math.max(er.xpCost, er.enchantLevel))) {
                        ItemStack enchanted = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.applyEnchantList(m[0], er.enchantments);
                        if (!player.inventory.addItemStackToInventory(enchanted)) player.dropItem(enchanted, false);
                        com.brilliafy.magicstorage.util.EnchantingCraftingHelper.consumeIngredients(m);
                        com.brilliafy.magicstorage.util.EnchantingCraftingHelper.prepareEnchantContext(er.enchantLevel);
                        try {
                            player.onEnchant(enchanted, er.xpCost);
                        } finally {
                            com.brilliafy.magicstorage.util.EnchantingCraftingHelper.clearEnchantContext();
                        }
                        for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                        onCraftMatrixChanged(matrix);
                        return true;
                    }
                }
                return false;
            }

            if (tile.hasAnvil() && com.brilliafy.magicstorage.util.AnvilCraftingHelper.isAnvilGrid(m) && com.brilliafy.magicstorage.util.AnvilCraftingHelper.canCraft(m[0], m[4], player)) {
                com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilResult ar = com.brilliafy.magicstorage.util.AnvilCraftingHelper.computeResult(m[0], m[4], player);
                if (ar != null && com.brilliafy.magicstorage.util.AnvilCraftingHelper.hasEnoughXp(player, ar.cost)) {
                    if (!player.inventory.addItemStackToInventory(ar.stack)) player.dropItem(ar.stack, false);
                    com.brilliafy.magicstorage.util.AnvilCraftingHelper.consumeIngredients(m, ar);
                    com.brilliafy.magicstorage.util.AnvilCraftingHelper.consumeXp(player, ar.cost);
                    for (int i = 0; i < 9; i++) matrix.setInventorySlotContents(i, m[i]);
                    onCraftMatrixChanged(matrix);
                    return true;
                }
                return false;
            }

            if (tile.hasBrewingStand() && com.brilliafy.magicstorage.util.PotionCraftingHelper.isPotionGrid(m) && com.brilliafy.magicstorage.util.PotionCraftingHelper.canCraft(m[0], m[1], m[3], m[4], m[5])) {
                List<ItemStack> results = com.brilliafy.magicstorage.util.PotionCraftingHelper.computeResult(m);
                if (!results.isEmpty()) {
                    for (ItemStack r : results) {
                        if (!player.inventory.addItemStackToInventory(r)) player.dropItem(r, false);
                    }
                    com.brilliafy.magicstorage.util.PotionCraftingHelper.consumeIngredients(m, player.getRNG());
                    for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                    onCraftMatrixChanged(matrix);
                    return true;
                }
                return false;
            }

            if (tile.hasRusticAdvancedCondenser() && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftAdvancedCondenser(m)) {
                ItemStack outputStack = com.brilliafy.magicstorage.util.RusticCraftingHelper.computeAdvancedCondenserResult(m);
                if (!outputStack.isEmpty()) {
                    if (!player.inventory.addItemStackToInventory(outputStack)) player.dropItem(outputStack, false);
                    com.brilliafy.magicstorage.util.RusticCraftingHelper.consumeAdvancedCondenserIngredients(m, player.getRNG());
                    for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                    onCraftMatrixChanged(matrix);
                    return true;
                }
                return false;
            }

            if (tile.hasRusticSimpleCondenser() && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftSimpleCondenser(m)) {
                ItemStack outputStack = com.brilliafy.magicstorage.util.RusticCraftingHelper.computeSimpleCondenserResult(m);
                if (!outputStack.isEmpty()) {
                    if (!player.inventory.addItemStackToInventory(outputStack)) player.dropItem(outputStack, false);
                    com.brilliafy.magicstorage.util.RusticCraftingHelper.consumeSimpleCondenserIngredients(m, player.getRNG());
                    for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                    onCraftMatrixChanged(matrix);
                    return true;
                }
                return false;
            }

            if (tile.hasRusticBrewingBarrel() && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftBrewing(m, tile)) {
                ItemStack outputStack = com.brilliafy.magicstorage.util.RusticCraftingHelper.computeBrewingResult(m, tile);
                if (!outputStack.isEmpty()) {
                    if (!player.inventory.addItemStackToInventory(outputStack)) player.dropItem(outputStack, false);
                    com.brilliafy.magicstorage.util.RusticCraftingHelper.consumeBrewingIngredients(m, player.getRNG(), tile);
                    for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                    onCraftMatrixChanged(matrix);
                    return true;
                }
                return false;
            }

            if (tile.hasRusticCrushingTub() && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftCrushing(m)) {
                ItemStack outputStack = com.brilliafy.magicstorage.util.RusticCraftingHelper.computeCrushingResult(m);
                if (!outputStack.isEmpty()) {
                    if (!player.inventory.addItemStackToInventory(outputStack)) player.dropItem(outputStack, false);
                    com.brilliafy.magicstorage.util.RusticCraftingHelper.consumeCrushingIngredients(m);
                    for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                    onCraftMatrixChanged(matrix);
                    return true;
                }
                return false;
            }

            if (tile.hasDisenchanterTable() && com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.isDisenchanterGrid(m) && com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.canCraft(m[4], m[2])) {
                boolean isVoiding = tile.isDisenchanterVoiding();
                boolean isBulk = tile.isDisenchanterBulk();
                ItemStack outputStack = com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.computeResult(m[4], m[2], isVoiding, isBulk);
                if (!outputStack.isEmpty()) {
                    if (!player.inventory.addItemStackToInventory(outputStack)) player.dropItem(outputStack, false);
                    com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.consumeIngredients(m, isVoiding, isBulk);
                    for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                    onCraftMatrixChanged(matrix);
                    return true;
                }
                return false;
            }

            if (tile.hasBountifulBaublesReforger() && com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.isBaubleReforgeGrid(m) && com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.canCraft(m[8])) {
                com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.BaubleReforgeResult bbr =
                    com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.computeResult(m[8], player, tile);
                if (bbr != null && bbr.hasEnoughXp) {
                    ItemStack cleanStack = bbr.actualReforgedStack.copy();
                    if (!player.inventory.addItemStackToInventory(cleanStack)) player.dropItem(cleanStack, false);
                    com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.consumeIngredients(m, bbr, player, tile);
                    for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                    onCraftMatrixChanged(matrix);
                    return true;
                }
                return false;
            }

            if (tile.hasQualityToolsReforger() && com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.isQualityToolsGrid(m) && com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.canCraft(m[4], m[8])) {
                ItemStack outputStack = com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.computeResult(m[4], m[8], tile);
                if (!outputStack.isEmpty()) {
                    if (!player.inventory.addItemStackToInventory(outputStack)) player.dropItem(outputStack, false);
                    com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.consumeIngredients(m, tile);
                    for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                    onCraftMatrixChanged(matrix);
                    return true;
                }
                return false;
            }

            if (tile.hasFurnace() && com.brilliafy.magicstorage.util.SmeltingCraftingHelper.canCraft(m)) {
                ItemStack smelted = com.brilliafy.magicstorage.util.SmeltingCraftingHelper.computeResult(m);
                if (!smelted.isEmpty()) {
                    if (!player.inventory.addItemStackToInventory(smelted)) player.dropItem(smelted, false);
                    com.brilliafy.magicstorage.util.SmeltingCraftingHelper.consumeIngredients(m, player.getRNG());
                    for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                    onCraftMatrixChanged(matrix);
                    return true;
                }
                return false;
            }
        }

        // Vanilla Crafting Table recipe
        net.minecraft.item.crafting.IRecipe recipe = CraftingManager.findMatchingRecipe(matrix, player.world);
        if (recipe != null) {
            ItemStack res = recipe.getCraftingResult(matrix);
            if (!res.isEmpty()) {
                ItemStack resCopy = res.copy();
                if (!player.inventory.addItemStackToInventory(resCopy)) player.dropItem(resCopy, false);
                // Consume ingredients strictly from matrix
                net.minecraft.util.NonNullList<ItemStack> remaining = CraftingManager.getRemainingItems(matrix, player.world);
                for (int i = 0; i < matrix.getSizeInventory(); i++) {
                    ItemStack s = matrix.getStackInSlot(i);
                    if (!s.isEmpty()) {
                        matrix.decrStackSize(i, 1);
                    }
                    if (i < remaining.size()) {
                        ItemStack rem = remaining.get(i);
                        if (!rem.isEmpty()) {
                            if (matrix.getStackInSlot(i).isEmpty()) {
                                matrix.setInventorySlotContents(i, rem);
                            } else if (!player.inventory.addItemStackToInventory(rem)) {
                                player.dropItem(rem, false);
                            }
                        }
                    }
                }
                onCraftMatrixChanged(matrix);
                return true;
            }
        }
        return false;
    }

    protected void craftShift(EntityPlayer player, TileStorageHeart tile) {
        if (tile == null || matrix == null || result == null) return;
        if (result.getStackInSlot(0).isEmpty()) return;
        if (anvilResultLocked || bountifulBaublesResultLocked || reskillableResultLocked) return;

        this.recipeLocked = true;
        boolean anyCrafted = false;
        int maxCrafts = 64 * 36; // Guard limit

        while (maxCrafts-- > 0) {
            ItemStack res = result.getStackInSlot(0);
            if (res.isEmpty()) break;
            if (anvilResultLocked || bountifulBaublesResultLocked || reskillableResultLocked) break;

            if (!canPlayerInventoryAccept(player, res)) break;

            ItemStack[] m = new ItemStack[9];
            for (int i = 0; i < 9; i++) m[i] = matrix.getStackInSlot(i);

            boolean craftedSingle = doSingleCraftFromMatrixOnly(player, tile, m);
            if (!craftedSingle) break;
            anyCrafted = true;
        }

        this.recipeLocked = false;

        if (anyCrafted) {
            onCraftMatrixChanged(matrix);
            detectAndSendChanges();
            syncAllSlots(player);
            sendItemRefresh(player, tile);
        }
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
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            TileStorageHeart tileMaster = getTileMaster();

            // Slot 0 = craft result
            if (slotIndex == 0 && !isSimple) {
                if (playerIn.world.isRemote) return ItemStack.EMPTY;

                if (anvilResultLocked || enchantResultLocked || bountifulBaublesResultLocked || reskillableResultLocked) {
                    return ItemStack.EMPTY;
                }

                if (!slot.getHasStack()) return ItemStack.EMPTY;
                ItemStack initialTarget = slot.getStack().copy();
                if (initialTarget.isEmpty()) return ItemStack.EMPTY;

                ItemStack[] m = new ItemStack[9];
                for (int i = 0; i < 9; i++) m[i] = matrix.getStackInSlot(i);

                boolean isEnchanting = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.isEnchantingGrid(m) && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.canCraft(m[0], m[3], m[4], m[5]);
                boolean isSingleOnlyStation = isEnchanting 
                    || (com.brilliafy.magicstorage.util.AnvilCraftingHelper.isAnvilGrid(m) && com.brilliafy.magicstorage.util.AnvilCraftingHelper.canCraft(m[0], m[4], playerIn))
                    || (com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.isBaubleReforgeGrid(m) && com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.canCraft(m[8]))
                    || com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.isQualityToolsGrid(m)
                    || com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.isDisenchanterGrid(m);

                ItemStack firstCraftResult = ItemStack.EMPTY;
                int maxCrafts = isSingleOnlyStation ? 1 : (64 * 36); // Safety limit

                this.isShiftCrafting = true;
                try {
                    while (maxCrafts-- > 0) {
                        if (!slot.getHasStack()) break;
                        ItemStack currentSlotStack = slot.getStack();
                        if (currentSlotStack.isEmpty()) break;

                        // RECIPE LOCKING: If matrix output changes to a different item, stop immediately!
                        if (!net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(initialTarget, currentSlotStack)) {
                            break;
                        }

                        ItemStack craftedItem = slot.onTake(playerIn, currentSlotStack.copy());
                        if (craftedItem.isEmpty()) break;

                        if (firstCraftResult.isEmpty()) firstCraftResult = craftedItem.copy();

                        craftedItem.getItem().onCreated(craftedItem, playerIn.world, playerIn);

                        // Transfer crafted item to player inventory (slots 10 to 45)
                        if (!this.mergeItemStack(craftedItem, 10, 46, true)) {
                            playerIn.dropItem(craftedItem, false);
                            break; // Inventory full
                        }
                    }
                } finally {
                    this.isShiftCrafting = false;
                }

                if (!playerIn.world.isRemote) {
                    onCraftMatrixChanged(matrix);
                    detectAndSendChanges();
                    syncAllSlots(playerIn);
                    if (tileMaster != null) sendItemRefresh(playerIn, tileMaster);
                }
                return firstCraftResult;
            }
            // Matrix slots (1-9) -> return to network
            else if (slotIndex >= 1 && slotIndex <= 9) {
                if (playerIn.world.isRemote) return ItemStack.EMPTY;
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
                if (playerIn.world.isRemote) return ItemStack.EMPTY;
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
    public void setAll(List<ItemStack> list) {
        boolean prevLocked = this.recipeLocked;
        this.recipeLocked = true;
        try {
            for (int i = 0; i < list.size(); ++i) {
                if (i < this.inventorySlots.size()) {
                    this.getSlot(i).putStack(list.get(i));
                }
            }
        } finally {
            this.recipeLocked = prevLocked;
        }
        onCraftMatrixChanged(matrix);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, net.minecraft.inventory.ClickType clickTypeIn, EntityPlayer player) {
        // Handle hotkey (SWAP) on result slot cleanly on both client and server
        if (clickTypeIn == net.minecraft.inventory.ClickType.SWAP && slotId == 0) {
            Slot slot = inventorySlots.get(0);
            if (slot == null || !slot.canTakeStack(player)) return ItemStack.EMPTY;
            if (slot.getHasStack()) {
                ItemStack targetStack = slot.getStack();
                if (targetStack.isEmpty()) return ItemStack.EMPTY;

                ItemStack resultStack = slot.onTake(player, targetStack.copy());
                if (resultStack.isEmpty()) return ItemStack.EMPTY;

                resultStack.getItem().onCreated(resultStack, player.world, player);
                ItemStack currentHotbar = player.inventory.getStackInSlot(dragType);

                boolean moved = false;
                if (currentHotbar.isEmpty()) {
                    player.inventory.setInventorySlotContents(dragType, resultStack);
                    moved = true;
                } else if (net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(currentHotbar, resultStack)) {
                    int space = currentHotbar.getMaxStackSize() - currentHotbar.getCount();
                    if (space >= resultStack.getCount()) {
                        currentHotbar.grow(resultStack.getCount());
                        moved = true;
                    } else {
                        currentHotbar.grow(space);
                        resultStack.shrink(space);
                        moved = this.mergeItemStack(resultStack, 10, 46, true);
                    }
                } else {
                    moved = this.mergeItemStack(resultStack, 10, 46, true);
                }

                if (!moved) {
                    player.dropItem(resultStack, false);
                }

                if (!player.world.isRemote) {
                    onCraftMatrixChanged(matrix);
                    detectAndSendChanges();
                    TileStorageHeart master = getTileMaster();
                    if (master != null) sendItemRefresh(player, master);
                    if (player instanceof EntityPlayerMP) {
                        EntityPlayerMP mp = (EntityPlayerMP) player;
                        mp.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(this.windowId, 0, result != null ? result.getStackInSlot(0) : ItemStack.EMPTY));
                        mp.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(this.windowId, dragType + 37, player.inventory.getStackInSlot(dragType)));
                    }
                }
                return targetStack;
            }
            return ItemStack.EMPTY;
        }

        // Capture matrix state before super processes the click
        ItemStack[] beforeMatrix = null;
        if (matrix != null && !player.world.isRemote) {
            beforeMatrix = new ItemStack[9];
            for (int i = 0; i < 9; i++) beforeMatrix[i] = matrix.getStackInSlot(i).copy();
        }

        ItemStack result = super.slotClick(slotId, dragType, clickTypeIn, player);

        // If a matrix slot changed or slot 0 was clicked, force recipe re-check and sync result
        if (matrix != null && !player.world.isRemote) {
            boolean changed = false;
            if (beforeMatrix != null) {
                for (int i = 0; i < 9; i++) {
                    ItemStack b = beforeMatrix[i];
                    ItemStack a = matrix.getStackInSlot(i);
                    if (!ItemStack.areItemStacksEqual(b, a)) {
                        changed = true;
                        break;
                    }
                }
            }
            if (changed || slotId == 0) {
                onCraftMatrixChanged(matrix);
                detectAndSendChanges();
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
            if (!playerIn.world.isRemote && (anvilResultLocked || enchantResultLocked || bountifulBaublesResultLocked || reskillableResultLocked)) return false;
            ItemStack cur = getStack();
            if (cur.isEmpty()) return false;

            if (cur.hasTagCompound()) {
                net.minecraft.nbt.NBTTagCompound display = cur.getSubCompound("display");
                if (display != null && display.hasKey("Lore", 9)) {
                    net.minecraft.nbt.NBTTagList lore = display.getTagList("Lore", 8);
                    for (int i = 0; i < lore.tagCount(); i++) {
                        String s = lore.getStringTagAt(i);
                        if (s.contains("Insufficient XP") || s.contains("Insufficient Level") || s.contains("Insufficient Skill Level")) {
                            return false;
                        }
                    }
                }
            }

            if (!playerIn.world.isRemote) {
                ItemStack[] m = new ItemStack[9];
                for (int i = 0; i < 9; i++) m[i] = matrix.getStackInSlot(i);

                // Check Enchanting Table XP / Level requirement
                if (com.brilliafy.magicstorage.util.EnchantingCraftingHelper.isEnchantingGrid(m) && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.canCraft(m[0], m[3], m[4], m[5])) {
                    int slot = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getEnchantTier(m[3], m[4], m[5]) - 1;
                    if (slot < 0) return false;
                    TileStorageHeart master = getTileMaster();
                    int power = (master != null) ? com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getPowerFromHeart(master, playerIn.world) : 0;
                    com.brilliafy.magicstorage.util.EnchantingCraftingHelper.EnchantResult er =
                        com.brilliafy.magicstorage.util.EnchantingCraftingHelper.simulateEnchant(m[0], playerIn, power, slot);
                    if (er == null) return false;
                    int reqXp = Math.max(er.xpCost, er.enchantLevel);
                    if (!com.brilliafy.magicstorage.util.EnchantingCraftingHelper.hasEnoughXp(playerIn, reqXp)) {
                        return false;
                    }
                }

                // Check Anvil XP
                if (com.brilliafy.magicstorage.util.AnvilCraftingHelper.isAnvilGrid(m) && com.brilliafy.magicstorage.util.AnvilCraftingHelper.canCraft(m[0], m[4], playerIn)) {
                    com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilResult ar = com.brilliafy.magicstorage.util.AnvilCraftingHelper.computeResult(m[0], m[4], playerIn);
                    if (ar == null || !com.brilliafy.magicstorage.util.AnvilCraftingHelper.hasEnoughXp(playerIn, ar.cost)) {
                        return false;
                    }
                }

                // Check Bountiful Baubles Reforge XP
                if (com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.isBaubleReforgeGrid(m) && com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.canCraft(m[8])) {
                    com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.BaubleReforgeResult bbr =
                        com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.computeResult(m[8], playerIn, getTileMaster());
                    if (bbr == null || !bbr.hasEnoughXp) {
                        return false;
                    }
                }
            }

            return super.canTakeStack(playerIn);
        }

        @Override
        public ItemStack decrStackSize(int amount) {
            if (!canTakeStack(playerInv.player)) return ItemStack.EMPTY;
            return super.decrStackSize(amount);
        }

        public void syncAllSlots(EntityPlayer playerIn) {
            ContainerMagicStorageBase.this.syncAllSlots(playerIn);
        }

        public boolean isCustomRecipe(ItemStack[] m) {
            if (m == null || m.length < 9) return false;
            if (com.brilliafy.magicstorage.config.ModConfig.enableEnchantingTable && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.isEnchantingGrid(m) && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.canCraft(m[0], m[3], m[4], m[5])) return true;
            if (com.brilliafy.magicstorage.config.ModConfig.enableAnvil && com.brilliafy.magicstorage.util.AnvilCraftingHelper.isAnvilGrid(m) && com.brilliafy.magicstorage.util.AnvilCraftingHelper.canCraft(m[0], m[4], playerInv.player)) return true;
            if (com.brilliafy.magicstorage.config.ModConfig.enableBrewingStand && com.brilliafy.magicstorage.util.PotionCraftingHelper.isPotionGrid(m)) return true;
            if (com.brilliafy.magicstorage.config.ModConfig.enableRusticAlchemy && (com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftAdvancedCondenser(m) || com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftSimpleCondenser(m))) return true;
            if (com.brilliafy.magicstorage.config.ModConfig.enableRusticBrewing && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftBrewing(m, null)) return true;
            if (com.brilliafy.magicstorage.config.ModConfig.enableRusticCrushing && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftCrushing(m)) return true;
            if (com.brilliafy.magicstorage.config.ModConfig.enableDisenchanterTable && com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.isDisenchanterGrid(m)) return true;
            if (com.brilliafy.magicstorage.config.ModConfig.enableBountifulBaublesReforger && com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.isBaubleReforgeGrid(m) && com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.canCraft(m[8])) return true;
            if (com.brilliafy.magicstorage.config.ModConfig.enableQualityToolsReforger && com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.isQualityToolsGrid(m)) return true;
            if (com.brilliafy.magicstorage.config.ModConfig.enableFurnace && com.brilliafy.magicstorage.util.SmeltingCraftingHelper.canCraft(m)) return true;
            return false;
        }

        private void handleCustomAutofill(EntityPlayer playerIn, TileStorageHeart master, ItemStack[] beforeMatrix, boolean isAlchemy) {
            if (isShiftCrafting || autofillMode == 0 || master == null) return;
            boolean allowInventory = autofillMode == 2;

            // 1. Specific Alchemy Water Bucket autofill:
            // If slot 8 was WATER_BUCKET and is now empty BUCKET, replace with another WATER_BUCKET from network/inventory
            if (isAlchemy && beforeMatrix != null && beforeMatrix.length > 8 && !beforeMatrix[8].isEmpty() && beforeMatrix[8].getItem() == net.minecraft.init.Items.WATER_BUCKET) {
                ItemStack currentSlot8 = matrix.getStackInSlot(8);
                if (!currentSlot8.isEmpty() && currentSlot8.getItem() == net.minecraft.init.Items.BUCKET) {
                    ItemStack waterBucket = master.extractItem(s -> !s.isEmpty() && s.getItem() == net.minecraft.init.Items.WATER_BUCKET, 1, false);
                    if (!waterBucket.isEmpty()) {
                        ItemStack leftoverBucket = master.insertItem(currentSlot8, false);
                        if (!leftoverBucket.isEmpty()) {
                            if (!playerIn.inventory.addItemStackToInventory(leftoverBucket)) {
                                playerIn.dropItem(leftoverBucket, false);
                            }
                        }
                        matrix.setInventorySlotContents(8, waterBucket);
                    } else if (allowInventory) {
                        for (int slotIdx = 0; slotIdx < playerIn.inventory.mainInventory.size(); slotIdx++) {
                            ItemStack invStack = playerIn.inventory.mainInventory.get(slotIdx);
                            if (!invStack.isEmpty() && invStack.getItem() == net.minecraft.init.Items.WATER_BUCKET) {
                                ItemStack takenBucket = invStack.splitStack(1);
                                playerIn.inventory.addItemStackToInventory(currentSlot8);
                                matrix.setInventorySlotContents(8, takenBucket);
                                break;
                            }
                        }
                    }
                }
            }

            // 2. Crushing Tub ingredient autofill:
            if (beforeMatrix != null && !beforeMatrix[0].isEmpty() && com.brilliafy.magicstorage.util.RusticCraftingHelper.isCrushable(beforeMatrix[0])) {
                int reqCrush = com.brilliafy.magicstorage.util.RusticCraftingHelper.getRequiredCrushingAmount(beforeMatrix[0]);
                int curCrush = matrix.getStackInSlot(0).isEmpty() ? 0 : matrix.getStackInSlot(0).getCount();
                if (curCrush < reqCrush) {
                    int needed = reqCrush - curCrush;
                    ItemStack reqStack = master.extractItem(
                        s -> net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(s, beforeMatrix[0]),
                        needed, false);
                    if ((reqStack.isEmpty() || reqStack.getCount() < needed) && allowInventory) {
                        int remaining = needed - reqStack.getCount();
                        for (int slotIdx = 0; slotIdx < playerIn.inventory.mainInventory.size(); slotIdx++) {
                            ItemStack invStack = playerIn.inventory.mainInventory.get(slotIdx);
                            if (!invStack.isEmpty() && net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(invStack, beforeMatrix[0])) {
                                ItemStack taken = invStack.splitStack(Math.min(remaining, invStack.getCount()));
                                if (reqStack.isEmpty()) {
                                    reqStack = taken;
                                } else {
                                    reqStack.grow(taken.getCount());
                                }
                                remaining -= taken.getCount();
                                if (remaining <= 0) break;
                            }
                        }
                    }
                    if (!reqStack.isEmpty()) {
                        if (matrix.getStackInSlot(0).isEmpty()) {
                            matrix.setInventorySlotContents(0, reqStack);
                        } else {
                            matrix.getStackInSlot(0).grow(reqStack.getCount());
                        }
                    }
                }
            }

            // 3. Generic autofill for any slots that became empty:
            if (beforeMatrix != null) {
                for (int i = 0; i < 9; i++) {
                    if (i == 0 && com.brilliafy.magicstorage.util.RusticCraftingHelper.isCrushable(beforeMatrix[0])) {
                        continue;
                    }
                    if (matrix.getStackInSlot(i).isEmpty() && !beforeMatrix[i].isEmpty()) {
                        final int slot = i;
                        int fillAmount = 1;
                        ItemStack req = master.extractItem(
                            s -> net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(s, beforeMatrix[slot]),
                            fillAmount, false);

                        if ((req.isEmpty() || req.getCount() < fillAmount) && allowInventory) {
                            int needed = fillAmount - req.getCount();
                            for (int slotIdx = 0; slotIdx < playerIn.inventory.mainInventory.size(); slotIdx++) {
                                ItemStack invStack = playerIn.inventory.mainInventory.get(slotIdx);
                                if (!invStack.isEmpty() && net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(invStack, beforeMatrix[slot])) {
                                    ItemStack taken = invStack.splitStack(Math.min(needed, invStack.getCount()));
                                    if (req.isEmpty()) {
                                        req = taken;
                                    } else {
                                        req.grow(taken.getCount());
                                    }
                                    needed -= taken.getCount();
                                    if (needed <= 0) break;
                                }
                            }
                        }

                        if (!req.isEmpty()) matrix.setInventorySlotContents(i, req);
                    }
                }

                // 3. Furnace Fuel Autofill to meet minimum required burn time:
                if (beforeMatrix.length > 4 && !beforeMatrix[4].isEmpty() && com.brilliafy.magicstorage.util.SmeltingCraftingHelper.isFuel(beforeMatrix[4])) {
                    int totalInputs = 0;
                    for (int i = 0; i < 9; i++) {
                        if (i != 4 && !matrix.getStackInSlot(i).isEmpty() && com.brilliafy.magicstorage.util.SmeltingCraftingHelper.isSmeltable(matrix.getStackInSlot(i))) {
                            totalInputs += 1;
                        }
                    }
                    if (totalInputs > 0) {
                        ItemStack fuelCandidate = beforeMatrix[4];
                        int singleBurn = com.brilliafy.magicstorage.util.SmeltingCraftingHelper.getSingleBurnTime(fuelCandidate);
                        if (singleBurn > 0) {
                            int neededFuel = (int) Math.ceil((double)(totalInputs * 200) / (double) singleBurn);
                            neededFuel = Math.min(neededFuel, fuelCandidate.getMaxStackSize());
                            int currentFuel = matrix.getStackInSlot(4).isEmpty() ? 0 : matrix.getStackInSlot(4).getCount();
                            if (currentFuel < neededFuel) {
                                int toExtract = neededFuel - currentFuel;
                                ItemStack reqFuel = master.extractItem(
                                    s -> net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(s, fuelCandidate),
                                    toExtract, false);
                                if ((reqFuel.isEmpty() || reqFuel.getCount() < toExtract) && allowInventory) {
                                    int remaining = toExtract - reqFuel.getCount();
                                    for (int slotIdx = 0; slotIdx < playerIn.inventory.mainInventory.size(); slotIdx++) {
                                        ItemStack invStack = playerIn.inventory.mainInventory.get(slotIdx);
                                        if (!invStack.isEmpty() && net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(invStack, fuelCandidate)) {
                                            ItemStack taken = invStack.splitStack(Math.min(remaining, invStack.getCount()));
                                            if (reqFuel.isEmpty()) {
                                                reqFuel = taken;
                                            } else {
                                                reqFuel.grow(taken.getCount());
                                            }
                                            remaining -= taken.getCount();
                                            if (remaining <= 0) break;
                                        }
                                    }
                                }
                                if (!reqFuel.isEmpty()) {
                                    if (matrix.getStackInSlot(4).isEmpty()) {
                                        matrix.setInventorySlotContents(4, reqFuel);
                                    } else {
                                        matrix.getStackInSlot(4).grow(reqFuel.getCount());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public ItemStack onTake(EntityPlayer playerIn, ItemStack stack) {
            TileStorageHeart master = getTileMaster();
            
            ItemStack[] m = new ItemStack[9];
            for (int i = 0; i < 9; i++) m[i] = matrix.getStackInSlot(i);

            // Client side: predict matrix decrement for smooth response, server sends official slots
            if (playerIn.world.isRemote) {
                ItemStack cur = getStack();
                if (!cur.isEmpty() && cur.hasTagCompound()) {
                    net.minecraft.nbt.NBTTagCompound display = cur.getSubCompound("display");
                    if (display != null && display.hasKey("Lore", 9)) {
                        net.minecraft.nbt.NBTTagList lore = display.getTagList("Lore", 8);
                        for (int i = 0; i < lore.tagCount(); i++) {
                            if (lore.getStringTagAt(i).contains("Insufficient")) {
                                playerIn.inventory.setItemStack(ItemStack.EMPTY);
                                return ItemStack.EMPTY;
                            }
                        }
                    }
                }
                if (isCustomRecipe(m)) {
                    if (com.brilliafy.magicstorage.util.PotionCraftingHelper.isPotionGrid(m)) {
                        if (!m[1].isEmpty()) m[1].shrink(1);
                        for (int i = 3; i <= 5; i++) if (!m[i].isEmpty()) m[i].shrink(1);
                    } else if (com.brilliafy.magicstorage.util.SmeltingCraftingHelper.canCraft(m)) {
                        for (int i = 0; i < 9; i++) if (i != 4 && !m[i].isEmpty()) m[i].shrink(1);
                        int singleBurn = com.brilliafy.magicstorage.util.SmeltingCraftingHelper.getSingleBurnTime(m[4]);
                        if (singleBurn > 0) {
                            int needed = (int) Math.floor(200.0 / singleBurn);
                            if (needed > 0) m[4].shrink(Math.min(needed, m[4].getCount()));
                        }
                    } else if (com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftCrushing(m)) {
                        com.brilliafy.magicstorage.util.RusticCraftingHelper.consumeCrushingIngredients(m);
                    } else if (com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftBrewing(m, null)) {
                        if (!m[4].isEmpty()) m[4].shrink(m[4].getCount());
                    } else if (com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftSimpleCondenser(m) || com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftAdvancedCondenser(m)) {
                        int reqTicks = com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftAdvancedCondenser(m) ? 300 : 400;
                        if (!m[0].isEmpty()) m[0].shrink(1);
                        if (!m[1].isEmpty()) m[1].shrink(1);
                        if (!m[2].isEmpty()) m[2].shrink(1);
                        if (!m[3].isEmpty()) m[3].shrink(1);
                        if (!m[6].isEmpty()) m[6].shrink(1);
                        if (!m[7].isEmpty()) {
                            int singleBurn = net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(m[7]);
                            if (singleBurn > 0) {
                                int needed = (int) Math.floor((double) reqTicks / (double) singleBurn);
                                if (needed > 0) m[7].shrink(Math.min(needed, m[7].getCount()));
                            }
                        }
                    } else if (com.brilliafy.magicstorage.util.EnchantingCraftingHelper.isEnchantingGrid(m) && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.canCraft(m[0], m[3], m[4], m[5])) {
                        int slot = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getEnchantTier(m[3], m[4], m[5]) - 1;
                        if (slot >= 0) {
                            com.brilliafy.magicstorage.util.EnchantingCraftingHelper.EnchantResult er =
                                com.brilliafy.magicstorage.util.EnchantingCraftingHelper.simulateEnchant(m[0], playerIn, 15, slot);
                            if (er != null) {
                                ItemStack savedItem = m[0].copy();
                                com.brilliafy.magicstorage.util.EnchantingCraftingHelper.consumeIngredients(m);
                                ItemStack realItem = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.applyEnchantList(savedItem, er.enchantments);
                                stack.setTagCompound(realItem.hasTagCompound() ? realItem.getTagCompound().copy() : null);
                                stack.setCount(1);
                            } else {
                                com.brilliafy.magicstorage.util.EnchantingCraftingHelper.consumeIngredients(m);
                                if (stack.hasTagCompound()) {
                                    stack.getTagCompound().removeTag("display");
                                    stack.getTagCompound().removeTag("HideFlags");
                                }
                            }
                        }
                    } else if (com.brilliafy.magicstorage.util.AnvilCraftingHelper.isAnvilGrid(m) && com.brilliafy.magicstorage.util.AnvilCraftingHelper.canCraft(m[0], m[4], playerIn)) {
                        com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilResult ar = com.brilliafy.magicstorage.util.AnvilCraftingHelper.computeResult(m[0], m[4], playerIn);
                        if (!m[0].isEmpty()) m[0].shrink(1);
                        if (!m[4].isEmpty()) m[4].shrink(1);
                        if (ar != null && ar.stack != null && !ar.stack.isEmpty()) {
                            stack.setTagCompound(ar.stack.hasTagCompound() ? ar.stack.getTagCompound().copy() : null);
                            stack.setCount(1);
                        }
                    } else if (com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.isDisenchanterGrid(m)) {
                        if (!m[2].isEmpty()) m[2].shrink(1);
                    } else if (com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.isBaubleReforgeGrid(m) || com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.isQualityToolsGrid(m)) {
                        if (!m[8].isEmpty()) m[8].shrink(1);
                        m[4] = ItemStack.EMPTY;
                    }
                    for (int i = 0; i < 9; i++) matrix.setInventorySlotContents(i, m[i]);
                    onCraftMatrixChanged(matrix);
                    return stack;
                }

                // Vanilla crafting table on client side
                net.minecraft.util.NonNullList<ItemStack> nonnulllist = net.minecraft.item.crafting.CraftingManager.getRemainingItems(matrix, playerIn.world);
                for (int i = 0; i < 9; ++i) {
                    ItemStack itemstack = matrix.getStackInSlot(i);
                    ItemStack rem = nonnulllist.get(i);
                    if (!itemstack.isEmpty()) {
                        matrix.decrStackSize(i, 1);
                    }
                    if (!rem.isEmpty()) {
                        if (matrix.getStackInSlot(i).isEmpty()) {
                            matrix.setInventorySlotContents(i, rem);
                        } else if (!playerIn.inventory.addItemStackToInventory(rem)) {
                            playerIn.dropItem(rem, false);
                        }
                    }
                }
                this.onCrafting(stack);
                onCraftMatrixChanged(matrix);
                return stack;
            }
            
            // For custom recipes: handle everything ourselves, skip super.onTake
            if (ContainerMagicStorageBase.this.anvilResultLocked || ContainerMagicStorageBase.this.enchantResultLocked || ContainerMagicStorageBase.this.bountifulBaublesResultLocked || ContainerMagicStorageBase.this.reskillableResultLocked) {
                return ItemStack.EMPTY;
            }

            ItemStack[] beforeMatrix = new ItemStack[9];
            for (int i = 0; i < 9; i++) beforeMatrix[i] = m[i].copy();

            if (master != null) {
                if (master.hasEnchantingTable() && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.isEnchantingGrid(m) && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.canCraft(m[0], m[3], m[4], m[5])) {
                    int slot = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getEnchantTier(m[3], m[4], m[5]) - 1;
                    if (slot >= 0) {
                        int power = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.getPowerFromHeart(master, playerIn.world);
                        com.brilliafy.magicstorage.util.EnchantingCraftingHelper.EnchantResult er =
                            com.brilliafy.magicstorage.util.EnchantingCraftingHelper.simulateEnchant(m[0], playerIn, power, slot);
                        if (er != null && com.brilliafy.magicstorage.util.EnchantingCraftingHelper.hasEnoughXp(playerIn, Math.max(er.xpCost, er.enchantLevel))) {
                            ItemStack savedItem = m[0].copy();
                            com.brilliafy.magicstorage.util.EnchantingCraftingHelper.consumeIngredients(m);
                            ItemStack realItem = com.brilliafy.magicstorage.util.EnchantingCraftingHelper.applyEnchantList(savedItem, er.enchantments);
                            com.brilliafy.magicstorage.util.EnchantingCraftingHelper.prepareEnchantContext(er.enchantLevel);
                            try {
                                playerIn.onEnchant(realItem, er.xpCost);
                            } finally {
                                com.brilliafy.magicstorage.util.EnchantingCraftingHelper.clearEnchantContext();
                            }
                            playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                            stack.setTagCompound(realItem.hasTagCompound() ? realItem.getTagCompound().copy() : null);
                            stack.setCount(1);
                            for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                            handleCustomAutofill(playerIn, master, beforeMatrix, false);
                            onCraftMatrixChanged(matrix);
                            detectAndSendChanges();
                            syncAllSlots(playerIn);
                            if (playerIn instanceof net.minecraft.entity.player.EntityPlayerMP) {
                                ((net.minecraft.entity.player.EntityPlayerMP) playerIn).connection.sendPacket(
                                    new net.minecraft.network.play.server.SPacketSetExperience(
                                        playerIn.experience, playerIn.experienceTotal, playerIn.experienceLevel));
                            }
                            sendItemRefresh(playerIn, master);
                            return realItem;
                        }
                    }
                    return ItemStack.EMPTY;
                }
                if (master.hasAnvil() && com.brilliafy.magicstorage.util.AnvilCraftingHelper.isAnvilGrid(m) && com.brilliafy.magicstorage.util.AnvilCraftingHelper.canCraft(m[0], m[4], playerIn)) {
                    com.brilliafy.magicstorage.util.AnvilCraftingHelper.AnvilResult ar = com.brilliafy.magicstorage.util.AnvilCraftingHelper.computeResult(m[0], m[4], playerIn);
                    if (ar != null && com.brilliafy.magicstorage.util.AnvilCraftingHelper.hasEnoughXp(playerIn, ar.cost)) {
                        stack.onCrafting(playerIn.world, playerIn, 1);
                        com.brilliafy.magicstorage.util.AnvilCraftingHelper.consumeIngredients(m, ar);
                        com.brilliafy.magicstorage.util.AnvilCraftingHelper.consumeXp(playerIn, ar.cost);
                        playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_ANVIL_USE, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                        ItemStack realResult = ar.stack.copy();
                        stack.setTagCompound(realResult.getTagCompound());
                        stack.setCount(1);
                        for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                        handleCustomAutofill(playerIn, master, beforeMatrix, false);
                        onCraftMatrixChanged(matrix);
                        detectAndSendChanges();
                        syncAllSlots(playerIn);
                        if (playerIn instanceof net.minecraft.entity.player.EntityPlayerMP) {
                            ((net.minecraft.entity.player.EntityPlayerMP) playerIn).connection.sendPacket(
                                new net.minecraft.network.play.server.SPacketSetExperience(
                                    playerIn.experience, playerIn.experienceTotal, playerIn.experienceLevel));
                        }
                        sendItemRefresh(playerIn, master);
                        return stack;
                    }
                    return ItemStack.EMPTY;
                }
                if (master.hasBrewingStand() && com.brilliafy.magicstorage.util.PotionCraftingHelper.isPotionGrid(m) && com.brilliafy.magicstorage.util.PotionCraftingHelper.canCraft(m[0], m[1], m[3], m[4], m[5])) {
                    List<ItemStack> results = com.brilliafy.magicstorage.util.PotionCraftingHelper.computeResult(m);
                    if (!results.isEmpty()) {
                        stack.onCrafting(playerIn.world, playerIn, 1);
                        com.brilliafy.magicstorage.util.PotionCraftingHelper.consumeIngredients(m, playerIn.getRNG());
                        playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_BREWING_STAND_BREW, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                        for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                        handleCustomAutofill(playerIn, master, beforeMatrix, false);
                        onCraftMatrixChanged(matrix);
                        detectAndSendChanges();
                        syncAllSlots(playerIn);
                        sendItemRefresh(playerIn, master);
                        return stack;
                    }
                    return ItemStack.EMPTY;
                }
                if (master.hasRusticAdvancedCondenser() && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftAdvancedCondenser(m)) {
                    ItemStack res = com.brilliafy.magicstorage.util.RusticCraftingHelper.computeAdvancedCondenserResult(m);
                    if (!res.isEmpty()) {
                        stack.onCrafting(playerIn.world, playerIn, 1);
                        com.brilliafy.magicstorage.util.RusticCraftingHelper.consumeAdvancedCondenserIngredients(m, playerIn.getRNG());
                        playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_BREWING_STAND_BREW, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                        for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                        handleCustomAutofill(playerIn, master, beforeMatrix, true);
                        onCraftMatrixChanged(matrix);
                        detectAndSendChanges();
                        syncAllSlots(playerIn);
                        sendItemRefresh(playerIn, master);
                        return stack;
                    }
                    return ItemStack.EMPTY;
                }
                if (master.hasRusticSimpleCondenser() && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftSimpleCondenser(m)) {
                    ItemStack res = com.brilliafy.magicstorage.util.RusticCraftingHelper.computeSimpleCondenserResult(m);
                    if (!res.isEmpty()) {
                        stack.onCrafting(playerIn.world, playerIn, 1);
                        com.brilliafy.magicstorage.util.RusticCraftingHelper.consumeSimpleCondenserIngredients(m, playerIn.getRNG());
                        playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_BREWING_STAND_BREW, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                        for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                        handleCustomAutofill(playerIn, master, beforeMatrix, true);
                        onCraftMatrixChanged(matrix);
                        detectAndSendChanges();
                        syncAllSlots(playerIn);
                        sendItemRefresh(playerIn, master);
                        return stack;
                    }
                    return ItemStack.EMPTY;
                }
                if (master.hasRusticBrewingBarrel() && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftBrewing(m, master)) {
                    ItemStack res = com.brilliafy.magicstorage.util.RusticCraftingHelper.computeBrewingResult(m, master);
                    if (!res.isEmpty()) {
                        stack.onCrafting(playerIn.world, playerIn, 1);
                        com.brilliafy.magicstorage.util.RusticCraftingHelper.consumeBrewingIngredients(m, playerIn.getRNG(), master);
                        playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_BREWING_STAND_BREW, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                        for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                        handleCustomAutofill(playerIn, master, beforeMatrix, false);
                        onCraftMatrixChanged(matrix);
                        detectAndSendChanges();
                        syncAllSlots(playerIn);
                        sendItemRefresh(playerIn, master);
                        return stack;
                    }
                    return ItemStack.EMPTY;
                }
                if (master.hasRusticCrushingTub() && com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftCrushing(m)) {
                    ItemStack res = com.brilliafy.magicstorage.util.RusticCraftingHelper.computeCrushingResult(m);
                    if (!res.isEmpty()) {
                        stack.onCrafting(playerIn.world, playerIn, 1);
                        com.brilliafy.magicstorage.util.RusticCraftingHelper.consumeCrushingIngredients(m);
                        playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_SLIME_FALL, net.minecraft.util.SoundCategory.BLOCKS, 0.5F, MagicStorageRandom.nextFloat() * 0.1F + 0.9F);
                        for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                        handleCustomAutofill(playerIn, master, beforeMatrix, false);
                        onCraftMatrixChanged(matrix);
                        detectAndSendChanges();
                        syncAllSlots(playerIn);
                        sendItemRefresh(playerIn, master);
                        return stack;
                    }
                    return ItemStack.EMPTY;
                }
                if (master.hasDisenchanterTable() && com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.isDisenchanterGrid(m) && com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.canCraft(m[4], m[2])) {
                    boolean isVoiding = master.isDisenchanterVoiding();
                    boolean isBulk = master.isDisenchanterBulk();
                    ItemStack res = com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.computeResult(m[4], m[2], isVoiding, isBulk);
                    if (!res.isEmpty()) {
                        stack.onCrafting(playerIn.world, playerIn, 1);
                        com.brilliafy.magicstorage.util.DisenchanterCraftingHelper.consumeIngredients(m, isVoiding, isBulk);
                        playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                        for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                        handleCustomAutofill(playerIn, master, beforeMatrix, false);
                        onCraftMatrixChanged(matrix);
                        detectAndSendChanges();
                        syncAllSlots(playerIn);
                        sendItemRefresh(playerIn, master);
                        return stack;
                    }
                    return ItemStack.EMPTY;
                }
                if (master.hasBountifulBaublesReforger() && com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.isBaubleReforgeGrid(m) && com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.canCraft(m[8])) {
                    com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.BaubleReforgeResult bbr =
                        com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.computeResult(m[8], playerIn, master);
                    if (bbr != null && bbr.hasEnoughXp) {
                        stack.onCrafting(playerIn.world, playerIn, 1);
                        com.brilliafy.magicstorage.util.BountifulBaublesCraftingHelper.consumeIngredients(m, bbr, playerIn, master);
                        playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_ANVIL_USE, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                        ItemStack realResult = bbr.actualReforgedStack.copy();
                        stack.setTagCompound(realResult.hasTagCompound() ? realResult.getTagCompound().copy() : null);
                        stack.setCount(1);
                        for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                        handleCustomAutofill(playerIn, master, beforeMatrix, false);
                        onCraftMatrixChanged(matrix);
                        detectAndSendChanges();
                        syncAllSlots(playerIn);
                        if (playerIn instanceof net.minecraft.entity.player.EntityPlayerMP) {
                            ((net.minecraft.entity.player.EntityPlayerMP) playerIn).connection.sendPacket(
                                new net.minecraft.network.play.server.SPacketSetExperience(
                                    playerIn.experience, playerIn.experienceTotal, playerIn.experienceLevel));
                        }
                        sendItemRefresh(playerIn, master);
                        return stack;
                    }
                    return ItemStack.EMPTY;
                }
                if (master.hasQualityToolsReforger() && com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.isQualityToolsGrid(m) && com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.canCraft(m[4], m[8])) {
                    ItemStack res = com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.computeResult(m[4], m[8], master);
                    if (!res.isEmpty()) {
                        stack.onCrafting(playerIn.world, playerIn, 1);
                        com.brilliafy.magicstorage.util.QualityToolsCraftingHelper.consumeIngredients(m, master);
                        playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_ANVIL_USE, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                        ItemStack realResult = res.copy();
                        stack.setTagCompound(realResult.hasTagCompound() ? realResult.getTagCompound().copy() : null);
                        stack.setCount(1);
                        for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                        handleCustomAutofill(playerIn, master, beforeMatrix, false);
                        onCraftMatrixChanged(matrix);
                        detectAndSendChanges();
                        syncAllSlots(playerIn);
                        sendItemRefresh(playerIn, master);
                        return stack;
                    }
                    return ItemStack.EMPTY;
                }
                if (master.hasFurnace() && com.brilliafy.magicstorage.util.SmeltingCraftingHelper.canCraft(m)) {
                    stack.onCrafting(playerIn.world, playerIn, 1);
                    com.brilliafy.magicstorage.util.SmeltingCraftingHelper.consumeIngredients(m, playerIn.getRNG());
                    playerIn.world.playSound(null, playerIn.getPosition(), net.minecraft.init.SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);
                    for (int j = 0; j < 9; j++) matrix.setInventorySlotContents(j, m[j]);
                    handleCustomAutofill(playerIn, master, beforeMatrix, false);
                    onCraftMatrixChanged(matrix);
                    detectAndSendChanges();
                    syncAllSlots(playerIn);
                    sendItemRefresh(playerIn, master);
                    return stack;
                }
            }
            
            // Vanilla recipe: handle remaining items and matrix decr cleanly without vanilla SlotCrafting.grow bug
            List<ItemStack> lis = new ArrayList<>();
            for (int i = 0; i < matrix.getSizeInventory(); i++)
                lis.add(matrix.getStackInSlot(i).copy());

            net.minecraftforge.common.ForgeHooks.setCraftingPlayer(playerIn);
            net.minecraft.util.NonNullList<ItemStack> nonnulllist = net.minecraft.item.crafting.CraftingManager.getRemainingItems(matrix, playerIn.world);
            net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);
            for (int i = 0; i < nonnulllist.size(); ++i) {
                ItemStack itemstack = matrix.getStackInSlot(i);
                ItemStack rem = nonnulllist.get(i);
                if (!itemstack.isEmpty()) {
                    matrix.decrStackSize(i, 1);
                }
                if (!rem.isEmpty()) {
                    if (matrix.getStackInSlot(i).isEmpty()) {
                        matrix.setInventorySlotContents(i, rem);
                    } else if (!playerIn.inventory.addItemStackToInventory(rem)) {
                        playerIn.dropItem(rem, false);
                    }
                }
            }
            this.onCrafting(stack);
            // Auto-refill empty matrix slots: Priority 1 = Network, Priority 2 = Player Inventory (only during normal craft, NOT shift craft)
            if (!isShiftCrafting && autofillMode != 0) {
                boolean allowInventory = autofillMode == 2;
                for (int i = 0; i < matrix.getSizeInventory(); i++) {
                    if (matrix.getStackInSlot(i).isEmpty() && !lis.get(i).isEmpty()) {
                        final int slot = i;
                        int fillAmount = 1;
                        if (com.brilliafy.magicstorage.util.RusticCraftingHelper.canCraftCrushing(lis.toArray(new ItemStack[0])) && slot == 0) {
                            fillAmount = 4;
                        }
                        // 1st Priority: Storage Network
                        ItemStack req = master != null ? master.extractItem(
                            s -> net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(s, lis.get(slot)),
                            fillAmount, false) : ItemStack.EMPTY;

                        // 2nd Priority: Player Inventory
                        if ((req.isEmpty() || req.getCount() < fillAmount) && allowInventory) {
                            int needed = fillAmount - req.getCount();
                            for (int slotIdx = 0; slotIdx < playerIn.inventory.mainInventory.size(); slotIdx++) {
                                ItemStack invStack = playerIn.inventory.mainInventory.get(slotIdx);
                                if (!invStack.isEmpty() && net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(invStack, lis.get(slot))) {
                                    ItemStack taken = invStack.splitStack(Math.min(needed, invStack.getCount()));
                                    if (req.isEmpty()) {
                                        req = taken;
                                    } else {
                                        req.grow(taken.getCount());
                                    }
                                    needed -= taken.getCount();
                                    if (needed <= 0) break;
                                }
                            }
                        }

                        if (!req.isEmpty()) matrix.setInventorySlotContents(i, req);
                    }
                }

                // Furnace Fuel Autofill to meet minimum required burn time:
                if (lis.size() > 4 && !lis.get(4).isEmpty() && com.brilliafy.magicstorage.util.SmeltingCraftingHelper.isFuel(lis.get(4))) {
                    int totalInputs = 0;
                    for (int i = 0; i < 9; i++) {
                        if (i != 4 && !matrix.getStackInSlot(i).isEmpty() && com.brilliafy.magicstorage.util.SmeltingCraftingHelper.isSmeltable(matrix.getStackInSlot(i))) {
                            totalInputs += 1;
                        }
                    }
                    if (totalInputs > 0) {
                        ItemStack fuelCandidate = lis.get(4);
                        int singleBurn = com.brilliafy.magicstorage.util.SmeltingCraftingHelper.getSingleBurnTime(fuelCandidate);
                        if (singleBurn > 0) {
                            int neededFuel = (int) Math.ceil((double)(totalInputs * 200) / (double) singleBurn);
                            neededFuel = Math.min(neededFuel, fuelCandidate.getMaxStackSize());
                            int currentFuel = matrix.getStackInSlot(4).isEmpty() ? 0 : matrix.getStackInSlot(4).getCount();
                            if (currentFuel < neededFuel) {
                                int toExtract = neededFuel - currentFuel;
                                ItemStack reqFuel = master != null ? master.extractItem(
                                    s -> net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(s, fuelCandidate),
                                    toExtract, false) : ItemStack.EMPTY;
                                if ((reqFuel.isEmpty() || reqFuel.getCount() < toExtract) && allowInventory) {
                                    int remaining = toExtract - reqFuel.getCount();
                                    for (int slotIdx = 0; slotIdx < playerIn.inventory.mainInventory.size(); slotIdx++) {
                                        ItemStack invStack = playerIn.inventory.mainInventory.get(slotIdx);
                                        if (!invStack.isEmpty() && net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(invStack, fuelCandidate)) {
                                            ItemStack taken = invStack.splitStack(Math.min(remaining, invStack.getCount()));
                                            if (reqFuel.isEmpty()) {
                                                reqFuel = taken;
                                            } else {
                                                reqFuel.grow(taken.getCount());
                                            }
                                            remaining -= taken.getCount();
                                            if (remaining <= 0) break;
                                        }
                                    }
                                }
                                if (!reqFuel.isEmpty()) {
                                    if (matrix.getStackInSlot(4).isEmpty()) {
                                        matrix.setInventorySlotContents(4, reqFuel);
                                    } else {
                                        matrix.getStackInSlot(4).grow(reqFuel.getCount());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            onCraftMatrixChanged(matrix);
            detectAndSendChanges();
            syncAllSlots(playerIn);
            sendItemRefresh(playerIn, master);
            return stack;
        }
    }
}
