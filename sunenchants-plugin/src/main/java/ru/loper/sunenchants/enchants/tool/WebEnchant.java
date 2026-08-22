package ru.loper.sunenchants.enchants.tool;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
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
import ru.loper.sunenchants.utils.BulldozerUtils;
import ru.loper.sunenchants.utils.DurabilityUtils;
import ru.loper.sunenchants.utils.MaterialFilter;

@EnchantRegister(name = "web")
public class WebEnchant extends SEnchant {
    private BulldozerUtils miningUtils;
    private MaterialFilter materialFilter;
    private int maxBlocks;
    private int durabilityPerBlock;
    private NeighborMode neighborMode;
    private boolean allowCreative;
    private boolean allowAdventure;

    public WebEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        miningUtils = new BulldozerUtils(SunEnchants.getInstance().getEnchantsManager());
        materialFilter = new MaterialFilter(section.getConfigurationSection("block_filter"));
        maxBlocks = Math.max(1, section.getInt("max_blocks", 32));
        durabilityPerBlock = Math.max(0, section.getInt("durability_per_block", 1));
        allowCreative = section.getBoolean("allow_creative", false);
        allowAdventure = section.getBoolean("allow_adventure", false);

        try {
            neighborMode = NeighborMode.valueOf(
                    section.getString("neighbor_mode", "FACES").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            neighborMode = NeighborMode.FACES;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (BulldozerUtils.isInternalBreak(event.getBlock())) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Block start = event.getBlock();
        Material veinType = start.getType();

        if (!isApplied(tool) || !isGameModeAllowed(player.getGameMode()) || !materialFilter.allows(veinType, tool)) {
            return;
        }

        AbstractLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) return;

        Collection<ItemStack> collected = new ArrayList<>();
        int broken = breakVein(start, veinType, tool, player, collected);
        if (broken <= 0) return;

        level.playSounds(player);
        miningUtils.collectItems(player, tool, collected);
    }

    private int breakVein(
            Block start, Material veinType, ItemStack tool, Player player, Collection<ItemStack> collected) {
        int limit = Math.max(0, maxBlocks - 1);
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        int broken = 0;

        while (!queue.isEmpty() && broken < limit && tool.getAmount() > 0) {
            Block current = queue.poll();
            for (int[] offset : offsets()) {
                Block next = current.getRelative(offset[0], offset[1], offset[2]);
                if (!visited.add(next) || next.getType() != veinType || !materialFilter.allows(next.getType(), tool)) {
                    continue;
                }

                queue.add(next);
                BulldozerUtils.BreakResult result = miningUtils.breakBlock(tool, player, next, null);
                if (result == null) continue;

                miningUtils.processDrops(tool, player, next, result, collected);
                if (durabilityPerBlock > 0) {
                    DurabilityUtils.damage(tool, durabilityPerBlock);
                }
                broken++;
                if (broken >= limit) break;
            }
        }
        return broken;
    }

    private int[][] offsets() {
        if (neighborMode == NeighborMode.FACES) {
            return new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        }

        int[][] values = new int[26][3];
        int index = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    values[index++] = new int[] {x, y, z};
                }
            }
        }
        return values;
    }

    private boolean isGameModeAllowed(GameMode gameMode) {
        return switch (gameMode) {
            case SURVIVAL -> true;
            case CREATIVE -> allowCreative;
            case ADVENTURE -> allowAdventure;
            default -> false;
        };
    }

    private enum NeighborMode {
        FACES,
        ALL
    }
}
