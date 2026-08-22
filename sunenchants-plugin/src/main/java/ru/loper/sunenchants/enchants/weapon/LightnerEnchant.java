package ru.loper.sunenchants.enchants.weapon;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.IntLevel;

@EnchantRegister(name = "lightner", level = EnchantLevelType.INTEGER)
public class LightnerEnchant extends SEnchant {
    private int durationTicks = 200;
    private int amplifier = 1;

    public LightnerEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        this.durationTicks = section.getInt("duration-ticks", 200);
        this.amplifier = section.getInt("amplifier", 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player victim)) {
            return;
        }

        ItemStack tool = damager.getInventory().getItemInMainHand();
        if (!tool.hasItemMeta() || !isApplied(tool)) {
            return;
        }

        IntLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, durationTicks, amplifier));
        level.playSounds(damager);
    }
}
