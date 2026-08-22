package ru.loper.sunenchants.api.enchants.levels.impl;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;

@Getter
public class IntLevel extends AbstractLevel {
    private final int value;

    public IntLevel(@NotNull ConfigurationSection section) {
        super(section);
        value = section.getInt("value");
    }
}
