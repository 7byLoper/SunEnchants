package ru.loper.sunenchants.api.bootstrap;

import io.papermc.paper.registry.set.RegistryKeySet;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

public final class BootstrapEnchantConfigLoader {
    public BootstrapState loadFromResource(ClassLoader classLoader, String resourcePath) {
        try {
            YamlConfiguration config = loadResource(classLoader, resourcePath);
            return config.isList("enchants") ? loadCatalog(classLoader, config, null) : load(config);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load bootstrap config from resource: " + resourcePath, e);
        }
    }

    public BootstrapState loadFromResource(ClassLoader classLoader, String resourcePath, Path dataDirectory) {
        try {
            YamlConfiguration config = loadCatalogConfiguration(classLoader, resourcePath, dataDirectory);
            return config.isList("enchants")
                    ? loadCatalog(classLoader, config, dataDirectory)
                    : load(config, dataDirectory);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load bootstrap config from resource: " + resourcePath, e);
        }
    }

    private YamlConfiguration loadCatalogConfiguration(ClassLoader classLoader, String resourcePath, Path dataDirectory)
            throws IOException {
        if (dataDirectory != null) {
            Path catalogFile = dataDirectory.resolve(resourcePath);
            if (Files.isRegularFile(catalogFile)) {
                return YamlConfiguration.loadConfiguration(catalogFile.toFile());
            }
        }
        return loadResource(classLoader, resourcePath);
    }

    private YamlConfiguration loadResource(ClassLoader classLoader, String resourcePath) throws IOException {
        try (InputStream stream = classLoader.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }

            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        }
    }

    public BootstrapState load(ConfigurationSection root) {
        return load(root, null);
    }

    private BootstrapState load(ConfigurationSection root, Path dataDirectory) {
        List<BootstrapEnchantDefinition> definitions = new ArrayList<>();

        for (String enchantName : root.getKeys(false)) {
            addDefinition(definitions, enchantName, loadEnchantSection(root, enchantName, dataDirectory));
        }

        return new BootstrapState(definitions);
    }

    private BootstrapState loadCatalog(ClassLoader classLoader, ConfigurationSection catalog, Path dataDirectory) {
        List<BootstrapEnchantDefinition> definitions = new ArrayList<>();
        catalog.getStringList("enchants")
                .forEach(enchantName -> addDefinition(
                        definitions, enchantName, loadEnchantSection(classLoader, enchantName, dataDirectory)));
        return new BootstrapState(definitions);
    }

    private void addDefinition(
            List<BootstrapEnchantDefinition> definitions, String enchantName, ConfigurationSection enchantSection) {
        if (enchantSection == null) {
            return;
        }

        ConfigurationSection bootstrap = enchantSection.getConfigurationSection("bootstrap");
        if (bootstrap == null) {
            throw new IllegalStateException("Missing bootstrap section for enchant: " + enchantName);
        }

        var enchant = readDefinition(enchantName, enchantSection, bootstrap);
        if (enchant != null) {
            definitions.add(enchant);
        }
    }

    private ConfigurationSection loadEnchantSection(ConfigurationSection root, String enchantName, Path dataDirectory) {
        if (dataDirectory == null) {
            return root.getConfigurationSection(enchantName);
        }

        Path configFile = dataDirectory.resolve("enchants").resolve(enchantName + ".yml");
        return Files.isRegularFile(configFile)
                ? YamlConfiguration.loadConfiguration(configFile.toFile())
                : root.getConfigurationSection(enchantName);
    }

    private ConfigurationSection loadEnchantSection(ClassLoader classLoader, String enchantName, Path dataDirectory) {
        Path configFile =
                dataDirectory == null ? null : dataDirectory.resolve("enchants").resolve(enchantName + ".yml");
        YamlConfiguration defaults = tryLoadResource(classLoader, "enchants/" + enchantName + ".yml");

        if (configFile == null || !Files.isRegularFile(configFile)) {
            return defaults;
        }

        YamlConfiguration current = YamlConfiguration.loadConfiguration(configFile.toFile());
        if (defaults != null && mergeDefaultsAndMigrate(defaults, current)) {
            try {
                current.save(configFile.toFile());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to migrate enchant config: " + enchantName, e);
            }
        }
        return current;
    }

    private YamlConfiguration tryLoadResource(ClassLoader classLoader, String resourcePath) {
        try (InputStream stream = classLoader.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load enchant config resource: " + resourcePath, e);
        }
    }

    private boolean mergeDefaultsAndMigrate(YamlConfiguration defaults, YamlConfiguration current) {
        boolean changed = false;
        int defaultVersion = defaults.getInt("_config_version", 0);
        int currentVersion = current.getInt("_config_version", 0);

        if (defaultVersion > currentVersion) {
            for (String path : defaults.getStringList("_migration_force")) {
                current.set(path, defaults.get(path));
                changed = true;
            }
            for (String path : defaults.getStringList("_migration_remove")) {
                if (current.contains(path)) {
                    current.set(path, null);
                    changed = true;
                }
            }
            current.set("_config_version", defaultVersion);
            changed = true;
        }

        for (String path : defaults.getKeys(true)) {
            if (!defaults.isConfigurationSection(path) && !current.contains(path)) {
                current.set(path, defaults.get(path));
                changed = true;
            }
        }
        return changed;
    }

    private BootstrapEnchantDefinition readDefinition(
            String configName, ConfigurationSection enchantSection, ConfigurationSection bootstrap) {
        boolean enable = enchantSection.getBoolean("enable", true) && bootstrap.getBoolean("enable", true);
        if (!enable) {
            return null;
        }

        String keyValue = requireString(bootstrap, "key");
        String description = bootstrap.getString("description", configName);
        int maxLevel = bootstrap.getInt("max-level", 1);
        int weight = bootstrap.getInt("weight", 10);

        ConfigurationSection minCost = requireSection(bootstrap, "min-cost");
        ConfigurationSection maxCost = requireSection(bootstrap, "max-cost");

        int minBase = minCost.getInt("base", 1);
        int minAdditional = minCost.getInt("per-level-above-first", 10);
        int maxBase = maxCost.getInt("base", 20);
        int maxAdditional = maxCost.getInt("per-level-above-first", 10);
        int anvilCost = maxCost.getInt("anvil-cost", 10);

        String slotName = bootstrap.getString("active-slots", "ANY");
        EquipmentSlotGroup activeSlots = EquipmentSlotGroup.getByName(slotName.toUpperCase(Locale.ROOT));
        RegistryKeySet<@NotNull ItemType> supportedItems =
                BootstrapItemParser.createSupportedItems(enchantSection.getStringList("target"));
        RegistryKeySet<@NotNull ItemType> primaryItems = BootstrapItemParser.createSupportedItems(
                bootstrap.getStringList("primary-items").isEmpty()
                        ? enchantSection.getStringList("target")
                        : bootstrap.getStringList("primary-items"));

        return new BootstrapEnchantDefinition(
                configName,
                Key.key(keyValue),
                Component.text(description),
                maxLevel,
                weight,
                minBase,
                minAdditional,
                maxBase,
                maxAdditional,
                anvilCost,
                activeSlots,
                supportedItems,
                primaryItems);
    }

    private List<Material> parseMaterials(List<String> rawMaterials, String configName) {
        List<Material> result = new ArrayList<>();

        for (String raw : rawMaterials) {
            try {
                Material material = Material.valueOf(raw.toUpperCase(Locale.ROOT));
                if (!material.isItem()) {
                    throw new IllegalStateException("Material is not an item: " + raw + " in " + configName);
                }
                result.add(material);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unknown material '" + raw + "' in " + configName, e);
            }
        }

        if (result.isEmpty()) {
            throw new IllegalStateException("No target materials configured for " + configName);
        }

        return result;
    }

    private ConfigurationSection requireSection(ConfigurationSection root, String path) {
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalStateException("Missing section '" + path + "' in " + root.getCurrentPath());
        }
        return section;
    }

    private String requireString(ConfigurationSection root, String path) {
        String value = root.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing string '" + path + "' in " + root.getCurrentPath());
        }
        return value;
    }
}
