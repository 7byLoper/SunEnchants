package ru.loper.sunenchants.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.ConfigManager;
import ru.loper.suncore.api.config.CustomConfig;

public class EnchantsConfigManager extends ConfigManager {
    private Map<String, CustomConfig> enchantConfigs;

    public EnchantsConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        enchantConfigs = new LinkedHashMap<>();

        mergeResourceDefaults("enchants.yml");
        CustomConfig catalogConfig = new CustomConfig("enchants.yml", plugin);
        addCustomConfig(catalogConfig);

        List<String> enchantNames = readCatalogEnchantNames(catalogConfig);
        enchantNames.forEach(this::loadEnchantConfig);
    }

    @Override
    public void loadValues() {}

    public CustomConfig getEnchantConfig(String enchantName) {
        return enchantConfigs.get(normalizeEnchantName(enchantName));
    }

    public Set<String> getCatalogEnchantNames() {
        CustomConfig catalog = getCustomConfig("enchants.yml");
        if (catalog == null) {
            return Set.of();
        }
        return new LinkedHashSet<>(normalizeEnchantNames(catalog.getConfig().getStringList("enchants")));
    }

    public boolean isEnchantEnabled(String enchantName) {
        String normalized = normalizeEnchantName(enchantName);
        if (!getCatalogEnchantNames().contains(normalized)) {
            return false;
        }

        CustomConfig config = enchantConfigs.get(normalized);
        return config != null && config.getConfig().getBoolean("enable", true);
    }

    public boolean isDebugEnabled() {
        return getCustomConfig("enchants.yml").getConfig().getBoolean("debug", true);
    }

    public boolean isLegacyLoreMigrationEnabled() {
        return getCustomConfig("enchants.yml").getConfig().getBoolean("migration.legacy_lore.enabled", true);
    }

    public boolean isLegacyLoreRemovalEnabled() {
        return getCustomConfig("enchants.yml").getConfig().getBoolean("migration.legacy_lore.remove_on_modern", true);
    }

    public long getInventoryMigrationDelay() {
        return Math.max(
                0L, getCustomConfig("enchants.yml").getConfig().getLong("migration.legacy_lore.join_delay_ticks", 20L));
    }

    private List<String> readCatalogEnchantNames(CustomConfig catalogConfig) {
        if (catalogConfig.getConfig().isList("enchants")) {
            return normalizeEnchantNames(catalogConfig.getConfig().getStringList("enchants"));
        }

        migrateLegacyConfig(catalogConfig);
        return catalogConfig.getConfig().getKeys(false).stream()
                .filter(catalogConfig.getConfig()::isConfigurationSection)
                .map(this::normalizeEnchantName)
                .distinct()
                .toList();
    }

    private List<String> normalizeEnchantNames(List<String> rawNames) {
        return rawNames.stream()
                .map(this::normalizeEnchantName)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeEnchantName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private void loadEnchantConfig(String enchantName) {
        String normalized = normalizeEnchantName(enchantName);
        if (normalized.isBlank()) {
            return;
        }

        File configFile = new File(plugin.getDataFolder(), "enchants" + File.separator + normalized + ".yml");
        String resourcePath = "enchants/" + normalized + ".yml";

        if (!configFile.exists()) {
            if (!resourceExists(resourcePath)) {
                plugin.getLogger().warning("Unknown enchant in enchants.yml: " + normalized + ". It will be skipped.");
                return;
            }
            plugin.saveResource(resourcePath, false);
        }

        mergeResourceDefaults(resourcePath);

        CustomConfig config = new CustomConfig(configFile);
        enchantConfigs.put(normalized, config);
        addCustomConfig(config);
    }

    private boolean resourceExists(String resourcePath) {
        try (var stream = plugin.getResource(resourcePath)) {
            return stream != null;
        } catch (IOException e) {
            throw new IllegalStateException("Could not inspect resource: " + resourcePath, e);
        }
    }

    private void migrateLegacyConfig(CustomConfig legacyConfig) {
        legacyConfig.getConfig().getKeys(false).forEach(enchantName -> {
            File configFile = new File(plugin.getDataFolder(), "enchants" + File.separator + enchantName + ".yml");
            if (configFile.exists()) {
                return;
            }

            configFile.getParentFile().mkdirs();
            var enchantConfig = legacyConfig.getConfig().getConfigurationSection(enchantName);
            if (enchantConfig == null) {
                return;
            }

            var targetConfig = new YamlConfiguration();
            enchantConfig.getValues(true).forEach(targetConfig::set);
            targetConfig.set("enable", enchantConfig.getBoolean("enable", true));

            try {
                targetConfig.save(configFile);
            } catch (IOException e) {
                throw new IllegalStateException("Could not migrate enchant config: " + enchantName, e);
            }
        });
    }

    private void mergeResourceDefaults(String resourcePath) {
        File targetFile = new File(plugin.getDataFolder(), resourcePath.replace('/', File.separatorChar));
        if (!targetFile.exists()) {
            return;
        }

        try (var stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                return;
            }

            var defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            var current = YamlConfiguration.loadConfiguration(targetFile);
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

            var missingPaths = defaults.getKeys(true).stream()
                    .filter(path -> !defaults.isConfigurationSection(path))
                    .filter(path -> !current.contains(path))
                    .toList();

            missingPaths.forEach(path -> current.set(path, defaults.get(path)));
            changed |= !missingPaths.isEmpty();

            if (changed) {
                current.save(targetFile);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not merge config defaults: " + resourcePath, e);
        }
    }
}
