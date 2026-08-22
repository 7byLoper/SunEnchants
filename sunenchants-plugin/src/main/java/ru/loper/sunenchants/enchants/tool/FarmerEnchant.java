package ru.loper.sunenchants.enchants.tool;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.DoubleLevel;

@EnchantRegister(name = "farmer", level = EnchantLevelType.DOUBLE)
public class FarmerEnchant extends SEnchant {

    public FarmerEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {}

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || event.getEntity() instanceof Player) {
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

        event.setDroppedExp((int) (event.getDroppedExp() * level.getValue()));
        level.playSounds(killer);
    }
}
