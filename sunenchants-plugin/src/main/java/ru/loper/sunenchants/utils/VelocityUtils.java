package ru.loper.sunenchants.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.util.Vector;

@UtilityClass
public class VelocityUtils {

    public static Vector clamp(Vector vector, double maxLength) {
        if (maxLength <= 0 || vector.lengthSquared() <= maxLength * maxLength) {
            return vector;
        }
        return vector.clone().normalize().multiply(maxLength);
    }
}
