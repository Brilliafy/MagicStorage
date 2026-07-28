package com.brilliafy.magicstorage.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import java.util.List;

public interface IStorageInventory {
    void drawGradientRectP(int left, int top, int right, int bottom, int startColor, int endColor);
    FontRenderer getFont();
    boolean isPointInRegionP(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY);
    void renderToolTipP(ItemStack stack, int x, int y);
    void setStacks(List<ItemStack> stacks);
    void setCraftableStacks(List<ItemStack> stacks);
}
