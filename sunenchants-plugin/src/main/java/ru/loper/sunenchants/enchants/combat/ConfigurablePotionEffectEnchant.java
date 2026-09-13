package ru.loper.sunenchants.enchants.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.utils.CombatUtils;

public class ConfigurablePotionEffectEnchant extends SEnchant {
    private final Map<Integer, EffectSettings> settings = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public ConfigurablePotionEffectEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter,
            @NotNull String enchantName) {
        super(namespacedKey, textFormatter, levelFormatter, enchantName);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        settings.clear();
        cooldowns.clear();

        boolean affectsAttacker = "attacker".equalsIgnoreCase(section.getString("effect_target", "victim"));

        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) return;

        for (String key : levels.getKeys(false)) {
            ConfigurationSection level = levels.getConfigurationSection(key);
            if (level == null) continue;

            try {
                settings.put(
                        Integer.parseInt(key),
                        new EffectSettings(
                                getEffects(level.getStringList("effects")),
                                affectsAttacker,
                                Math.max(0L, level.getLong("cooldown_ms", 0L))));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    protected void onEnabledStateChanged(boolean enabled) {
        if (!enabled) {
            cooldowns.clear();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() <= 0.0D) return;

        Player attacker = CombatUtils.resolvePlayer(event.getDamager());
        ItemStack weapon = CombatUtils.resolveWeapon(event.getDamager());
        if (attacker == null || weapon == null) return;

        int enchantLevel = getAppliedLevel(weapon);
        EffectSettings value = settings.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (value == null
                || value.effects().isEmpty()
                || level == null
                || !isCooldownReady(attacker, value.cooldownMs())
                || !level.hasWorkChance())
            return;

        LivingEntity recipient = value.affectsAttacker() ? attacker : getVictim(event);
        if (recipient == null) return;

        value.effects().forEach(recipient::addPotionEffect);
        cooldowns.put(attacker.getUniqueId(), System.currentTimeMillis());
        level.playSounds(attacker);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    private LivingEntity getVictim(EntityDamageByEntityEvent event) {
        return event.getEntity() instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    private List<PotionEffect> getEffects(List<String> values) {
        List<PotionEffect> effects = new ArrayList<>();
        for (String value : values) {
            String[] parts = value.split(":", -1);
            if (parts.length != 3) continue;

            PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase(Locale.ROOT));
            if (type == null) continue;

            try {
                int amplifier = Math.max(0, Integer.parseInt(parts[1].trim()));
                int durationTicks = Math.max(1, Integer.parseInt(parts[2].trim()));
                effects.add(new PotionEffect(type, durationTicks, amplifier, false, true, true));
            } catch (NumberFormatException ignored) {
            }
        }

        return effects;
    }

    private boolean isCooldownReady(Player player, long cooldownMs) {
        if (cooldownMs <= 0L) return true;
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        return System.currentTimeMillis() - last >= cooldownMs;
    }

    private record EffectSettings(List<PotionEffect> effects, boolean affectsAttacker, long cooldownMs) {}
}
