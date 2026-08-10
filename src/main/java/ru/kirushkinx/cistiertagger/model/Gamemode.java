package ru.kirushkinx.cistiertagger.model;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Getter
public enum Gamemode {

    VANILLA  ("Vanilla",       "✦",            0xAE8FD6),
    SWORD    ("Sword",         "\uD83D\uDDE1",  0x7DC4E4),
    NETHERITE("Netherite Pot", "☠",            0xD20F39),
    DPOT     ("Diamond Pot",   "⚗",            0xF38BA8),
    UHC      ("UHC",           "❤",            0xF9E2AF),
    SMP      ("SMP",           "⛨",            0xFAB387),
    OP       ("OP",            "☄",            0x8BD5CA),
    MACE     ("Mace",          "\uD83D\uDD28", 0xB4BEFE),
    AXE      ("Axe",           "\uD83E\uDE93", 0x85C1DC);

    private static final Map<String, Gamemode> BY_API_KEY;

    static {
        HashMap<String, Gamemode> map = new HashMap<>();
        for (Gamemode gm : values()) {
            map.put(gm.apiKey(), gm);
        }
        BY_API_KEY = Map.copyOf(map);
    }

    private final @NotNull String displayName;
    private final @NotNull String iconString;
    private final int color;

    Gamemode(@NotNull String displayName, @NotNull String iconString, int color) {
        this.displayName = displayName;
        this.iconString = iconString;
        this.color = color;
    }

    public @NotNull String apiKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static @NotNull Optional<Gamemode> fromApiKey(@Nullable String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(BY_API_KEY.get(key.toLowerCase(Locale.ROOT)));
    }
}
