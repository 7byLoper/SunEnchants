package ru.loper.sunenchants.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.manager.EnchantsManager;
import ru.loper.sungrindstone.api.event.GrindStoneEvent;
import ru.loper.sungrindstone.manager.GrindStoneEnchantment;

@RequiredArgsConstructor
public class SunGrindStoneListener implements Listener {
    private final EnchantsManager enchantManager;

    @EventHandler
    public void onGrindStone(GrindStoneEvent event) {
        ItemStack itemStack = event.getGrindStoneItem();
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return;
        }

        List<Component> lore = itemMeta.lore();
        if (lore == null || lore.isEmpty()) {
            return;
        }

        List<Component> updatedLore = new ArrayList<>(lore);

        for (Enchantment enchantment : event.getRemoveEnchantments().stream()
                .map(GrindStoneEnchantment::enchantment)
                .toList()) {
            Optional<SEnchant> customEnchant = findCustomEnchant(enchantment);
            if (customEnchant.isEmpty()) {
                continue;
            }

            String enchantName = customEnchant.get().getDisplayName().replace("{level}", "");
            updatedLore.removeIf(line -> enchantManager.componentToLegacy(line).startsWith(enchantName));
        }

        itemMeta.lore(updatedLore.isEmpty() ? null : updatedLore);
        itemStack.setItemMeta(itemMeta);
        event.setGrindStoneItem(itemStack);
    }

    private Optional<SEnchant> findCustomEnchant(Enchantment enchantment) {
        return enchantManager.getEnchants().values().stream()
                .filter(custom -> enchantment.equals(custom.getBukkitEnchantment()))
                .findFirst();
    }
}
