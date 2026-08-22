package ru.loper.sunenchants.config;

import java.util.*;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.ConfigManager;
import ru.loper.suncore.api.config.CustomConfig;

public class TranslationsConfigManager extends ConfigManager {
    private Map<Integer, String> romanTranslations;
    private Map<Material, String> materialTranslations;

    @Getter
    private String filterItemOnMessage, filterItemOffMessage, filterUsageMessage, filterErrorMessage;

    public TranslationsConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        addCustomConfig(new CustomConfig("translations.yml", plugin));
        addCustomConfig(new CustomConfig("messages.yml", plugin));
    }

    @Override
    public void loadValues() {
        romanTranslations = new HashMap<>();
        loadRomanTranslations();
        materialTranslations = new EnumMap<>(Material.class);
        loadMaterialTranslations();

        filterItemOnMessage = message("filter_item_on", "");
        filterItemOffMessage = message("filter_item_off", "");
        filterUsageMessage = message("filter_usage", "/setfilter <предмет>");
        filterErrorMessage = message("filter_error_material", "Указанный материал не поддерживается.");
    }

    private void loadRomanTranslations() {
        ConfigurationSection translationSection =
                getTranslationsConfig().getConfig().getConfigurationSection("roman_symbols");
        if (translationSection == null) return;

        for (String key : translationSection.getKeys(false)) {
            int number = getInteger(key);
            if (number == -1) continue;

            romanTranslations.put(number, translationSection.getString(key));
        }
    }

    private void loadMaterialTranslations() {
        ConfigurationSection translationSection =
                getTranslationsConfig().getConfig().getConfigurationSection("material_translations");
        if (translationSection == null) return;

        for (String key : translationSection.getKeys(false)) {
            Material material = getMaterial(key);
            if (material == null) continue;

            materialTranslations.put(material, translationSection.getString(key));
        }
    }

    private int getInteger(String number) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getMaterialTranslation(Material material) {
        return materialTranslations.getOrDefault(material, "");
    }

    public Material getMaterialFromTranslation(String translation) {
        return materialTranslations.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(translation))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public String getRomanSymbol(int number) {
        return romanTranslations.getOrDefault(number, "");
    }

    private Material getMaterial(String material) {
        try {
            return Material.valueOf(material);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private CustomConfig getTranslationsConfig() {
        return getCustomConfig("translations.yml");
    }

    private CustomConfig getMessagesConfig() {
        return getCustomConfig("messages.yml");
    }

    public Collection<String> getMaterialTranslationsNames() {
        return Collections.unmodifiableCollection(materialTranslations.values());
    }

    public String message(String path, String fallback) {
        String value = getMessagesConfig().getConfig().getString(path);
        return value == null ? fallback : value;
    }
}
