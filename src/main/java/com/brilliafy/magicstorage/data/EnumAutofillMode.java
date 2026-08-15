package com.brilliafy.magicstorage.data;

public enum EnumAutofillMode {
    FULL(2),
    NETWORK_ONLY(1),
    DISABLED(0);

    private final int id;

    EnumAutofillMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public EnumAutofillMode next() {
        switch (this) {
            case FULL:
                return NETWORK_ONLY;
            case NETWORK_ONLY:
                return DISABLED;
            case DISABLED:
                return FULL;
            default:
                return FULL;
        }
    }

    public static EnumAutofillMode fromId(int id) {
        for (EnumAutofillMode mode : values()) {
            if (mode.id == id) return mode;
        }
        return FULL;
    }
}
