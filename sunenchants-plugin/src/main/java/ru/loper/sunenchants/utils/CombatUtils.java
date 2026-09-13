package ru.loper.sunenchants.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class CombatUtils {

    @Nullable
    public static Player resolvePlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    @Nullable
    public static ItemStack resolveWeapon(Entity damager) {
        if (damager instanceof Player player) {
            return HeldItemCache.mainHand(player);
        }
        if (damager instanceof Trident trident) {
            return trident.getItemStack();
        }
        return null;
    }
}
