package ru.loper.sunenchants.enchants.tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.manager.EnchantsManager;
import ru.loper.sunenchants.utils.BulldozerUtils;
import ru.loper.sunenchants.utils.MaterialFilter;
import ru.loper.sunenchants.utils.MessageUtils;

@EnchantRegister(name = "bulldozer")
public class BulldozerEnchant extends SEnchant {
    private final Set<UUID> disabledPlayers = new HashSet<>();
    private final Map<Integer, BulldozerLevel> values = new HashMap<>();

    private BulldozerUtils bulldozerUtils;
    private EnchantsManager enchantsManager;
    private MaterialFilter materialFilter;
    private String onMessage;
    private String offMessage;
    private boolean allowCreative;
    private boolean allowAdventure;

    public BulldozerEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        enchantsManager = SunEnchants.getInstance().getEnchantsManager();
        bulldozerUtils = new BulldozerUtils(enchantsManager);
        materialFilter = new MaterialFilter(section.getConfigurationSection("block_filter"));

        onMessage = section.getString("on_message", "<green>Бульдозер включен");
        offMessage = section.getString("off_message", "<red>Бульдозер выключен");
        allowCreative = section.getBoolean("allow_creative", false);
        allowAdventure = section.getBoolean("allow_adventure", false);

        values.clear();
        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) return;

        for (String key : levels.getKeys(false)) {
            ConfigurationSection level = levels.getConfigurationSection(key);
            if (level == null) continue;

            try {
                int number = Integer.parseInt(key);
                values.put(
                        number,
                        new BulldozerLevel(
                                Math.max(0, level.getInt("radius", 1)),
                                Math.max(0, level.getInt("max_blocks", 26)),
                                Math.max(0, level.getInt("durability_per_block", 1)),
                                parseShape(level.getString("shape", "CUBE"))));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;

        ItemStack item = event.getItem();
        if (item == null || !isApplied(item)) return;

        toggleBulldozerMode(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (BulldozerUtils.isInternalBreak(event.getBlock())) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isApplied(tool)
                || enchantsManager.hasEnchant("bulldozer_log", tool)
                || enchantsManager.hasEnchant("mega_bulldozer", tool)
                || disabledPlayers.contains(player.getUniqueId())
                || !isGameModeAllowed(player.getGameMode())) {
            return;
        }

        int enchantLevel = getAppliedLevel(tool);
        AbstractLevel level = getLevel(enchantLevel);
        BulldozerLevel settings = values.get(enchantLevel);
        Block center = event.getBlock();
        if (level == null
                || settings == null
                || !level.hasWorkChance()
                || !materialFilter.allows(center.getType(), tool)) {
            return;
        }

        Collection<ItemStack> collected = new ArrayList<>();
        int broken;
        if (settings.shape() == Shape.PLANE) {
            BlockFace face = player.getFacing();
            boolean useXZ = face == BlockFace.UP
                    || face == BlockFace.DOWN
                    || Math.abs(player.getLocation().getPitch()) > 45.0F;
            boolean useXY = face == BlockFace.NORTH || face == BlockFace.SOUTH;
            boolean useYZ = face == BlockFace.EAST || face == BlockFace.WEST;
            broken = bulldozerUtils.breakBlocksInPlane(
                    center,
                    tool,
                    player,
                    useXZ,
                    useXY,
                    useYZ,
                    collected,
                    null,
                    materialFilter,
                    settings.radius(),
                    settings.maxBlocks(),
                    settings.durabilityPerBlock());
        } else {
            broken = bulldozerUtils.breakBlocksInCube(
                    center,
                    tool,
                    player,
                    settings.radius(),
                    collected,
                    null,
                    materialFilter,
                    settings.maxBlocks(),
                    settings.durabilityPerBlock());
        }

        if (broken <= 0) return;
        level.playSounds(player);
        bulldozerUtils.collectItems(player, tool, collected);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        disabledPlayers.remove(event.getPlayer().getUniqueId());
    }

    public void toggleBulldozerMode(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabledPlayers.remove(uuid)) {
            MessageUtils.actionBar(player, onMessage);
            return;
        }

        disabledPlayers.add(uuid);
        MessageUtils.actionBar(player, offMessage);
    }

    public boolean hasBulldozer(Player player) {
        return disabledPlayers.contains(player.getUniqueId());
    }

    private boolean isGameModeAllowed(GameMode gameMode) {
        return switch (gameMode) {
            case SURVIVAL -> true;
            case CREATIVE -> allowCreative;
            case ADVENTURE -> allowAdventure;
            default -> false;
        };
    }

    private Shape parseShape(String value) {
        try {
            return Shape.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Shape.CUBE;
        }
    }

    private enum Shape {
        PLANE,
        CUBE
    }

    private record BulldozerLevel(int radius, int maxBlocks, int durabilityPerBlock, Shape shape) {}
}
