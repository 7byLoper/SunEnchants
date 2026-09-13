package ru.loper.sunenchants.enchants.armor;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.IntLevel;
import ru.loper.sunenchants.api.utils.RandomUtils;

@EnchantRegister(name = "impenetrable", level = EnchantLevelType.INTEGER)
public class ImpenetrableEnchant extends SEnchant {
    private final Map<UUID, ImpenetrableData> activePlayers = new HashMap<>();

    public ImpenetrableEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {}

    @Override
    protected void onEnabledStateChanged(boolean enabled) {
        activePlayers.clear();
        if (enabled) {
            Bukkit.getOnlinePlayers().forEach(this::checkPlayerEquipment);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkPlayerEquipment(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        cleanupPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        cleanupPlayer(event.getPlayer());
        checkPlayerEquipment(player);
    }

    private void cleanupPlayer(Player event) {
        UUID uuid = event.getUniqueId();
        activePlayers.remove(uuid);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!isEnabled() || !(event.getEntity() instanceof Player player)) return;

        ImpenetrableData data = activePlayers.get(player.getUniqueId());
        if (data == null) return;

        if (RandomUtils.getPercent() < data.getChance()) {
            data.getLevel().playSounds(player);
            double damageReduction = event.getDamage() * (data.getValue() / 100.0);
            event.setDamage(Math.max(0, event.getDamage() - damageReduction));
        }
    }

    private void checkPlayerEquipment(Player player) {
        activePlayers.remove(player.getUniqueId());

        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (ItemStack item : armorContents) {
            if (item != null && isApplied(item)) {
                IntLevel level = getLevel(item);
                if (level != null) {
                    ImpenetrableData data =
                            activePlayers.computeIfAbsent(player.getUniqueId(), k -> new ImpenetrableData(level));
                    data.addChance(level.getWorkChance());
                    data.addValue(level.getValue());
                }
            }
        }
    }

    private static class ImpenetrableData {
        @Getter
        private final IntLevel level;

        private int chance;

        @Getter
        private int value;

        public ImpenetrableData(IntLevel level) {
            this.level = level;
        }

        public void addChance(int chance) {
            this.chance += chance;
        }

        public void addValue(int value) {
            this.value += value;
        }

        public int getChance() {
            return Math.min(chance, 100);
        }
    }
}
