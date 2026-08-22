package ru.loper.sunenchants.api.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.bukkit.Material;

@Getter
public class FilterData {
    private final List<Material> filterMaterials;
    private final String player;

    public FilterData(String player) {
        filterMaterials = new ArrayList<>();
        this.player = player;
    }

    public List<Material> getFilterMaterials() {
        return Collections.unmodifiableList(filterMaterials);
    }

    public void addFilterMaterial(Material material) {
        filterMaterials.add(material);
    }

    public void removeFilterMaterial(Material material) {
        filterMaterials.remove(material);
    }

    public void addFilterMaterials(List<Material> materials) {
        filterMaterials.addAll(materials);
    }

    public boolean hasFilterMaterial(Material material) {
        return filterMaterials.contains(material);
    }
}
