package ru.loper.sunenchants.api.enchants;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.utils.EnchantUtils;

@Getter
@Setter(AccessLevel.PROTECTED)
public abstract class SEnchant implements Listener {
    private final NamespacedKey enchantKey;
    private final EnchantLevelType levelType;
    private final String enchantName;
    private final EnchantTextFormatter textFormatter;
    private final EnchantLevelFormatter levelFormatter;

    private String displayName;
    private boolean coursed;
    private boolean enabled = true;
    private int maxLevel;
    private List<Material> allowedTools = Collections.emptyList();
    private Map<Integer, AbstractLevel> enchantmentLevels = Collections.emptyMap();

    @Setter
    private Enchantment bukkitEnchantment;

    protected SEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        this.enchantKey = namespacedKey;
        this.textFormatter = textFormatter;
        this.levelFormatter = levelFormatter;

        EnchantRegister register = getRegister(getClass());
        this.enchantName = register.name();
        this.levelType = register.level();
    }

    protected SEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter,
            @NotNull String enchantName) {
        this.enchantKey = namespacedKey;
        this.textFormatter = textFormatter;
        this.levelFormatter = levelFormatter;
        this.enchantName = enchantName;
        this.levelType = EnchantLevelType.DEFAULT;
    }

    private static EnchantRegister getRegister(Class<?> type) {
        EnchantRegister register = type.getAnnotation(EnchantRegister.class);
        if (register == null) {
            throw new IllegalStateException("Missing @EnchantRegister on " + type.getName());
        }
        return register;
    }

    public void registerListener(@NotNull Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public boolean canEnchantItem(@NotNull ItemStack item) {
        return enabled && allowedTools != null && allowedTools.contains(item.getType());
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        onEnabledStateChanged(enabled);
    }

    protected void onEnabledStateChanged(boolean enabled) {}

    public void loadValues(@NotNull FileConfiguration config) {
        ConfigurationSection enchantSection = config.getConfigurationSection(enchantName);
        if (enchantSection == null) {
            return;
        }

        loadValues(enchantSection);
    }

    public void loadValues(@NotNull ConfigurationSection section) {
        parseConfig(section);
    }

    public void parseConfig(@NotNull ConfigurationSection section) {
        this.displayName = textFormatter.format(section.getString("display_name", enchantName));
        this.maxLevel = section.getInt("max_level");
        this.allowedTools = EnchantUtils.parseTools(section.getStringList("target"));
        this.enchantmentLevels = levelType.getEnchantLevels(section.getConfigurationSection("levels"));
        this.coursed = section.getBoolean("cursed", section.getBoolean("coursed", false));

        parseValues(section);
    }

    protected abstract void parseValues(@NotNull ConfigurationSection section);

    public boolean isApplied(@Nullable ItemStack itemStack) {
        if (!enabled || itemStack == null || !itemStack.hasItemMeta() || bukkitEnchantment == null) {
            return false;
        }

        return itemStack.containsEnchantment(bukkitEnchantment);
    }

    public int getAppliedLevel(@Nullable ItemStack itemStack) {
        if (!enabled || itemStack == null || !itemStack.hasItemMeta() || bukkitEnchantment == null) {
            return 0;
        }

        return itemStack.getEnchantmentLevel(bukkitEnchantment);
    }

    @Nullable
    public <T extends AbstractLevel> T getLevel(@NotNull ItemStack itemStack) {
        if (!enabled || bukkitEnchantment == null) {
            return null;
        }

        int level = itemStack.getEnchantmentLevel(bukkitEnchantment);
        return getLevel(level);
    }

    @Nullable
    public <T extends AbstractLevel> T getLevel(int level) {
        if (level <= 0 || enchantmentLevels == null || !enchantmentLevels.containsKey(level)) {
            return null;
        }

        return castLevel(enchantmentLevels.get(level));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T extends AbstractLevel> T castLevel(@Nullable AbstractLevel abstractLevel) {
        if (abstractLevel == null) {
            return null;
        }

        try {
            return (T) abstractLevel;
        } catch (ClassCastException ignored) {
            return null;
        }
    }

    public @NotNull Component displayName(int level) {
        String formattedLevel = levelFormatter.format(level);
        return Component.text(displayName.replace("{level}", formattedLevel));
    }
}
