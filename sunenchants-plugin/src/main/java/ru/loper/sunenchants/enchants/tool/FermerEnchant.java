package ru.loper.sunenchants.enchants.tool;

import java.util.ArrayList;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.IntLevel;
import ru.loper.sunenchants.utils.HeldItemCache;

@EnchantRegister(name = "fermer", level = EnchantLevelType.INTEGER)
public class FermerEnchant extends SEnchant {

    public FermerEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {}

    public boolean isSeed(Material material) {
        return material == Material.WHEAT_SEEDS
                || material == Material.CARROT
                || material == Material.POTATO
                || material == Material.BEETROOT_SEEDS
                || material == Material.NETHER_WART;
    }

    @EventHandler
    public void onBlockDropItems(BlockDropItemEvent event) {
        ItemStack tool = HeldItemCache.mainHand(event.getPlayer());
        if (!isApplied(tool)) {
            return;
        }

        IntLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        level.playSounds(event.getPlayer());

        for (Item item : new ArrayList<>(event.getItems())) {
            ItemStack drop = item.getItemStack();
            if (!isSeed(drop.getType())) {
                continue;
            }

            int boost = drop.getAmount() / 100 * level.getValue();
            drop.setAmount(Math.max(1, drop.getAmount() + boost));

            item.setItemStack(drop);
        }
    }
}
