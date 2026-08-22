package ru.loper.sunenchants.api.bootstrap;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class BootstrapState {
    private final List<BootstrapEnchantDefinition> definitions;

    public BootstrapState(List<BootstrapEnchantDefinition> definitions) {
        this.definitions = List.copyOf(definitions);
    }

    public List<BootstrapEnchantDefinition> getDefinitions() {
        return Collections.unmodifiableList(definitions);
    }

    public Optional<BootstrapEnchantDefinition> findByConfigName(String configName) {
        return definitions.stream()
                .filter(definition -> definition.getConfigName().equalsIgnoreCase(configName))
                .findFirst();
    }
}
