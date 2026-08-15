/*
 * Portions of this file contain code adapted from Storage Network
 * by Lothrazar (https://github.com/Lothrazar/Storage-Network).
 *
 * Copyright (c) Lothrazar
 * Licensed under the MIT License.
 */
package com.brilliafy.magicstorage.jei;

import net.minecraft.item.ItemStack;

public class SearchSettings {

    private static boolean jeiSearchSync = true;
    private static boolean keepSearch = true;
    private static com.brilliafy.magicstorage.data.EnumAutofillMode autofillMode = com.brilliafy.magicstorage.data.EnumAutofillMode.FULL;
    private static String search = "";

    public static boolean isJeiSearchSynced() { return jeiSearchSync; }
    public static void setJeiSearchSync(boolean v) { jeiSearchSync = v; }
    public static boolean isSearchKept() { return keepSearch; }
    public static void setKeepSearch(boolean v) { keepSearch = v; }
    public static com.brilliafy.magicstorage.data.EnumAutofillMode getAutofillMode() { return autofillMode; }
    public static void setAutofillMode(com.brilliafy.magicstorage.data.EnumAutofillMode mode) { autofillMode = mode; }
    public static boolean isAutofill() { return autofillMode != com.brilliafy.magicstorage.data.EnumAutofillMode.DISABLED; }

    public static String getSearch() {
        if (JeiHooks.isJeiLoaded() && jeiSearchSync) {
            if (keepSearch) return JeiHooks.getFilterText();
            else JeiHooks.setFilterText("");
        } else if (keepSearch) {
            return search;
        }
        return "";
    }

    public static void setSearch(String s) {
        if (JeiHooks.isJeiLoaded() && jeiSearchSync) {
            JeiHooks.setFilterText(s);
        }
        if (keepSearch) search = s;
    }
}
