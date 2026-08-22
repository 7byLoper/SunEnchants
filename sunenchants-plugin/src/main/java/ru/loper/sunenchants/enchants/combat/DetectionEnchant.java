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

@EnchantRegister(name = "detection")
public class DetectionEnchant extends SEnchant {
    private final Map<Integer, Integer> durationByLevel = new HashMap<>();

    public DetectionEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        durationByLevel.clear();
        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) return;

        for (String key : levels.getKeys(false)) {
            try {
                durationByLevel.put(Integer.parseInt(key), Math.max(1, levels.getInt(key + ".duration_ticks", 60)));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || event.getFinalDamage() <= 0.0D) return;

        ItemStack weapon = CombatUtils.resolveWeapon(event.getDamager());
        int enchantLevel = getAppliedLevel(weapon);
        Integer duration = durationByLevel.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (duration == null || level == null || !level.hasWorkChance()) return;

        victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0, false, false, true), true);
        Player attacker = CombatUtils.resolvePlayer(event.getDamager());
        if (attacker != null) level.playSounds(attacker);
    }
}
