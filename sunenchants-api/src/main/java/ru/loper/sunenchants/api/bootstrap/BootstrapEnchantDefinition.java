package ru.loper.sunenchants.api.bootstrap;

import io.papermc.paper.registry.set.RegistryKeySet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public final class BootstrapEnchantDefinition {
    private final String configName;
    private final Key key;
    private final Component description;
    private final int maxLevel;
    private final int weight;
    private final int minBaseCost;
    private final int minAdditionalCost;
    private final int maxBaseCost;
    private final int maxAdditionalCost;
    private final int anvilCost;
    private final EquipmentSlotGroup activeSlots;
    private final RegistryKeySet<@NotNull ItemType> supportedItems;
    private final RegistryKeySet<@NotNull ItemType> primaryItems;
}
