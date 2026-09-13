package ru.loper.sunenchants.api.utils;

import java.util.IdentityHashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class EnchantSnapshotCache {
    private static final int MAX_ENTRIES = 2048;
    private static final boolean COMPONENTS_AVAILABLE = classPresent("io.papermc.paper.datacomponent.DataComponentTypes");

    private static final Map<ItemStack, Map<Enchantment, Integer>> CACHE = new IdentityHashMap<>();

    private static volatile boolean active;

    private EnchantSnapshotCache() {}

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

    public static void invalidate(@Nullable ItemStack stack) {
        if (stack != null && !CACHE.isEmpty() && Bukkit.isPrimaryThread()) {
            CACHE.remove(stack);
        }
    }

    public static Map<Enchantment, Integer> enchantments(@Nullable ItemStack stack) {
        if (stack == null) {
            return Map.of();
        }

        if (!active || !Bukkit.isPrimaryThread()) {
            return read(stack);
        }

        Map<Enchantment, Integer> cached = CACHE.get(stack);
        if (cached != null) {
            return cached;
        }

        Map<Enchantment, Integer> snapshot = read(stack);
        if (CACHE.size() < MAX_ENTRIES) {
            CACHE.put(stack, snapshot);
        }
        return snapshot;
    }

    public static boolean hasLore(ItemStack stack) {
        if (COMPONENTS_AVAILABLE) {
            return ComponentEnchantReader.hasLore(stack);
        }
        if (!stack.hasItemMeta()) {
            return false;
        }

        var meta = stack.getItemMeta();
        return meta != null && meta.hasLore();
    }

    private static Map<Enchantment, Integer> read(ItemStack stack) {
        if (stack.getAmount() <= 0 || stack.getType().isAir()) {
            return Map.of();
        }

        return COMPONENTS_AVAILABLE ? ComponentEnchantReader.enchantments(stack) : stack.getEnchantments();
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}
