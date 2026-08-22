package ru.loper.sunenchants.api.enchants.levels.impl;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;

@Getter
public class DoubleLevel extends AbstractLevel {
    private final double value;

    public DoubleLevel(@NotNull ConfigurationSection section) {
        super(section);
        value = section.getDouble("value");
    }
}
