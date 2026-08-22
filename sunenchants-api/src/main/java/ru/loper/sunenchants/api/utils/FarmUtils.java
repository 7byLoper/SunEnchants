package ru.loper.sunenchants.api.utils;

import java.util.*;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.block.Block;

@UtilityClass
public class FarmUtils {
    private static final Map<Material, Material> CROP_TO_SEED = new HashMap<>();
    private static final Set<Material> FULLY_GROWN_CROPS = new HashSet<>();

    static {
        CROP_TO_SEED.put(Material.WHEAT, Material.WHEAT_SEEDS);
        CROP_TO_SEED.put(Material.CARROTS, Material.CARROT);
        CROP_TO_SEED.put(Material.POTATOES, Material.POTATO);
        CROP_TO_SEED.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        CROP_TO_SEED.put(Material.NETHER_WART, Material.NETHER_WART);

        FULLY_GROWN_CROPS.addAll(Arrays.asList(
                Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.NETHER_WART, Material.BEETROOTS));
    }

    public static boolean isFullyGrown(Block block) {
        if (block.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
            return ageable.getAge() == ageable.getMaximumAge();
        }

        return false;
    }

    public static boolean isFullyGrownCrops(Material material) {
        return FULLY_GROWN_CROPS.contains(material);
    }

    public static Material getCropFromSeed(Material material) {
        return CROP_TO_SEED.getOrDefault(material, null);
    }
}
