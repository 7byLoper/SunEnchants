package ru.loper.sunenchants.utils;

import java.util.concurrent.ThreadLocalRandom;
import lombok.experimental.UtilityClass;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import ru.loper.sunenchants.api.utils.EnchantUtils;

@UtilityClass
public class DurabilityUtils {

    public static boolean damage(ItemStack item, int attempts) {
        if (item == null || item.getType().isAir() || attempts <= 0) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable) || meta.isUnbreakable()) {
            return false;
        }

        int unbreaking = item.getEnchantmentLevel(EnchantUtils.UNBREAKING);
        int damage = 0;
        for (int i = 0; i < attempts; i++) {
            if (shouldDamage(item, unbreaking)) {
                damage++;
            }
        }

        if (damage <= 0) {
            return false;
        }

        int newDamage = damageable.getDamage() + damage;
        if (newDamage >= item.getType().getMaxDurability()) {
            item.setAmount(0);
            return true;
        }

        damageable.setDamage(newDamage);
        item.setItemMeta(meta);
        return false;
    }

    public static int getRemaining(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getType().getMaxDurability() <= 0) {
            return Integer.MAX_VALUE;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable) || meta.isUnbreakable()) {
            return Integer.MAX_VALUE;
        }

        return Math.max(0, item.getType().getMaxDurability() - damageable.getDamage());
    }

    private static boolean shouldDamage(ItemStack item, int unbreakingLevel) {
        if (unbreakingLevel <= 0) {
            return true;
        }

        if (isArmor(item)) {
            double damageChance = 0.6D + 0.4D / (unbreakingLevel + 1.0D);
            return ThreadLocalRandom.current().nextDouble() < damageChance;
        }

        return ThreadLocalRandom.current().nextInt(unbreakingLevel + 1) == 0;
    }

    public static boolean isArmor(ItemStack item) {
        String name = item.getType().name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS");
    }
}
