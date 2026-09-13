package ru.loper.sunenchants.enchants.trident;

import java.util.Comparator;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.utilities.VersionHelper;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.IntLevel;

@EnchantRegister(name = "auto_aim", level = EnchantLevelType.INTEGER)
public class AutoAimEnchant extends SEnchant {

    public AutoAimEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {}

    @EventHandler
    public void onTridentThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) {
            return;
        }

        if (!(trident.getShooter() instanceof Player player)) {
            return;
        }

        ItemStack tridentItem = getTridentItem(trident);

        if (!isApplied(tridentItem)) {
            return;
        }

        IntLevel level = getLevel(tridentItem);
        if (level == null) {
            return;
        }

        startCorrect(trident, player, level.getValue());
    }

    private void startCorrect(Trident trident, Player player, int maxDegree) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isEnabled() || trident.isDead() || !trident.isValid() || trident.isOnGround()) {
                    this.cancel();
                    return;
                }

                LivingEntity target = findNearestTarget(player, trident);
                if (target == null) {
                    return;
                }

                correctTrajectory(trident, target, maxDegree);
            }
        }.runTaskTimer(SunEnchants.getInstance(), 1L, 1L);
    }

    private LivingEntity findNearestTarget(Player shooter, Trident trident) {
        List<Entity> nearbyEntities = trident.getNearbyEntities(20, 10, 20);

        return nearbyEntities.stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .filter(entity -> !entity.equals(shooter))
                .filter(entity -> !entity.equals(trident))
                .filter(entity -> entity.getLocation().distance(trident.getLocation()) <= 20)
                .min(Comparator.comparingDouble(entity -> entity.getLocation().distance(trident.getLocation())))
                .orElse(null);
    }

    private void correctTrajectory(Trident trident, LivingEntity target, int maxDegree) {
        Vector currentVelocity = trident.getVelocity();
        double currentSpeed = currentVelocity.length();

        Vector toTarget = target.getEyeLocation()
                .toVector()
                .subtract(trident.getLocation().toVector());
        Vector idealDirection = toTarget.normalize();

        Vector currentDirection = currentVelocity.normalize();

        double angle = Math.toDegrees(currentDirection.angle(idealDirection));

        if (angle <= 5 || angle > maxDegree) {
            return;
        }

        double correctionFactor = 0.2;
        Vector newDirection = currentDirection
                .multiply(1 - correctionFactor)
                .add(idealDirection.multiply(correctionFactor))
                .normalize();

        trident.setVelocity(newDirection.multiply(currentSpeed));
    }

    private @NotNull ItemStack getTridentItem(Trident trident) {
        return VersionHelper.CURRENT_VERSION < 1204 ? trident.getItem() : trident.getItemStack();
    }
}
