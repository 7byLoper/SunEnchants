package ru.loper.sunenchants.commands.filter;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.sunenchants.config.TranslationsConfigManager;
import ru.loper.sunenchants.manager.FilterDataManager;
import ru.loper.sunenchants.utils.MessageUtils;

@RequiredArgsConstructor
public class SetFilterCommand implements CommandExecutor, TabCompleter {
    private final TranslationsConfigManager translationsConfig;
    private final FilterDataManager filterDataManager;

    @Override
    public boolean onCommand(
            @NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(commandSender instanceof Player player)) {
            MessageUtils.send(
                    commandSender,
                    translationsConfig.message("player_only", "<red>Эта команда доступна только игрокам."));
            return true;
        }

        if (args.length == 0) {
            MessageUtils.send(player, translationsConfig.getFilterUsageMessage());
            return true;
        }

        String name = String.join(" ", args).toLowerCase();

        Material material = translationsConfig.getMaterialFromTranslation(name);
        if (material == null) {
            MessageUtils.send(player, translationsConfig.getFilterErrorMessage());
            return true;
        }

        if (filterDataManager.hasFilterMaterial(player.getName(), material)) {
            MessageUtils.send(player, translationsConfig.getFilterItemOffMessage(), java.util.Map.of("item", name));
            filterDataManager.removeFilterMaterial(player.getName(), material);
            return true;
        }

        MessageUtils.send(player, translationsConfig.getFilterItemOnMessage(), java.util.Map.of("item", name));
        filterDataManager.addFilterMaterial(player.getName(), material);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        String name = String.join(" ", args).toLowerCase();
        return translationsConfig.getMaterialTranslationsNames().stream()
                .filter(trans -> trans.toLowerCase().startsWith(name))
                .toList();
    }
}
