package ru.loper.sunenchants.enchants.weapon;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.IntLevel;
import ru.loper.sunmobmoney.api.events.MobMoneyEvent;

@EnchantRegister(name = "rich", level = EnchantLevelType.INTEGER)
public class RichEnchant extends SEnchant {

    public RichEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {}

    @EventHandler
    public void onMobMoney(MobMoneyEvent event) {
        ItemStack itemStack = event.getItem();
        if (!itemStack.hasItemMeta() || !isApplied(itemStack)) {
            return;
        }

        IntLevel level = getLevel(itemStack);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        level.playSounds(event.getPlayer());
        double addReward = event.getReward() / 100 * level.getValue();
        event.setReward(event.getReward() + addReward);
    }
}
