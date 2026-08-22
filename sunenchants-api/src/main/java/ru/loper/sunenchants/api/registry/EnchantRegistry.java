package ru.loper.sunenchants.api.registry;

import java.util.Map;
import org.bukkit.inventory.meta.ItemMeta;
import ru.loper.sunenchants.api.enchants.SEnchant;

public interface EnchantRegistry {
    void register(SEnchant enchant);

    ItemMeta applyEnchant(ItemMeta meta, SEnchant enchant, int level);

    ItemMeta addStoredEnchant(ItemMeta meta, SEnchant enchant, int level);

    Map<SEnchant, Integer> getEnchantments(ItemMeta meta);

    Map<SEnchant, Integer> getStoredEnchantments(ItemMeta meta);
}
