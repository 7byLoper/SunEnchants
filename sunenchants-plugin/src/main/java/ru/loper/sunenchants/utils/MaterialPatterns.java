package ru.loper.sunenchants.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;

@UtilityClass
public class MaterialPatterns {

    public static List<String> normalize(Collection<String> values) {
        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(value.trim().toUpperCase(Locale.ROOT));
            }
        }
        return List.copyOf(result);
    }

    public static boolean matchesAny(Material material, Collection<String> patterns) {
        if (material == null) {
            return false;
        }

        String name = material.name();
        for (String pattern : patterns) {
            if (matches(name, pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String name, String pattern) {
        if ("*".equals(pattern)) {
            return true;
        }
        if (pattern.startsWith("*") && pattern.endsWith("*") && pattern.length() > 2) {
            return name.contains(pattern.substring(1, pattern.length() - 1));
        }
        if (pattern.startsWith("*")) {
            return name.endsWith(pattern.substring(1));
        }
        if (pattern.endsWith("*")) {
            return name.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return name.equals(pattern);
    }
}
