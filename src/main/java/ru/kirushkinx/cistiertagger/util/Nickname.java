package ru.kirushkinx.cistiertagger.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@UtilityClass
public class Nickname {

    public static @NotNull String normalize(@NotNull String nickname) {
        return nickname.toLowerCase(Locale.ROOT);
    }
}
