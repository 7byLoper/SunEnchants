package ru.loper.sunenchants.enchants.armor;

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;

@EnchantRegister(name = "jumper")
public class JumperEnchant extends SEnchant {
    private static final int LONG_DURATION = Integer.MAX_VALUE;

    private final Map<Integer, Integer> amplifierByLevel = new HashMap<>();
    private final Map<UUID, Integer> appliedAmplifiers = new HashMap<>();
    private final Set<UUID> internalPotionChanges = new HashSet<>();

    public JumperEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        amplifierByLevel.clear();
        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) {
            return;
        }

        for (String key : levels.getKeys(false)) {
            try {
                amplifierByLevel.put(Integer.parseInt(key), Math.max(0, levels.getInt(key + ".amplifier", 0)));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    protected void onEnabledStateChanged(boolean enabled) {
        if (enabled) {
            SunEnchants.getInstance().getServer().getOnlinePlayers().forEach(this::runNextTick);
            return;
        }

        SunEnchants.getInstance().getServer().getOnlinePlayers().forEach(this::removeOwnEffect);
        appliedAmplifiers.clear();
        internalPotionChanges.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        runNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        runNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEquipmentChange(EntityEquipmentChangedEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !event.getEquipmentChanges().containsKey(EquipmentSlot.FEET)) {
            return;
        }

        runNextTick(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getModifiedType() != PotionEffectType.JUMP_BOOST) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (internalPotionChanges.contains(uuid)) {
            return;
        }

        if (event.getAction() == EntityPotionEffectEvent.Action.CHANGED && !event.isOverride()) {
            return;
        }

        appliedAmplifiers.remove(uuid);
        runNextTick(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        removeOwnEffect(event.getPlayer());
    }

    private void runNextTick(Player player) {
        SunEnchants plugin = SunEnchants.getInstance();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                update(player);
            }
        });
    }

    private void update(Player player) {
        if (!isEnabled()) {
            removeOwnEffect(player);
            return;
        }

        UUID uuid = player.getUniqueId();
        ItemStack boots = player.getInventory().getBoots();
        Integer requiredAmplifier = amplifierByLevel.get(getAppliedLevel(boots));
        PotionEffect current = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
        Integer appliedAmplifier = appliedAmplifiers.get(uuid);

        if (requiredAmplifier == null) {
            if (appliedAmplifier != null && isOurEffect(current, appliedAmplifier)) {
                withInternalPotionChange(player, () -> player.removePotionEffect(PotionEffectType.JUMP_BOOST));
            }
            appliedAmplifiers.remove(uuid);
            return;
        }

        if (appliedAmplifier == null && isOurEffect(current, requiredAmplifier)) {
            appliedAmplifiers.put(uuid, requiredAmplifier);
            return;
        }

        if (appliedAmplifier != null && isOurEffect(current, appliedAmplifier)) {
            if (appliedAmplifier == requiredAmplifier) {
                return;
            }

            withInternalPotionChange(player, () -> player.removePotionEffect(PotionEffectType.JUMP_BOOST));
            current = null;
            appliedAmplifiers.remove(uuid);
        }

        if (current != null && current.getAmplifier() >= requiredAmplifier) {
            return;
        }

        PotionEffect effect =
                new PotionEffect(PotionEffectType.JUMP_BOOST, LONG_DURATION, requiredAmplifier, false, false, true);
        withInternalPotionChange(player, () -> player.addPotionEffect(effect, true));
        appliedAmplifiers.put(uuid, requiredAmplifier);
    }

    private void removeOwnEffect(Player player) {
        UUID uuid = player.getUniqueId();
        Integer appliedAmplifier = appliedAmplifiers.remove(uuid);
        PotionEffect current = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
        if (appliedAmplifier != null && isOurEffect(current, appliedAmplifier)) {
            withInternalPotionChange(player, () -> player.removePotionEffect(PotionEffectType.JUMP_BOOST));
        }
        internalPotionChanges.remove(uuid);
    }

    private boolean isOurEffect(PotionEffect effect, int amplifier) {
        return effect != null && effect.getAmplifier() == amplifier && effect.getDuration() > 20 * 60 * 60;
    }

    private void withInternalPotionChange(Player player, Runnable action) {
        UUID uuid = player.getUniqueId();
        internalPotionChanges.add(uuid);
        try {
            action.run();
        } finally {
            internalPotionChanges.remove(uuid);
        }
    }
}
