package com.brilliafy.magicstorage.jei;

import com.brilliafy.magicstorage.gui.GuiStorageAccess;
import mezz.jei.api.gui.IAdvancedGuiHandler;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class StorageGuiHandler implements IAdvancedGuiHandler<GuiStorageAccess> {

    @Override
    public Class<GuiStorageAccess> getGuiContainerClass() {
        return GuiStorageAccess.class;
    }

    @Nullable
    @Override
    public Object getIngredientUnderMouse(GuiStorageAccess guiContainer, int mouseX, int mouseY) {
        ItemStack stack = guiContainer.getStackUnderMouse();
        return !stack.isEmpty() ? stack : null;
    }
}
