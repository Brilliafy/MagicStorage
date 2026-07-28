package com.brilliafy.magicstorage.gui;

import com.brilliafy.magicstorage.container.ContainerCraftingAccess;
import com.brilliafy.magicstorage.data.EnumSortType;
import com.brilliafy.magicstorage.tile.TileCraftingAccess;
import net.minecraft.util.math.BlockPos;

public class GuiCraftingAccessRequest extends GuiCraftingAccess {

    private final ContainerCraftingAccess container;
    private final TileCraftingAccess tile;
    private boolean downwards = false;
    private EnumSortType sortType = EnumSortType.NAME;

    public GuiCraftingAccessRequest(ContainerCraftingAccess container, TileCraftingAccess tile) {
        super(container);
        this.container = container;
        this.tile = tile;
    }

    @Override
    public boolean getDownwards() { return downwards; }

    @Override
    public void setDownwards(boolean d) { this.downwards = d; }

    @Override
    public EnumSortType getSort() { return sortType; }

    @Override
    public void setSort(EnumSortType s) { this.sortType = s; }

    @Override
    public BlockPos getPos() { return tile != null ? tile.getPos() : BlockPos.ORIGIN; }

    @Override
    protected int getDim() { return tile != null ? tile.getWorld().provider.getDimension() : 0; }

    @Override
    protected boolean isScreenValid() { return true; }
}
