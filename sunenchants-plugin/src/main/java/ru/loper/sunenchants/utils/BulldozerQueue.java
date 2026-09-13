package ru.loper.sunenchants.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class BulldozerQueue {
    private static final Deque<PendingBreak> QUEUE = new ArrayDeque<>();
    private static final int MAX_QUEUED_JOBS = 256;

    private BulldozerQueue() {}

    static void enqueue(PendingBreak pending) {
        if (QUEUE.size() >= MAX_QUEUED_JOBS) {
            return;
        }
        QUEUE.addLast(pending);
    }

    public static void clear() {
        QUEUE.clear();
    }

    public static int size() {
        return QUEUE.size();
    }

    public static void tick() {
        int jobs = QUEUE.size();
        for (int i = 0; i < jobs; i++) {
            PendingBreak pending = QUEUE.pollFirst();
            if (pending == null) {
                return;
            }

            if (pending.process()) {
                QUEUE.addLast(pending);
            }
        }
    }

    public static final class PendingBreak {
        private final BulldozerUtils utils;
        private final Player player;
        private final ItemStack tool;
        private final List<Block> targets;
        private final Set<Block> processedBlocks;
        private final MaterialFilter filter;
        private final int durabilityPerBlock;
        private final int budget;

        private int remaining;
        private int index;

        PendingBreak(
                BulldozerUtils utils,
                Player player,
                ItemStack tool,
                List<Block> targets,
                int fromIndex,
                @Nullable Set<Block> processedBlocks,
                @Nullable MaterialFilter filter,
                int remaining,
                int durabilityPerBlock,
                int budget) {
            this.utils = utils;
            this.player = player;
            this.tool = tool;
            this.targets = targets;
            this.index = fromIndex;
            this.processedBlocks = processedBlocks;
            this.filter = filter;
            this.remaining = remaining;
            this.durabilityPerBlock = durabilityPerBlock;
            this.budget = Math.max(1, budget);
        }

        private boolean process() {
            if (!player.isOnline() || tool.getAmount() <= 0 || remaining <= 0 || index >= targets.size()) {
                return false;
            }

            Collection<ItemStack> collected = new ArrayList<>();
            BulldozerUtils.RunResult result = utils.runTargets(
                    targets,
                    index,
                    tool,
                    player,
                    collected,
                    processedBlocks,
                    filter,
                    Math.min(remaining, budget),
                    durabilityPerBlock);

            index = result.visited();
            remaining -= result.broken();

            utils.collectItems(player, tool, collected);

            return remaining > 0 && index < targets.size() && tool.getAmount() > 0;
        }
    }
}
