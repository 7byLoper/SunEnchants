package ru.loper.sunenchants.api.enchants.levels;

import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.sound.SoundPlayer;

@Getter
@RequiredArgsConstructor
public class AbstractLevel {
    private final int workChance, tableChance;
    private final Sound aloneSound, aroundSound;
    private final float aloneVolume, alonePitch, aroundVolume, aroundPitch;
    private final boolean combining;

    public AbstractLevel(@NotNull ConfigurationSection section) {
        SoundSettings aloneSettings = getSoundSettings(section.getString("sounds.alone", ""));
        aloneSound = aloneSettings.sound();
        aloneVolume = aloneSettings.volume();
        alonePitch = aloneSettings.pitch();

        SoundSettings aroundSettings = getSoundSettings(section.getString("sounds.around", ""));
        aroundSound = aroundSettings.sound();
        aroundVolume = aroundSettings.volume();
        aroundPitch = aroundSettings.pitch();

        workChance = section.getInt("chances.work", 100);
        tableChance = section.getInt("chances.table_enchant", 3);

        combining = section.getBoolean("combining", true);
    }

    private static SoundSettings getSoundSettings(@NotNull String value) {
        String[] parts = value.split(";", -1);
        Sound sound = getSound(parts[0].trim());
        float volume = parts.length > 1 ? getSoundParameter(parts[1]) : 1.0F;
        float pitch = parts.length > 2 ? getSoundParameter(parts[2]) : 1.0F;
        return new SoundSettings(sound, volume, pitch);
    }

    private static Sound getSound(@NotNull String name) {
        try {
            return Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase()));
        } catch (Exception e) {
            return null;
        }
    }

    private static float getSoundParameter(String value) {
        try {
            return Math.max(0.0F, Float.parseFloat(value.trim()));
        } catch (NumberFormatException e) {
            return 1.0F;
        }
    }

    public boolean hasTableChance() {
        return rollChance(tableChance);
    }

    public boolean hasWorkChance() {
        return rollChance(workChance);
    }

    private boolean rollChance(int chance) {
        if (chance <= 0) {
            return false;
        }
        if (chance >= 100) {
            return true;
        }
        return ThreadLocalRandom.current().nextInt(100) < chance;
    }

    public void playSounds(Player player) {
        playAlone(player);
        playAround(player);
    }

    public void playAround(Player player) {
        if (aroundSound == null) return;
        Location location = player.getLocation();
        SoundPlayer.play(location, aroundSound, aroundVolume, aroundPitch);
    }

    public void playAlone(Player player) {
        if (aloneSound == null) return;
        SoundPlayer.play(player, aloneSound, aloneVolume, alonePitch);
    }

    private record SoundSettings(Sound sound, float volume, float pitch) {
    }
}
