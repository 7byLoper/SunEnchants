package ru.loper.sunenchants.api.utils;

import java.util.EnumSet;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@UtilityClass
public class ToolUtils {
    private static final Set<Material> PICKAXE_MATERIALS = EnumSet.of(
            Material.WOODEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE);

    private static final Set<Material> AXE_MATERIALS = EnumSet.of(
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE);

    private static final Set<Material> SHOVEL_MATERIALS = EnumSet.of(
            Material.WOODEN_SHOVEL,
            Material.STONE_SHOVEL,
            Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL,
            Material.DIAMOND_SHOVEL,
            Material.NETHERITE_SHOVEL);

    private static final Set<Material> HOE_MATERIALS = EnumSet.of(
            Material.WOODEN_HOE,
            Material.STONE_HOE,
            Material.IRON_HOE,
            Material.GOLDEN_HOE,
            Material.DIAMOND_HOE,
            Material.NETHERITE_HOE);

    private static final Set<String> PICKAXE_KEYWORDS = Set.of(
            "ore",
            "stone",
            "brick",
            "quartz",
            "deepslate",
            "blackstone",
            "obsidian",
            "terracotta",
            "basalt",
            "netherrack",
            "purpur",
            "prismarine",
            "concrete",
            "andesite",
            "diorite",
            "granite",
            "cobblestone",
            "mossy_cobblestone",
            "stone_brick",
            "redstone_lamp",
            "cauldron",
            "anvil",
            "iron_",
            "gold_",
            "diamond_",
            "emerald_",
            "lapis_",
            "coal_block",
            "nether_brick");

    private static final Set<Material> PICKAXE_BLOCKS = Set.of(
            Material.SPAWNER,
            Material.DISPENSER,
            Material.DROPPER,
            Material.FURNACE,
            Material.SMOKER,
            Material.BLAST_FURNACE,
            Material.ANCIENT_DEBRIS);

    private static final Set<String> AXE_KEYWORDS = Set.of(
            "log",
            "wood",
            "plank",
            "stem",
            "hyphae",
            "fence",
            "fence_gate",
            "door",
            "trapdoor",
            "sign",
            "stairs_wood",
            "slab_wood",
            "bookshelf",
            "chest",
            "barrel",
            "crafting_table",
            "jukebox",
            "note_block",
            "ladder");

    private static final Set<Material> AXE_BLOCKS =
            Set.of(Material.PUMPKIN, Material.CARVED_PUMPKIN, Material.JACK_O_LANTERN, Material.MELON);

    private static final Set<String> SHOVEL_KEYWORDS = Set.of(
            "dirt",
            "sand",
            "gravel",
            "clay",
            "snow",
            "podzol",
            "mycelium",
            "soul_sand",
            "soul_soil",
            "farmland",
            "grass_path");

    private static final Set<Material> SHOVEL_BLOCKS = Set.of(Material.GRASS_BLOCK);

    public static boolean isEffectiveTool(Material blockType, ItemStack tool) {
        String blockName = blockType.toString().toLowerCase();

        if (isPickaxe(tool)) {
            return PICKAXE_KEYWORDS.stream().anyMatch(blockName::contains) || PICKAXE_BLOCKS.contains(blockType);
        } else if (isAxe(tool)) {
            return AXE_KEYWORDS.stream().anyMatch(blockName::contains) || AXE_BLOCKS.contains(blockType);
        } else if (isShovel(tool)) {
            return SHOVEL_KEYWORDS.stream().anyMatch(blockName::contains) || SHOVEL_BLOCKS.contains(blockType);
        }
        return false;
    }

    public static boolean isPickaxe(ItemStack tool) {
        return tool != null && PICKAXE_MATERIALS.contains(tool.getType());
    }

    public static boolean isAxe(ItemStack tool) {
        return tool != null && AXE_MATERIALS.contains(tool.getType());
    }

    public static boolean isShovel(ItemStack tool) {
        return tool != null && SHOVEL_MATERIALS.contains(tool.getType());
    }

    public static boolean isHoe(ItemStack tool) {
        return tool != null && HOE_MATERIALS.contains(tool.getType());
    }
}
