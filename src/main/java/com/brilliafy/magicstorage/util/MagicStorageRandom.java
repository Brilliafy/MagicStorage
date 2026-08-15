package com.brilliafy.magicstorage.util;

import java.util.concurrent.ThreadLocalRandom;

public final class MagicStorageRandom {

    private MagicStorageRandom() {}

    /**
     * Rolls a true uniform chance [0.0, 1.0) with 53 bits of precision.
     * Returns true if roll is strictly less than chance.
     */
    public static boolean rollChance(double chance) {
        if (chance <= 0.0) return false;
        if (chance >= 1.0) return true;
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    public static double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    public static float nextFloat() {
        return ThreadLocalRandom.current().nextFloat();
    }

    public static int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    public static int nextInt(int origin, int bound) {
        return ThreadLocalRandom.current().nextInt(origin, bound);
    }
}
