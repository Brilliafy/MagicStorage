package com.brilliafy.magicstorage.network;

import com.brilliafy.magicstorage.container.ContainerMagicStorageBase;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        this.nbt = ByteBufUtils.readTag(buf);
        this.maxTransfer = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.nbt);
        buf.writeBoolean(this.maxTransfer);
    }

    @Override
    public IMessage onMessage(RecipeMessage message, MessageContext ctx) {
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

                int fuelSlotIndex = message.nbt.hasKey("fuelSlot") ? message.nbt.getInteger("fuelSlot") : -1;
                int reqBurnTicks = message.nbt.hasKey("reqBurnTicks") ? message.nbt.getInteger("reqBurnTicks") : 200;

                // ============================================================
                // CASE 1: FUEL RECIPES (Smelting slot 4, Alchemy slot 7)
                // ============================================================
                if (fuelSlotIndex >= 0) {
                    List<ItemStack> allInputCandidates = new ArrayList<>();
                    List<Integer> inputSlots = new ArrayList<>();
                    for (int slot = 0; slot < 9; slot++) {
                        if (slot == fuelSlotIndex) continue;
                        if (message.nbt.hasKey("s" + slot)) {
                            inputSlots.add(slot);
                            NBTTagList invList = message.nbt.getTagList("s" + slot, 10);
                            for (int i = 0; i < invList.tagCount(); i++) {
                                ItemStack cand = new ItemStack(invList.getCompoundTagAt(i));
                                if (!cand.isEmpty()) allInputCandidates.add(cand);
                            }
                        }
                    }

                    ItemStack chosenFuel = findBestFuel(heart, player, reqBurnTicks, allInputCandidates);
                    if (chosenFuel.isEmpty()) {
                        chosenFuel = new ItemStack(net.minecraft.init.Items.COAL);
                    }
                    int singleBurn = net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(chosenFuel);
                    int maxFuelStack = chosenFuel.getMaxStackSize();

                    boolean isSmelting = message.nbt.getBoolean("isSmelting") || fuelSlotIndex == 4;

                    if (isSmelting) {
                        ItemStack sampleInputCandidate = ItemStack.EMPTY;
                        List<ItemStack> inputCandidatesList = new ArrayList<>();
                        if (!inputSlots.isEmpty() && message.nbt.hasKey("s" + inputSlots.get(0))) {
                            NBTTagList firstList = message.nbt.getTagList("s" + inputSlots.get(0), 10);
                            for (int k = 0; k < firstList.tagCount(); k++) {
                                ItemStack c = new ItemStack(firstList.getCompoundTagAt(k));
                                if (!c.isEmpty()) inputCandidatesList.add(c);
                            }
                        }

                        for (ItemStack c : inputCandidatesList) {
                            if (countAvailable(heart, player, c) > 0) {
                                sampleInputCandidate = c.copy();
                                break;
                            }
                        }
                        if (sampleInputCandidate.isEmpty() && !inputCandidatesList.isEmpty()) {
                            sampleInputCandidate = inputCandidatesList.get(0).copy();
                            if (sampleInputCandidate.getMetadata() == net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE) {
                                sampleInputCandidate.setItemDamage(0);
                            }
                        }

                        boolean isSameItem = !sampleInputCandidate.isEmpty() && matchesCandidate(chosenFuel, sampleInputCandidate);
                        int maxInputStack = sampleInputCandidate.isEmpty() ? 64 : sampleInputCandidate.getMaxStackSize();

                        if (isSameItem) {
                            // SAME ITEM AS FUEL & INPUT (e.g. burning logs to smelt logs into charcoal)
                            int totalAvailable = countAvailable(heart, player, chosenFuel);
                            int numInputSlots = Math.max(1, inputSlots.size());

                            if (message.maxTransfer) {
                                int maxByFuelSlot = (singleBurn > 0) ? (int) Math.floor((double) (maxFuelStack * singleBurn) / (double) reqBurnTicks) : 0;
                                int maxByTotal = (singleBurn > 0 && totalAvailable > 0)
                                        ? (int) Math.floor((double) (totalAvailable * singleBurn) / (double) (reqBurnTicks + singleBurn))
                                        : 0;
                                int maxSmeltable = Math.min(numInputSlots * maxInputStack, Math.min(maxByFuelSlot, maxByTotal));
                                int reqFuel = (maxSmeltable > 0 && singleBurn > 0)
                                        ? (int) Math.ceil((double) (maxSmeltable * reqBurnTicks) / (double) singleBurn)
                                        : 1;
                                int fuelCount = Math.min(maxFuelStack, Math.max(reqFuel, totalAvailable - maxSmeltable));
                                int totalToExtract = Math.min(totalAvailable, maxSmeltable + fuelCount);

                                if (totalToExtract > 0) {
                                    ItemStack extracted = extractFromNetworkAndPlayer(heart, player, chosenFuel, totalToExtract);
                                    int actualTotal = extracted.getCount();
                                    int actualFuel = Math.min(maxFuelStack, Math.min(actualTotal, fuelCount));
                                    int actualSmelt = actualTotal - actualFuel;

                                    // Ensure actualFuel is ALWAYS enough to smelt actualSmelt
                                    if (actualSmelt > 0 && singleBurn > 0) {
                                        int minFuelNeeded = (int) Math.ceil((double) (actualSmelt * reqBurnTicks) / (double) singleBurn);
                                        if (actualFuel < minFuelNeeded && actualFuel < maxFuelStack) {
                                            int diff = Math.min(maxFuelStack - actualFuel, minFuelNeeded - actualFuel);
                                            diff = Math.min(diff, actualSmelt);
                                            actualFuel += diff;
                                            actualSmelt -= diff;
                                        }
                                    }

                                    if (actualFuel > 0) {
                                        ItemStack fuelStack = extracted.copy();
                                        fuelStack.setCount(actualFuel);
                                        container.getCraftMatrix().setInventorySlotContents(fuelSlotIndex, fuelStack);
                                    }

                                    if (actualSmelt > 0) {
                                        distributeEvenly(container, inputSlots, extracted, actualSmelt);
                                    }
                                }
                            } else {
                                // Single click
                                int reqFuel = singleBurn > 0 ? (int) Math.ceil((double) (1 * reqBurnTicks) / (double) singleBurn) : 1;
                                if (maxFuelStack == 1 && reqFuel > 1) {
                                    // Cannot satisfy with 1 unstackable item
                                } else {
                                    int totalToExtract = Math.min(totalAvailable, 1 + reqFuel);
                                    if (totalToExtract > 0) {
                                        ItemStack extracted = extractFromNetworkAndPlayer(heart, player, chosenFuel, totalToExtract);
                                        int actualTotal = extracted.getCount();
                                        int actualFuel = Math.min(maxFuelStack, Math.min(actualTotal, reqFuel));
                                        int actualSmelt = actualTotal - actualFuel;

                                        if (actualFuel > 0) {
                                            ItemStack fuelStack = extracted.copy();
                                            fuelStack.setCount(actualFuel);
                                            container.getCraftMatrix().setInventorySlotContents(fuelSlotIndex, fuelStack);
                                        }

                                        if (actualSmelt > 0 && !inputSlots.isEmpty()) {
                                            ItemStack inStack = extracted.copy();
                                            inStack.setCount(actualSmelt);
                                            container.getCraftMatrix().setInventorySlotContents(inputSlots.get(0), inStack);
                                        }
                                    }
                                }
                            }
                        } else {
                            // UNIFORM SMELTING WITH DIFFERENT FUEL (e.g. Iron Ore + Coal, or Olive Logs + Planks)
                            int totalAvailableFuel = countAvailable(heart, player, chosenFuel);
                            int totalAvailableInput = countAvailable(heart, player, sampleInputCandidate);
                            int numInputSlots = Math.max(1, inputSlots.size());

                            if (message.maxTransfer) {
                                int maxFuelPossibleInSlot = Math.min(maxFuelStack, totalAvailableFuel);
                                int maxSmeltableByFuel = (singleBurn > 0 && maxFuelPossibleInSlot > 0)
                                        ? (int) Math.floor((double) (maxFuelPossibleInSlot * singleBurn) / (double) reqBurnTicks)
                                        : 0;
                                int targetSmelt = maxFuelPossibleInSlot > 0
                                        ? Math.min(numInputSlots * maxInputStack, Math.min(totalAvailableInput, maxSmeltableByFuel))
                                        : Math.min(numInputSlots * maxInputStack, totalAvailableInput);

                                int reqFuel = (targetSmelt > 0 && singleBurn > 0)
                                        ? (int) Math.ceil((double) (targetSmelt * reqBurnTicks) / (double) singleBurn)
                                        : 1;
                                int fuelCount = Math.min(maxFuelStack, Math.min(totalAvailableFuel, Math.max(reqFuel, maxFuelPossibleInSlot)));

                                if (targetSmelt > 0) {
                                    ItemStack extractedInput = extractFromNetworkAndPlayer(heart, player, sampleInputCandidate, targetSmelt);
                                    distributeEvenly(container, inputSlots, extractedInput, extractedInput.getCount());
                                }
                                if (fuelCount > 0) {
                                    ItemStack extractedFuel = extractFromNetworkAndPlayer(heart, player, chosenFuel, fuelCount);
                                    container.getCraftMatrix().setInventorySlotContents(fuelSlotIndex, extractedFuel);
                                }
                            } else {
                                // Single click
                                int reqFuel = singleBurn > 0 ? (int) Math.ceil((double) (1 * reqBurnTicks) / (double) singleBurn) : 1;
                                if (maxFuelStack == 1 && reqFuel > 1) {
                                    // Cannot satisfy with 1 unstackable item
                                } else {
                                    ItemStack extractedInput = extractFromNetworkAndPlayer(heart, player, sampleInputCandidate, 1);
                                    if (!extractedInput.isEmpty() && !inputSlots.isEmpty()) {
                                        container.getCraftMatrix().setInventorySlotContents(inputSlots.get(0), extractedInput);
                                    }
                                    int fuelCount = Math.min(maxFuelStack, reqFuel);
                                    ItemStack extractedFuel = extractFromNetworkAndPlayer(heart, player, chosenFuel, fuelCount);
                                    if (!extractedFuel.isEmpty()) {
                                        container.getCraftMatrix().setInventorySlotContents(fuelSlotIndex, extractedFuel);
                                    }
                                }
                            }
                        }
                    } else {
                        // MULTI-INGREDIENT RECIPE WITH FUEL (e.g. Alchemy)
                        int reqFuel = singleBurn > 0 ? (int) Math.ceil((double) reqBurnTicks / (double) singleBurn) : 1;
                        if (maxFuelStack == 1 && reqFuel > 1) {
                            // Skip unstackable fuel that cannot provide enough burn ticks
                        } else {
                            int totalAvailableFuel = countAvailable(heart, player, chosenFuel);
                            int fuelCount = message.maxTransfer
                                    ? Math.min(maxFuelStack, Math.min(totalAvailableFuel, Math.max(reqFuel, 64)))
                                    : Math.min(maxFuelStack, reqFuel);
                            ItemStack extractedFuel = extractFromNetworkAndPlayer(heart, player, chosenFuel, fuelCount);
                            if (!extractedFuel.isEmpty()) {
                                container.getCraftMatrix().setInventorySlotContents(fuelSlotIndex, extractedFuel);
                            }
                        }

                        // Fill the other slots normally
                        fillStandardIngredientSlots(container, heart, player, message, inputSlots);
                    }

                } else {
                    // ============================================================
                    // CASE 2: STANDARD RECIPES (Crafting Table, Anvil, Enchanting, Brewing)
                    // ============================================================
                    List<Integer> allSlots = new ArrayList<>();
                    for (int slot = 0; slot < 9; slot++) {
                        if (message.nbt.hasKey("s" + slot)) allSlots.add(slot);
                    }
                    fillStandardIngredientSlots(container, heart, player, message, allSlots);
                }

                container.onCraftMatrixChanged(container.getCraftMatrix());
                container.detectAndSendChanges();
                for (int i = 0; i < 9; i++) {
                    player.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(container.windowId, i + 1, container.getCraftMatrix().getStackInSlot(i)));
                }

                // Refresh client item list
                List<ItemStack> allItems = heart.getAllItems();
                com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendTo(
                    new com.brilliafy.magicstorage.network.NetworkHandler.StackRefreshClientMessage(allItems, new ArrayList<>()),
                    player);
            }
        });
        return null;
    }

    private static void distributeEvenly(ContainerMagicStorageBase container, List<Integer> slots, ItemStack sample, int totalCount) {
        if (slots == null || slots.isEmpty() || totalCount <= 0 || sample.isEmpty()) return;
        int slotCount = slots.size();
        int basePerSlot = totalCount / slotCount;
        int remainder = totalCount % slotCount;

        int maxStack = sample.getMaxStackSize();
        for (int i = 0; i < slotCount; i++) {
            int slotIndex = slots.get(i);
            int countForSlot = Math.min(maxStack, basePerSlot + (i < remainder ? 1 : 0));
            if (countForSlot > 0) {
                ItemStack slotStack = sample.copy();
                slotStack.setCount(countForSlot);
                container.getCraftMatrix().setInventorySlotContents(slotIndex, slotStack);
            }
        }
    }

    private static void fillStandardIngredientSlots(ContainerMagicStorageBase container, TileStorageHeart heart, EntityPlayer player, RecipeMessage message, List<Integer> targetSlots) {
        class IngredientGroup {
            final ItemStack candidate;
            final List<Integer> slots = new ArrayList<>();
            IngredientGroup(ItemStack candidate) { this.candidate = candidate; }
        }

        List<IngredientGroup> groups = new ArrayList<>();
        for (int slot : targetSlots) {
            if (message.nbt.hasKey("s" + slot)) {
                NBTTagList invList = message.nbt.getTagList("s" + slot, 10);
                List<ItemStack> candidatesForSlot = new ArrayList<>();
                for (int i = 0; i < invList.tagCount(); i++) {
                    ItemStack cand = new ItemStack(invList.getCompoundTagAt(i));
                    if (!cand.isEmpty()) candidatesForSlot.add(cand);
                }

                if (!candidatesForSlot.isEmpty()) {
                    ItemStack chosenCandidate = ItemStack.EMPTY;
                    // 1. Strict match from player inventory
                    for (ItemStack c : candidatesForSlot) {
                        for (ItemStack invStack : player.inventory.mainInventory) {
                            if (matchesCandidate(invStack, c, true)) {
                                chosenCandidate = invStack.copy();
                                chosenCandidate.setCount(Math.max(1, c.getCount()));
                                break;
                            }
                        }
                        if (!chosenCandidate.isEmpty()) break;
                    }
                    // 2. Strict match from storage network
                    if (chosenCandidate.isEmpty()) {
                        for (ItemStack c : candidatesForSlot) {
                            ItemStack found = heart.extractItem(s -> matchesCandidate(s, c, true), 1, true);
                            if (!found.isEmpty()) {
                                chosenCandidate = found.copy();
                                chosenCandidate.setCount(Math.max(1, c.getCount()));
                                break;
                            }
                        }
                    }
                    // 3. Relaxed match from player inventory
                    if (chosenCandidate.isEmpty()) {
                        for (ItemStack c : candidatesForSlot) {
                            for (ItemStack invStack : player.inventory.mainInventory) {
                                if (matchesCandidate(invStack, c, false)) {
                                    chosenCandidate = invStack.copy();
                                    chosenCandidate.setCount(Math.max(1, c.getCount()));
                                    break;
                                }
                            }
                            if (!chosenCandidate.isEmpty()) break;
                        }
                    }
                    // 4. Relaxed match from storage network
                    if (chosenCandidate.isEmpty()) {
                        for (ItemStack c : candidatesForSlot) {
                            ItemStack found = heart.extractItem(s -> matchesCandidate(s, c, false), 1, true);
                            if (!found.isEmpty()) {
                                chosenCandidate = found.copy();
                                chosenCandidate.setCount(Math.max(1, c.getCount()));
                                break;
                            }
                        }
                    }
                    // Fallback to first candidate
                    if (chosenCandidate.isEmpty()) {
                        chosenCandidate = candidatesForSlot.get(0).copy();
                        if (chosenCandidate.getMetadata() == net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE) {
                            chosenCandidate.setItemDamage(0);
                        }
                    }

                    if (!chosenCandidate.isEmpty()) {
                        if (slot == 0 && com.brilliafy.magicstorage.util.RusticCraftingHelper.isCrushable(chosenCandidate)) {
                            chosenCandidate.setCount(Math.max(chosenCandidate.getCount(), com.brilliafy.magicstorage.util.RusticCraftingHelper.getRequiredCrushingAmount(chosenCandidate)));
                        }
                        IngredientGroup targetGroup = null;
                        for (IngredientGroup g : groups) {
                            if (matchesCandidate(g.candidate, chosenCandidate, true)) {
                                targetGroup = g;
                                break;
                            }
                        }
                        if (targetGroup == null) {
                            targetGroup = new IngredientGroup(chosenCandidate);
                            groups.add(targetGroup);
                        }
                        targetGroup.slots.add(slot);
                    }
                }
            }
        }

        for (IngredientGroup group : groups) {
            int slotCount = group.slots.size();
            int maxSlotCap = group.candidate.getMaxStackSize();
            int targetPerSlot = message.maxTransfer ? maxSlotCap : Math.max(1, group.candidate.getCount());
            int totalTargetForGroup = targetPerSlot * slotCount;

            ItemStack extracted = extractFromNetworkAndPlayer(heart, player, group.candidate, totalTargetForGroup);
            if (!extracted.isEmpty()) {
                distributeEvenly(container, group.slots, extracted, extracted.getCount());
            }
        }
    }

    private static int countAvailable(TileStorageHeart heart, EntityPlayer player, ItemStack candidate) {
        if (candidate == null || candidate.isEmpty()) return 0;
        int count = 0;
        if (heart != null) {
            for (ItemStack s : heart.getAllItems()) {
                if (!s.isEmpty() && matchesCandidate(s, candidate, false)) {
                    count += s.getCount();
                }
            }
        }
        if (player != null && player.inventory != null) {
            for (ItemStack s : player.inventory.mainInventory) {
                if (!s.isEmpty() && matchesCandidate(s, candidate, false)) {
                    count += s.getCount();
                }
            }
        }
        return count;
    }

    private static ItemStack extractFromNetworkAndPlayer(TileStorageHeart heart, EntityPlayer player, ItemStack candidate, int amount) {
        if (candidate == null || candidate.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        int totalExtracted = 0;
        ItemStack sample = ItemStack.EMPTY;

        // 1. Player inventory with strict match
        if (player != null && player.inventory != null) {
            int needed = amount - totalExtracted;
            for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
                ItemStack invStack = player.inventory.mainInventory.get(i);
                if (!invStack.isEmpty() && matchesCandidate(invStack, candidate, true)) {
                    int take = Math.min(needed, invStack.getCount());
                    invStack.shrink(take);
                    totalExtracted += take;
                    needed -= take;
                    if (sample.isEmpty()) sample = invStack.copy();
                    if (needed <= 0) break;
                }
            }
        }

        // 2. Network with strict match
        if (totalExtracted < amount && heart != null) {
            ItemStack fromNet = heart.extractItem(s -> matchesCandidate(s, candidate, true), amount - totalExtracted, false);
            if (!fromNet.isEmpty()) {
                if (sample.isEmpty()) sample = fromNet.copy();
                totalExtracted += fromNet.getCount();
            }
        }

        // 3. Fallback to relaxed match from player inventory
        if (totalExtracted < amount && player != null && player.inventory != null) {
            int needed = amount - totalExtracted;
            for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
                ItemStack invStack = player.inventory.mainInventory.get(i);
                if (!invStack.isEmpty() && matchesCandidate(invStack, candidate, false)) {
                    int take = Math.min(needed, invStack.getCount());
                    invStack.shrink(take);
                    totalExtracted += take;
                    needed -= take;
                    if (sample.isEmpty()) sample = invStack.copy();
                    if (needed <= 0) break;
                }
            }
        }

        // 4. Fallback to relaxed match from network
        if (totalExtracted < amount && heart != null) {
            int needed = amount - totalExtracted;
            ItemStack fromNet = heart.extractItem(s -> matchesCandidate(s, candidate, false), needed, false);
            if (!fromNet.isEmpty()) {
                totalExtracted += fromNet.getCount();
                if (sample.isEmpty()) sample = fromNet.copy();
            }
        }

        if (totalExtracted <= 0) return ItemStack.EMPTY;
        ItemStack result = sample.isEmpty() ? candidate.copy() : sample.copy();
        result.setCount(totalExtracted);
        return result;
    }

    private static float getRusticQuality(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().hasKey("Fluid", 10)) {
            NBTTagCompound fTag = stack.getTagCompound().getCompoundTag("Fluid");
            if (fTag.hasKey("Tag", 10) && fTag.getCompoundTag("Tag").hasKey("Quality")) {
                return fTag.getCompoundTag("Tag").getFloat("Quality");
            }
        }
        return -1.0f;
    }

    private static boolean matchesCandidate(ItemStack storageStack, ItemStack candidate) {
        return matchesCandidate(storageStack, candidate, true);
    }

    private static boolean matchesCandidate(ItemStack storageStack, ItemStack candidate, boolean strictQuality) {
        if (storageStack.isEmpty() || candidate.isEmpty()) return false;
        if (storageStack.getItem() != candidate.getItem()) return false;
        if (candidate.getMetadata() != net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE &&
            candidate.getMetadata() != storageStack.getMetadata()) {
            return false;
        }

        // Potion matching
        if (candidate.getItem() == net.minecraft.init.Items.POTIONITEM ||
            candidate.getItem() == net.minecraft.init.Items.SPLASH_POTION ||
            candidate.getItem() == net.minecraft.init.Items.LINGERING_POTION) {
            if (candidate.hasTagCompound() && candidate.getTagCompound().hasKey("Potion")) {
                String candPotion = candidate.getTagCompound().getString("Potion");
                String storPotion = storageStack.hasTagCompound() ? storageStack.getTagCompound().getString("Potion") : "";
                if (!candPotion.isEmpty() && !candPotion.equals(storPotion)) {
                    return false;
                }
            }
        }

        // Rustic Fluid Bottle matching
        if (candidate.hasTagCompound() && candidate.getTagCompound().hasKey("Fluid")) {
            String candFluid = candidate.getTagCompound().getCompoundTag("Fluid").getString("FluidName");
            String storFluid = storageStack.hasTagCompound() && storageStack.getTagCompound().hasKey("Fluid")
                ? storageStack.getTagCompound().getCompoundTag("Fluid").getString("FluidName") : "";
            if (!candFluid.isEmpty() && !candFluid.equals(storFluid)) {
                return false;
            }
            if (strictQuality) {
                float candQ = getRusticQuality(candidate);
                if (candQ >= 0.0f) {
                    float storQ = getRusticQuality(storageStack);
                    if (storQ < 0.0f || Math.abs(candQ - storQ) > 0.005f) {
                        return false;
                    }
                }
            }
        }

        // Enchanted Book matching
        if (candidate.getItem() == net.minecraft.init.Items.ENCHANTED_BOOK) {
            net.minecraft.nbt.NBTTagList candEnch = net.minecraft.item.ItemEnchantedBook.getEnchantments(candidate);
            if (!candEnch.isEmpty()) {
                if (storageStack.getItem() != net.minecraft.init.Items.ENCHANTED_BOOK) return false;
                net.minecraft.nbt.NBTTagList storEnch = net.minecraft.item.ItemEnchantedBook.getEnchantments(storageStack);
                for (int i = 0; i < candEnch.tagCount(); i++) {
                    short id = candEnch.getCompoundTagAt(i).getShort("id");
                    short lvl = candEnch.getCompoundTagAt(i).getShort("lvl");
                    boolean found = false;
                    for (int j = 0; j < storEnch.tagCount(); j++) {
                        if (storEnch.getCompoundTagAt(j).getShort("id") == id && storEnch.getCompoundTagAt(j).getShort("lvl") == lvl) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) return false;
                }
            }
        }

        // Strict NBT and Damage matching (for QualityTools, Baubles, enchants, custom tags)
        if (strictQuality) {
            NBTTagCompound candTag = com.brilliafy.magicstorage.util.ItemMatchHelper.getComparableNBT(candidate);
            if (candTag != null) {
                NBTTagCompound storTag = com.brilliafy.magicstorage.util.ItemMatchHelper.getComparableNBT(storageStack);
                if (storTag == null || !candTag.equals(storTag)) {
                    return false;
                }
            }
            if (candidate.isItemStackDamageable() && candidate.getItemDamage() > 0) {
                if (storageStack.getItemDamage() != candidate.getItemDamage()) {
                    return false;
                }
            }
        }

        // Unenchanted vs Enchanted Equipment matching
        if (!candidate.isItemEnchanted() && candidate.getItem() != net.minecraft.init.Items.ENCHANTED_BOOK) {
            if (storageStack.isItemEnchanted() || storageStack.getItem() == net.minecraft.init.Items.ENCHANTED_BOOK) {
                return false;
            }
        } else if (candidate.isItemEnchanted()) {
            net.minecraft.nbt.NBTTagList candEnch = candidate.getEnchantmentTagList();
            if (!candEnch.isEmpty()) {
                if (!storageStack.isItemEnchanted()) return false;
                net.minecraft.nbt.NBTTagList storEnch = storageStack.getEnchantmentTagList();
                for (int i = 0; i < candEnch.tagCount(); i++) {
                    short id = candEnch.getCompoundTagAt(i).getShort("id");
                    short lvl = candEnch.getCompoundTagAt(i).getShort("lvl");
                    boolean found = false;
                    for (int j = 0; j < storEnch.tagCount(); j++) {
                        if (storEnch.getCompoundTagAt(j).getShort("id") == id && storEnch.getCompoundTagAt(j).getShort("lvl") == lvl) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) return false;
                }
            }
        }

        return true;
    }

    private static boolean hasBookPower(ItemStack stack, EntityPlayer player) {
        if (stack == null || stack.isEmpty()) return false;
        net.minecraft.item.Item item = stack.getItem();
        if (item == null) return false;
        if (item == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.BOOKSHELF)) return true;
        if (item == net.minecraft.init.Items.BOOK || item == net.minecraft.init.Items.ENCHANTED_BOOK || item == net.minecraft.init.Items.WRITABLE_BOOK || item == net.minecraft.init.Items.WRITTEN_BOOK) return true;
        net.minecraft.block.Block b = net.minecraft.block.Block.getBlockFromItem(item);
        if (b != null && b != net.minecraft.init.Blocks.AIR) {
            try {
                if (player != null && player.world != null) {
                    float power = b.getEnchantPowerBonus(player.world, net.minecraft.util.math.BlockPos.ORIGIN);
                    if (power > 0.0F) return true;
                }
            } catch (Throwable ignored) {}
            String name = b.getRegistryName() != null ? b.getRegistryName().toString().toLowerCase() : "";
            if (name.contains("bookshelf") || name.contains("book_shelf") || name.contains("tome")) return true;
        }
        return false;
    }

    public static class FuelInfo {
        public final ItemStack stack;
        public final int burnTime;
        public final int totalAvailable;
        public final int score;

        public FuelInfo(ItemStack stack, int burnTime, int totalAvailable, int score) {
            this.stack = stack;
            this.burnTime = burnTime;
            this.totalAvailable = totalAvailable;
            this.score = score;
        }
    }

    private static ItemStack findBestFuel(TileStorageHeart heart, EntityPlayer player, int reqTicks, List<ItemStack> inputCandidates) {
        Map<String, FuelInfo> fuelMap = new LinkedHashMap<>();

        int totalInputCount = 0;
        if (inputCandidates != null && !inputCandidates.isEmpty()) {
            for (ItemStack inp : inputCandidates) {
                totalInputCount += countAvailable(heart, player, inp);
            }
        }
        final int finalInputCount = totalInputCount;

        java.util.function.Consumer<ItemStack> evaluate = s -> {
            if (s == null || s.isEmpty()) return;
            int bt = net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(s);
            if (bt <= 0) return;

            int maxStack = s.getMaxStackSize();
            // If item is unstackable (maxStack == 1) and single burn time is less than required ticks, skip it
            if (maxStack == 1 && bt < reqTicks) {
                return;
            }

            String key = s.getItem().getRegistryName() + "@" + s.getMetadata();
            FuelInfo existing = fuelMap.get(key);
            int count = s.getCount() + (existing != null ? existing.totalAvailable : 0);

            boolean isBookPower = hasBookPower(s, player);
            boolean isInput = false;
            if (inputCandidates != null) {
                for (ItemStack inp : inputCandidates) {
                    if (matchesCandidate(s, inp)) {
                        isInput = true;
                        break;
                    }
                }
            }

            boolean isStandardFuel = (s.getItem() == net.minecraft.init.Items.COAL && (s.getMetadata() == 0 || s.getMetadata() == 1))
                    || s.getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.COAL_BLOCK)
                    || s.getItem() == net.minecraft.init.Items.BLAZE_ROD
                    || s.getItem() == net.minecraft.init.Items.LAVA_BUCKET;

            boolean isEquipmentOrUnstackable = maxStack == 1 || s.isItemStackDamageable();

            int effCount = Math.min(maxStack, count);
            long burnCapacity = (long) effCount * bt;
            int maxItemsSmeltable = reqTicks > 0 ? (int) (burnCapacity / reqTicks) : 0;

            int tier;
            if (isBookPower) {
                tier = 0;
            } else if (isEquipmentOrUnstackable && !isStandardFuel) {
                tier = 1; // Swords, tools, unstackable items ranked below regular fuels
            } else if (isStandardFuel) {
                tier = 6; // Coal, Charcoal, Coal Block, Blaze Rod, Lava Bucket
            } else if (count >= 64) {
                tier = 5; // Large stockpile of external stackable fuel (e.g. 354 Planks)
            } else if (!isInput && (finalInputCount <= 0 || maxItemsSmeltable >= finalInputCount)) {
                tier = 4; // External fuel that can smelt the full input batch
            } else if (isInput) {
                tier = 3; // Self-fuel (burning the item being smelted): uses full batch
            } else {
                tier = 2; // Meager external scraps (e.g. 2 stairs) that bottleneck a large input
            }

            fuelMap.put(key, new FuelInfo(s.copy(), bt, count, tier));
        };

        if (heart != null) {
            for (ItemStack s : heart.getAllItems()) {
                evaluate.accept(s);
            }
        }
        if (player != null && player.inventory != null) {
            for (ItemStack s : player.inventory.mainInventory) {
                evaluate.accept(s);
            }
        }

        if (fuelMap.isEmpty()) return ItemStack.EMPTY;

        List<FuelInfo> list = new ArrayList<>(fuelMap.values());
        list.sort((a, b) -> {
            // 1. Tier (higher first)
            if (a.score != b.score) return Integer.compare(b.score, a.score);

            // Special preference: Coal (meta 0) > Charcoal (meta 1) in tier 6
            if (a.score == 6) {
                boolean aCoal = a.stack.getItem() == net.minecraft.init.Items.COAL && a.stack.getMetadata() == 0;
                boolean bCoal = b.stack.getItem() == net.minecraft.init.Items.COAL && b.stack.getMetadata() == 0;
                if (aCoal && !bCoal) return -1;
                if (!aCoal && bCoal) return 1;

                boolean aChar = a.stack.getItem() == net.minecraft.init.Items.COAL && a.stack.getMetadata() == 1;
                boolean bChar = b.stack.getItem() == net.minecraft.init.Items.COAL && b.stack.getMetadata() == 1;
                if (aChar && !bChar) return -1;
                if (!aChar && bChar) return 1;
            }

            // 2. Usable slot burn capacity (effectiveCountInSlot * burnTime) descending
            int effCountA = Math.min(a.stack.getMaxStackSize(), a.totalAvailable);
            int effCountB = Math.min(b.stack.getMaxStackSize(), b.totalAvailable);
            long capA = (long) effCountA * a.burnTime;
            long capB = (long) effCountB * b.burnTime;
            if (capA != capB) return Long.compare(capB, capA);

            // 3. Total stockpile count descending
            if (a.totalAvailable != b.totalAvailable) return Integer.compare(b.totalAvailable, a.totalAvailable);

            // 4. Single burn time descending
            if (a.burnTime != b.burnTime) return Integer.compare(b.burnTime, a.burnTime);

            // 5. Deterministic string tie-breaker
            String nameA = a.stack.getItem().getRegistryName() + "@" + a.stack.getMetadata();
            String nameB = b.stack.getItem().getRegistryName() + "@" + b.stack.getMetadata();
            return nameA.compareTo(nameB);
        });

        FuelInfo best = list.get(0);
        ItemStack chosen = best.stack.copy();
        int maxStack = chosen.getMaxStackSize();
        int needed = (int) Math.ceil((double) reqTicks / (double) best.burnTime);
        chosen.setCount(Math.min(maxStack, Math.max(1, needed)));
        return chosen;
    }
}
