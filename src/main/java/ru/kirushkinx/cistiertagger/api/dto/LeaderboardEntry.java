package ru.kirushkinx.cistiertagger.api.dto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record LeaderboardEntry(
        @Nullable String userId,
        @NotNull String nickname,
        int points,
        @Nullable Map<String, String> currentTiers,
        @Nullable Map<String, Object> customization,
        @Nullable String skinHash,
        @Nullable String renderType
) {}
