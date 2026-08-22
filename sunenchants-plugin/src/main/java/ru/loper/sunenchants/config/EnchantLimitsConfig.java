package ru.loper.sunenchants.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.ConfigManager;
import ru.loper.suncore.api.config.CustomConfig;

@Getter
public class EnchantLimitsConfig extends ConfigManager {
    private Map<Enchantment, Integer> enchantmentLevelsLimit;
    private Map<Enchantment, List<Material>> disableEnchants;

    private boolean costScalingEnabled;
    private int costScalingAmount;

    private boolean specialUnbreakingEnabled;
    private int specialUnbreakingChanceBase;
    private int specialUnbreakingChancePerLevel;
    private boolean blockedMending;
    private boolean blockedAnvilRepair;
    private Set<Material> specialUnbreakingMaterials;

    public EnchantLimitsConfig(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        addCustomConfig(new CustomConfig("limits.yml", plugin));
    }

    @Override
    public void loadValues() {
        CustomConfig config = getLimitsConfig();

        costScalingEnabled = config.getConfig().getBoolean("cost_scaling.enable", true);
        costScalingAmount = config.getConfig().getInt("cost_scaling.amount", 30);

        specialUnbreakingEnabled = config.getConfig().getBoolean("special_unbreaking.enable", true);
        specialUnbreakingChanceBase = config.getConfig().getInt("special_unbreaking.chance_base", 47);
        specialUnbreakingChancePerLevel = config.getConfig().getInt("special_unbreaking.chance_per_level", 10);
        blockedMending = config.getConfig().getBoolean("special_unbreaking.blocked_mending", true);
        blockedAnvilRepair = config.getConfig().getBoolean("special_unbreaking.blocked_anvil_repair", true);
        specialUnbreakingMaterials = config.getConfig().getStringList("special_unbreaking.materials").stream()
                .map(this::parseMaterial)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        enchantmentLevelsLimit = new HashMap<>();
        loadLimits(config);

        disableEnchants = new HashMap<>();
        loadDisableEnchants(config);
    }

    private void loadLimits(CustomConfig config) {
        ConfigurationSection limitsSection = config.getConfig().getConfigurationSection("limits");
        if (limitsSection == null) {
            return;
        }

        for (String enchantName : limitsSection.getKeys(false)) {
            Enchantment enchant = getEnchantmentByName(enchantName);
            if (enchant != null) {
                int limit = limitsSection.getInt(enchantName);
                enchantmentLevelsLimit.put(enchant, limit);
            }
        }
    }

    private void loadDisableEnchants(CustomConfig config) {
        ConfigurationSection disableSection = config.getConfig().getConfigurationSection("disable_enchants");
        if (disableSection == null) {
            return;
        }

        for (String enchantName : disableSection.getKeys(false)) {
            Enchantment enchant = getEnchantmentByName(enchantName);
            if (enchant == null) {
                continue;
            }

            List<Material> materialList = disableSection.getStringList(enchantName).stream()
                    .map(this::parseMaterial)
                    .filter(Objects::nonNull)
                    .toList();

            disableEnchants.put(enchant, materialList);
        }
    }

    public CustomConfig getLimitsConfig() {
        return getCustomConfig("limits.yml");
    }

    public int getEnchantmentLimit(Enchantment enchantment) {
        return enchantmentLevelsLimit.getOrDefault(enchantment, enchantment.getMaxLevel());
    }

    public boolean isEnchantDisabled(Enchantment enchantment, Material material) {
        return disableEnchants.getOrDefault(enchantment, List.of()).contains(material);
    }

    public boolean isSpecialUnbreakingItem(Material material) {
        return specialUnbreakingEnabled && specialUnbreakingMaterials.contains(material);
    }

    public int getSpecialUnbreakingChance(int level) {
        return Math.max(0, Math.min(100, specialUnbreakingChanceBase + level * specialUnbreakingChancePerLevel));
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private Enchantment getEnchantmentByName(String name) {
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
        if (enchantment != null) {
            return enchantment;
        }

        enchantment = Enchantment.getByKey(NamespacedKey.fromString(name.toLowerCase()));
        if (enchantment != null) {
            return enchantment;
        }

        return Enchantment.getByName(name.toUpperCase());
    }

    public boolean isDisabled(Enchantment enchant, ItemStack item) {
        return getDisableEnchants().getOrDefault(enchant, List.of()).contains(item.getType());
    }
}
