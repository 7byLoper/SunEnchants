package ru.loper.sunenchants.bootstrap;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.registry.event.RegistryEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.bootstrap.BootstrapEnchantConfigLoader;
import ru.loper.sunenchants.api.bootstrap.BootstrapState;
import ru.loper.sunenchants.api.bootstrap.PaperEnchantBootstrapBinder;

public final class SunEnchantsBootstrap implements PluginBootstrap {
    private BootstrapState bootstrapState;

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        bootstrapState = new BootstrapEnchantConfigLoader()
                .loadFromResource(getClass().getClassLoader(), "enchants.yml", context.getDataDirectory());

        context.getLifecycleManager()
                .registerEventHandler(RegistryEvents.ENCHANTMENT
                        .compose()
                        .newHandler(event -> PaperEnchantBootstrapBinder.registerAll(event, bootstrapState)));
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        try {
            Class<?> pluginClass = Class.forName("ru.loper.sunenchants.SunEnchants");
            return (JavaPlugin)
                    pluginClass.getDeclaredConstructor(BootstrapState.class).newInstance(bootstrapState);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create SunEnchants plugin instance", e);
        }
    }
}
