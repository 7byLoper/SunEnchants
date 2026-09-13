package ru.loper.sunenchants.listeners;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.manager.EnchantsManager;

@RequiredArgsConstructor
public class EnchantListener implements Listener {
    private final EnchantsManager enchantManager;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemEnchant(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            event.getEnchantsToAdd().keySet().removeIf(enchant -> enchantManager.hasCustomConflict(itemMeta, enchant));
        }

        for (SEnchant enchant : getApplicableEnchants(item)) {
            Enchantment bukkitEnchant = enchant.getBukkitEnchantment();
            if (bukkitEnchant == null || event.getEnchantsToAdd().containsKey(bukkitEnchant)) {
                continue;
            }

            int level = rollLevel(enchant.getEnchantmentLevels());
            if (level > 0) {
                event.getEnchantsToAdd().keySet().removeIf(enchant::conflictsWith);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    enchantManager.removeConflictingEnchants(meta, enchant);
                    item.setItemMeta(meta);
                }
                event.getEnchantsToAdd().put(bukkitEnchant, level);
            }
        }
    }

    private List<SEnchant> getApplicableEnchants(ItemStack item) {
        return enchantManager.getEnchants().values().stream()
                .filter(enchant -> enchant.canEnchantItem(item))
                .filter(enchant -> !enchant.isApplied(item))
                .toList();
    }

    private int rollLevel(Map<Integer, AbstractLevel> levels) {
        int totalChance = levels.values().stream()
                .mapToInt(AbstractLevel::getTableChance)
                .filter(chance -> chance > 0)
                .sum();

        if (totalChance <= 0 || ThreadLocalRandom.current().nextInt(100) >= Math.min(100, totalChance)) {
            return -1;
        }

        int roll = ThreadLocalRandom.current().nextInt(totalChance);
        int accumulated = 0;

        for (Map.Entry<Integer, AbstractLevel> entry : levels.entrySet()) {
            int chance = Math.max(0, entry.getValue().getTableChance());
            accumulated += chance;
            if (roll < accumulated) {
                return entry.getKey();
            }
        }

        return -1;
    }
}
