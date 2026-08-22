package ru.loper.sunenchants.api.bootstrap;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class BootstrapItemParser {
    public static RegistryKeySet<@NotNull ItemType> createSupportedItems(List<String> targetItems) {
        Set<TypedKey<@NotNull ItemType>> keys = new HashSet<>();

        for (String raw : targetItems) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String id = raw.trim().toLowerCase(Locale.ROOT);

            if (!id.contains(":")) {
                id = "minecraft:" + id;
            }

            TypedKey<@NotNull ItemType> key = TypedKey.create(RegistryKey.ITEM, Key.key(id));
            keys.add(key);
        }

        return RegistrySet.keySet(RegistryKey.ITEM, keys);
    }
}
