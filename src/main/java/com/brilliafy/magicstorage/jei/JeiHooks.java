/*
 * Portions of this file contain code adapted from Storage Network
 * by Lothrazar (https://github.com/Lothrazar/Storage-Network).
 *
 * Copyright (c) Lothrazar
 * Licensed under the MIT License.
 */
package com.brilliafy.magicstorage.jei;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;

public class JeiHooks {

    private static boolean jeiLoaded;

    public static boolean isJeiLoaded() { return jeiLoaded; }
    public static void setJeiLoaded(boolean v) { jeiLoaded = v; }

    public static String getFilterText() {
        try {
            if (jeiLoaded) return getJeiTextInternal();
        } catch (Exception e) {}
        return "";
    }

    public static void setFilterText(String s) {
        try {
            if (jeiLoaded) setJeiTextInternal(s);
        } catch (Exception e) {}
    }

    @Optional.Method(modid = "jei")
    private static void setJeiTextInternal(String s) {
        mezz.jei.Internal.getRuntime().getIngredientFilter().setFilterText(s);
    }

    @Optional.Method(modid = "jei")
    private static String getJeiTextInternal() {
        return mezz.jei.Internal.getRuntime().getIngredientFilter().getFilterText();
    }

    @Optional.Method(modid = "jei")
    public static void testJeiKeybind(int keyCode, ItemStack stackUnderMouse) {
        final boolean showRecipe = mezz.jei.config.KeyBindings.showRecipe.isActiveAndMatches(keyCode);
        final boolean showUses = mezz.jei.config.KeyBindings.showUses.isActiveAndMatches(keyCode);
        if (showRecipe || showUses) {
            mezz.jei.api.recipe.IFocus.Mode mode = showRecipe ? mezz.jei.api.recipe.IFocus.Mode.OUTPUT : mezz.jei.api.recipe.IFocus.Mode.INPUT;
            mezz.jei.Internal.getRuntime().getRecipesGui().show(new mezz.jei.gui.Focus<>(mode, stackUnderMouse));
        }
    }
}
