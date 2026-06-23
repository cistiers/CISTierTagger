package ru.kirushkinx.cistiertagger.api.dto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SearchResponse(
        @Nullable String query,
        int offset,
        int limit,
        int count,
        @NotNull List<SearchResultEntry> results
) {}
