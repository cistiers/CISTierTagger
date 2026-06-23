package ru.kirushkinx.cistiertagger.model;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Getter
public enum Tier {

    HT1(0xF9E2AF, 60),
    LT1(0xCFA565, 45),
    HT2(0xCDD6F4, 30),
    LT2(0xA6ADC8, 20),
    HT3(0xFAB387, 10),
    LT3(0xC68258, 6),
    HT4(0x89B4FA, 4),
    LT4(0x6985C8, 3),
    HT5(0x9399B2, 2),
    LT5(0x6C7086, 1),

    RHT1(Constants.RETIRED_COLOR, 0),
    RLT1(Constants.RETIRED_COLOR, 0),
    RHT2(Constants.RETIRED_COLOR, 0),
    RLT2(Constants.RETIRED_COLOR, 0);

    private static final class Constants {
        private static final int RETIRED_COLOR = 0x81C8BE;
    }

    private static final Map<String, Tier> BY_API_KEY;

    static {
        HashMap<String, Tier> map = new HashMap<>();
        for (Tier tier : values()) {
            map.put(tier.apiKey(), tier);
        }
        BY_API_KEY = Map.copyOf(map);
    }

    private final int color;
    private final int points;

    Tier(int color, int points) {
        this.color = color;
        this.points = points;
    }

    public @NotNull String apiKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public @NotNull String displayName() {
        return name();
    }

    public boolean isHigh() {
        String n = name();
        return n.startsWith("HT") || n.startsWith("RHT");
    }

    public boolean isRetired() {
        return name().startsWith("R");
    }

    public boolean isBetterThan(@NotNull Tier other) {
        return this.points > other.points;
    }

    public static @NotNull Optional<Tier> fromApiKey(@Nullable String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(BY_API_KEY.get(key.toLowerCase(Locale.ROOT)));
    }
}
