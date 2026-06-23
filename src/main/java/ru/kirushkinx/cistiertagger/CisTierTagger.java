package ru.kirushkinx.cistiertagger;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kirushkinx.cistiertagger.api.CisTiersClient;
import ru.kirushkinx.cistiertagger.api.UpdateChecker;
import ru.kirushkinx.cistiertagger.cache.DumpCache;
import ru.kirushkinx.cistiertagger.cache.ProfileCache;
import ru.kirushkinx.cistiertagger.cache.SkinCache;
import ru.kirushkinx.cistiertagger.command.CisTierCommand;
import ru.kirushkinx.cistiertagger.config.ConfigManager;
import ru.kirushkinx.cistiertagger.config.ModConfig;

import java.nio.file.Path;
import java.time.Duration;

public final class CisTierTagger implements ClientModInitializer {

    public static final @NotNull String MOD_ID = "cistiertagger";
    public static final @NotNull String URL = "https://cistiers.com/";
    public static final Duration DUMP_REFRESH = Duration.ofMinutes(10);

    @Getter
    private static final @NotNull Logger logger = LoggerFactory.getLogger(CisTierTagger.class);

    @Getter
    private static CisTiersClient httpClient;

    @Getter
    private static DumpCache dumpCache;

    @Getter
    private static ProfileCache profileCache;

    @Getter
    private static SkinCache skinCache;

    @Getter
    private static ConfigManager configManager;

    @Getter
    private static UpdateChecker updateChecker;

    public static @NotNull ModConfig config() {
        return configManager.get();
    }

    public static @NotNull Path cacheDir() {
        return FabricLoader.getInstance().getGameDir().resolve("cache").resolve(MOD_ID);
    }

    @Override
    public void onInitializeClient() {
        configManager = new ConfigManager();
        configManager.load();

        httpClient = new CisTiersClient();
        dumpCache = new DumpCache(httpClient);
        profileCache = new ProfileCache(httpClient);
        skinCache = new SkinCache();
        updateChecker = new UpdateChecker();

        dumpCache.start(DUMP_REFRESH);
        updateChecker.checkAsync();

        CisTierCommand.register();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());

        logger.info("CisTierTagger initialised");
    }

    private void shutdown() {
        if (dumpCache != null) dumpCache.shutdown();
        if (updateChecker != null) updateChecker.shutdown();
        if (httpClient != null) httpClient.shutdown();
        if (configManager != null) configManager.save();
    }
}
