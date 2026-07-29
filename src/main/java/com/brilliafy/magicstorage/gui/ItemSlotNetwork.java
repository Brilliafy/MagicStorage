/*
 * Portions of this file contain code adapted from Storage Network
 * by Lothrazar (https://github.com/Lothrazar/Storage-Network).
 *
 * Copyright (c) Lothrazar
 * Licensed under the MIT License.
 */
package com.brilliafy.magicstorage.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public class ItemSlotNetwork {

    final IStorageInventory gui;
    final ItemStack stack;
    final int xPos, yPos;
    final int size;
    final int guiLeft, guiTop;
    final boolean showSize;

    public ItemSlotNetwork(IStorageInventory gui, ItemStack stack, int xPos, int yPos, int size, int guiLeft, int guiTop, boolean showSize) {
        this.gui = gui;
        this.stack = stack;
        this.xPos = xPos;
        this.yPos = yPos;
        this.size = size;
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
        this.showSize = showSize;
    }

    public boolean isMouseOverSlot(int mouseX, int mouseY) {
        return gui.isPointInRegionP(xPos - guiLeft, yPos - guiTop, 16, 16, mouseX, mouseY);
    }

    /** Draw only the item icon — no count text (counts rendered separately via renderCounts for proper z-ordering) */
    public void drawSlot(int mouseX, int mouseY) {
        if (stack.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        RenderItem itemRender = mc.getRenderItem();

        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(stack, xPos, yPos);
        itemRender.renderItemOverlayIntoGUI(mc.fontRenderer, stack, xPos, yPos, "");
        RenderHelper.disableStandardItemLighting();

        if (isMouseOverSlot(mouseX, mouseY)) {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            Gui.drawRect(xPos, yPos, xPos + 16, yPos + 16, 0x80FFFFFF);
            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
            // Reset color — drawRect leaves color.tinted (alpha=0.5), which bleeds into count text
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
    /** Render count text for all slots in a single pass, on top of all items */
    public static void renderCounts(List<ItemSlotNetwork> slots) {
        Minecraft mc = Minecraft.getMinecraft();
        // Fully reset GL state from item rendering — lighting tints the text grey
        GlStateManager.disableLighting();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        for (ItemSlotNetwork slot : slots) {
            if (slot.stack.isEmpty() || !slot.showSize || slot.size <= 1) continue;

            String s;
            if (slot.size >= 10000000) {
                s = String.format("%.1fM", slot.size / 1000000.0);
            } else if (slot.size >= 100000) {
                s = String.format("%.0fk", slot.size / 1000.0);
            } else if (slot.size >= 1000) {
                s = String.format("%.1fk", slot.size / 1000.0);
            } else {
                s = String.valueOf(slot.size);
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(slot.xPos + 17, slot.yPos + 9, 0);
            GlStateManager.scale(0.75F, 0.75F, 1.0F);
            GlStateManager.translate(-(slot.xPos + 17), -(slot.yPos + 9), 0);
            GlStateManager.disableDepth();
            GlStateManager.disableBlend();
            mc.fontRenderer.drawStringWithShadow(TextFormatting.WHITE + s,
                slot.xPos + 17 - mc.fontRenderer.getStringWidth(s) * 0.75F, slot.yPos + 9, 0xFFFFFF);
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }

    public void drawTooltip(int mouseX, int mouseY) {
        if (!stack.isEmpty()) {
            try {
                List<String> tooltip = stack.getTooltip(
                    Minecraft.getMinecraft().player,
                    Minecraft.getMinecraft().gameSettings.advancedItemTooltips
                        ? net.minecraft.client.util.ITooltipFlag.TooltipFlags.ADVANCED
                        : net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL
                );
                gui.renderToolTipP(stack, mouseX, mouseY);
            } catch (Throwable e) { }
        }
    }

    public ItemStack getStack() { return stack; }
}
