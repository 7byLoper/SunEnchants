package ru.loper.sunenchants.enchants.tool;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.utils.FarmUtils;
import ru.loper.sunenchants.utils.HeldItemCache;

@EnchantRegister(name = "sowing", level = EnchantLevelType.DOUBLE)
public class SowingEnchant extends SEnchant {

    public SowingEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {}

    @EventHandler(ignoreCancelled = true)
    public void onCropsBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        ItemStack tool = HeldItemCache.mainHand(player);

        if (!isApplied(tool)) {
            return;
        }

        Material blockType = block.getType();
        if (!FarmUtils.isFullyGrownCrops(blockType) || !FarmUtils.isFullyGrown(block)) {
            return;
        }

        AbstractLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) {
            return;
        }
        Bukkit.getScheduler()
                .runTaskLater(
                        SunEnchants.getInstance(),
                        () -> {
                            if (!isEnabled()) {
                                return;
                            }

                            Material seedType = FarmUtils.getCropFromSeed(blockType);
                            if (!hasSeedInInventory(player, seedType)) {
                                return;
                            }

                            plantSeed(player, block, seedType, blockType, level);
                        },
                        1L);
    }

    private void plantSeed(Player player, Block block, Material seedType, Material blockType, AbstractLevel level) {
        consumeSeed(player, seedType);

        block.setType(blockType);

        Ageable ageable = (Ageable) block.getBlockData();
        ageable.setAge(0);
        block.setBlockData(ageable);

        level.playSounds(player);
    }

    private void consumeSeed(Player player, Material seedType) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == seedType) {
                item.setAmount(item.getAmount() - 1);
                break;
            }
        }
    }

    private boolean hasSeedInInventory(Player player, Material seedType) {
        PlayerInventory inventory = player.getInventory();

        for (ItemStack item : inventory.getStorageContents()) {
            if (item != null && item.getType() == seedType) {
                return true;
            }
        }

        return false;
    }
}
