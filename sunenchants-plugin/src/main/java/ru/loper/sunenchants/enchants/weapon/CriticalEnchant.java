package ru.loper.sunenchants.enchants.weapon;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.IntLevel;

@EnchantRegister(name = "critical", level = EnchantLevelType.INTEGER)
public class CriticalEnchant extends SEnchant {
    public CriticalEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        ItemStack weapon = damager.getInventory().getItemInMainHand();
        if (!weapon.hasItemMeta() || !isApplied(weapon)) {
            return;
        }

        IntLevel level = getLevel(weapon);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        double newDamage = event.getDamage() + event.getDamage() / 100 * level.getValue();
        event.setDamage(newDamage);
        level.playSounds(damager);
    }
}
