package ru.loper.sunenchants.modern;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.enchantments.Enchantment;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.registry.AbstractRegistry;

public final class ModernRegistry extends AbstractRegistry {
    @Override
    public void register(SEnchant enchant) {
        Enchantment bukkitEnchant = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .get(enchant.getEnchantKey());

        if (bukkitEnchant == null) {
            throw new IllegalStateException("Modern enchant not found in registry: " + enchant.getEnchantKey());
        }

        enchant.setBukkitEnchantment(bukkitEnchant);
        enchantmentMap.put(bukkitEnchant, enchant);
    }
}
