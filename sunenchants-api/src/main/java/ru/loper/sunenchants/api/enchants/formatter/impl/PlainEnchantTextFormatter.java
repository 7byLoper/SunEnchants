package ru.loper.sunenchants.api.enchants.formatter.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.suncore.api.colorize.StringColorize;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;

public final class PlainEnchantTextFormatter implements EnchantTextFormatter {
    @Override
    public @NotNull String format(@Nullable String input) {
        return input == null ? "" : StringColorize.parse(input);
    }
}
