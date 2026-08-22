package ru.loper.sunenchants.enchants.projectile;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.AbstractLevel;

@EnchantRegister(name = "returning")
public class ReturningEnchant extends SEnchant {
    private final Set<UUID> pending = new HashSet<>();
    private FullInventoryMode fullInventoryMode = FullInventoryMode.DROP_NEAR_OWNER;
    private boolean allowCreative;
    private boolean allowAdventure;

    public ReturningEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        allowCreative = section.getBoolean("allow_creative", false);
        allowAdventure = section.getBoolean("allow_adventure", false);
        try {
            fullInventoryMode = FullInventoryMode.valueOf(
                    section.getString("full_inventory", "DROP_NEAR_OWNER").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            fullInventoryMode = FullInventoryMode.DROP_NEAR_OWNER;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident) || !(trident.getShooter() instanceof Player owner)) return;
        if (!isGameModeAllowed(owner.getGameMode())) return;

        ItemStack original = trident.getItemStack();
        int enchantLevel = getAppliedLevel(original);
        AbstractLevel level = getLevel(enchantLevel);
        if (level == null || !level.hasWorkChance() || !pending.add(trident.getUniqueId())) return;

        ItemStack returnedItem = original.clone();
        UUID projectileId = trident.getUniqueId();
        SunEnchants.getInstance().getServer().getScheduler().runTask(SunEnchants.getInstance(), () -> {
            pending.remove(projectileId);
            if (!trident.isValid()) return;

            Location projectileLocation = trident.getLocation();
            trident.remove();

            if (owner.isOnline()) {
                Map<Integer, ItemStack> leftovers = owner.getInventory().addItem(returnedItem);
                if (!leftovers.isEmpty()) {
                    Location dropLocation = fullInventoryMode == FullInventoryMode.DROP_AT_PROJECTILE
                            ? projectileLocation
                            : owner.getLocation();
                    leftovers.values().forEach(item -> dropLocation.getWorld().dropItemNaturally(dropLocation, item));
                }
                level.playSounds(owner);
                return;
            }

            projectileLocation.getWorld().dropItemNaturally(projectileLocation, returnedItem);
        });
    }

    private boolean isGameModeAllowed(GameMode gameMode) {
        return switch (gameMode) {
            case SURVIVAL -> true;
            case CREATIVE -> allowCreative;
            case ADVENTURE -> allowAdventure;
            default -> false;
        };
    }

    private enum FullInventoryMode {
        DROP_NEAR_OWNER,
        DROP_AT_PROJECTILE
    }
}
