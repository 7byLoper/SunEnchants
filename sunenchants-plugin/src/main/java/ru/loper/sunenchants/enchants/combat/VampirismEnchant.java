package ru.loper.sunenchants.enchants.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.utils.CombatUtils;

@EnchantRegister(name = "vampirism")
public class VampirismEnchant extends SEnchant {
    private final Map<Integer, EffectSettings> settings = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public VampirismEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        settings.clear();
        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) return;

        for (String key : levels.getKeys(false)) {
            ConfigurationSection level = levels.getConfigurationSection(key);
            if (level == null) continue;
            try {
                settings.put(
                        Integer.parseInt(key),
                        new EffectSettings(
                                Math.max(1, level.getInt("duration_ticks", 40)),
                                Math.max(0, level.getInt("amplifier", 0)),
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || event.getFinalDamage() <= 0.0D) return;

        Player attacker = CombatUtils.resolvePlayer(event.getDamager());
        ItemStack weapon = CombatUtils.resolveWeapon(event.getDamager());
        if (attacker == null || weapon == null) return;

        int enchantLevel = getAppliedLevel(weapon);
        EffectSettings value = settings.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (value == null || level == null || !isCooldownReady(attacker, value.cooldownMs()) || !level.hasWorkChance())
            return;

        attacker.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.REGENERATION, value.durationTicks(), value.amplifier(), false, true, true),
                true);
        cooldowns.put(attacker.getUniqueId(), System.currentTimeMillis());
        level.playSounds(attacker);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    private boolean isCooldownReady(Player player, long cooldownMs) {
        if (cooldownMs <= 0L) return true;
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        return System.currentTimeMillis() - last >= cooldownMs;
    }

    private record EffectSettings(int durationTicks, int amplifier, long cooldownMs) {}
}
