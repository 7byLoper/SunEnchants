package ru.loper.sunenchants.enchants.combat;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
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
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.utils.CombatUtils;
import ru.loper.sunenchants.utils.DurabilityUtils;

@EnchantRegister(name = "oxidation")
public class OxidationEnchant extends SEnchant {
    private final Map<Integer, Integer> extraDamageByLevel = new HashMap<>();

    public OxidationEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        extraDamageByLevel.clear();
        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) return;

        for (String key : levels.getKeys(false)) {
            try {
                extraDamageByLevel.put(Integer.parseInt(key), Math.max(0, levels.getInt(key + ".extra_damage", 1)));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || event.getFinalDamage() <= 0.0D) return;

        ItemStack weapon = CombatUtils.resolveWeapon(event.getDamager());
        int enchantLevel = getAppliedLevel(weapon);
        Integer extraDamage = extraDamageByLevel.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (extraDamage == null || extraDamage <= 0 || level == null || !level.hasWorkChance()) return;

        ItemStack[] armor = victim.getInventory().getArmorContents();
        boolean changed = false;
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (item == null || item.getType().isAir() || !DurabilityUtils.isArmor(item)) continue;

            DurabilityUtils.damage(item, extraDamage);
            if (item.getAmount() <= 0) armor[i] = null;
            changed = true;
        }

        if (changed) victim.getInventory().setArmorContents(armor);
        Player attacker = CombatUtils.resolvePlayer(event.getDamager());
        if (attacker != null) level.playSounds(attacker);
    }
}
