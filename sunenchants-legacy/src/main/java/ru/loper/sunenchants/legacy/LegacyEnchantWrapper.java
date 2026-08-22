package ru.loper.sunenchants.legacy;

import io.papermc.paper.enchantments.EnchantmentRarity;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.EntityCategory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.api.enchants.SEnchant;

public class LegacyEnchantWrapper extends Enchantment {
    private final SEnchant customEnchant;

    public LegacyEnchantWrapper(SEnchant customEnchant) {
        super(customEnchant.getEnchantKey());
        this.customEnchant = customEnchant;
    }

    @Override
    public @NotNull String getName() {
        return customEnchant.getEnchantName();
    }

    @Override
    public int getMaxLevel() {
        return customEnchant.getMaxLevel();
    }

    @Override
    public int getStartLevel() {
        return 1;
    }

    @Override
    public @NotNull EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.ALL;
    }

    @Override
    public boolean isTreasure() {
        return true;
    }

    @Override
    public boolean isCursed() {
        return customEnchant.isCoursed();
    }

    @Override
    public boolean conflictsWith(@NotNull Enchantment other) {
        return false;
    }

    @Override
    public boolean canEnchantItem(@NotNull ItemStack item) {
        return customEnchant.getAllowedTools() != null
                && customEnchant.getAllowedTools().contains(item.getType());
    }

    @Override
    public @NotNull Component displayName(int level) {
        return customEnchant.displayName(level);
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }

    @Override
    public @NotNull EnchantmentRarity getRarity() {
        return EnchantmentRarity.UNCOMMON;
    }

    @Override
    public float getDamageIncrease(int level, @NotNull EntityCategory entityCategory) {
        return 0;
    }

    @Override
    public @NotNull Set<EquipmentSlot> getActiveSlots() {
        return Set.of();
    }
}
