package ru.loper.sunenchants.api.bootstrap;

import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryComposeEvent;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import lombok.experimental.UtilityClass;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class PaperEnchantBootstrapBinder {
    public static void registerAll(
            RegistryComposeEvent<@NotNull Enchantment, EnchantmentRegistryEntry.@NotNull Builder> event,
            BootstrapState state) {
        for (BootstrapEnchantDefinition definition : state.getDefinitions()) {
            event.registry()
                    .register(
                            EnchantmentKeys.create(definition.getKey()),
                            builder -> builder.description(definition.getDescription())
                                    .maxLevel(definition.getMaxLevel())
                                    .supportedItems(definition.getSupportedItems())
                                    .primaryItems(definition.getPrimaryItems())
                                    .weight(definition.getWeight())
                                    .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                            definition.getMinBaseCost(), definition.getMinAdditionalCost()))
                                    .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                            definition.getMaxBaseCost(), definition.getMaxAdditionalCost()))
                                    .anvilCost(definition.getAnvilCost())
                                    .activeSlots(definition.getActiveSlots()));
        }
    }
}
