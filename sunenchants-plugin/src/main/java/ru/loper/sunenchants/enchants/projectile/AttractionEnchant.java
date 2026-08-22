package ru.loper.sunenchants.enchants.projectile;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.utils.VelocityUtils;

@EnchantRegister(name = "attraction")
public class AttractionEnchant extends SEnchant {
    private final Map<Integer, PullSettings> settings = new HashMap<>();
    private final NamespacedKey handledKey;

    public AttractionEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
        handledKey = new NamespacedKey(SunEnchants.getInstance(), "attraction_handled");
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
                        new PullSettings(
                                Math.max(0.0D, levels.getDouble(key + ".force", 1.0D)),
                                Math.max(0.0D, levels.getDouble(key + ".max_velocity", 3.0D)),
                                levels.getDouble(key + ".vertical_multiplier", 1.0D),
                                Math.max(0.0D, levels.getDouble(key + ".max_distance", 32.0D))));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)
                || !(trident.getShooter() instanceof Player owner)
                || !(event.getHitEntity() instanceof LivingEntity target)
                || target.equals(owner)
                || !owner.isOnline()
                || owner.getWorld() != target.getWorld()
                || trident.getPersistentDataContainer().has(handledKey, PersistentDataType.BYTE)) {
            return;
        }

        int enchantLevel = getAppliedLevel(trident.getItemStack());
        PullSettings value = settings.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (value == null || level == null) return;
        trident.getPersistentDataContainer().set(handledKey, PersistentDataType.BYTE, (byte) 1);
        if (!level.hasWorkChance()) return;

        Location from = target.getLocation();
        Location to = owner.getLocation();
        double distance = from.distance(to);
        if (distance <= 0.01D || distance > value.maxDistance()) return;

        Vector velocity = to.toVector().subtract(from.toVector()).normalize().multiply(value.force());
        velocity.setY(velocity.getY() * value.verticalMultiplier());
        target.setVelocity(VelocityUtils.clamp(velocity, value.maxVelocity()));
        level.playSounds(owner);
    }

    private record PullSettings(double force, double maxVelocity, double verticalMultiplier, double maxDistance) {}
}
