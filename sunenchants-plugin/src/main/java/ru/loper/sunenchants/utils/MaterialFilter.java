package ru.loper.sunenchants.utils;

import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import ru.loper.sunenchants.api.utils.ToolUtils;

public final class MaterialFilter {
    private final List<String> blacklist;
    private final FilterRule defaultRule;
    private final FilterRule pickaxeRule;
    private final FilterRule axeRule;
    private final FilterRule shovelRule;
    private final FilterRule hoeRule;

    public MaterialFilter(ConfigurationSection section) {
        blacklist = MaterialPatterns.normalize(section == null ? List.of() : section.getStringList("blacklist"));
        defaultRule = readRule(section == null ? null : section.getConfigurationSection("default"));
        pickaxeRule =
                readRuleOrDefault(section == null ? null : section.getConfigurationSection("pickaxe"), defaultRule);
        axeRule = readRuleOrDefault(section == null ? null : section.getConfigurationSection("axe"), defaultRule);
        shovelRule = readRuleOrDefault(section == null ? null : section.getConfigurationSection("shovel"), defaultRule);
        hoeRule = readRuleOrDefault(section == null ? null : section.getConfigurationSection("hoe"), defaultRule);
    }

    public boolean allows(Material material, ItemStack tool) {
        if (material == null || material.isAir() || MaterialPatterns.matchesAny(material, blacklist)) {
            return false;
        }

        FilterRule rule = resolveRule(tool);
        return switch (rule.mode()) {
            case ALL -> true;
            case EFFECTIVE -> ToolUtils.isEffectiveTool(material, tool);
            case WHITELIST -> MaterialPatterns.matchesAny(material, rule.materials());
        };
    }

    private FilterRule resolveRule(ItemStack tool) {
        if (ToolUtils.isPickaxe(tool)) return pickaxeRule;
        if (ToolUtils.isAxe(tool)) return axeRule;
        if (ToolUtils.isShovel(tool)) return shovelRule;
        if (ToolUtils.isHoe(tool)) return hoeRule;
        return defaultRule;
    }

    private FilterRule readRuleOrDefault(ConfigurationSection section, FilterRule fallback) {
        return section == null ? fallback : readRule(section);
    }

    private FilterRule readRule(ConfigurationSection section) {
        if (section == null) {
            return new FilterRule(FilterMode.EFFECTIVE, List.of());
        }

        FilterMode mode;
        try {
            mode = FilterMode.valueOf(section.getString("mode", "EFFECTIVE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            mode = FilterMode.EFFECTIVE;
        }
        return new FilterRule(mode, MaterialPatterns.normalize(section.getStringList("blocks")));
    }

    private enum FilterMode {
        ALL,
        EFFECTIVE,
        WHITELIST
    }

    private record FilterRule(FilterMode mode, List<String> materials) {}
}
