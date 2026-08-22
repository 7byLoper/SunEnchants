package ru.loper.sunenchants.commands.enchant;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.executor.BaseCommandExecutor;
import ru.loper.suncore.api.command.register.CommandRegister;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.commands.enchant.impl.EnchantSubCommand;
import ru.loper.sunenchants.commands.enchant.impl.GiveSubCommand;
import ru.loper.sunenchants.commands.enchant.impl.ReloadSubCommand;
import ru.loper.sunenchants.config.TranslationsConfigManager;
import ru.loper.sunenchants.utils.MessageUtils;

@CommandRegister(name = "senchants", permission = "senchants.command.use")
public final class EnchantsCommand extends BaseCommandExecutor {
    private final SunEnchants plugin;
    private final TranslationsConfigManager messages;

    public EnchantsCommand(SunEnchants plugin) {
        super(plugin);
        this.plugin = plugin;
        this.messages = plugin.getTranslationsConfigManager();
    }

    @Override
    public void registerWrappers() {
        addSubCommand(new EnchantSubCommand(plugin.getEnchantsManager(), messages));
        addSubCommand(new GiveSubCommand(plugin.getEnchantsManager(), messages));
        addSubCommand(new ReloadSubCommand(plugin));
    }

    @Override
    public String getNoPermissionMessage() {
        return MessageUtils.toLegacy(
                messages.message("no_permission", "<red>У вас нет прав на использование этой команды."));
    }

    @Override
    public void handleNoArguments(@NotNull CommandSender sender) {
        MessageUtils.send(sender, messages.message("admin_usage", "<white>/senchants enchant|givebook|reload"));
    }

    @Override
    public void handleNoSubCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        MessageUtils.send(
                sender, messages.message("unknown_subcommand", "<red>Неизвестная подкоманда. Используйте /senchants."));
    }
}
