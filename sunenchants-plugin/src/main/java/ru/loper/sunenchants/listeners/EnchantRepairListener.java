package ru.loper.sunenchants.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.Nullable;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.utils.EnchantSnapshotCache;
import ru.loper.sunenchants.manager.EnchantsManager;

@RequiredArgsConstructor
public class EnchantRepairListener implements Listener {
    private final EnchantsManager enchantsManager;

    private final Map<String, LegacyMatch> legacyLookup = new HashMap<>();

    private int lookupRevision = -1;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!isMigrationEnabled()) {
            return;
        }

        Bukkit.getScheduler()
                .runTaskLater(
                        SunEnchants.getInstance(),
                        () -> migratePlayer(event.getPlayer()),
                        enchantsManager.getEnchantsConfig().getInventoryMigrationDelay());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isMigrationEnabled()) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (!hasLegacyCandidate(item)) {
            return;
        }

        if (fixItem(item)) {
            event.setCurrentItem(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!isMigrationEnabled() || event.getInventory() instanceof EnchantingInventory) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        fixInventory(player.getInventory());
    }

    private boolean isMigrationEnabled() {
        return enchantsManager.getEnchantsConfig().isLegacyLoreMigrationEnabled();
    }

    private void migratePlayer(Player player) {
        if (!player.isOnline()) {
            return;
        }

        fixInventory(player.getInventory());
    }

    private void fixInventory(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        boolean changed = false;

        for (ItemStack item : contents) {
            if (hasLegacyCandidate(item)) {
                changed |= fixItem(item);
            }
        }

        if (changed) {
            inventory.setContents(contents);
        }
    }

    private boolean hasLegacyCandidate(@Nullable ItemStack item) {
        return item != null && !item.getType().isAir() && EnchantSnapshotCache.hasLore(item);
    }

    private boolean fixItem(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        var meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) {
            return false;
        }

        Map<SEnchant, Integer> legacyEnchants = findLegacyEnchants(lore);
        if (legacyEnchants.isEmpty()) {
            return false;
        }

        List<Map.Entry<SEnchant, Integer>> missingEnchants = new ArrayList<>();
        for (Map.Entry<SEnchant, Integer> entry : legacyEnchants.entrySet()) {
            if (!hasEnchant(item, entry.getKey(), entry.getValue())) {
                missingEnchants.add(entry);
            }
        }

        missingEnchants.forEach(entry -> enchantsManager.addEnchant(item, entry.getKey(), entry.getValue()));
        boolean loreRemoved = removeLegacyLore(item, legacyEnchants);
        return !missingEnchants.isEmpty() || loreRemoved;
    }

    private Map<SEnchant, Integer> findLegacyEnchants(List<Component> lore) {
        Map<String, LegacyMatch> lookup = legacyLookup();
        Map<SEnchant, Integer> found = new HashMap<>();

        for (Component line : lore) {
            LegacyMatch match = lookup.get(enchantsManager.componentToLegacy(line));
            if (match != null) {
                found.putIfAbsent(match.enchant(), match.level());
            }
        }
        return found;
    }

    private Map<String, LegacyMatch> legacyLookup() {
        int revision = enchantsManager.getRevision();
        if (revision == lookupRevision) {
            return legacyLookup;
        }

        legacyLookup.clear();
        for (SEnchant enchant : enchantsManager.getEnchants().values()) {
            for (int level : enchant.getEnchantmentLevels().keySet()) {
                legacyLookup.putIfAbsent(
                        enchantsManager.componentToLegacy(enchant.displayName(level)),
                        new LegacyMatch(enchant, level));
            }
        }

        lookupRevision = revision;
        return legacyLookup;
    }

    private boolean hasEnchant(ItemStack item, SEnchant enchant, int level) {
        var meta = item.getItemMeta();
        var registry = enchantsManager.getRegistry();

        Integer currentLevel = meta instanceof EnchantmentStorageMeta
                ? registry.getStoredEnchantments(meta).get(enchant)
                : registry.getEnchantments(meta).get(enchant);

        return currentLevel != null && currentLevel == level;
    }

    private boolean removeLegacyLore(ItemStack item, Map<SEnchant, Integer> legacyEnchants) {
        if (!SunEnchants.getInstance().isModernRegister()
                || !enchantsManager.getEnchantsConfig().isLegacyLoreRemovalEnabled()
                || legacyEnchants.isEmpty()) {
            return false;
        }

        var meta = item.getItemMeta();
        List<Component> lore = meta == null ? null : meta.lore();
        if (meta == null || lore == null) {
            return false;
        }

        Set<String> legacyLines = new HashSet<>();
        legacyEnchants.forEach(
                (enchant, level) -> legacyLines.add(enchantsManager.componentToLegacy(enchant.displayName(level))));

        List<Component> migratedLore = new ArrayList<>(lore.size());
        for (Component line : lore) {
            if (!legacyLines.contains(enchantsManager.componentToLegacy(line))) {
                migratedLore.add(line);
            }
        }

        if (Objects.equals(lore, migratedLore)) {
            return false;
        }

        meta.lore(migratedLore.isEmpty() ? null : migratedLore);
        item.setItemMeta(meta);
        return true;
    }

    private record LegacyMatch(SEnchant enchant, int level) {}
}
