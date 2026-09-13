package ru.loper.sunenchants.enchants.tool;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.sound.SoundPlayer;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.utils.BulldozerUtils;
import ru.loper.sunenchants.utils.HeldItemCache;

@EnchantRegister(name = "pinger")
public class PingerEnchant extends SEnchant {
    private final Map<UUID, Long> lastWarning = new ConcurrentHashMap<>();

    private int threshold;
    private long cooldownMs;
    private Sound sound;
    private float volume;
    private float pitch;

    public PingerEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        threshold = Math.max(0, section.getInt("remaining_durability_threshold", 20));
        cooldownMs = Math.max(0L, section.getLong("cooldown_ms", 3000L));
        volume = (float) Math.max(0.0D, section.getDouble("volume", 1.0D));
        pitch = (float) Math.max(0.0D, section.getDouble("pitch", 1.0D));

        String soundName = section.getString("sound", "block.note_block.pling");
        sound = soundName == null ? null : Registry.SOUNDS.get(NamespacedKey.minecraft(soundName.toLowerCase()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        warnIfNeeded(event.getPlayer(), event.getItem(), event.getDamage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (BulldozerUtils.isProtectionCheck(event.getBlock())) {
            return;
        }
        warnIfNeeded(event.getPlayer(), HeldItemCache.mainHand(event.getPlayer()), 0);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastWarning.remove(event.getPlayer().getUniqueId());
    }

    private void warnIfNeeded(Player player, ItemStack item, int pendingDamage) {
        if (item == null
                || item.getAmount() <= 0
                || !isApplied(item)
                || !(item.getItemMeta() instanceof Damageable damageable)) {
            return;
        }

        AbstractLevel level = getLevel(item);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        int remaining = item.getType().getMaxDurability() - damageable.getDamage() - Math.max(0, pendingDamage);
        if (remaining > threshold) {
            return;
        }

        long now = System.currentTimeMillis();
        long last = lastWarning.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldownMs) {
            return;
        }

        lastWarning.put(player.getUniqueId(), now);
        if (sound != null) {
            SoundPlayer.play(player, sound, volume, pitch);
        }
        level.playSounds(player);
    }
}
