package ru.loper.sunenchants.listeners;

import com.destroystokyo.paper.event.inventory.PrepareGrindstoneEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.config.EnchantLimitsConfig;
import ru.loper.sunenchants.manager.EnchantsManager;

@RequiredArgsConstructor
public class AnvilListener implements Listener {
    private final EnchantsManager enchantManager;
    private final EnchantLimitsConfig limitsConfig;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack firstItem = event.getInventory().getFirstItem();
        ItemStack secondItem = event.getInventory().getSecondItem();

        if (firstItem == null || secondItem == null) {
            return;
        }

        boolean firstIsBook = firstItem.getType() == Material.ENCHANTED_BOOK;
        boolean secondIsBook = secondItem.getType() == Material.ENCHANTED_BOOK;

        if (firstIsBook && secondIsBook) {
            handleBookMerge(event, firstItem, secondItem);
            return;
        }

        if (secondIsBook) {
            handleEnchantedBook(event, firstItem, secondItem);
            return;
        }

        if (firstIsBook) {
            handleEnchantedBook(event, secondItem, firstItem);
            return;
        }

        if (firstItem.getType() == secondItem.getType()
                && firstItem.getType() != Material.PLAYER_HEAD
                && firstItem.getType() != Material.TOTEM_OF_UNDYING) {
            handleItemMerge(event, firstItem, secondItem);
        }
    }

    private void handleBookMerge(PrepareAnvilEvent event, ItemStack firstBook, ItemStack secondBook) {
        EnchantmentStorageMeta firstMeta = (EnchantmentStorageMeta) firstBook.getItemMeta();
        EnchantmentStorageMeta secondMeta = (EnchantmentStorageMeta) secondBook.getItemMeta();
        if (firstMeta == null || secondMeta == null) {
            return;
        }

        ItemStack result = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta resultMeta = (EnchantmentStorageMeta) result.getItemMeta();
        if (resultMeta == null) {
            return;
        }

        boolean modified = false;

        for (Map.Entry<Enchantment, Integer> entry :
                firstMeta.getStoredEnchants().entrySet()) {
            resultMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
        }

        for (Map.Entry<Enchantment, Integer> entry :
                secondMeta.getStoredEnchants().entrySet()) {
            Enchantment enchantment = entry.getKey();
            int secondLevel = entry.getValue();

            if (resultMeta.hasStoredEnchant(enchantment)) {
                int firstLevel = resultMeta.getStoredEnchantLevel(enchantment);
                int mergedLevel =
                        mergeVanillaLevels(firstLevel, secondLevel, limitsConfig.getEnchantmentLimit(enchantment));
                resultMeta.removeStoredEnchant(enchantment);
                resultMeta.addStoredEnchant(enchantment, mergedLevel, true);
            } else {
                resultMeta.addStoredEnchant(enchantment, secondLevel, true);
            }
        }

        Map<SEnchant, Integer> firstCustom = enchantManager.getRegistry().getStoredEnchantments(firstMeta);
        Map<SEnchant, Integer> secondCustom = enchantManager.getRegistry().getStoredEnchantments(secondMeta);

        for (Map.Entry<SEnchant, Integer> entry : firstCustom.entrySet()) {
            SEnchant enchant = entry.getKey();
            int firstLevel = entry.getValue();
            Integer secondLevel = secondCustom.get(enchant);

            if (secondLevel != null) {
                int newLevel = calculateMergedLevel(enchant, firstLevel, secondLevel);
                AbstractLevel level = enchant.getLevel(newLevel);
                if (level == null || !level.isCombining()) {
                    enchantManager.getRegistry().addStoredEnchant(resultMeta, enchant, firstLevel);
                    modified = true;
                    continue;
                }

                enchantManager.getRegistry().addStoredEnchant(resultMeta, enchant, newLevel);
                modified = true;
            } else {
                enchantManager.getRegistry().addStoredEnchant(resultMeta, enchant, firstLevel);
                modified = true;
            }
        }

        for (Map.Entry<SEnchant, Integer> entry : secondCustom.entrySet()) {
            if (firstCustom.containsKey(entry.getKey())) {
                continue;
            }

            enchantManager.getRegistry().addStoredEnchant(resultMeta, entry.getKey(), entry.getValue());
            modified = true;
        }

        if (modified) {
            enchantManager.updateBookEnchantLore(resultMeta);
        }

        result.setItemMeta(resultMeta);
        event.setResult(result);
        setRepairCost(event, resultMeta.getStoredEnchants().size());
    }

    private void handleEnchantedBook(PrepareAnvilEvent event, ItemStack targetItem, ItemStack bookItem) {
        ItemMeta targetMeta = targetItem.getItemMeta();
        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) bookItem.getItemMeta();
        if (targetMeta == null || bookMeta == null) {
            return;
        }

        ItemStack result = targetItem.clone();
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta == null) {
            return;
        }

        boolean modified = false;

        for (Map.Entry<Enchantment, Integer> entry :
                bookMeta.getStoredEnchants().entrySet()) {
            Enchantment enchantment = entry.getKey();
            int bookLevel = entry.getValue();

            if (isVanillaEnchantBlocked(targetItem, enchantment)) {
                continue;
            }

            int currentLevel = resultMeta.getEnchantLevel(enchantment);
            int newLevel = currentLevel > 0
                    ? mergeVanillaLevels(currentLevel, bookLevel, limitsConfig.getEnchantmentLimit(enchantment))
                    : bookLevel;

            resultMeta.addEnchant(enchantment, newLevel, true);
            modified = true;
        }

        Map<SEnchant, Integer> customBookEnchants = enchantManager.getRegistry().getStoredEnchantments(bookMeta);
        for (Map.Entry<SEnchant, Integer> entry : customBookEnchants.entrySet()) {
            SEnchant enchant = entry.getKey();
            int bookLevel = entry.getValue();

            if (!enchant.canEnchantItem(targetItem)) {
                continue;
            }

            Enchantment bukkitEnchant = enchant.getBukkitEnchantment();
            if (bukkitEnchant == null) {
                continue;
            }

            if (isVanillaEnchantBlocked(targetItem, bukkitEnchant)) {
                continue;
            }

            int existingLevel = resultMeta.getEnchantLevel(bukkitEnchant);
            if (existingLevel > 0) {
                int newLevel = calculateMergedLevel(enchant, existingLevel, bookLevel);
                AbstractLevel level = enchant.getLevel(newLevel);
                if (level == null || !level.isCombining()) {
                    continue;
                }

                resultMeta = enchantManager.applyEnchant(resultMeta, enchant, newLevel);
            } else {
                resultMeta = enchantManager.applyEnchant(resultMeta, enchant, bookLevel);
            }

            modified = true;
        }

        if (modified) {
            enchantManager.updateEnchantLore(resultMeta);
            result.setItemMeta(resultMeta);
            event.setResult(result);
            setRepairCost(event, resultMeta.getEnchants().size());
        }
    }

    private void handleItemMerge(PrepareAnvilEvent event, ItemStack firstItem, ItemStack secondItem) {
        ItemMeta firstMeta = firstItem.getItemMeta();
        ItemMeta secondMeta = secondItem.getItemMeta();
        if (firstMeta == null || secondMeta == null) {
            return;
        }

        ItemStack result = firstItem.clone();
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta == null) {
            return;
        }

        boolean modified = false;

        for (Map.Entry<Enchantment, Integer> entry : secondMeta.getEnchants().entrySet()) {
            Enchantment enchantment = entry.getKey();
            int secondLevel = entry.getValue();

            if (isVanillaEnchantBlocked(firstItem, enchantment)) {
                continue;
            }

            int firstLevel = resultMeta.getEnchantLevel(enchantment);
            int newLevel = firstLevel > 0
                    ? mergeVanillaLevels(firstLevel, secondLevel, limitsConfig.getEnchantmentLimit(enchantment))
                    : secondLevel;

            resultMeta.addEnchant(enchantment, newLevel, true);
            modified = true;
        }

        Map<SEnchant, Integer> firstCustom = enchantManager.getRegistry().getEnchantments(firstMeta);
        Map<SEnchant, Integer> secondCustom = enchantManager.getRegistry().getEnchantments(secondMeta);

        for (SEnchant enchant : enchantManager.getEnchants().values()) {
            Integer firstLevel = firstCustom.get(enchant);
            Integer secondLevel = secondCustom.get(enchant);

            Enchantment bukkitEnchant = enchant.getBukkitEnchantment();
            if (bukkitEnchant != null && isVanillaEnchantBlocked(firstItem, bukkitEnchant)) {
                continue;
            }

            if (firstLevel == null && secondLevel != null) {
                if (!enchant.canEnchantItem(firstItem)) {
                    continue;
                }

                resultMeta = enchantManager.applyEnchant(resultMeta, enchant, secondLevel);
                modified = true;
                continue;
            }

            if (firstLevel != null && secondLevel == null) {
                resultMeta = enchantManager.applyEnchant(resultMeta, enchant, firstLevel);
                modified = true;
                continue;
            }

            if (firstLevel != null) {
                int newLevel = calculateMergedLevel(enchant, firstLevel, secondLevel);
                AbstractLevel level = enchant.getLevel(newLevel);
                if (level == null || !level.isCombining()) {
                    resultMeta = enchantManager.applyEnchant(resultMeta, enchant, firstLevel);
                    modified = true;
                    continue;
                }

                resultMeta = enchantManager.applyEnchant(resultMeta, enchant, newLevel);
                modified = true;
            }
        }

        if (modified) {
            enchantManager.updateEnchantLore(resultMeta);
            result.setItemMeta(resultMeta);
            event.setResult(result);
            setRepairCost(event, resultMeta.getEnchants().size());
        }
    }

    private boolean isVanillaEnchantBlocked(ItemStack item, Enchantment enchantment) {
        return limitsConfig.isDisabled(enchantment, item);
    }

    private int mergeVanillaLevels(int firstLevel, int secondLevel, int maxLevel) {
        int currentMax = Math.max(firstLevel, secondLevel);

        if (currentMax >= maxLevel) {
            return currentMax;
        }

        if (firstLevel == secondLevel) {
            return firstLevel + 1;
        }

        return currentMax;
    }

    private int calculateMergedLevel(SEnchant enchant, int firstLevel, int secondLevel) {
        int currentMax = Math.max(firstLevel, secondLevel);
        int maxLevel = enchant.getMaxLevel();

        if (currentMax >= maxLevel) {
            return currentMax;
        }

        if (firstLevel == secondLevel) {
            return firstLevel + 1;
        }

        return currentMax;
    }

    private void setRepairCost(PrepareAnvilEvent event, int enchantCount) {
        int price = enchantCount * 3;
        event.getInventory().setRepairCost(price);
        event.getInventory().setMaximumRepairCost(price);
    }

    @EventHandler
    public void onGrindstonePrepare(PrepareGrindstoneEvent event) {
        ItemStack result = event.getResult();
        if (result == null) {
            return;
        }

        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return;
        }

        removeCustomEnchantsFromLore(meta);
        result.setItemMeta(meta);
        event.setResult(result);
    }

    private void removeCustomEnchantsFromLore(ItemMeta meta) {
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) {
            return;
        }

        List<Component> updatedLore = new ArrayList<>(lore);

        for (SEnchant enchant : enchantManager.getEnchants().values()) {
            String enchantName = enchant.getDisplayName().replace("{level}", "");
            updatedLore.removeIf(line -> enchantManager.componentToLegacy(line).startsWith(enchantName));
        }

        meta.lore(updatedLore.isEmpty() ? null : updatedLore);
    }
}
