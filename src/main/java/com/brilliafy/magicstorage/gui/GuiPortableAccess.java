package com.brilliafy.magicstorage.gui;

import com.brilliafy.magicstorage.container.ContainerPortableAccess;
import com.brilliafy.magicstorage.data.EnumSortType;
import com.brilliafy.magicstorage.reference.ModInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class GuiPortableAccess extends GuiCraftingAccess {

    private final ContainerPortableAccess container;

    public GuiPortableAccess(ContainerPortableAccess container) {
        super(container);
        this.container = container;
        this.isSimple = container.isSimple();
        if (isSimple) {
            texture = new ResourceLocation(ModInfo.MOD_ID, "textures/gui/request_full.png");
        }
    }

    @Override
    public boolean getDownwards() {
        ItemStack remote = container.getRemoteStack();
        if (!remote.isEmpty() && remote.hasTagCompound())
            return remote.getTagCompound().getBoolean("down");
        return false;
    }

    @Override
    public void setDownwards(boolean d) {
        ItemStack remote = container.getRemoteStack();
        if (!remote.isEmpty() && remote.hasTagCompound())
            remote.getTagCompound().setBoolean("down", d);
    }

    @Override
    public EnumSortType getSort() {
        ItemStack remote = container.getRemoteStack();
        if (!remote.isEmpty() && remote.hasTagCompound() && remote.getTagCompound().hasKey("sort"))
            return EnumSortType.valueOf(remote.getTagCompound().getString("sort"));
        return EnumSortType.NAME;
    }

    @Override
    public void setSort(EnumSortType s) {
        ItemStack remote = container.getRemoteStack();
        if (!remote.isEmpty() && remote.hasTagCompound())
            remote.getTagCompound().setString("sort", s.toString());
    }

    @Override
    public BlockPos getPos() {
        return BlockPos.ORIGIN;
    }

    @Override
    protected int getDim() {
        ItemStack remote = container.getRemoteStack();
        if (!remote.isEmpty() && remote.hasTagCompound())
            return remote.getTagCompound().getInteger("dim");
        return 0;
    }

    @Override
    protected boolean isScreenValid() {
        return !container.getRemoteStack().isEmpty();
    }
}
