package ru.kirushkinx.cistiertagger.api.dto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record SearchResultEntry(
        @NotNull String nickname,
        @Nullable String userId,
        @Nullable String createdAt,
        int totalPoints,
        @Nullable Map<String, String> currentTiers,
        @Nullable Map<String, Object> customization
) {}
