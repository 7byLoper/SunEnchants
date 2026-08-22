package ru.loper.sunenchants.commands.enchant.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.config.TranslationsConfigManager;
import ru.loper.sunenchants.manager.EnchantsManager;
import ru.loper.sunenchants.utils.MessageUtils;

@SubCommandRegister(permission = "senchants.command.enchant", aliases = "enchant")
public final class EnchantSubCommand implements BuildableCommand {
    private final EnchantsManager enchantManager;
    private final TranslationsConfigManager messages;

    public EnchantSubCommand(EnchantsManager enchantManager, TranslationsConfigManager messages) {
        this.enchantManager = enchantManager;
        this.messages = messages;
    }

    @Override
    public void handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            send(sender, "enchant_usage", Map.of());
            return;
        }
        if (!(sender instanceof Player player)) {
            send(sender, "player_only", Map.of());
            return;
        }

        SEnchant enchantment = enchantManager.getEnchant(args[1].toLowerCase());
        if (enchantment == null) {
            send(
                    sender,
                    "enchant_not_found",
                    Map.of(
                            "enchant",
                            args[1],
                            "available",
                            String.join(", ", enchantManager.getEnchants().keySet())));
            return;
        }
        if (args.length < 3) {
            send(sender, "enchant_level_required", Map.of("enchant", args[1]));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            send(sender, "enchant_air", Map.of());
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException ignored) {
            send(sender, "invalid_level", Map.of("level", args[2]));
            return;
        }

        if (!enchantment.getEnchantmentLevels().containsKey(level)) {
            send(sender, "enchant_level_unavailable", Map.of("level", level));
            return;
        }
        if (!enchantment.canEnchantItem(item)) {
            send(sender, "enchant_item_unsupported", Map.of("enchant", enchantment.getEnchantName()));
            return;
        }

        enchantManager.addEnchant(item, enchantment, level);
        player.updateInventory();
        send(sender, "enchant_success", Map.of("enchant", enchantment.getEnchantName(), "level", level));
    }

    private void send(CommandSender sender, String path, Map<String, ?> replacements) {
        MessageUtils.send(sender, messages.message(path, path), replacements);
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            return enchantManager.getEnchants().keySet().stream()
                    .filter(option -> option.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        if (args.length == 3) {
            SEnchant enchantment = enchantManager.getEnchant(args[1].toLowerCase());
            if (enchantment == null) return Collections.emptyList();
            return enchantment.getEnchantmentLevels().keySet().stream()
                    .map(String::valueOf)
                    .filter(option -> option.startsWith(args[2]))
                    .toList();
        }
        return Collections.emptyList();
    }
}
