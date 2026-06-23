package ru.kirushkinx.cistiertagger.api.dto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record ProfileResponse(
        @Nullable String type,
        @NotNull String nickname,
        @Nullable Map<String, Object> customization,
        @Nullable String avatarUrl,
        @Nullable TierStats tierStats,
        @Nullable Integer rankPosition,
        @Nullable String id,
        @Nullable String discordId
) {

    public record TierStats(
            int totalPoints,
            @NotNull List<CurrentTier> currentTiers,
            @NotNull List<TierHistoryEntry> tierHistory
    ) {}

    public record CurrentTier(
            @NotNull String kit,
            @NotNull String tier,
            int points
    ) {}

    public record TierHistoryEntry(
            @Nullable String id,
            @NotNull String tier,
            @NotNull String kit,
            @Nullable String comment,
            @NotNull List<Battle> battles,
            @NotNull String date,
            int points
    ) {}

    public record Battle(
            @NotNull String score,
            @Nullable String comment,
            @NotNull String opponent
    ) {}
}
