package ru.loper.sunenchants.enchants.tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
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

@EnchantRegister(name = "bulldozer_log")
public class BulldozerLogEnchant extends SEnchant {
    private final Map<Integer, LumberjackLevel> values = new HashMap<>();

    private BulldozerUtils blockBreakUtils;
    private MaterialFilter materialFilter;
    private boolean allowCreative;
    private boolean allowAdventure;

    public BulldozerLogEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        EnchantsManager enchantsManager = SunEnchants.getInstance().getEnchantsManager();
        blockBreakUtils = new BulldozerUtils(enchantsManager);
        materialFilter = new MaterialFilter(section.getConfigurationSection("block_filter"));
        allowCreative = section.getBoolean("allow_creative", false);
        allowAdventure = section.getBoolean("allow_adventure", false);

        values.clear();
        ConfigurationSection levels = section.getConfigurationSection("levels");
        if (levels == null) {
            return;
        }

        for (String key : levels.getKeys(false)) {
            ConfigurationSection level = levels.getConfigurationSection(key);
            if (level == null) {
                continue;
            }

            try {
                values.put(
                        Integer.parseInt(key),
                        new LumberjackLevel(
                                Math.max(0, level.getInt("radius", 1)),
                                Math.max(0, level.getInt("max_blocks", 8)),
                                Math.max(0, level.getInt("durability_per_block", 1))));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (BulldozerUtils.isInternalBreak(event.getBlock())) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isApplied(tool) || !isGameModeAllowed(player.getGameMode())) {
            return;
        }

        int enchantLevel = getAppliedLevel(tool);
        AbstractLevel level = getLevel(enchantLevel);
        LumberjackLevel settings = values.get(enchantLevel);
        Block center = event.getBlock();
        if (level == null
                || settings == null
                || !level.hasWorkChance()
                || !materialFilter.allows(center.getType(), tool)) {
            return;
        }

        BlockFace face = player.getFacing();
        boolean useXZ = face == BlockFace.UP
                || face == BlockFace.DOWN
                || Math.abs(player.getLocation().getPitch()) > 45.0F;
        boolean useXY = face == BlockFace.NORTH || face == BlockFace.SOUTH;
        boolean useYZ = face == BlockFace.EAST || face == BlockFace.WEST;

        Collection<ItemStack> collected = new ArrayList<>();
        int broken = blockBreakUtils.breakBlocksInPlane(
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
        if (broken <= 0) {
            return;
        }

        level.playSounds(player);
        blockBreakUtils.collectItems(player, tool, collected);
    }

    private boolean isGameModeAllowed(GameMode gameMode) {
        return switch (gameMode) {
            case SURVIVAL -> true;
            case CREATIVE -> allowCreative;
            case ADVENTURE -> allowAdventure;
            default -> false;
        };
    }

    private record LumberjackLevel(int radius, int maxBlocks, int durabilityPerBlock) {}
}
