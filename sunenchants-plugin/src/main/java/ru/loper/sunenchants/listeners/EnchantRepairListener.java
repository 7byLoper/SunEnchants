package ru.loper.sunenchants.listeners;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
import ru.loper.sunenchants.manager.EnchantsManager;

@RequiredArgsConstructor
public class EnchantRepairListener implements Listener {
    private final EnchantsManager enchantsManager;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!enchantsManager.getEnchantsConfig().isLegacyLoreMigrationEnabled()) {
            return;
        }

        Bukkit.getScheduler()
                .runTaskLater(
                        SunEnchants.getInstance(),
                        () -> fixInventory(event.getPlayer()),
                        enchantsManager.getEnchantsConfig().getInventoryMigrationDelay());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enchantsManager.getEnchantsConfig().isLegacyLoreMigrationEnabled()) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) {
            return;
        }

        if (fixItem(item)) {
            event.setCurrentItem(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!enchantsManager.getEnchantsConfig().isLegacyLoreMigrationEnabled()
                || event.getInventory() instanceof EnchantingInventory) {
            return;
        }

        fixInventory(event.getInventory());
    }

    private void fixInventory(Player player) {
        fixInventory(player.getInventory());
    }

    private void fixInventory(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        boolean changed = false;

        for (ItemStack item : contents) {
            changed |= fixItem(item);
        }

        if (changed) {
            inventory.setContents(contents);
        }
    }

    private boolean fixItem(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
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

        List<String> legacyLore =
                lore.stream().map(enchantsManager::componentToLegacy).toList();

        Map<SEnchant, Integer> legacyEnchants = enchantsManager.getEnchants().values().stream()
                .flatMap(enchant -> Optional.ofNullable(findLevelInLegacyLore(legacyLore, enchant)).stream()
                        .map(level -> Map.entry(enchant, level)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var missingEnchants = legacyEnchants.entrySet().stream()
                .filter(entry -> !hasEnchant(item, entry.getKey(), entry.getValue()))
                .toList();

        missingEnchants.forEach(entry -> enchantsManager.addEnchant(item, entry.getKey(), entry.getValue()));
        boolean loreRemoved = removeLegacyLore(item, legacyEnchants);
        return !missingEnchants.isEmpty() || loreRemoved;
    }

    private boolean hasEnchant(ItemStack item, SEnchant enchant, int level) {
        var meta = item.getItemMeta();
        var registry = enchantsManager.getRegistry();

        Integer currentLevel = meta instanceof EnchantmentStorageMeta
                ? registry.getStoredEnchantments(meta).get(enchant)
                : registry.getEnchantments(meta).get(enchant);

        return currentLevel != null && currentLevel == level;
    }

    private Integer findLevelInLegacyLore(List<String> legacyLore, SEnchant enchant) {
        return enchant.getEnchantmentLevels().keySet().stream()
                .filter(level -> legacyLore.contains(enchantsManager.componentToLegacy(enchant.displayName(level))))
                .findFirst()
                .orElse(null);
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

        var legacyLines = legacyEnchants.entrySet().stream()
                .map(entry -> enchantsManager.componentToLegacy(entry.getKey().displayName(entry.getValue())))
                .collect(Collectors.toSet());
        List<Component> migratedLore = lore.stream()
                .filter(line -> !legacyLines.contains(enchantsManager.componentToLegacy(line)))
                .toList();

        if (Objects.equals(lore, migratedLore)) {
            return false;
        }

        meta.lore(migratedLore.isEmpty() ? null : migratedLore);
        item.setItemMeta(meta);
        return true;
    }
}
