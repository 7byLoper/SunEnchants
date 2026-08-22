package ru.loper.sunenchants.listeners;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import ru.loper.sunenchants.api.utils.EnchantUtils;
import ru.loper.sunenchants.config.EnchantLimitsConfig;

@RequiredArgsConstructor
public class EnchantsLimitListener implements Listener {
    private final EnchantLimitsConfig limitsConfig;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack left = event.getInventory().getFirstItem();
        ItemStack right = event.getInventory().getSecondItem();
        ItemStack result = event.getInventory().getResult();

        if (left == null || right == null || result == null) {
            return;
        }

        boolean isLeftBook = left.getItemMeta() instanceof EnchantmentStorageMeta;
        boolean isRightBook = right.getItemMeta() instanceof EnchantmentStorageMeta;

        boolean enchantModified;

        if (isLeftBook && isRightBook) {
            enchantModified = handleBookMerge(left, right, result);
        } else if (isLeftBook || isRightBook) {
            enchantModified = handleItemBookMerge(left, right, result, isLeftBook);
        } else {
            enchantModified = handleItemMerge(left, right, result);
        }

        if (enchantModified && limitsConfig.isCostScalingEnabled()) {
            event.getInventory().setRepairCost(limitsConfig.getCostScalingAmount());
        }

        event.setResult(result);
    }

    private boolean handleBookMerge(ItemStack left, ItemStack right, ItemStack result) {
        EnchantmentStorageMeta leftMeta = (EnchantmentStorageMeta) left.getItemMeta();
        EnchantmentStorageMeta rightMeta = (EnchantmentStorageMeta) right.getItemMeta();
        EnchantmentStorageMeta resultMeta = (EnchantmentStorageMeta) result.getItemMeta();

        boolean modified = false;

        resultMeta.getStoredEnchants().keySet().forEach(resultMeta::removeStoredEnchant);

        for (Map.Entry<Enchantment, Integer> leftEntry :
                leftMeta.getStoredEnchants().entrySet()) {
            Enchantment enchant = leftEntry.getKey();
            int leftLevel = leftEntry.getValue();

            if (rightMeta.hasStoredEnchant(enchant)) {
                int rightLevel = rightMeta.getStoredEnchantLevel(enchant);
                int newLevel = processLevel(enchant, leftLevel, rightLevel);
                resultMeta.addStoredEnchant(enchant, newLevel, true);
                if (newLevel > leftLevel) modified = true;
            } else {
                resultMeta.addStoredEnchant(enchant, leftLevel, true);
            }
        }

        for (Map.Entry<Enchantment, Integer> rightEntry :
                rightMeta.getStoredEnchants().entrySet()) {
            Enchantment enchant = rightEntry.getKey();
            int rightLevel = rightEntry.getValue();

            if (!leftMeta.hasStoredEnchant(enchant)) {
                resultMeta.addStoredEnchant(enchant, rightLevel, true);
                modified = true;
            }
        }

        result.setItemMeta(resultMeta);
        return modified;
    }

    private boolean handleItemBookMerge(ItemStack left, ItemStack right, ItemStack result, boolean leftIsBook) {
        ItemStack item = leftIsBook ? right : left;
        ItemStack book = leftIsBook ? left : right;

        ItemMeta resultMeta = result.getItemMeta();
        ItemMeta itemMeta = item.getItemMeta();
        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();

        boolean modified = false;

        resultMeta.getEnchants().keySet().forEach(resultMeta::removeEnchant);

        for (Map.Entry<Enchantment, Integer> itemEntry : itemMeta.getEnchants().entrySet()) {
            Enchantment enchant = itemEntry.getKey();
            int itemLevel = itemEntry.getValue();

            if (limitsConfig.isDisabled(enchant, item)) continue;

            if (bookMeta.hasStoredEnchant(enchant)) {
                int bookLevel = bookMeta.getStoredEnchantLevel(enchant);
                int newLevel = processLevel(enchant, itemLevel, bookLevel);
                resultMeta.addEnchant(enchant, newLevel, true);
                if (newLevel > itemLevel) modified = true;
            } else {
                resultMeta.addEnchant(enchant, itemLevel, true);
            }
        }

        for (Map.Entry<Enchantment, Integer> bookEntry :
                bookMeta.getStoredEnchants().entrySet()) {
            Enchantment enchant = bookEntry.getKey();
            int bookLevel = bookEntry.getValue();

            if (limitsConfig.isDisabled(enchant, item)) continue;

            if (!itemMeta.hasEnchant(enchant)) {
                resultMeta.addEnchant(enchant, bookLevel, true);
                modified = true;
            }
        }

        result.setItemMeta(resultMeta);
        return modified;
    }

    private boolean handleItemMerge(ItemStack left, ItemStack right, ItemStack result) {
        ItemMeta leftMeta = left.getItemMeta();
        ItemMeta rightMeta = right.getItemMeta();
        ItemMeta resultMeta = result.getItemMeta();

        boolean modified = false;

        resultMeta.getEnchants().keySet().forEach(resultMeta::removeEnchant);

        for (Map.Entry<Enchantment, Integer> leftEntry : leftMeta.getEnchants().entrySet()) {
            Enchantment enchant = leftEntry.getKey();
            int leftLevel = leftEntry.getValue();

            if (limitsConfig.isDisabled(enchant, result)) continue;

            if (rightMeta.hasEnchant(enchant)) {
                int rightLevel = rightMeta.getEnchantLevel(enchant);
                int newLevel = processLevel(enchant, leftLevel, rightLevel);
                resultMeta.addEnchant(enchant, newLevel, true);
                if (newLevel > leftLevel) modified = true;
            } else {
                resultMeta.addEnchant(enchant, leftLevel, true);
            }
        }

        for (Map.Entry<Enchantment, Integer> rightEntry :
                rightMeta.getEnchants().entrySet()) {
            Enchantment enchant = rightEntry.getKey();
            int rightLevel = rightEntry.getValue();

            if (limitsConfig.isDisabled(enchant, result)) continue;

            if (!leftMeta.hasEnchant(enchant)) {
                resultMeta.addEnchant(enchant, rightLevel, true);
                modified = true;
            }
        }

        result.setItemMeta(resultMeta);
        return modified;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (!limitsConfig.isSpecialUnbreakingItem(item.getType())) {
            return;
        }

        int unbreakingLevel = item.getEnchantmentLevel(EnchantUtils.UNBREAKING);
        if (unbreakingLevel <= 0) {
            return;
        }

        int chance = limitsConfig.getSpecialUnbreakingChance(unbreakingLevel);
        if (ThreadLocalRandom.current().nextInt(100) < chance) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemMend(PlayerItemMendEvent event) {
        if (!limitsConfig.isBlockedMending()) {
            return;
        }

        ItemStack item = event.getItem();
        if (!limitsConfig.isSpecialUnbreakingItem(item.getType())) {
            return;
        }

        event.setCancelled(true);
    }

    private int processLevel(Enchantment enchant, int level1, int level2) {
        int maxCurrent = Math.max(level1, level2);
        int limit = limitsConfig.getEnchantmentLimit(enchant);

        if (maxCurrent >= limit) {
            return maxCurrent;
        }

        if (level1 == level2) {
            return level1 + 1;
        }

        return maxCurrent;
    }
}
