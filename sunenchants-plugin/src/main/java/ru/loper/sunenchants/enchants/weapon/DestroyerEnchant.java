package ru.loper.sunenchants.enchants.weapon;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.IntLevel;
import ru.loper.sunenchants.api.utils.EnchantUtils;
import ru.loper.sunenchants.utils.HeldItemCache;

@EnchantRegister(name = "destroyer", level = EnchantLevelType.INTEGER)
public class DestroyerEnchant extends SEnchant {
    public DestroyerEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        if (damager.getAttackCooldown() < 0.95F) {
            return;
        }

        ItemStack weapon = HeldItemCache.mainHand(damager);
        if (!isApplied(weapon)) {
            return;
        }

        IntLevel level = getLevel(weapon);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        damageArmor(target, level.getValue());
    }

    private void damageArmor(Player player, int percent) {
        PlayerInventory inventory = player.getInventory();
        double damageMultiplier = 1 + (percent / 100.0);
        int baseDamage = ThreadLocalRandom.current().nextInt(5);

        for (ItemStack armor : inventory.getArmorContents()) {
            if (armor == null || armor.getType().isAir() || armor.getType().getMaxDurability() <= 0) {
                continue;
            }

            ItemMeta meta = armor.getItemMeta();
            if (!(meta instanceof Damageable damageable) || meta.isUnbreakable()) {
                continue;
            }

            int finalDamage = Math.max(1, (int) Math.round(baseDamage * damageMultiplier));
            int appliedDamage = calculateAppliedDamage(armor, finalDamage);

            if (appliedDamage <= 0) {
                continue;
            }

            int newDamage = damageable.getDamage() + appliedDamage;
            if (newDamage >= armor.getType().getMaxDurability()) {
                armor.setAmount(0);
                continue;
            }

            damageable.setDamage(newDamage);
            armor.setItemMeta(meta);
        }
    }

    private int calculateAppliedDamage(ItemStack armor, int attempts) {
        int unbreakingLevel = armor.getEnchantmentLevel(EnchantUtils.UNBREAKING);
        int ignoreChance = getSpecialUnbreakingIgnoreChance(unbreakingLevel);

        int appliedDamage = 0;
        for (int i = 0; i < attempts; i++) {
            if (!shouldIgnoreDurabilityLoss(ignoreChance)) {
                appliedDamage++;
            }
        }

        return appliedDamage;
    }

    private boolean shouldIgnoreDurabilityLoss(int ignoreChance) {
        return ThreadLocalRandom.current().nextInt(100) < ignoreChance;
    }

    private int getSpecialUnbreakingIgnoreChance(int unbreakingLevel) {
        return Math.max(0, Math.min(100, 47 + (unbreakingLevel * 10)));
    }
}
