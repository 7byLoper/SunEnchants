package ru.loper.sunenchants.enchants.tool;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.api.utils.FarmUtils;

@EnchantRegister(name = "delicate")
public class DelicateEnchant extends SEnchant {

    public DelicateEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {}

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!tool.hasItemMeta() || !isApplied(tool)) {
            return;
        }

        AbstractLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        if (!FarmUtils.isFullyGrownCrops(block.getType()) || FarmUtils.isFullyGrown(block)) {
            return;
        }

        event.setCancelled(true);
        level.playSounds(player);
    }
}
