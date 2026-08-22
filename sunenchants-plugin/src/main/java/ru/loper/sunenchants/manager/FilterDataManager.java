package ru.loper.sunenchants.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.models.FilterData;

public class FilterDataManager {
    private final CustomConfig dataConfig;
    private final Map<String, FilterData> filtersData;

    public FilterDataManager() {
        dataConfig = new CustomConfig("data/filters.yml", true, SunEnchants.getInstance());
        filtersData = new HashMap<>();
        loadFilters();
    }

    public List<Material> getFilterMaterials(String player) {
        return filtersData.computeIfAbsent(player, k -> new FilterData(player)).getFilterMaterials();
    }

    public void addFilterMaterial(String player, Material material) {
        filtersData.computeIfAbsent(player, k -> new FilterData(player)).addFilterMaterial(material);
    }

    public void removeFilterMaterial(String player, Material material) {
        filtersData.computeIfAbsent(player, k -> new FilterData(player)).removeFilterMaterial(material);
    }

    public boolean hasFilterMaterial(String player, Material material) {
        return filtersData.computeIfAbsent(player, k -> new FilterData(player)).hasFilterMaterial(material);
    }

    public void saveFilters() {
        ConfigurationSection section = dataConfig.getConfig().createSection("filters");
        filtersData.forEach((k, v) -> section.set(
                k, v.getFilterMaterials().stream().map(Material::name).toList()));
        dataConfig.saveConfig();
    }

    private void loadFilters() {
        ConfigurationSection section = dataConfig.getConfig().getConfigurationSection("filters");
        if (section == null) return;

        section.getKeys(false).forEach(key -> {
            List<Material> materials = section.getStringList(key).stream()
                    .map(Material::getMaterial)
                    .filter(Objects::nonNull)
                    .toList();
            FilterData filterData = new FilterData(key);
            filterData.addFilterMaterials(materials);
            filtersData.put(key, filterData);
        });
    }
}
