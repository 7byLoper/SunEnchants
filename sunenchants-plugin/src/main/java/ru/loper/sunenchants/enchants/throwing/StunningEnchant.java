package ru.loper.sunenchants.enchants.throwing;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.EffectsLevel;

@EnchantRegister(name = "stunning", level = EnchantLevelType.EFFECTS)
public class StunningEnchant extends SEnchant {

    public StunningEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (!itemStack.hasItemMeta() || !isApplied(itemStack)) {
            return;
        }

        Entity entity = event.getHitEntity();
        if (entity == null || !(event instanceof LivingEntity livingEntity)) {
            return;
        }

        EffectsLevel level = getLevel(itemStack);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        livingEntity.addPotionEffects(level.getEffects());
        level.playSounds(player);
    }
}
