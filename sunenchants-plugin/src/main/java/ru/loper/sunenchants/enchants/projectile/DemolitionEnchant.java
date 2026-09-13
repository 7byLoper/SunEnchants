package ru.loper.sunenchants.enchants.projectile;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.sound.SoundPlayer;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.utils.VelocityUtils;

@EnchantRegister(name = "demolition")
public class DemolitionEnchant extends SEnchant {
    private final Map<Integer, ExplosionSettings> settings = new HashMap<>();
    private final NamespacedKey handledKey;

    public DemolitionEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
        handledKey = new NamespacedKey(SunEnchants.getInstance(), "demolition_handled");
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        settings.clear();
        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) {
            return;
        }

        for (String key : levels.getKeys(false)) {
            ConfigurationSection level = levels.getConfigurationSection(key);
            if (level == null) {
                continue;
            }

            try {
                settings.put(
                        Integer.parseInt(key),
                        new ExplosionSettings(
                                Math.max(0.1D, level.getDouble("power", 1.0D)),
                                Math.max(0.1D, level.getDouble("radius", 4.0D)),
                                Math.max(0.0D, level.getDouble("damage", 6.0D)),
                                Math.max(0.0D, level.getDouble("knockback", 1.0D)),
                                level.getBoolean("damage_owner", false),
                                Math.max(1, level.getInt("particle_count", 1)),
                                Math.max(0.0F, (float) level.getDouble("sound_volume", 1.0D)),
                                Math.max(0.0F, (float) level.getDouble("sound_pitch", 1.0D)),
                                Math.max(0.0D, level.getDouble("minimum_vertical_knockback", 0.0D)),
                                Math.max(0.1D, level.getDouble("max_knockback_velocity", 4.0D))));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getProjectile() instanceof Projectile projectile)) {
            return;
        }

        ItemStack bow = event.getBow();
        int level = getAppliedLevel(bow);
        if (level > 0) {
            projectile.getPersistentDataContainer().set(getEnchantKey(), PersistentDataType.INTEGER, level);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile.getPersistentDataContainer().has(handledKey, PersistentDataType.BYTE)) {
            return;
        }

        projectile.getPersistentDataContainer().set(handledKey, PersistentDataType.BYTE, (byte) 1);

        int enchantLevel = resolveLevel(projectile);
        ExplosionSettings value = settings.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (value == null || level == null || !level.hasWorkChance()) {
            return;
        }

        Player owner = projectile.getShooter() instanceof Player player ? player : null;
        explode(projectile.getLocation(), owner, value);
        if (owner != null) {
            level.playSounds(owner);
        }
    }

    private int resolveLevel(Projectile projectile) {
        if (projectile instanceof Trident trident) {
            return getAppliedLevel(trident.getItemStack());
        }

        Integer level = projectile.getPersistentDataContainer().get(getEnchantKey(), PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }

    private void explode(Location location, Player owner, ExplosionSettings settings) {
        if (location.getWorld() == null) {
            return;
        }

        location.getWorld().spawnParticle(Particle.EXPLOSION, location, settings.particleCount());
        SoundPlayer.play(location, Sound.ENTITY_GENERIC_EXPLODE, settings.soundVolume(), settings.soundPitch());

        double radius = settings.radius();
        for (Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || (!settings.damageOwner() && living.equals(owner))) {
                continue;
            }

            double distance = living.getLocation().distance(location);
            if (distance > radius) {
                continue;
            }

            double falloff = Math.max(0.0D, 1.0D - distance / radius);
            double damage = settings.damage() * falloff * settings.power();
            if (damage > 0.0D) {
                living.damage(damage);
            }

            applyKnockback(location, living, settings, falloff);
        }
    }

    private void applyKnockback(Location origin, LivingEntity living, ExplosionSettings settings, double falloff) {
        Vector knockback = living.getLocation().toVector().subtract(origin.toVector());
        if (knockback.lengthSquared() <= 0.0001D) {
            return;
        }

        knockback.normalize().multiply(settings.knockback() * falloff * settings.power());
        knockback.setY(Math.max(settings.minimumVerticalKnockback(), knockback.getY()));
        living.setVelocity(VelocityUtils.clamp(living.getVelocity().add(knockback), settings.maxKnockbackVelocity()));
    }

    private record ExplosionSettings(
            double power,
            double radius,
            double damage,
            double knockback,
            boolean damageOwner,
            int particleCount,
            float soundVolume,
            float soundPitch,
            double minimumVerticalKnockback,
            double maxKnockbackVelocity) {}
}
