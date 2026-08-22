package ru.loper.sunenchants.api.enchants.formatter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface EnchantTextFormatter {
    @NotNull
    String format(@Nullable String input);
}
