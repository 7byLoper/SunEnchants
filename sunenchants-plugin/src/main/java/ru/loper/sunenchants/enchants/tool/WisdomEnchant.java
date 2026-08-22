package ru.loper.sunenchants.enchants.tool;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.DoubleLevel;

@EnchantRegister(name = "wisdom", level = EnchantLevelType.DOUBLE)
public class WisdomEnchant extends SEnchant {
    private boolean ignorePlayerDeaths = true;

    public WisdomEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        this.ignorePlayerDeaths = section.getBoolean("ignore-player-deaths", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (ignorePlayerDeaths && event.getEntity() instanceof Player) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        ItemStack tool = killer.getInventory().getItemInMainHand();
        if (!tool.hasItemMeta() || !isApplied(tool)) {
            return;
        }

        DoubleLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        double actualMultiplier = Math.max(1, level.getValue());
        event.setDroppedExp((int) Math.round(event.getDroppedExp() * actualMultiplier));
        level.playSounds(killer);
    }
}
