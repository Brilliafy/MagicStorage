package com.brilliafy.magicstorage.gui;

import com.brilliafy.magicstorage.reference.ModInfo;
import com.brilliafy.magicstorage.tile.TileStorageHeart;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;

public class GuiStorageHeart extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ModInfo.MOD_ID, "textures/gui/crafting_interface.png");

    public GuiStorageHeart(InventoryPlayer playerInv, TileStorageHeart heart) {
        super(new com.brilliafy.magicstorage.container.ContainerStorageHeart(playerInv, heart));
        // 2 rows × 10 columns of storage slots at 18px spacing
        // Texture: top (title y=3-6, 4px) + gap + slots (y=18, 36) + gap + player inv (y=84-136) + hotbar (y=142-158) + bottom
        // ySize aligns with the slot rows in the texture: slots end at y=52, player inv starts at y=84 in the container
        // Using xSize=192 to fit 10 columns (8+9*18=170 + right margin)
        this.xSize = 192;
        // ySize calculated: title=12px + slots(2*18=36) + gap(30) + player inv(3*18=54) + hotbar(18) + bottom(14) = 164
        this.ySize = 164;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString("Storage Heart", 8, 6, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        super.renderHoveredToolTip(mouseX, mouseY);
    }
}
