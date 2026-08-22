package ru.loper.sunenchants.api.enchants.levels.impl;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;

@Getter
public class EffectsLevel extends AbstractLevel {
    private static final Logger LOGGER = Logger.getLogger("SunEnchants");
    private final List<PotionEffect> effects;

    public EffectsLevel(@NotNull ConfigurationSection section) {
        super(section);
        effects = section.getStringList("value").stream()
                .map(EffectsLevel::parsePotionEffect)
                .filter(Objects::nonNull)
                .toList();
    }

    private static PotionEffect parsePotionEffect(String input) {
        try {
            String[] parts = input.split(";");
            if (parts.length < 3) return null;

            PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
            if (type == null) return null;

            int duration = Integer.parseInt(parts[1]);
            int amplifier = Integer.parseInt(parts[2]);

            boolean ambient = parts.length > 3 && Boolean.parseBoolean(parts[3]);
            boolean particles = parts.length > 4 && Boolean.parseBoolean(parts[4]);
            boolean icon = parts.length > 5 && Boolean.parseBoolean(parts[5]);

            return new PotionEffect(type, duration, amplifier, ambient, particles, icon);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Invalid potion effect definition: " + input, exception);
            return null;
        }
    }
}
