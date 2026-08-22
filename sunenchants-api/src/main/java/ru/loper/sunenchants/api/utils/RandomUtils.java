package ru.loper.sunenchants.api.utils;

import java.util.concurrent.ThreadLocalRandom;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RandomUtils {
    public static int getPercent() {
        return ThreadLocalRandom.current().nextInt(101);
    }
}
