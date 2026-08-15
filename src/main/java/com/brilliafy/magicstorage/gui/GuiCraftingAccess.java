package com.brilliafy.magicstorage.gui;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mojang.realmsclient.gui.ChatFormatting;
import com.brilliafy.magicstorage.MagicStorage;
import com.brilliafy.magicstorage.data.EnumSortType;
import com.brilliafy.magicstorage.jei.JeiHooks;
import com.brilliafy.magicstorage.jei.SearchSettings;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;

public abstract class GuiCraftingAccess extends GuiContainer implements IStorageInventory {

    private static final int HEIGHT = 256;
    private static final int WIDTH = 176;
    protected ResourceLocation texture = new ResourceLocation(MagicStorage.MODID, "textures/gui/request.png");
    protected int page = 1, maxPage = 1;
    public List<ItemStack> stacks, craftableStacks;
    protected ItemStack stackUnderMouse = ItemStack.EMPTY;
    protected GuiTextField searchBar;
    protected GuiStorageButton directionBtn, sortBtn, keepBtn, jeiBtn, clearTextBtn, autofillBtn;
    protected List<ItemSlotNetwork> slots;
    protected List<ItemStack> displayedStacks = null;
    protected Set<Integer> zeroStacks = new TreeSet<>();
    protected long lastClick;
    private boolean forceFocus;
    protected boolean isSimple;
    protected com.brilliafy.magicstorage.container.ContainerMagicStorageBase magicContainer;

    public GuiCraftingAccess(com.brilliafy.magicstorage.container.ContainerMagicStorageBase container) {
        super(container);
        this.magicContainer = container;
        this.xSize = WIDTH;
        this.ySize = HEIGHT;
        this.stacks = Lists.newArrayList();
        this.craftableStacks = Lists.newArrayList();
        TileStorageHeart heart = container.getTileMaster();
        if (heart != null) stacks = heart.getAllItems();
        this.displayedStacks = Lists.newArrayList();
        lastClick = System.currentTimeMillis();
    }

    protected boolean canClick() {
        return System.currentTimeMillis() > lastClick + 100L;
    }

    public void setStacks(List<ItemStack> stacks) {
        this.stacks = stacks;
        zeroStacks.clear();
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && displayedStacks != null) {
            for (int i = 0; i < displayedStacks.size(); i++) {
                ItemStack stack = displayedStacks.get(i);
                boolean match = false;
                for (ItemStack newStack : stacks) {
                    if (ItemHandlerHelper.canItemStacksStack(newStack, stack)) {
                        match = true;
                        stack.setCount(newStack.getCount());
                        break;
                    }
                }
                if (!match) { zeroStacks.add(i); stack.setCount(1); }
            }
        }
    }

    public void setCraftableStacks(List<ItemStack> stacks) {
        this.craftableStacks = stacks;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        searchBar = new GuiTextField(0, fontRenderer, guiLeft + 81, guiTop + 96, 85, fontRenderer.FONT_HEIGHT);
        searchBar.setMaxStringLength(30);
        if (isSimple) searchBar.y += 64;
        searchBar.setEnableBackgroundDrawing(false);
        searchBar.setVisible(true);
        searchBar.setTextColor(16777215);
        searchBar.setFocused(false);
        searchBar.setText(SearchSettings.getSearch());
        directionBtn = new GuiStorageButton(0, guiLeft + 7, searchBar.y - 3, "");
        addButton(directionBtn);
        sortBtn = new GuiStorageButton(1, guiLeft + 21, searchBar.y - 3, "");
        addButton(sortBtn);
        keepBtn = new GuiStorageButton(6, guiLeft + 35, searchBar.y - 3, "");
        addButton(keepBtn);
        jeiBtn = new GuiStorageButton(4, guiLeft + 49, searchBar.y - 3, "");
        if (JeiHooks.isJeiLoaded()) addButton(jeiBtn);
        clearTextBtn = new GuiStorageButton(5, guiLeft + 64, searchBar.y - 3, "X");
        addButton(clearTextBtn);
        autofillBtn = new GuiStorageButton(7, guiLeft + 152, searchBar.y + 14, "");
        if (!isSimple) addButton(autofillBtn);
        // Request items and sync autofill setting to server
        com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendToServer(
            new com.brilliafy.magicstorage.network.NetworkHandler.AutofillMessage(SearchSettings.getAutofillMode().getId()));
        com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendToServer(
            new com.brilliafy.magicstorage.network.NetworkHandler.RequestMessage(-1, net.minecraft.item.ItemStack.EMPTY, false, false));
    }

    private int getLines() { return isSimple ? 8 : 4; }
    private int getColumns() { return 9; }

    public abstract boolean getDownwards();
    public abstract void setDownwards(boolean d);
    public abstract EnumSortType getSort();
    public abstract void setSort(EnumSortType s);
    public abstract BlockPos getPos();
    protected abstract int getDim();
    protected abstract boolean isScreenValid();

    protected boolean inField(int mouseX, int mouseY) {
        int h = 90;
        if (isSimple) h += 60;
        return mouseX > (guiLeft + 7) && mouseX < (guiLeft + xSize - 7) && mouseY > (guiTop + 7) && mouseY < (guiTop + h);
    }

    protected boolean inSearchbar(int mouseX, int mouseY) {
        return isPointInRegion(searchBar.x - guiLeft, searchBar.y - guiTop, searchBar.width, fontRenderer.FONT_HEIGHT, mouseX, mouseY);
    }

    @Override public void drawGradientRectP(int left, int top, int right, int bottom, int startColor, int endColor) { super.drawGradientRect(left, top, right, bottom, startColor, endColor); }
    @Override public FontRenderer getFont() { return this.fontRenderer; }
    @Override public boolean isPointInRegionP(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) { return super.isPointInRegion(rectX, rectY, rectWidth, rectHeight, pointX, pointY); }
    @Override public void renderToolTipP(ItemStack stack, int x, int y) { super.renderToolTip(stack, x, y); }

    protected boolean doesStackMatchSearch(ItemStack stack) {
        String text = searchBar.getText();
        if (text.startsWith("@")) return stack.getItem().getRegistryName().getNamespace().toLowerCase().contains(text.substring(1).toLowerCase());
        if (text.startsWith("#")) {
            try {
                String s = Joiner.on(' ').join(stack.getTooltip(mc.player, TooltipFlags.NORMAL));
                return ChatFormatting.stripFormatting(s).toLowerCase().contains(text.substring(1).toLowerCase());
            } catch (Exception e) { return false; }
        }
        if (text.startsWith("$")) {
            for (int id : OreDictionary.getOreIDs(stack))
                if (OreDictionary.getOreName(id).toLowerCase().contains(text.substring(1).toLowerCase())) return true;
            return false;
        }
        if (text.startsWith("%")) {
            for (CreativeTabs tab : stack.getItem().getCreativeTabs())
                if (tab != null && tab.getTabLabel().toLowerCase().contains(text.substring(1).toLowerCase())) return true;
            return false;
        }
        return stack.getDisplayName().toLowerCase().contains(text.toLowerCase());
    }

    @Override
    public void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        if (!isScreenValid()) return;
        this.drawDefaultBackground();
        renderTextures();
        // Use cached stacks from container (synced from server) instead of client-side heart query
        if (magicContainer != null) {
            stacks = magicContainer.getCachedStacks();
        }
        if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || displayedStacks == null) {
            displayedStacks = applySearchTextToSlots();
            sortStackWrappers(displayedStacks);
        }
        applyScrollPaging(displayedStacks);
        rebuildItemSlots(displayedStacks);
        renderItemSlots(mouseX, mouseY);
        searchBar.drawTextBox();
    }

    private void renderTextures() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(texture);
        drawTexturedModalRect((width - xSize) / 2, (height - ySize) / 2, 0, 0, xSize, ySize);
    }

    private List<ItemStack> applySearchTextToSlots() {
        String text = searchBar.getText();
        if (text.equals("")) return Lists.newArrayList(stacks);
        List<ItemStack> result = Lists.newArrayList();
        for (ItemStack s : stacks)
            if (doesStackMatchSearch(s)) result.add(s);
        return result;
    }

    private void renderItemSlots(int mouseX, int mouseY) {
        stackUnderMouse = ItemStack.EMPTY;
        for (ItemSlotNetwork slot : slots) {
            slot.drawSlot(mouseX, mouseY);
            if (slot.isMouseOverSlot(mouseX, mouseY)) stackUnderMouse = slot.getStack();
        }
        if (slots.isEmpty()) stackUnderMouse = ItemStack.EMPTY;
        // Second pass: count texts on top of all items
        ItemSlotNetwork.renderCounts(slots);
    }

    private void rebuildItemSlots(List<ItemStack> stacksToDisplay) {
        slots = Lists.newArrayList();
        int index = (page - 1) * getColumns();
        for (int row = 0; row < getLines(); row++) {
            for (int col = 0; col < getColumns(); col++) {
                if (index >= stacksToDisplay.size()) break;
                ItemStack stack = stacksToDisplay.get(index);
                int count = zeroStacks.contains(index) ? 0 : stack.getCount();
                slots.add(new ItemSlotNetwork(this, stack, guiLeft + 8 + col * 18, guiTop + 10 + row * 18, count, guiLeft, guiTop, true));
                index++;
            }
        }
    }

    private void applyScrollPaging(List<ItemStack> stacksToDisplay) {
        maxPage = stacksToDisplay.size() / getColumns();
        if (stacksToDisplay.size() % getColumns() != 0) maxPage++;
        maxPage -= (getLines() - 1);
        if (maxPage < 1) maxPage = 1;
        if (page < 1) page = 1;
        if (page > maxPage) page = maxPage;
    }

    private void sortStackWrappers(List<ItemStack> list) {
        Collections.sort(list, new Comparator<ItemStack>() {
            int mul = getDownwards() ? -1 : 1;
            @Override
            public int compare(ItemStack o1, ItemStack o2) {
                switch (getSort()) {
                    case AMOUNT: return Integer.compare(o2.getCount(), o1.getCount()) * mul;
                    case NAME: return o2.getDisplayName().compareToIgnoreCase(o1.getDisplayName()) * mul;
                    case MOD: return o2.getItem().getRegistryName().getNamespace().compareToIgnoreCase(o1.getItem().getRegistryName().getNamespace()) * mul;
                }
                return 0;
            }
        });
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        super.renderHoveredToolTip(mouseX, mouseY);
        if (!isScreenValid()) { mc.player.closeScreen(); return; }
        try { drawTooltips(mouseX, mouseY); } catch (Throwable e) { MagicStorage.LOGGER.error(e.getMessage()); }
    }

    @Override
    public void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        if (!isScreenValid()) return;
        if (forceFocus) { searchBar.setFocused(true); if (searchBar.isFocused()) forceFocus = false; }
    }

    private void drawTooltips(int mouseX, int mouseY) {
        for (ItemSlotNetwork s : slots)
            if (s != null && s.isMouseOverSlot(mouseX, mouseY)) s.drawTooltip(mouseX, mouseY);
        if (inSearchbar(mouseX, mouseY)) {
            List<String> lis = Lists.newArrayList();
            lis.add(I18n.format(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? "gui.storagenetwork.fil.tooltip_0" : "gui.storagenetwork.shift"));
            drawHoveringText(lis, mouseX, mouseY);
        }
        if (clearTextBtn != null && clearTextBtn.isMouseOver()) drawHoveringText(Lists.newArrayList(I18n.format("gui.storagenetwork.tooltip_clear")), mouseX, mouseY);
        if (sortBtn != null && sortBtn.isMouseOver()) drawHoveringText(Lists.newArrayList(I18n.format("gui.storagenetwork.req.tooltip_" + getSort().ordinal())), mouseX, mouseY);
        if (keepBtn != null && keepBtn.isMouseOver()) drawHoveringText(Lists.newArrayList(I18n.format(SearchSettings.isSearchKept() ? "gui.storagenetwork.fil.tooltip_keep_on" : "gui.storagenetwork.fil.tooltip_keep_off")), mouseX, mouseY);
        if (directionBtn != null && directionBtn.isMouseOver()) drawHoveringText(Lists.newArrayList(I18n.format(getDownwards() ? "gui.storagenetwork.sort.down" : "gui.storagenetwork.sort.up")), mouseX, mouseY);
        if (jeiBtn != null && jeiBtn.isMouseOver()) drawHoveringText(Lists.newArrayList(I18n.format(SearchSettings.isJeiSearchSynced() ? "gui.storagenetwork.fil.tooltip_jei_on" : "gui.storagenetwork.fil.tooltip_jei_off")), mouseX, mouseY);
        if (autofillBtn != null && autofillBtn.isMouseOver()) {
            String tooltipKey;
            switch (SearchSettings.getAutofillMode()) {
                case FULL:
                    tooltipKey = "gui.magicstorage.autofill.full";
                    break;
                case NETWORK_ONLY:
                    tooltipKey = "gui.magicstorage.autofill.network";
                    break;
                case DISABLED:
                default:
                    tooltipKey = "gui.magicstorage.autofill.off";
                    break;
            }
            drawHoveringText(Lists.newArrayList(I18n.format(tooltipKey)), mouseX, mouseY);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        if (!SearchSettings.isSearchKept()) {
            SearchSettings.setSearch("");
        }
    }

    @Override
    public void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (button == null) return;
        boolean doSort = true;
        if (button.id == directionBtn.id) setDownwards(!getDownwards());
        else if (button.id == sortBtn.id) setSort(getSort().next());
        else if (button.id == keepBtn.id) { doSort = false; SearchSettings.setKeepSearch(!SearchSettings.isSearchKept()); SearchSettings.setSearch(searchBar.getText()); }
        else if (button.id == jeiBtn.id) { doSort = false; SearchSettings.setJeiSearchSync(!SearchSettings.isJeiSearchSynced()); SearchSettings.setSearch(searchBar.getText()); }
        else if (button.id == clearTextBtn.id) { doSort = false; clearSearch(); forceFocus = true; }
        else if (button.id == autofillBtn.id) {
            doSort = false;
            SearchSettings.setAutofillMode(SearchSettings.getAutofillMode().next());
            com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendToServer(
                new com.brilliafy.magicstorage.network.NetworkHandler.AutofillMessage(SearchSettings.getAutofillMode().getId()));
        }
    }

    private void clearSearch() { searchBar.setText(""); SearchSettings.setSearch(""); }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchBar.setFocused(false);
        if (inSearchbar(mouseX, mouseY)) {
            searchBar.setFocused(true);
            if (mouseButton == 1) clearSearch();
        } else if (!isSimple && isPointInRegion(63, 110, 7, 7, mouseX, mouseY)) {
            // Clear crafting grid — send to server (SSN pattern)
            com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendToServer(
                new com.brilliafy.magicstorage.network.NetworkHandler.ClearRecipeMessage());
            com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendToServer(
                new com.brilliafy.magicstorage.network.NetworkHandler.RequestMessage(-1, net.minecraft.item.ItemStack.EMPTY, false, false));
            lastClick = System.currentTimeMillis();
        } else {
            ItemStack carried = mc.player.inventory.getItemStack();
            boolean middle = mouseButton == 2;
            if (middle && !carried.isEmpty()) return;
            if (!stackUnderMouse.isEmpty() && (mouseButton == 0 || mouseButton == 1 || mouseButton == 2) && carried.isEmpty() && canClick()) {
                com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendToServer(
                    new com.brilliafy.magicstorage.network.NetworkHandler.RequestMessage(mouseButton, stackUnderMouse, isShiftKeyDown(), mouseButton == 2));
                lastClick = System.currentTimeMillis();
            } else if (!carried.isEmpty() && inField(mouseX, mouseY) && canClick()) {
                // Insert items into network via server
                int dim = mc.player.world.provider.getDimension();
                com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendToServer(
                    new com.brilliafy.magicstorage.network.NetworkHandler.InsertMessage(dim, mouseButton));
                lastClick = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!stackUnderMouse.isEmpty() && com.brilliafy.magicstorage.jei.JeiHooks.isJeiKeybind(keyCode)) {
            try { com.brilliafy.magicstorage.jei.JeiHooks.testJeiKeybind(keyCode, stackUnderMouse); } catch (Throwable ignored) {}
            return;
        }

        // Read the private hoveredSlot field from GuiContainer
        net.minecraft.inventory.Slot hoveredSlot = null;
        try {
            java.lang.reflect.Field f = net.minecraft.client.gui.inventory.GuiContainer.class.getDeclaredField("hoveredSlot");
            f.setAccessible(true);
            hoveredSlot = (net.minecraft.inventory.Slot) f.get(this);
        } catch (Exception ignored) {}
        
        // Handle result slot hotkey: craft + move to hotbar via SWAP click
        // (server intercepts SWAP on slot 0 to craft instead of swapping ghosts)
        boolean hotkeyHandled = false;
        if (hoveredSlot != null && hoveredSlot.slotNumber == 0 && mc.player.inventory.getItemStack().isEmpty()) {
            for (int i = 0; i < 9; i++) {
                if (mc.gameSettings.keyBindsHotbar[i].isActiveAndMatches(keyCode)) {
                    // Send a SWAP click — server handles crafting in slotClick override
                    mc.playerController.windowClick(inventorySlots.windowId, 0, i, net.minecraft.inventory.ClickType.SWAP, mc.player);
                    hotkeyHandled = true;
                    break;
                }
            }
        }
        
        if (!hotkeyHandled) {
            Keyboard.enableRepeatEvents(true);
            if (searchBar.isFocused() && searchBar.textboxKeyTyped(typedChar, keyCode)) {
                SearchSettings.setSearch(searchBar.getText());
                return;
            }
            if (!searchBar.isFocused() && !stackUnderMouse.isEmpty() && mc.gameSettings.keyBindDrop.isActiveAndMatches(keyCode)) {
                boolean all = isCtrlKeyDown();
                com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendToServer(
                    new com.brilliafy.magicstorage.network.NetworkHandler.DropMessage(stackUnderMouse, all));
                return;
            }
            super.keyTyped(typedChar, keyCode);  // calls checkHotbarKeys ONCE internally
        }
    }

    public ItemStack getStackUnderMouse() { return stackUnderMouse; }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchBar != null) searchBar.updateCursorCounter();
        if (SearchSettings.isJeiSearchSynced() && com.brilliafy.magicstorage.jei.JeiHooks.isJeiLoaded()) {
            String jeiText = com.brilliafy.magicstorage.jei.JeiHooks.getFilterText();
            if (jeiText != null && searchBar != null && !jeiText.equals(searchBar.getText())) {
                searchBar.setText(jeiText);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int i = Mouse.getX() * width / mc.displayWidth;
        int j = height - Mouse.getY() * height / mc.displayHeight - 1;
        if (inField(i, j)) {
            int m = Mouse.getEventDWheel();
            if (m > 0 && page > 1) page--;
            if (m < 0 && page < maxPage) page++;
        }
    }

    public class GuiStorageButton extends GuiButton {
        public GuiStorageButton(int id, int x, int y, String str) { super(id, x, y, 14, 14, str); }
        public GuiStorageButton(int id, int x, int y, int width, String str) { super(id, x, y, width, 14, str); }
        @Override
        public void drawButton(Minecraft mc, int x, int y, float pticks) {
            if (this.visible) {
                FontRenderer fr = mc.fontRenderer;
                mc.getTextureManager().bindTexture(texture);
                GL11.glColor4f(1, 1, 1, 1);
                hovered = x >= this.x && y >= this.y && x < this.x + this.width && y < this.y + this.height;
                int k = getHoverState(hovered);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.blendFunc(770, 771);
                drawTexturedModalRect(this.x, this.y, 162 + 14 * k, 0, width, height);
                if (id == directionBtn.id) drawTexturedModalRect(this.x + 4, this.y + 3, WIDTH + (getDownwards() ? 6 : 0), 14, 6, 8);
                if (id == sortBtn.id) drawTexturedModalRect(this.x + 4, this.y + 3, 188 + (getSort() == EnumSortType.AMOUNT ? 6 : getSort() == EnumSortType.MOD ? 12 : 0), 14, 6, 8);
                if (id == keepBtn.id) drawTexturedModalRect(this.x + 4, this.y + 3, WIDTH + (SearchSettings.isSearchKept() ? 12 : 18), 22, 6, 8);
                if (id == jeiBtn.id) drawTexturedModalRect(this.x + 4, this.y + 3, WIDTH + (SearchSettings.isJeiSearchSynced() ? 0 : 6), 22, 6, 7);
                if (autofillBtn != null && id == autofillBtn.id) {
                    if (SearchSettings.getAutofillMode() == com.brilliafy.magicstorage.data.EnumAutofillMode.FULL) {
                        drawTexturedModalRect(this.x + 4, this.y + 3, 182, 29, 6, 8);
                    } else if (SearchSettings.getAutofillMode() == com.brilliafy.magicstorage.data.EnumAutofillMode.NETWORK_ONLY) {
                        drawTexturedModalRect(this.x + 4, this.y + 3, 176, 29, 6, 8);
                    } else {
                        drawTexturedModalRect(this.x + 4, this.y + 3, 188, 14, 6, 8);
                    }
                }
                mouseDragged(mc, x, y);
                drawCenteredString(fr, displayString, this.x + width / 2, this.y + (height - 8) / 2, 14737632);
            }
        }
    }
}
