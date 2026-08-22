package ru.loper.sunenchants.enchants.armor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.utils.BulldozerUtils;
import ru.loper.sunenchants.utils.DurabilityUtils;

@EnchantRegister(name = "lava_move")
public class LavaMoveEnchant extends SEnchant {
    private final Map<Integer, LavaSettings> settings = new HashMap<>();
    private final Map<UUID, Long> lastProcess = new ConcurrentHashMap<>();

    private boolean restoreEnabled;
    private long restoreAfterTicks;

    public LavaMoveEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        restoreEnabled = section.getBoolean("restore.enabled", false);
        restoreAfterTicks = Math.max(1L, section.getLong("restore.after_ticks", 100L));

        settings.clear();
        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) return;

        for (String key : levels.getKeys(false)) {
            try {
                int number = Integer.parseInt(key);
                settings.put(
                        number,
                        new LavaSettings(
                                Math.max(0, levels.getInt(key + ".radius", 1)),
                                Math.max(0, levels.getInt(key + ".max_lava_level", 0)),
                                Math.max(0L, levels.getLong(key + ".cooldown_ms", 150L)),
                                Math.max(0, levels.getInt(key + ".durability_per_block", 0))));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();
        int enchantLevel = getAppliedLevel(boots);
        LavaSettings value = settings.get(enchantLevel);
        AbstractLevel level = getLevel(enchantLevel);
        if (value == null || level == null || !isCooldownReady(player, value.cooldownMs()) || !level.hasWorkChance())
            return;

        int changed = freezeLava(player, event.getTo().getBlock().getRelative(BlockFace.DOWN), value);
        if (changed <= 0) return;

        lastProcess.put(player.getUniqueId(), System.currentTimeMillis());
        if (value.durabilityPerBlock() > 0 && boots != null) {
            DurabilityUtils.damage(boots, changed * value.durabilityPerBlock());
            if (boots.getAmount() <= 0) player.getInventory().setBoots(null);
        }
        level.playSounds(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastProcess.remove(event.getPlayer().getUniqueId());
    }

    private int freezeLava(Player player, Block center, LavaSettings value) {
        int changed = 0;
        for (int x = -value.radius(); x <= value.radius(); x++) {
            for (int z = -value.radius(); z <= value.radius(); z++) {
                Block lava = center.getRelative(x, 0, z);
                if (lava.getType() != Material.LAVA
                        || !lava.getRelative(BlockFace.UP).isEmpty()) continue;
                if (!(lava.getBlockData() instanceof Levelled levelled) || levelled.getLevel() > value.maxLavaLevel())
                    continue;
                if (!BulldozerUtils.canModify(lava, player)) continue;

                BlockData original = lava.getBlockData().clone();
                lava.setType(Material.OBSIDIAN, false);
                changed++;
                if (restoreEnabled) scheduleRestore(lava, original);
            }
        }
        return changed;
    }

    private void scheduleRestore(Block block, BlockData original) {
        SunEnchants.getInstance()
                .getServer()
                .getScheduler()
                .runTaskLater(
                        SunEnchants.getInstance(),
                        () -> {
                            if (block.getType() == Material.OBSIDIAN) {
                                block.setBlockData(original, false);
                            }
                        },
                        restoreAfterTicks);
    }

    private boolean isCooldownReady(Player player, long cooldownMs) {
        if (cooldownMs <= 0L) return true;
        return System.currentTimeMillis() - lastProcess.getOrDefault(player.getUniqueId(), 0L) >= cooldownMs;
    }

    private record LavaSettings(int radius, int maxLavaLevel, long cooldownMs, int durabilityPerBlock) {}
}
