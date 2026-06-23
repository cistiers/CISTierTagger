package ru.kirushkinx.cistiertagger.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record PlayerTierData(
        @NotNull String nickname,
        @NotNull Map<Gamemode, Tier> tiers,
        @Nullable String skinHash,
        @NotNull String renderType
) {

    public @NotNull Optional<Tier> tierFor(@NotNull Gamemode gamemode) {
        return Optional.ofNullable(tiers.get(gamemode));
    }

    public @NotNull Optional<Map.Entry<Gamemode, Tier>> selectByPriority(@NotNull List<Gamemode> priority) {
        for (Gamemode gm : priority) {
            Tier tier = tiers.get(gm);
            if (tier != null) {
                return Optional.of(Map.entry(gm, tier));
            }
        }
        return tiers.entrySet().stream().findFirst();
    }

    public @NotNull Optional<Map.Entry<Gamemode, Tier>> selectHighest() {
        Map.Entry<Gamemode, Tier> best = null;
        for (Map.Entry<Gamemode, Tier> e : tiers.entrySet()) {
            if (e.getValue().isRetired()) continue;
            if (best == null || e.getValue().isBetterThan(best.getValue())) {
                best = e;
            }
        }
        return Optional.ofNullable(best);
    }

    public @NotNull Optional<Map.Entry<Gamemode, Tier>> selectHighest(@NotNull Set<Gamemode> allowed) {
        Map.Entry<Gamemode, Tier> best = null;
        for (Map.Entry<Gamemode, Tier> e : tiers.entrySet()) {
            if (e.getValue().isRetired()) continue;
            if (!allowed.contains(e.getKey())) continue;
            if (best == null || e.getValue().isBetterThan(best.getValue())) {
                best = e;
            }
        }
        return Optional.ofNullable(best);
    }

    public boolean isEmpty() {
        return tiers.isEmpty();
    }

    public static @NotNull PlayerTierData empty(@NotNull String nickname) {
        return new PlayerTierData(nickname, new EnumMap<>(Gamemode.class), null, "default");
    }
}
