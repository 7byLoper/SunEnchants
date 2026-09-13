package ru.loper.sunenchants.enchants.interact;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.IntLevel;
import ru.loper.sunenchants.utils.BulldozerUtils;
import ru.loper.sunenchants.utils.HeldItemCache;
import ru.loper.sunenchants.utils.MessageUtils;

@EnchantRegister(name = "indestructible", level = EnchantLevelType.INTEGER)
public class IndestructibleEnchant extends SEnchant {
    private String message;
    private String title;

    public IndestructibleEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        this.message = section.getString("warning.message", "");
        this.title = section.getString("warning.title", "");
    }

    public void sendAlert(Player player) {
        if (!message.isEmpty()) {
            MessageUtils.send(player, message);
        }

        if (!title.isEmpty()) {
            String[] parts = title.split(";", 2);
            if (parts.length == 2) {
                player.sendTitle(
                        MessageUtils.renderLegacy(player, parts[0]),
                        MessageUtils.renderLegacy(player, parts[1]),
                        20,
                        70,
                        10);
            } else {
                player.sendTitle(MessageUtils.renderLegacy(player, parts[0]), "", 20, 70, 10);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageItem(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        onUse(event, item, player, event.getDamage());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (BulldozerUtils.isProtectionCheck(event.getBlock())) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack tool = HeldItemCache.mainHand(player);
        onUse(event, tool, player, 0);
    }

    private void onUse(Cancellable event, ItemStack tool, Player player, int incomingDamage) {
        if (!isApplied(tool)) {
            return;
        }

        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }

        IntLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        int remainingUses = tool.getType().getMaxDurability() - damageable.getDamage() - incomingDamage;

        if (remainingUses > level.getValue()) {
            return;
        }

        sendAlert(player);
        event.setCancelled(true);
    }
}
