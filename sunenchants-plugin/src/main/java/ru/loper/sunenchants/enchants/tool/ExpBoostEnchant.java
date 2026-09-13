package ru.loper.sunenchants.enchants.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.utils.CombatUtils;
import ru.loper.sunenchants.utils.HeldItemCache;
import ru.loper.sunenchants.utils.MaterialPatterns;

@EnchantRegister(name = "exp_boost")
public class ExpBoostEnchant extends SEnchant {
    private final Map<Integer, ExperienceLevel> values = new HashMap<>();
    private List<String> miningBlocks = List.of("*_ORE", "ANCIENT_DEBRIS");

    public ExpBoostEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        List<String> configuredMiningBlocks = section.getStringList("mining_blocks");
        miningBlocks = MaterialPatterns.normalize(
                configuredMiningBlocks.isEmpty() ? List.of("*_ORE", "ANCIENT_DEBRIS") : configuredMiningBlocks);

        values.clear();
        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) {
            return;
        }

        for (String key : levels.getKeys(false)) {
            ConfigurationSection level = levels.getConfigurationSection(key);
            if (level == null) continue;

            int number;
            try {
                number = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }

            double fallback = level.getDouble("value", 1.0D);
            values.put(
                    number,
                    new ExperienceLevel(
                            level.getDouble("mining_multiplier", fallback),
                            level.getDouble("kill_multiplier", fallback)));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Material blockType = event.getBlock().getType();
        if (event.getExpToDrop() <= 0 || !MaterialPatterns.matchesAny(blockType, miningBlocks)) {
            return;
        }

        ItemStack tool = HeldItemCache.mainHand(event.getPlayer());
        int enchantLevel = getAppliedLevel(tool);
        ExperienceLevel value = values.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (value == null || level == null || !level.hasWorkChance()) {
            return;
        }

        event.setExpToDrop(scale(event.getExpToDrop(), value.miningMultiplier()));
        level.playSounds(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Projectile projectile)) {
            return;
        }

        ItemStack bow = event.getBow();
        int level = getAppliedLevel(bow);
        if (level > 0) {
            projectile.getPersistentDataContainer().set(getEnchantKey(), PersistentDataType.INTEGER, level);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player
                || event.getDroppedExp() <= 0
                || event.getEntity().getKiller() == null) {
            return;
        }

        int enchantLevel = resolveKillLevel(event);
        ExperienceLevel value = values.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (value == null || level == null || !level.hasWorkChance()) {
            return;
        }

        event.setDroppedExp(scale(event.getDroppedExp(), value.killMultiplier()));
        level.playSounds(event.getEntity().getKiller());
    }

    private int resolveKillLevel(EntityDeathEvent event) {
        if (!(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent)) {
            return 0;
        }

        Entity damager = damageEvent.getDamager();
        if (damager instanceof Projectile projectile) {
            Integer taggedLevel =
                    projectile.getPersistentDataContainer().get(getEnchantKey(), PersistentDataType.INTEGER);
            if (taggedLevel != null) {
                return taggedLevel;
            }
        }

        ItemStack weapon = CombatUtils.resolveWeapon(damager);
        return getAppliedLevel(weapon);
    }

    private int scale(int experience, double multiplier) {
        return Math.max(0, (int) Math.round(experience * Math.max(0.0D, multiplier)));
    }

    private record ExperienceLevel(double miningMultiplier, double killMultiplier) {}
}
