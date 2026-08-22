package ru.loper.sunenchants.legacy;

import java.lang.reflect.Field;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.registry.AbstractRegistry;

public final class LegacyRegistry extends AbstractRegistry {
    public LegacyRegistry() {
        forceAllowEnchantRegistration();
    }

    private void forceAllowEnchantRegistration() {
        try {
            Field keyField = Enchantment.class.getDeclaredField("acceptingNew");
            keyField.setAccessible(true);
            keyField.set(null, true);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error unlocking enchantments", e);
        }
    }

    @Override
    public void register(SEnchant enchant) {
        LegacyEnchantWrapper wrapper = new LegacyEnchantWrapper(enchant);
        enchant.setBukkitEnchantment(wrapper);
        enchantmentMap.put(wrapper, enchant);

        try {
            Enchantment.registerEnchantment(wrapper);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().log(Level.WARNING, "Enchantment already registered: " + enchant.getEnchantKey(), e);
        }
    }
}
