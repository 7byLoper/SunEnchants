package ru.loper.sunenchants.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.loper.sunenchants.api.utils.ToolUtils;
import ru.loper.sunenchants.enchants.misc.FilterEnchant;
import ru.loper.sunenchants.enchants.tool.MagnetEnchant;
import ru.loper.sunenchants.enchants.tool.MeltingEnchant;
import ru.loper.sunenchants.manager.EnchantsManager;

@RequiredArgsConstructor
public class BulldozerUtils {
    private static final Set<Block> INTERNAL_BREAKS = ConcurrentHashMap.newKeySet();
    private static final Set<Block> PROTECTION_CHECKS = ConcurrentHashMap.newKeySet();

    private final EnchantsManager enchantsManager;

    public static boolean isInternalBreak(Block block) {
        return INTERNAL_BREAKS.contains(block);
    }

    public static boolean isProtectionCheck(Block block) {
        return PROTECTION_CHECKS.contains(block);
    }

    public static boolean canModify(Block block, Player player) {
        if (!INTERNAL_BREAKS.add(block)) {
            return false;
        }

        PROTECTION_CHECKS.add(block);
        try {
            BlockBreakEvent event = new BlockBreakEvent(block, player);
            event.setDropItems(false);
            event.setExpToDrop(0);
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        } finally {
            PROTECTION_CHECKS.remove(block);
            INTERNAL_BREAKS.remove(block);
        }
    }

    public boolean isBreakable(Block block, ItemStack tool) {
        return isBreakable(block, tool, null);
    }

    public boolean isBreakable(Block block, ItemStack tool, MaterialFilter filter) {
        if (block == null || block.isEmpty() || block.isLiquid()) {
            return false;
        }

        Material type = block.getType();
        if (type == Material.BEDROCK || type == Material.BARRIER) {
            return false;
        }

        return filter == null ? ToolUtils.isEffectiveTool(type, tool) : filter.allows(type, tool);
    }

    public int breakBlocksInPlane(
            Block center,
            ItemStack tool,
            Player player,
            boolean useXZ,
            boolean useXY,
            boolean useYZ,
            Collection<ItemStack> collectedItems,
            Set<Block> processedBlocks) {
        return breakBlocksInPlane(
                center, tool, player, useXZ, useXY, useYZ, collectedItems, processedBlocks, null, 1, 9, 0);
    }

    public int breakBlocksInPlane(
            Block center,
            ItemStack tool,
            Player player,
            boolean useXZ,
            boolean useXY,
            boolean useYZ,
            Collection<ItemStack> collectedItems,
            Set<Block> processedBlocks,
            MaterialFilter filter,
            int maxBlocks) {
        return breakBlocksInPlane(
                center, tool, player, useXZ, useXY, useYZ, collectedItems, processedBlocks, filter, 1, maxBlocks, 0);
    }

    public int breakBlocksInPlane(
            Block center,
            ItemStack tool,
            Player player,
            boolean useXZ,
            boolean useXY,
            boolean useYZ,
            Collection<ItemStack> collectedItems,
            Set<Block> processedBlocks,
            MaterialFilter filter,
            int radius,
            int maxBlocks) {
        return breakBlocksInPlane(
                center,
                tool,
                player,
                useXZ,
                useXY,
                useYZ,
                collectedItems,
                processedBlocks,
                filter,
                radius,
                maxBlocks,
                0);
    }

    public int breakBlocksInPlane(
            Block center,
            ItemStack tool,
            Player player,
            boolean useXZ,
            boolean useXY,
            boolean useYZ,
            Collection<ItemStack> collectedItems,
            Set<Block> processedBlocks,
            MaterialFilter filter,
            int radius,
            int maxBlocks,
            int durabilityPerBlock) {
        int safeRadius = Math.max(0, radius);
        int safeMaxBlocks = Math.max(0, maxBlocks);
        int count = 0;

        for (int a = -safeRadius; a <= safeRadius && count < safeMaxBlocks && tool.getAmount() > 0; a++) {
            for (int b = -safeRadius; b <= safeRadius && count < safeMaxBlocks && tool.getAmount() > 0; b++) {
                Block target = getRelativeBlock(center, a, b, useXZ, useXY, useYZ);
                if (target.equals(center)) {
                    continue;
                }

                count = breakBlock(
                        tool, player, collectedItems, processedBlocks, count, target, filter, durabilityPerBlock);
            }
        }
        return count;
    }

    public int breakBlocksInCube(
            Block center,
            ItemStack tool,
            Player player,
            int radius,
            Collection<ItemStack> collectedItems,
            Set<Block> processedBlocks) {
        int safeRadius = Math.max(0, radius);
        int diameter = safeRadius * 2 + 1;
        return breakBlocksInCube(
                center,
                tool,
                player,
                safeRadius,
                collectedItems,
                processedBlocks,
                null,
                diameter * diameter * diameter,
                0);
    }

    public int breakBlocksInCube(
            Block center,
            ItemStack tool,
            Player player,
            int radius,
            Collection<ItemStack> collectedItems,
            Set<Block> processedBlocks,
            MaterialFilter filter,
            int maxBlocks) {
        return breakBlocksInCube(center, tool, player, radius, collectedItems, processedBlocks, filter, maxBlocks, 0);
    }

    public int breakBlocksInCube(
            Block center,
            ItemStack tool,
            Player player,
            int radius,
            Collection<ItemStack> collectedItems,
            Set<Block> processedBlocks,
            MaterialFilter filter,
            int maxBlocks,
            int durabilityPerBlock) {
        int safeRadius = Math.max(0, radius);
        int safeMaxBlocks = Math.max(0, maxBlocks);
        int count = 0;

        for (int x = -safeRadius; x <= safeRadius && count < safeMaxBlocks && tool.getAmount() > 0; x++) {
            for (int y = -safeRadius; y <= safeRadius && count < safeMaxBlocks && tool.getAmount() > 0; y++) {
                for (int z = -safeRadius; z <= safeRadius && count < safeMaxBlocks && tool.getAmount() > 0; z++) {
                    Block target = center.getRelative(x, y, z);
                    if (target.equals(center)) {
                        continue;
                    }

                    count = breakBlock(
                            tool, player, collectedItems, processedBlocks, count, target, filter, durabilityPerBlock);
                }
            }
        }
        return count;
    }

    private int breakBlock(
            ItemStack tool,
            Player player,
            Collection<ItemStack> collectedItems,
            Set<Block> processedBlocks,
            int count,
            Block target,
            MaterialFilter filter,
            int durabilityPerBlock) {
        if (!isBreakable(target, tool, filter) || INTERNAL_BREAKS.contains(target)) {
            return count;
        }

        BreakResult result = breakBlock(tool, player, target, processedBlocks);
        if (result == null) {
            return count;
        }

        processDrops(tool, player, target, result, collectedItems);
        if (durabilityPerBlock > 0) {
            DurabilityUtils.damage(tool, durabilityPerBlock);
        }
        return count + 1;
    }

    public BreakResult breakBlock(ItemStack tool, Player player, Block block, Set<Block> processedBlocks) {
        if (!INTERNAL_BREAKS.add(block)) {
            return null;
        }

        if (processedBlocks != null) {
            processedBlocks.add(block);
        }

        try {
            BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
            Bukkit.getPluginManager().callEvent(breakEvent);
            if (breakEvent.isCancelled()) {
                return null;
            }

            World world = block.getWorld();
            Location location = block.getLocation().add(0.5D, 0.5D, 0.5D);
            BreakResult result = breakEvent.isDropItems() ? collectDrops(block, tool, player) : BreakResult.empty();

            int experience = Math.max(0, breakEvent.getExpToDrop());
            block.setType(Material.AIR, true);

            if (experience > 0) {
                world.spawn(location, ExperienceOrb.class).setExperience(experience);
            }
            return result;
        } finally {
            INTERNAL_BREAKS.remove(block);
            if (processedBlocks != null) {
                processedBlocks.remove(block);
            }
        }
    }

    private BreakResult collectDrops(Block block, ItemStack tool, Player player) {
        Collection<ItemStack> resourceDrops = new ArrayList<>(block.getDrops(tool, player));
        Collection<ItemStack> containerDrops = collectContainerContents(block);
        return new BreakResult(resourceDrops, containerDrops);
    }

    private Collection<ItemStack> collectContainerContents(Block block) {
        if (block.getType().name().endsWith("_SHULKER_BOX")) {
            return Collections.emptyList();
        }

        BlockState state = block.getState();
        Inventory inventory;
        if (state instanceof Chest chest) {
            inventory = chest.getBlockInventory();
        } else if (state instanceof Container container) {
            inventory = container.getInventory();
        } else {
            return Collections.emptyList();
        }

        Collection<ItemStack> contents = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir()) {
                contents.add(item.clone());
            }
        }
        return contents;
    }

    public void processDrops(
            ItemStack tool, Player player, Block block, BreakResult result, Collection<ItemStack> collectedItems) {
        processDrops(tool, player, block, result.resourceDrops(), collectedItems);

        if (result.containerDrops().isEmpty()) {
            return;
        }

        if (enchantsManager.hasEnchant("magnet", tool)) {
            collectedItems.addAll(result.containerDrops());
            return;
        }

        result.containerDrops().forEach(item -> block.getWorld().dropItemNaturally(block.getLocation(), item));
    }

    public void processDrops(
            ItemStack tool,
            Player player,
            Block block,
            Collection<ItemStack> drops,
            Collection<ItemStack> collectedItems) {
        enchantsManager
                .findEnchant("filter", FilterEnchant.class)
                .filter(enchant -> enchant.isApplied(tool))
                .ifPresent(enchant -> enchant.filterDrop(drops, player));

        enchantsManager
                .findEnchant("melting", MeltingEnchant.class)
                .filter(enchant -> enchant.isApplied(tool))
                .ifPresent(enchant -> enchant.meltDrops(drops, tool));

        if (enchantsManager.hasEnchant("magnet", tool)) {
            collectedItems.addAll(drops);
            return;
        }

        drops.forEach(item -> block.getWorld().dropItemNaturally(block.getLocation(), item));
    }

    public void collectItems(Player player, ItemStack tool, Collection<ItemStack> collectedItems) {
        if (collectedItems.isEmpty()) {
            return;
        }

        enchantsManager
                .findEnchant("magnet", MagnetEnchant.class)
                .filter(enchant -> enchant.isApplied(tool))
                .ifPresent(enchant -> enchant.dropItems(player, collectedItems));
    }

    public void applyToolDamage(ItemStack tool, int brokenBlocksCount) {
        DurabilityUtils.damage(tool, brokenBlocksCount);
    }

    public record BreakResult(Collection<ItemStack> resourceDrops, Collection<ItemStack> containerDrops) {
        public static BreakResult empty() {
            return new BreakResult(Collections.emptyList(), Collections.emptyList());
        }
    }

    public Block getRelativeBlock(Block center, int a, int b, boolean useXZ, boolean useXY, boolean useYZ) {
        if (useXZ) {
            return center.getRelative(a, 0, b);
        }
        if (useXY) {
            return center.getRelative(a, b, 0);
        }
        return useYZ ? center.getRelative(0, a, b) : center.getRelative(a, 0, b);
    }
}
