package ru.loper.sunenchants.utils;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.loper.suncore.api.colorize.TextFormatter;

@UtilityClass
public class MessageUtils {
    public void send(CommandSender sender, String raw) {
        send(sender, raw, Map.of());
    }

    public void send(CommandSender sender, String raw, Map<String, ?> replacements) {
        if (sender == null || raw == null || raw.isBlank()) {
            return;
        }
        TextFormatter.send(sender, TextFormatter.replace(raw, replacements));
    }

    public void actionBar(Player player, String raw) {
        if (player == null || raw == null || raw.isBlank()) {
            return;
        }

        try {
            player.sendActionBar(TextFormatter.component(player, raw));
        } catch (NoSuchMethodError | AbstractMethodError error) {
            player.sendActionBar(TextFormatter.legacy(player, raw));
        }
    }

    public String renderLegacy(Player player, String raw) {
        return TextFormatter.legacy(player, raw);
    }

    public String toLegacy(String raw) {
        return TextFormatter.legacy(null, raw);
    }
}
