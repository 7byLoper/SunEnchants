package ru.loper.sunenchants.api.utils;

import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import ru.loper.suncore.api.utilities.VersionHelper;

@UtilityClass
public class EnchantUtils {
    public static final Enchantment UNBREAKING =
            VersionHelper.CURRENT_VERSION >= 1204 ? Enchantment.UNBREAKING : Enchantment.getByName("DURABILITY");

    public static List<Material> parseTools(List<String> tools) {
        return tools.stream()
                .map(Material::getMaterial)
                .filter(Objects::nonNull)
                .toList();
    }
}
