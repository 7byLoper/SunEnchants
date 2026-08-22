package ru.loper.sunenchants.api.enchants;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;

@Retention(value = RetentionPolicy.RUNTIME)
public @interface EnchantRegister {
    String name();

    EnchantLevelType level() default EnchantLevelType.DEFAULT;
}
