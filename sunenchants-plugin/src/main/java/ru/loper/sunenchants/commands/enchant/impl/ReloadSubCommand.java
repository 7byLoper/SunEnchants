package ru.loper.sunenchants.commands.enchant.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.utils.MessageUtils;

@RequiredArgsConstructor
@SubCommandRegister(permission = "senchants.command.reload", aliases = "reload")
public class ReloadSubCommand implements BuildableCommand {
    private final SunEnchants plugin;

    @Override
    public void handle(@NotNull CommandSender sender, @NotNull String[] args) {
        long start = System.currentTimeMillis();

        plugin.getConfigManager().reloadAll();
        plugin.getTranslationsConfigManager().reloadAll();
        plugin.getLimitsConfig().reloadAll();

        var restartRequired = plugin.getEnchantsManager().reloadEnchants();

        long time = System.currentTimeMillis() - start;

        MessageUtils.send(
                sender,
                plugin.getTranslationsConfigManager()
                        .message("reload_success", "<green>Конфиги перезагружены за {time} мс."),
                java.util.Map.of("time", time));
        if (!restartRequired.isEmpty()) {
            MessageUtils.send(
                    sender,
                    plugin.getTranslationsConfigManager()
                            .message(
                                    "reload_restart_required",
                                    "<yellow>Для синхронизации registry нужен рестарт: {enchants}"),
                    java.util.Map.of("enchants", String.join(", ", restartRequired)));
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender commandSender, @NotNull String[] args) {
        return List.of();
    }
}
