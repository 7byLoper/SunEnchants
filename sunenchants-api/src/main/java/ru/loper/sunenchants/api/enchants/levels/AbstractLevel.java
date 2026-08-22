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
    private final boolean combining;

    public AbstractLevel(@NotNull ConfigurationSection section) {
        aloneSound = getSound(section.getString("sounds.alone", ""));
        aroundSound = getSound(section.getString("sounds.around", ""));

        workChance = section.getInt("chances.work", 100);
        tableChance = section.getInt("chances.table_enchant", 3);

        combining = section.getBoolean("combining", true);
    }

    private static Sound getSound(@NotNull String name) {
        try {
            return Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase()));
        } catch (Exception e) {
            return null;
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
        SoundPlayer.play(player.getLocation(), aroundSound, 1, 1);
    }

    public void playAlone(Player player) {
        if (aloneSound == null) return;
        SoundPlayer.play(player, aloneSound, 1, 1);
    }
}
