package ru.loper.sunenchants.enchants.weapon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.economy.EconomyEditor;
import ru.loper.suncore.api.economy.EconomyServices;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.DoubleLevel;
import ru.loper.sunenchants.utils.HeldItemCache;
import ru.loper.sunenchants.utils.MessageUtils;

@EnchantRegister(name = "stealer", level = EnchantLevelType.DOUBLE)
public class StealerEnchant extends SEnchant {
    private final Map<UUID, UUID> damagedPlayers = new HashMap<>();
    private EconomyEditor economy;

    private double maxMoney = 45000.0;

    private String killerMessage = "";
    private String victimMessage = "";
    private String vaultMissingMessage = "";

    public StealerEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        this.maxMoney = section.getDouble("max-money", 45000.0);
        this.killerMessage = section.getString("messages.killer", "");
        this.victimMessage = section.getString("messages.victim", "");
        this.vaultMissingMessage = section.getString("messages.vault-missing", "");
    }

    @Override
    protected void onEnabledStateChanged(boolean enabled) {
        if (!enabled) {
            damagedPlayers.clear();
        }
    }

    private EconomyEditor getEconomy() {
        if (economy == null) {
            economy = EconomyServices.vaultEconomy();
        }

        return economy;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player victim)) {
            return;
        }

        ItemStack tool = HeldItemCache.mainHand(damager);
        if (!isApplied(tool)) {
            return;
        }

        DoubleLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        damagedPlayers.put(victim.getUniqueId(), damager.getUniqueId());
        level.playSounds(damager);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        UUID damagerId = damagedPlayers.remove(victim.getUniqueId());
        Player killer = victim.getKiller();
        if (killer == null || damagerId == null || !damagerId.equals(killer.getUniqueId())) {
            return;
        }

        ItemStack tool = HeldItemCache.mainHand(killer);
        if (!isApplied(tool)) {
            return;
        }

        DoubleLevel level = getLevel(tool);
        if (level == null) {
            return;
        }

        var economy = getEconomy();
        if (economy == null) {
            if (!vaultMissingMessage.isEmpty()) {
                MessageUtils.send(killer, vaultMissingMessage);
            }
            return;
        }

        double actualPercent = level.getValue();
        double money = economy.getBalance(victim) * (actualPercent / 100.0);
        double finalMoney = maxMoney > 0 ? Math.min(money, maxMoney) : money;
        if (finalMoney <= 0) {
            return;
        }

        economy.withdrawBalance(victim, finalMoney);
        economy.investBalance(killer, finalMoney);

        sendMessage(killer, killerMessage, killer, victim, finalMoney, actualPercent);
        sendMessage(victim, victimMessage, killer, victim, finalMoney, actualPercent);
    }

    private void sendMessage(
            Player receiver, String message, Player killer, Player victim, double money, double actualPercent) {
        if (message == null || message.isEmpty()) {
            return;
        }

        MessageUtils.send(
                receiver,
                message,
                Map.of(
                        "killer",
                        killer.getName(),
                        "victim",
                        victim.getName(),
                        "money",
                        (int) money,
                        "percent",
                        actualPercent));
    }
}
