package ru.loper.sunenchants.commands.enchant.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.config.TranslationsConfigManager;
import ru.loper.sunenchants.manager.EnchantsManager;
import ru.loper.sunenchants.utils.MessageUtils;

@SubCommandRegister(permission = "senchants.command.givebook", aliases = "givebook")
public final class GiveSubCommand implements BuildableCommand {
    private final EnchantsManager enchantManager;
    private final TranslationsConfigManager messages;

    public GiveSubCommand(EnchantsManager enchantManager, TranslationsConfigManager messages) {
        this.enchantManager = enchantManager;
        this.messages = messages;
    }

    @Override
    public void handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 4) {
            send(sender, "givebook_usage", Map.of());
            return;
        }

        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) {
            send(sender, "player_offline", Map.of("player", args[1]));
            return;
        }

        SEnchant enchantment = enchantManager.getEnchant(args[2].toLowerCase());
        if (enchantment == null) {
            send(
                    sender,
                    "enchant_not_found",
                    Map.of(
                            "enchant",
                            args[2],
                            "available",
                            String.join(", ", enchantManager.getEnchants().keySet())));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException ignored) {
            send(sender, "invalid_level", Map.of("level", args[3]));
            return;
        }
        if (!enchantment.getEnchantmentLevels().containsKey(level)) {
            send(sender, "enchant_level_unavailable", Map.of("level", level));
            return;
        }

        var leftovers = player.getInventory().addItem(enchantManager.getEnchantBook(enchantment, level));
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        send(
                sender,
                "givebook_success",
                Map.of(
                        "player", player.getName(),
                        "enchant", enchantment.getEnchantName(),
                        "level", level));
    }

    private void send(CommandSender sender, String path, Map<String, ?> replacements) {
        MessageUtils.send(sender, messages.message(path, path), replacements);
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(HumanEntity::getName)
                    .filter(option -> option.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        if (args.length == 3) {
            return enchantManager.getEnchants().keySet().stream()
                    .filter(option -> option.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }
        if (args.length == 4) {
            SEnchant enchantment = enchantManager.getEnchant(args[2].toLowerCase());
            if (enchantment == null) return Collections.emptyList();
            return enchantment.getEnchantmentLevels().keySet().stream()
                    .map(String::valueOf)
                    .filter(option -> option.startsWith(args[3]))
                    .toList();
        }
        return Collections.emptyList();
    }
}
