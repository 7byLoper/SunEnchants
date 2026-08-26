package ru.loper.sunenchants.enchants.projectile;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
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

@EnchantRegister(name = "scout")
public class ScoutEnchant extends SEnchant {
    private static final double GRAVITY_PER_TICK = 0.08D;

    private final Map<Integer, MovementSettings> settings = new HashMap<>();
    private final NamespacedKey handledKey;

    public ScoutEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
        handledKey = new NamespacedKey(SunEnchants.getInstance(), "scout_handled");
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
                        new MovementSettings(
                                Math.max(0.0D, levels.getDouble(key + ".force", 1.0D)),
                                Math.max(0.0D, levels.getDouble(key + ".max_velocity", 3.0D)),
                                Math.max(0.0D, levels.getDouble(key + ".max_distance", 48.0D))));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident) || !(trident.getShooter() instanceof Player player)) return;
        if (!player.isOnline() || player.getWorld() != trident.getWorld()) return;
        if (trident.getPersistentDataContainer().has(handledKey, PersistentDataType.BYTE)) return;

        int enchantLevel = getAppliedLevel(trident.getItemStack());
        MovementSettings value = settings.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (value == null || level == null) return;
        trident.getPersistentDataContainer().set(handledKey, PersistentDataType.BYTE, (byte) 1);
        if (!level.hasWorkChance()) return;

        Location from = player.getLocation();
        Location to = trident.getLocation();
        double distance = from.distance(to);
        if (distance <= 0.01D || distance > value.maxDistance()) return;

        Vector velocity = getVelocityToTarget(from, to, value.force());
        player.setVelocity(VelocityUtils.clamp(velocity, value.maxVelocity()));
        level.playSounds(player);
    }

    private Vector getVelocityToTarget(Location from, Location to, double horizontalSpeed) {
        double horizontalX = to.getX() - from.getX();
        double horizontalZ = to.getZ() - from.getZ();
        double horizontalDistance = Math.hypot(horizontalX, horizontalZ);
        if (horizontalDistance <= 0.01D || horizontalSpeed <= 0.0D) {
            return new Vector();
        }

        double flightTicks = horizontalDistance / horizontalSpeed;
        double verticalSpeed = (to.getY() - from.getY()) / flightTicks
                + GRAVITY_PER_TICK * (flightTicks - 1.0D) / 2.0D;

        return new Vector(
                horizontalX / horizontalDistance * horizontalSpeed,
                verticalSpeed,
                horizontalZ / horizontalDistance * horizontalSpeed);
    }

    private record MovementSettings(double force, double maxVelocity, double maxDistance) {}
}
