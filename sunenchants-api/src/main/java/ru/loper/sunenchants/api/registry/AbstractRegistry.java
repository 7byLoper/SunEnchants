package ru.loper.sunenchants.api.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import ru.loper.sunenchants.api.enchants.SEnchant;

public abstract class AbstractRegistry implements EnchantRegistry {
    protected final Map<Enchantment, SEnchant> enchantmentMap = new HashMap<>();

    @Override
    public ItemMeta applyEnchant(ItemMeta meta, SEnchant enchant, int level) {
        if (enchant.isEnabled() && enchant.getBukkitEnchantment() != null) {
            meta.addEnchant(enchant.getBukkitEnchantment(), level, true);
        }
        return meta;
    }

    @Override
    public ItemMeta addStoredEnchant(ItemMeta meta, SEnchant enchant, int level) {
        if (meta instanceof EnchantmentStorageMeta storageMeta
                && enchant.isEnabled()
                && enchant.getBukkitEnchantment() != null) {
            storageMeta.addStoredEnchant(enchant.getBukkitEnchantment(), level, true);
        }
        return meta;
    }

    @Override
    public Map<SEnchant, Integer> getEnchantments(ItemMeta meta) {
        return meta.getEnchants().entrySet().stream()
                .filter(entry -> enchantmentMap.containsKey(entry.getKey()))
                .filter(entry -> enchantmentMap.get(entry.getKey()).isEnabled())
                .collect(Collectors.toMap(entry -> enchantmentMap.get(entry.getKey()), Map.Entry::getValue));
    }

    @Override
    public Map<SEnchant, Integer> getStoredEnchantments(ItemMeta meta) {
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            return storageMeta.getStoredEnchants().entrySet().stream()
                    .filter(entry -> enchantmentMap.containsKey(entry.getKey()))
                    .filter(entry -> enchantmentMap.get(entry.getKey()).isEnabled())
                    .collect(Collectors.toMap(entry -> enchantmentMap.get(entry.getKey()), Map.Entry::getValue));
        }
        return Map.of();
    }
}
