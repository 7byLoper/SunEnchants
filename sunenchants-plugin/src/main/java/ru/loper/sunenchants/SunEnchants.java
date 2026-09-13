package ru.loper.sunenchants;

import java.io.File;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.loper.suncore.api.command.CommandServices;
import ru.loper.suncore.api.hook.antirelog.AntiRelogHook;
import ru.loper.sunenchants.api.bootstrap.BootstrapState;
import ru.loper.sunenchants.api.registry.EnchantRegistry;
import ru.loper.sunenchants.api.utils.EnchantSnapshotCache;
import ru.loper.sunenchants.commands.enchant.EnchantsCommand;
import ru.loper.sunenchants.commands.filter.SetFilterCommand;
import ru.loper.sunenchants.config.EnchantLimitsConfig;
import ru.loper.sunenchants.config.EnchantsConfigManager;
import ru.loper.sunenchants.config.TranslationsConfigManager;
import ru.loper.sunenchants.legacy.LegacyRegistry;
import ru.loper.sunenchants.listeners.*;
import ru.loper.sunenchants.manager.EnchantsManager;
import ru.loper.sunenchants.manager.FilterDataManager;
import ru.loper.sunenchants.modern.ModernRegistry;
import ru.loper.sunenchants.utils.BulldozerQueue;
import ru.loper.sunenchants.utils.HeldItemCache;

@Getter
public class SunEnchants extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 34031;

    @Getter
    private static SunEnchants instance;

    private final BootstrapState bootstrapState;

    private EnchantLimitsConfig limitsConfig;
    private EnchantsConfigManager configManager;
    private TranslationsConfigManager translationsConfigManager;
    private FilterDataManager filterDataManager;
    private EnchantsManager enchantsManager;
    private EnchantRegistry enchantRegistry;

    private boolean modernRegister;

    public SunEnchants() {
        this(null);
    }

    public SunEnchants(BootstrapState bootstrapState) {
        this.bootstrapState = bootstrapState;
    }

    @Override
    public void onLoad() {
        instance = this;

        ensureDefaultResource("enchants.yml");

        limitsConfig = new EnchantLimitsConfig(this);
        configManager = new EnchantsConfigManager(this);
        enchantRegistry = createRegistry();
        enchantsManager = new EnchantsManager(configManager, this, enchantRegistry, bootstrapState);

        enchantsManager.registerEnchants();
    }

    @Override
    public void onEnable() {
        instance = this;

        printWatermark();
        setupMetrics();

        if (getServer().getPluginManager().isPluginEnabled("AntiRelog")) {
            AntiRelogHook.hook(this);
        }

        translationsConfigManager = new TranslationsConfigManager(this);
        filterDataManager = new FilterDataManager();

        enchantsManager.registerListeners();
        registerListeners();
        registerCommands();
        startCacheTask();
    }

    private void printWatermark() {
        getLogger().info("");
        getLogger().info("  SunEnchants v" + getDescription().getVersion());
        getLogger().info("  Developed by GloomDev - t.me/gloomdev");
        getLogger().info("");
    }

    private void setupMetrics() {
        new Metrics(this, BSTATS_PLUGIN_ID);
    }

    @Override
    public void onDisable() {
        EnchantSnapshotCache.deactivate();
        HeldItemCache.deactivate();
        BulldozerQueue.clear();
    }

    private void startCacheTask() {
        getServer().getScheduler().runTaskTimer(
                this,
                () -> {
                    EnchantSnapshotCache.reset();
                    HeldItemCache.reset();
                    BulldozerQueue.tick();
                },
                1L,
                1L);

        EnchantSnapshotCache.activate();
        HeldItemCache.activate();
    }

    private EnchantRegistry createRegistry() {
        try {
            Class.forName("io.papermc.paper.registry.RegistryAccess");
            modernRegister = true;
            return new ModernRegistry();
        } catch (ClassNotFoundException e) {
            modernRegister = false;
            return new LegacyRegistry();
        }
    }

    private void registerCommands() {
        new EnchantsCommand(this).registerWrappers();

        CommandServices.registrar()
                .registerCommand("setfilter", new SetFilterCommand(translationsConfigManager, filterDataManager), this);
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new EnchantsLimitListener(limitsConfig, enchantsManager), this);
        if (!modernRegister) {
            pluginManager.registerEvents(new AnvilListener(enchantsManager, limitsConfig), this);
        }
        pluginManager.registerEvents(new EnchantListener(enchantsManager), this);
        pluginManager.registerEvents(new EnchantRepairListener(enchantsManager), this);
        pluginManager.registerEvents(new ItemCacheListener(), this);

        if (pluginManager.isPluginEnabled("SunGrindStone")) {
            pluginManager.registerEvents(new SunGrindStoneListener(enchantsManager), this);
        }
    }

    private void ensureDefaultResource(String resourceName) {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder");
        }

        File file = new File(getDataFolder(), resourceName);
        if (!file.exists()) {
            saveResource(resourceName, false);
        }
    }
}
