package ru.loper.sunenchants.api.enchants.formatter;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface EnchantLevelFormatter {
    @NotNull
    String format(int level);
}
