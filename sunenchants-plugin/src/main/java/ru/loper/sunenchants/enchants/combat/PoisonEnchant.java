package ru.loper.sunenchants.enchants.combat;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

@EnchantRegister(name = "poison")
public class PoisonEnchant extends SEnchant {
    private final Map<Integer, EffectSettings> settings = new HashMap<>();

    public PoisonEnchant(
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
                                Math.max(1, level.getInt("duration_ticks", 60)),
                                Math.max(0, level.getInt("amplifier", 0))));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || event.getFinalDamage() <= 0.0D) return;

        ItemStack weapon = CombatUtils.resolveWeapon(event.getDamager());
        int enchantLevel = getAppliedLevel(weapon);
        EffectSettings value = settings.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (value == null || level == null || !level.hasWorkChance()) return;

        victim.addPotionEffect(
                new PotionEffect(PotionEffectType.POISON, value.durationTicks(), value.amplifier(), false, true, true),
                true);

        Player attacker = CombatUtils.resolvePlayer(event.getDamager());
        if (attacker != null) level.playSounds(attacker);
    }

    private record EffectSettings(int durationTicks, int amplifier) {}
}
