package com.brilliafy.magicstorage.gui;

import com.brilliafy.magicstorage.container.ContainerStorageAccess;
import com.brilliafy.magicstorage.jei.SearchSettings;
import com.brilliafy.magicstorage.network.NetworkHandler;
import com.brilliafy.magicstorage.reference.ModInfo;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiStorageAccess extends GuiContainer implements IStorageInventory {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ModInfo.MOD_ID, "textures/gui/request_full.png");
    private final com.brilliafy.magicstorage.gui.IStorageContainer container;
    protected int page = 1, maxPage = 1;
    protected GuiTextField searchBar;
    protected ItemStack stackUnderMouse = ItemStack.EMPTY;
    protected long lastClick;
    protected List<ItemSlotNetwork> slots = new ArrayList<>();

    public GuiStorageAccess(InventoryPlayer playerInv, com.brilliafy.magicstorage.gui.IStorageContainer container) {
        super((net.minecraft.inventory.Container) container);
        this.container = container;
        this.xSize = 176;
        this.ySize = 250;
    }

    private boolean canClick() { return System.currentTimeMillis() > lastClick + 100L; }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        searchBar = new GuiTextField(0, fontRenderer, guiLeft + 81, guiTop + 160, 85, fontRenderer.FONT_HEIGHT);
        searchBar.setMaxStringLength(30);
        searchBar.setEnableBackgroundDrawing(false);
        searchBar.setVisible(true);
        searchBar.setTextColor(16777215);
        searchBar.setFocused(false);
        searchBar.setText("");
        // Request items from server (mouseButton=-1 signals refresh)
        com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendToServer(
            new com.brilliafy.magicstorage.network.NetworkHandler.RequestMessage(-1, net.minecraft.item.ItemStack.EMPTY, false, false));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {}

    // IStorageInventory implementation
    @Override public void drawGradientRectP(int left, int top, int right, int bottom, int startColor, int endColor) {
        super.drawGradientRect(left, top, right, bottom, startColor, endColor);
    }
    @Override public net.minecraft.client.gui.FontRenderer getFont() { return this.fontRenderer; }
    @Override public boolean isPointInRegionP(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
        return super.isPointInRegion(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
    }
    @Override public void renderToolTipP(net.minecraft.item.ItemStack stack, int x, int y) {
        super.renderToolTip(stack, x, y);
    }
    @Override public void setStacks(List<ItemStack> stacks) {
        // Store in container so drawGuiContainerBackgroundLayer picks it up
        container.setStacks(stacks);
    }
    @Override public void setCraftableStacks(List<ItemStack> stacks) {}

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        List<ItemStack> items = new ArrayList<>(container.getCachedStacks());

        String search = searchBar != null ? searchBar.getText() : "";
        if (!search.isEmpty()) {
            List<ItemStack> filtered = new ArrayList<>();
            for (ItemStack s : items)
                if (s.getDisplayName().toLowerCase().contains(search.toLowerCase())) filtered.add(s);
            items = filtered;
        }

        items.sort((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));

        int perPage = getLines() * getColumns();
        maxPage = Math.max(1, (items.size() + perPage - 1) / perPage);
        if (page > maxPage) page = maxPage;
        if (page < 1) page = 1;

        slots.clear();
        int index = (page - 1) * getColumns();
        for (int row = 0; row < getLines(); row++) {
            for (int col = 0; col < getColumns(); col++) {
                if (index >= items.size()) break;
                ItemStack stack = items.get(index);
                slots.add(new ItemSlotNetwork(this, stack,
                    guiLeft + 8 + col * 18, guiTop + 10 + row * 18,
                    stack.getCount(), guiLeft, guiTop, true));
                index++;
            }
        }

        stackUnderMouse = ItemStack.EMPTY;
        for (ItemSlotNetwork slot : slots) {
            slot.drawSlot(mouseX, mouseY);
            if (slot.isMouseOverSlot(mouseX, mouseY)) stackUnderMouse = slot.getStack();
        }
        ItemSlotNetwork.renderCounts(slots);

        if (searchBar != null) searchBar.drawTextBox();
    }

    private int getLines() { return 8; }
    private int getColumns() { return 9; }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchBar.setFocused(false);
        if (inSearchbar(mouseX, mouseY)) {
            searchBar.setFocused(true);
            if (mouseButton == 1) searchBar.setText("");
        } else {
            ItemStack carried = mc.player.inventory.getItemStack();
            boolean middle = mouseButton == 2;
            if (middle && !carried.isEmpty()) return;
            if (!stackUnderMouse.isEmpty() && (mouseButton == 0 || mouseButton == 1 || mouseButton == 2) && carried.isEmpty() && canClick()) {
                // Request items from server — match SSN behavior: pass shift key state
                NetworkHandler.INSTANCE.sendToServer(
                    new NetworkHandler.RequestMessage(mouseButton, stackUnderMouse, isShiftKeyDown(), middle));
                lastClick = System.currentTimeMillis();
            } else if (!carried.isEmpty() && inField(mouseX, mouseY) && canClick()) {
                // Insert items into network
                int dim = mc.player.world.provider.getDimension();
                NetworkHandler.INSTANCE.sendToServer(
                    new NetworkHandler.InsertMessage(dim, mouseButton));
                lastClick = System.currentTimeMillis();
            }
        }
    }

    private boolean inField(int mouseX, int mouseY) {
        int h = 90 + 60;
        return mouseX > (guiLeft + 7) && mouseX < (guiLeft + xSize - 7)
            && mouseY > (guiTop + 7) && mouseY < (guiTop + h);
    }

    private boolean inSearchbar(int mouseX, int mouseY) {
        return isPointInRegion(searchBar.x - guiLeft, searchBar.y - guiTop,
            searchBar.width, fontRenderer.FONT_HEIGHT, mouseX, mouseY);
    }

    public ItemStack getStackUnderMouse() { return stackUnderMouse; }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchBar != null && searchBar.isFocused()) {
            if (searchBar.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
        }
        if (!stackUnderMouse.isEmpty() && com.brilliafy.magicstorage.jei.JeiHooks.isJeiKeybind(keyCode)) {
            try { com.brilliafy.magicstorage.jei.JeiHooks.testJeiKeybind(keyCode, stackUnderMouse); } catch (Throwable ignored) {}
            return;
        }
        if ((searchBar == null || !searchBar.isFocused()) && !stackUnderMouse.isEmpty() && mc.gameSettings.keyBindDrop.isActiveAndMatches(keyCode)) {
            boolean all = isCtrlKeyDown();
            com.brilliafy.magicstorage.network.NetworkHandler.INSTANCE.sendToServer(
                new com.brilliafy.magicstorage.network.NetworkHandler.DropMessage(stackUnderMouse, all));
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchBar != null) searchBar.updateCursorCounter();
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
}
