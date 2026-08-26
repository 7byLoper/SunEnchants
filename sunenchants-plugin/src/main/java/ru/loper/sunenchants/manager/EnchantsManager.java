package ru.loper.sunenchants.manager;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.bootstrap.BootstrapEnchantDefinition;
import ru.loper.sunenchants.api.bootstrap.BootstrapState;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.formatter.impl.PlainEnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.formatter.impl.RomanEnchantLevelFormatter;
import ru.loper.sunenchants.api.registry.EnchantRegistry;
import ru.loper.sunenchants.config.EnchantsConfigManager;
import ru.loper.sunenchants.enchants.combat.ConfigurablePotionEffectEnchant;

@Getter
public class EnchantsManager {
    private final SunEnchants plugin;
    private final EnchantsConfigManager enchantsConfig;
    private final EnchantRegistry registry;
    private final BootstrapState bootstrapState;
    private final EnchantTextFormatter textFormatter;
    private final EnchantLevelFormatter levelFormatter;
    private final Map<String, SEnchant> enchants;
    private final Map<String, SEnchant> registeredEnchants;

    public EnchantsManager(
            EnchantsConfigManager enchantsConfig,
            SunEnchants plugin,
            EnchantRegistry registry,
            BootstrapState bootstrapState) {
        this(
                enchantsConfig,
                plugin,
                registry,
                bootstrapState,
                new PlainEnchantTextFormatter(),
                new RomanEnchantLevelFormatter());
    }

    public EnchantsManager(
            EnchantsConfigManager enchantsConfig,
            SunEnchants plugin,
            EnchantRegistry registry,
            BootstrapState bootstrapState,
            EnchantTextFormatter textFormatter,
            EnchantLevelFormatter levelFormatter) {
        this.plugin = plugin;
        this.enchantsConfig = enchantsConfig;
        this.registry = registry;
        this.bootstrapState = bootstrapState;
        this.textFormatter = textFormatter;
        this.levelFormatter = levelFormatter;
        this.enchants = new LinkedHashMap<>();
        this.registeredEnchants = new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Set<Class<? extends SEnchant>> findAnnotatedClasses() {
        Reflections reflections = new Reflections("ru.loper.sunenchants.enchants", Scanners.TypesAnnotated);
        return reflections.getTypesAnnotatedWith(EnchantRegister.class).stream()
                .filter(SEnchant.class::isAssignableFrom)
                .map(clazz -> (Class<? extends SEnchant>) clazz)
                .collect(Collectors.toSet());
    }

    public void registerListeners() {
        enchants.values().forEach(enchant -> enchant.registerListener(plugin));
    }

    public void registerEnchants() {
        enchants.clear();
        registeredEnchants.clear();

        findAnnotatedClasses().stream()
                .sorted(Comparator.comparing(Class::getName))
                .forEach(enchantClass -> {
                    try {
                        EnchantRegister register = enchantClass.getAnnotation(EnchantRegister.class);
                        if (register == null) {
                            throw new IllegalStateException("Missing @EnchantRegister on " + enchantClass.getName());
                        }

                        String name = register.name();
                        var config = enchantsConfig.getEnchantConfig(name);
                        if (config == null || !config.getConfig().getBoolean("enable", true)) {
                            logSkippedEnchant(name, config);
                            return;
                        }
                        if (isConfigurablePotionEffect(config)) {
                            return;
                        }

                        NamespacedKey namespacedKey = resolveKey(name);
                        SEnchant enchant = createEnchant(enchantClass, namespacedKey);

                        registerEnchant(enchant, config);
                    } catch (Exception e) {
                        plugin.getLogger()
                                .log(Level.SEVERE, "Failed to register enchant: " + enchantClass.getName(), e);
                    }
                });

        enchantsConfig.getCatalogEnchantNames().stream()
                .filter(name -> !registeredEnchants.containsKey(name))
                .forEach(this::registerConfigurableEnchant);
    }

    private void registerConfigurableEnchant(String name) {
        CustomConfig config = enchantsConfig.getEnchantConfig(name);
        if (config == null || !config.getConfig().getBoolean("enable", true)) {
            return;
        }
        if (!isConfigurablePotionEffect(config)) {
            logSkippedEnchant(name, config);
            return;
        }

        try {
            SEnchant enchant = new ConfigurablePotionEffectEnchant(resolveKey(name), textFormatter, levelFormatter, name);
            registerEnchant(enchant, config);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to register configurable enchant: " + name, e);
        }
    }

    private boolean isConfigurablePotionEffect(CustomConfig config) {
        return "potion_effect".equalsIgnoreCase(config.getConfig().getString("type", ""));
    }

    private void registerEnchant(SEnchant enchant, CustomConfig config) {
        enchant.loadValues((ConfigurationSection) config.getConfig());
        registry.register(enchant);
        enchant.setEnabled(true);
        registeredEnchants.put(enchant.getEnchantName(), enchant);
        enchants.put(enchant.getEnchantName(), enchant);
        logLoadedEnchant(enchant, config);
    }

    private SEnchant createEnchant(Class<? extends SEnchant> enchantClass, NamespacedKey key) throws Exception {
        try {
            Constructor<? extends SEnchant> constructor = enchantClass.getConstructor(
                    NamespacedKey.class, EnchantTextFormatter.class, EnchantLevelFormatter.class);
            return constructor.newInstance(key, textFormatter, levelFormatter);
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Constructor<? extends SEnchant> constructor = enchantClass.getConstructor(NamespacedKey.class);
            return constructor.newInstance(key);
        } catch (NoSuchMethodException ignored) {
        }

        throw new NoSuchMethodException("No supported constructor found for " + enchantClass.getName());
    }

    private void logSkippedEnchant(String name, CustomConfig config) {
        if (!enchantsConfig.isDebugEnabled()) {
            return;
        }

        String source = config == null ? "missing" : config.getFile().getAbsolutePath();
        plugin.getLogger().info("[SunEnchants debug] enchant=%s status=skipped source=%s".formatted(name, source));
    }

    private void logLoadedEnchant(SEnchant enchant, CustomConfig config) {
        if (!enchantsConfig.isDebugEnabled()) {
            return;
        }

        var section = config.getConfig().getConfigurationSection("levels");
        String configuredLevels =
                section == null ? "missing" : section.getKeys(false).toString();
        String loadedLevels = enchant.getEnchantmentLevels().keySet().toString();
        plugin.getLogger()
                .info("[SunEnchants debug] enchant=%s source=%s configuredLevels=%s loadedLevels=%s"
                        .formatted(
                                enchant.getEnchantName(),
                                config.getFile().getAbsolutePath(),
                                configuredLevels,
                                loadedLevels));
    }

    private NamespacedKey resolveKey(String enchantName) {
        Optional<BootstrapEnchantDefinition> definition = findBootstrapDefinition(enchantName);
        return definition
                .map(bootstrapEnchantDefinition -> toNamespacedKey(bootstrapEnchantDefinition.getKey()))
                .orElseGet(() -> new NamespacedKey(plugin, normalizeKey(enchantName)));
    }

    private Optional<BootstrapEnchantDefinition> findBootstrapDefinition(String enchantName) {
        if (bootstrapState == null) {
            return Optional.empty();
        }

        return bootstrapState.findByConfigName(enchantName);
    }

    private NamespacedKey toNamespacedKey(Key key) {
        return new NamespacedKey(key.namespace(), key.value());
    }

    private String normalizeKey(String value) {
        return value.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public Map<String, SEnchant> getEnchants() {
        return Collections.unmodifiableMap(enchants);
    }

    public SEnchant getEnchant(String name) {
        return enchants.get(name);
    }

    public boolean hasEnchant(String name, ItemStack item) {
        return Optional.ofNullable(getEnchant(name))
                .map(enchant -> enchant.isApplied(item))
                .orElse(false);
    }

    public <T extends SEnchant> Optional<T> findEnchant(String name, Class<T> type) {
        return Optional.ofNullable(getEnchant(name)).filter(type::isInstance).map(type::cast);
    }

    public Set<String> reloadEnchants() {
        Set<String> catalogNames = enchantsConfig.getCatalogEnchantNames();
        Set<String> restartRequired = new LinkedHashSet<>();
        enchants.clear();

        registeredEnchants.values().forEach(enchant -> {
            String name = enchant.getEnchantName();
            var config = enchantsConfig.getEnchantConfig(name);
            boolean enabled = catalogNames.contains(name)
                    && config != null
                    && config.getConfig().getBoolean("enable", true);

            enchant.setEnabled(enabled);
            if (!enabled) {
                restartRequired.add(name);
                return;
            }

            enchant.loadValues((ConfigurationSection) config.getConfig());
            enchants.put(name, enchant);
        });

        catalogNames.stream()
                .filter(name -> !registeredEnchants.containsKey(name))
                .filter(name -> {
                    var config = enchantsConfig.getEnchantConfig(name);
                    return config == null || config.getConfig().getBoolean("enable", true);
                })
                .forEach(restartRequired::add);

        if (!restartRequired.isEmpty()) {
            plugin.getLogger()
                    .warning("Enchant registry changed after bootstrap. Restart required for: "
                            + String.join(", ", restartRequired));
        }
        return Collections.unmodifiableSet(restartRequired);
    }

    public ItemMeta applyEnchant(ItemMeta meta, SEnchant enchant, int level) {
        if (!plugin.isModernRegister()) {
            List<Component> newLore = new ArrayList<>();
            String enchantName = enchant.getDisplayName().replace("{level}", "");
            List<Component> oldLore = meta.lore();

            if (oldLore != null) {
                oldLore.stream()
                        .filter(line -> !componentToLegacy(line).startsWith(enchantName))
                        .forEach(newLore::add);
            }

            newLore.add(enchant.displayName(level));
            meta.lore(newLore);
        }

        return registry.applyEnchant(meta, enchant, level);
    }

    public ItemStack getEnchantBook(SEnchant enchant, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        addEnchant(book, enchant, level);
        return book;
    }

    public void addEnchant(ItemStack item, SEnchant enchant, int level) {
        if (item == null || enchant == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        if (item.getType() == Material.ENCHANTED_BOOK) {
            meta = registry.addStoredEnchant(meta, enchant, level);
            if (!plugin.isModernRegister()) {
                meta.lore(List.of(enchant.displayName(level)));
            }
        } else {
            meta = applyEnchant(meta, enchant, level);
        }

        item.setItemMeta(meta);
    }

    public void updateBookEnchantLore(EnchantmentStorageMeta meta) {
        if (meta == null || plugin.isModernRegister()) {
            return;
        }

        List<Component> newLore = new ArrayList<>();

        registry.getStoredEnchantments(meta).forEach((enchant, level) -> newLore.add(enchant.displayName(level)));

        appendVanillaLore(meta.lore(), newLore);
        meta.lore(newLore.isEmpty() ? null : newLore);
    }

    public void updateEnchantLore(ItemMeta meta) {
        if (meta == null || plugin.isModernRegister()) {
            return;
        }

        List<Component> newLore = new ArrayList<>();

        registry.getEnchantments(meta).forEach((enchant, level) -> newLore.add(enchant.displayName(level)));

        appendVanillaLore(meta.lore(), newLore);
        meta.lore(newLore.isEmpty() ? null : newLore);
    }

    private void appendVanillaLore(List<Component> oldLore, List<Component> newLore) {
        if (oldLore == null) {
            return;
        }

        oldLore.stream().filter(this::isNotCustomEnchantLore).forEach(newLore::add);
    }

    private boolean isNotCustomEnchantLore(Component line) {
        String legacyLine = componentToLegacy(line);
        return registeredEnchants.values().stream()
                .noneMatch(enchant ->
                        legacyLine.startsWith(enchant.getDisplayName().replace("{level}", "")));
    }

    public String componentToLegacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
