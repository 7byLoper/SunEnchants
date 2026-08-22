package ru.loper.sunenchants.enchants.tool;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import ru.loper.automine.api.automine.event.MineBlockBreakEvent;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;
import ru.loper.sunenchants.enchants.misc.FilterEnchant;
import ru.loper.sunenchants.manager.EnchantsManager;

@EnchantRegister(name = "magnet")
public class MagnetEnchant extends SEnchant {
    private EnchantsManager enchantsManager;

    public MagnetEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        this.enchantsManager = SunEnchants.getInstance().getEnchantsManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDropItems(BlockDropItemEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (!tool.hasItemMeta() || !isApplied(tool)) return;

        AbstractLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) return;

        Player player = event.getPlayer();
        level.playSounds(player);

        List<ItemStack> items =
                event.getItems().stream().map(Item::getItemStack).collect(Collectors.toList());

        enchantsManager
                .findEnchant("filter", FilterEnchant.class)
                .filter(filter -> filter.isApplied(tool))
                .ifPresent(filter -> filter.filterDrop(items, player));

        event.setCancelled(true);
        dropItems(event.getPlayer(), items);
    }

    @EventHandler
    public void onAutoMineBreak(MineBlockBreakEvent event) {
        ItemStack tool = event.getTool();
        if (!tool.hasItemMeta() || !isApplied(tool)) return;

        AbstractLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) return;

        enchantsManager
                .findEnchant("filter", FilterEnchant.class)
                .filter(filter -> filter.isApplied(tool))
                .ifPresent(filter -> filter.filterDrop(event.getDrop(), event.getPlayer()));

        level.playSounds(event.getPlayer());
        dropItems(event.getPlayer(), event.getDrop());
        event.setDropItems(false);
    }

    public void dropItems(Player player, Collection<ItemStack> items) {
        PlayerInventory inventory = player.getInventory();
        Location location = player.getLocation();

        for (ItemStack item : items) {
            addToInventoryOrDrop(inventory, item, location);
        }
    }

    private void addToInventoryOrDrop(PlayerInventory inventory, ItemStack item, Location location) {
        Map<Integer, ItemStack> leftover = inventory.addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack left : leftover.values()) {
                location.getWorld().dropItemNaturally(location, left);
            }
        }
    }
}
