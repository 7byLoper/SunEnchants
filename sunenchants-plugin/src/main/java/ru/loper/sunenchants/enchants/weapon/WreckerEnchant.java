package ru.loper.sunenchants.enchants.weapon;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
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
import ru.loper.sunholybossevent.event.boss.Boss;
import ru.loper.sunenchants.utils.HeldItemCache;

@EnchantRegister(name = "wrecker", level = EnchantLevelType.INTEGER)
public class WreckerEnchant extends SEnchant {
    private boolean bossesPlugin;

    public WreckerEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        bossesPlugin = Bukkit.getPluginManager().isPluginEnabled("SunHolyBossEvent");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!bossesPlugin) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        ItemStack itemStack = HeldItemCache.mainHand(player);
        if (!isApplied(itemStack)) {
            return;
        }

        if (Boss.getBoss(entity.getUniqueId()) == null) {
            return;
        }

        IntLevel level = getLevel(itemStack);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        double newDamage = event.getDamage() + event.getDamage() / 100 * level.getValue();
        event.setDamage(newDamage);
        level.playSounds(player);
    }
}
