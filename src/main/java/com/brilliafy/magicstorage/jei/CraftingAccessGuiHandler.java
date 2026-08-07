package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.gui.GuiCraftingAccess;
import mezz.jei.api.gui.IAdvancedGuiHandler;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class CraftingAccessGuiHandler implements IAdvancedGuiHandler<GuiCraftingAccess> {

    @Override
    public Class<GuiCraftingAccess> getGuiContainerClass() {
        return GuiCraftingAccess.class;
    }

    @Nullable
    @Override
    public Object getIngredientUnderMouse(GuiCraftingAccess guiContainer, int mouseX, int mouseY) {
        ItemStack stack = guiContainer.getStackUnderMouse();
        return !stack.isEmpty() ? stack : null;
    }
}
