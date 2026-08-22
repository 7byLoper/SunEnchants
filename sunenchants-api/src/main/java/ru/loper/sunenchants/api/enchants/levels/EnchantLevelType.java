package ru.loper.sunenchants.api.enchants.levels;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import ru.loper.sunenchants.api.enchants.levels.impl.DoubleLevel;
import ru.loper.sunenchants.api.enchants.levels.impl.EffectsLevel;
import ru.loper.sunenchants.api.enchants.levels.impl.IntLevel;

public enum EnchantLevelType {
    DEFAULT,
    INTEGER,
    DOUBLE,
    EFFECTS;

    public static int parseInteger(String number) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public Map<Integer, AbstractLevel> getEnchantLevels(ConfigurationSection section) {
        Map<Integer, AbstractLevel> enchantLevels = new HashMap<>();
        if (section == null) return enchantLevels;

        for (String key : section.getKeys(false)) {
            ConfigurationSection levelSection = section.getConfigurationSection(key);
            if (levelSection == null) continue;

            AbstractLevel level =
                    switch (this) {
                        case DOUBLE -> new DoubleLevel(levelSection);
                        case INTEGER -> new IntLevel(levelSection);
                        case EFFECTS -> new EffectsLevel(levelSection);
                        default -> new AbstractLevel(levelSection);
                    };
            enchantLevels.put(parseInteger(key), level);
        }

        return enchantLevels;
    }
}
