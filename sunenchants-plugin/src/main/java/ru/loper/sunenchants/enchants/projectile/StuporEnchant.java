package ru.loper.sunenchants.enchants.projectile;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;

@EnchantRegister(name = "stupor")
public class StuporEnchant extends SEnchant {
    private final Map<Integer, StuporSettings> settings = new HashMap<>();
    private final NamespacedKey handledKey;

    public StuporEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
        handledKey = new NamespacedKey(SunEnchants.getInstance(), "stupor_handled");
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        settings.clear();
        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) return;

        for (String key : levels.getKeys(false)) {
            try {
                int number = Integer.parseInt(key);
                settings.put(
                        number,
                        new StuporSettings(
                                Math.max(1, levels.getInt(key + ".weakness.duration_ticks", 40)),
                                Math.max(0, levels.getInt(key + ".weakness.amplifier", 0)),
                                Math.max(1, levels.getInt(key + ".slowness.duration_ticks", 40)),
                                Math.max(0, levels.getInt(key + ".slowness.amplifier", 0))));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident) || !(event.getHitEntity() instanceof LivingEntity target))
            return;
        if (trident.getPersistentDataContainer().has(handledKey, PersistentDataType.BYTE)) return;

        int enchantLevel = getAppliedLevel(trident.getItemStack());
        StuporSettings value = settings.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (value == null || level == null) return;
        trident.getPersistentDataContainer().set(handledKey, PersistentDataType.BYTE, (byte) 1);
        if (!level.hasWorkChance()) return;

        target.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.WEAKNESS,
                        value.weaknessDuration(),
                        value.weaknessAmplifier(),
                        false,
                        true,
                        true),
                true);
        target.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SLOWNESS,
                        value.slownessDuration(),
                        value.slownessAmplifier(),
                        false,
                        true,
                        true),
                true);

        if (trident.getShooter() instanceof Player player) level.playSounds(player);
    }

    private record StuporSettings(
            int weaknessDuration, int weaknessAmplifier, int slownessDuration, int slownessAmplifier) {}
}
