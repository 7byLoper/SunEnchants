package ru.loper.sunenchants.enchants.projectile;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.utils.VelocityUtils;

@EnchantRegister(name = "sniper")
public class SniperEnchant extends SEnchant {
    private final Map<Integer, SniperSettings> settings = new HashMap<>();

    public SniperEnchant(
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
            try {
                int level = Integer.parseInt(key);
                settings.put(
                        level,
                        new SniperSettings(
                                Math.max(0.0D, levels.getDouble(key + ".velocity_multiplier", 1.0D)),
                                Math.max(0.0D, levels.getDouble(key + ".max_velocity", 8.0D))));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getProjectile() instanceof Projectile projectile))
            return;

        ItemStack bow = event.getBow();
        int enchantLevel = getAppliedLevel(bow);
        SniperSettings value = settings.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (value == null || level == null || !level.hasWorkChance()) return;

        Vector velocity = projectile.getVelocity().multiply(value.velocityMultiplier());
        projectile.setVelocity(VelocityUtils.clamp(velocity, value.maxVelocity()));
        level.playSounds(player);
    }

    private record SniperSettings(double velocityMultiplier, double maxVelocity) {}
}
