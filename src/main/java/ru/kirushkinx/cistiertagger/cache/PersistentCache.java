package ru.kirushkinx.cistiertagger.cache;

import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.api.dto.DumpResponse;
import ru.kirushkinx.cistiertagger.util.Json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@Slf4j
public class PersistentCache {

    private static final String FILE_NAME = "dump-cache.json";

    private final @NotNull Path file;

    public PersistentCache() {
        this.file = CacheDir.root().resolve(FILE_NAME);
    }

    public @NotNull Optional<DumpResponse> load() {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            DumpResponse parsed = Json.GSON.fromJson(reader, DumpResponse.class);
            return Optional.ofNullable(parsed);
        } catch (IOException | JsonSyntaxException e) {
            log.warn("Failed to load persistent cache from {}", file, e);
            return Optional.empty();
        }
    }

    public void save(@NotNull DumpResponse dump) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(FILE_NAME + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                Json.GSON.toJson(dump, writer);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.warn("Failed to persist dump cache to {}", file, e);
        }
    }
}
