package ru.kirushkinx.cistiertagger.api.dto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record DumpResponse(
        @NotNull Map<String, RawPlayer> data
) {
    public record RawPlayer(
            @NotNull Map<String, String> tiers,
            @Nullable String skinHash,
            @Nullable String renderType
    ) {}
}
