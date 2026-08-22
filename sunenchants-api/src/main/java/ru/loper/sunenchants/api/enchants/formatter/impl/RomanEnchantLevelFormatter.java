package ru.loper.sunenchants.api.enchants.formatter.impl;

import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;

public final class RomanEnchantLevelFormatter implements EnchantLevelFormatter {
    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    @Override
    public @NotNull String format(int level) {
        if (level <= 0) {
            return String.valueOf(level);
        }

        StringBuilder builder = new StringBuilder();
        int value = level;

        for (int i = 0; i < VALUES.length; i++) {
            while (value >= VALUES[i]) {
                builder.append(SYMBOLS[i]);
                value -= VALUES[i];
            }
        }

        return builder.toString();
    }
}
