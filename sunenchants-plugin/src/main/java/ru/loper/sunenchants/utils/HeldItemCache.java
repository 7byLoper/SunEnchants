package ru.loper.sunenchants.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class HeldItemCache {
    private static final Map<UUID, Entry> CACHE = new HashMap<>();

    private static volatile boolean active;

    private HeldItemCache() {}

    public static void activate() {
        active = true;
    }

    public static void deactivate() {
        active = false;
        CACHE.clear();
    }

    public static void reset() {
        CACHE.clear();
    }

    public static void invalidate(Player player) {
        if (!CACHE.isEmpty() && Bukkit.isPrimaryThread()) {
            CACHE.remove(player.getUniqueId());
        }
    }

    public static ItemStack mainHand(Player player) {
        ItemStack live = player.getInventory().getItemInMainHand();
        if (!active || !Bukkit.isPrimaryThread()) {
            return live;
        }

        Entry entry = CACHE.get(player.getUniqueId());
        if (entry != null && entry.type == live.getType() && entry.amount == live.getAmount()) {
            return entry.stack;
        }

        CACHE.put(player.getUniqueId(), new Entry(live, live.getType(), live.getAmount()));
        return live;
    }

    private record Entry(ItemStack stack, Material type, int amount) {}
}
