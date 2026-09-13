package ru.loper.sunenchants.api.utils;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import io.papermc.paper.datacomponent.item.ItemLore;
import java.util.Map;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

final class ComponentEnchantReader {

    private ComponentEnchantReader() {}

    static Map<Enchantment, Integer> enchantments(ItemStack stack) {
        ItemEnchantments enchantments = stack.getData(DataComponentTypes.ENCHANTMENTS);
        return enchantments == null ? Map.of() : enchantments.enchantments();
    }

    static boolean hasLore(ItemStack stack) {
        ItemLore lore = stack.getData(DataComponentTypes.LORE);
        return lore != null && !lore.lines().isEmpty();
    }
}
