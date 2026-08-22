package ru.loper.sunenchants.enchants.misc;

import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.automine.api.automine.event.MineBlockBreakEvent;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;

@EnchantRegister(name = "filter")
public class FilterEnchant extends SEnchant {
    private final SunEnchants plugin = SunEnchants.getInstance();

    public FilterEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {}

    public void filterDrop(@NonNull Collection<ItemStack> drop, Player player) {
        List<Material> filterMaterials = plugin.getFilterDataManager().getFilterMaterials(player.getName());
        drop.removeIf(item -> filterMaterials.contains(item.getType()));
    }

    public void filterItemDrop(@NonNull Collection<Item> drop, Player player) {
        List<Material> filterMaterials = plugin.getFilterDataManager().getFilterMaterials(player.getName());
        drop.removeIf(item -> filterMaterials.contains(item.getItemStack().getType()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!tool.hasItemMeta() || !isApplied(tool)) {
            return;
        }

        AbstractLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        filterItemDrop(event.getItems(), player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAutoMineBreak(MineBlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = event.getTool();

        if (!tool.hasItemMeta() || !isApplied(tool)) {
            return;
        }

        AbstractLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        filterDrop(event.getDrop(), player);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || event.getEntity() instanceof Player) {
            return;
        }

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!weapon.hasItemMeta() || !isApplied(weapon)) {
            return;
        }

        AbstractLevel level = getLevel(weapon);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        level.playSounds(killer);
        filterDrop(event.getDrops(), killer);
    }
}
