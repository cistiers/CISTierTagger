package ru.kirushkinx.cistiertagger;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kirushkinx.cistiertagger.api.CisTiersClient;
import ru.kirushkinx.cistiertagger.api.UpdateChecker;
import ru.kirushkinx.cistiertagger.cache.DumpCache;
import ru.kirushkinx.cistiertagger.command.CisTierCommand;
import ru.kirushkinx.cistiertagger.config.ConfigManager;
import ru.kirushkinx.cistiertagger.network.RestrictionNetworking;

public final class CisTierTagger implements ClientModInitializer {

    public static final @NotNull String MOD_ID = "cistiertagger";
    public static final @NotNull String URL = "https://cistiers.com/";

    @Getter
    private static final @NotNull Logger logger = LoggerFactory.getLogger(CisTierTagger.class);
    public static Minecraft mc;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        DumpCache.init();
        UpdateChecker.checkAsync();

        CisTierCommand.register();
        RestrictionNetworking.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> mc = client);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());

        logger.info("CisTierTagger initialised");
    }

    private void shutdown() {
        DumpCache.shutdown();
        UpdateChecker.shutdown();
        CisTiersClient.shutdown();
        ConfigManager.save();
    }
}
