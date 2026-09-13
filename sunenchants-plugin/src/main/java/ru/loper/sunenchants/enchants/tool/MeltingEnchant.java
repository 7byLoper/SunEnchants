package ru.loper.sunenchants.enchants.tool;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.utils.HeldItemCache;

@EnchantRegister(name = "melting")
public class MeltingEnchant extends SEnchant {
    private final Map<Material, Material> meltingMap = new EnumMap<>(Material.class);
    private SilkTouchMode silkTouchMode = SilkTouchMode.SKIP;

    public MeltingEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        meltingMap.clear();
        ConfigurationSection replaceSection = section.getConfigurationSection("replace");
        if (replaceSection != null) {
            loadMeltingMap(replaceSection);
        }

        try {
            silkTouchMode = SilkTouchMode.valueOf(
                    section.getString("silk_touch", "SKIP").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            silkTouchMode = SilkTouchMode.SKIP;
        }
    }

    private void loadMeltingMap(@NonNull ConfigurationSection replaceSection) {
        replaceSection.getKeys(false).stream()
                .map(key -> new AbstractMap.SimpleImmutableEntry<>(
                        getMaterial(key), getMaterial(replaceSection.getString(key, ""))))
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .forEach(entry -> meltingMap.put(entry.getKey(), entry.getValue()));
    }

    private Material getMaterial(String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockDropItems(BlockDropItemEvent event) {
        ItemStack tool = HeldItemCache.mainHand(event.getPlayer());
        if (!isApplied(tool) || !canMelt(tool)) {
            return;
        }

        AbstractLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        level.playSounds(event.getPlayer());
        for (Item item : new ArrayList<>(event.getItems())) {
            item.setItemStack(meltItem(item.getItemStack()));
        }
    }

    public Collection<ItemStack> meltDrops(Collection<ItemStack> drops, ItemStack tool) {
        if (!canMelt(tool)) {
            return drops;
        }
        drops.forEach(this::meltItem);
        return drops;
    }

    public Collection<ItemStack> meltDrops(Collection<ItemStack> drops) {
        drops.forEach(this::meltItem);
        return drops;
    }

    public ItemStack meltItem(ItemStack item) {
        Material replacement = meltingMap.get(item.getType());
        if (replacement != null) {
            item.setType(replacement);
        }
        return item;
    }

    private boolean canMelt(ItemStack tool) {
        return tool == null || silkTouchMode == SilkTouchMode.MELT || !tool.containsEnchantment(Enchantment.SILK_TOUCH);
    }

    private enum SilkTouchMode {
        SKIP,
        MELT
    }
}
