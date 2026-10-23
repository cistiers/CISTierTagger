package ru.kirushkinx.cistiertagger.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kirushkinx.cistiertagger.catalog.TierCatalog;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@EqualsAndHashCode(of = "id")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Gamemode {
    private static final Map<String, Gamemode> identities = new ConcurrentHashMap<>();
    private static volatile TierCatalog catalog = TierCatalog.empty();
    private final String id;

    public String apiKey() {
        return id;
    }

    public String getDisplayName() {
        return definition().map(TierCatalog.Mode::displayName).orElse(id);
    }

    public String getIconString() {
        return definition().map(TierCatalog.Mode::textIcon).orElse("?");
    }

    public int getColor() {
        return definition().map(mode -> Integer.parseInt(mode.color().substring(1), 16)).orElse(0xFFFFFF);
    }

    public boolean isActive() {
        return definition().map(TierCatalog.Mode::isActive).orElse(false);
    }

    public Optional<TierCatalog.Mode> definition() {
        return Optional.ofNullable(catalog.tierlists().get(id));
    }

    public static TierCatalog catalog() {
        return catalog;
    }

    public static void install(TierCatalog value) {
        catalog = value;
    }

    public static Gamemode[] values() {
        return catalog.tierlists().keySet().stream().map(Gamemode::of).toArray(Gamemode[]::new);
    }

    public static Gamemode of(String key) {
        String id = key.toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9][a-z0-9_]{0,63}")) throw new IllegalArgumentException("Invalid tierlist id");
        return identities.computeIfAbsent(id, Gamemode::new);
    }

    public static @NotNull Optional<Gamemode> fromApiKey(@Nullable String key) {
        if (key == null || !key.toLowerCase(Locale.ROOT).matches("[a-z0-9][a-z0-9_]{0,63}")) return Optional.empty();
        return Optional.of(of(key));
    }
}
