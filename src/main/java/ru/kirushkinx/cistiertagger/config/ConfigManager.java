package ru.kirushkinx.cistiertagger.config;

import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.decorate.Badge;
import ru.kirushkinx.cistiertagger.util.Json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
public class ConfigManager {

    private static final String FILE_NAME = "config.json";

    private final @NotNull Path file;

    @Getter
    private @NotNull ModConfig config = new ModConfig();

    public ConfigManager() {
        this.file = FabricLoader.getInstance().getConfigDir()
                .resolve(CisTierTagger.MOD_ID)
                .resolve(FILE_NAME);
    }

    public @NotNull ModConfig get() {
        return config;
    }

    public synchronized void load() {
        if (!Files.exists(file)) {
            log.info("Config not found at {}, using defaults", file);
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ModConfig parsed = Json.PRETTY.fromJson(reader, ModConfig.class);
            if (parsed != null) {
                config = parsed;
            }
        } catch (IOException | JsonSyntaxException e) {
            log.warn("Failed to read config, falling back to defaults", e);
        }
    }

    public synchronized void save() {
        Badge.bumpGeneration();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(FILE_NAME + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                Json.PRETTY.toJson(config, writer);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.warn("Failed to write config to {}", file, e);
        }
    }
}
