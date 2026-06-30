package ru.kirushkinx.cistiertagger.cache;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kirushkinx.cistiertagger.api.CisTiersClient;
import ru.kirushkinx.cistiertagger.api.dto.DumpResponse;
import ru.kirushkinx.cistiertagger.decorate.Badge;
import ru.kirushkinx.cistiertagger.model.Gamemode;
import ru.kirushkinx.cistiertagger.model.PlayerTierData;
import ru.kirushkinx.cistiertagger.model.Tier;
import ru.kirushkinx.cistiertagger.util.Async;
import ru.kirushkinx.cistiertagger.util.Nickname;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@UtilityClass
public class DumpCache {

    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(10);
    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofMinutes(2);

    private static final @NotNull ScheduledExecutorService scheduler = Async.daemonScheduler(1, "cistiers-cache");
    private static final @NotNull PersistentCache disk = new PersistentCache();
    private static final @NotNull AtomicReference<Map<String, PlayerTierData>> store = new AtomicReference<>(Map.of());

    @Getter
    private volatile @NotNull RefreshState state = RefreshState.IDLE;

    @Getter
    private volatile Instant lastRefresh;

    @Getter
    private volatile int lastEntryCount;

    private @Nullable ScheduledFuture<?> refreshTask;

    public static synchronized void init() {
        Duration clamped = REFRESH_INTERVAL.compareTo(MIN_REFRESH_INTERVAL) < 0
                ? MIN_REFRESH_INTERVAL : REFRESH_INTERVAL;
        disk.load().ifPresent(dump -> {
            applyDump(dump);
            log.info("Loaded persistent cache: {} players", store.get().size());
        });
        stopRefresh();
        long seconds = Math.max(1, clamped.toSeconds());
        refreshTask = scheduler.scheduleWithFixedDelay(
                DumpCache::refreshSilently, 0L, seconds, TimeUnit.SECONDS);
    }

    public static synchronized void shutdown() {
        stopRefresh();
        scheduler.shutdownNow();
    }

    public static @NotNull Optional<PlayerTierData> lookup(@NotNull String nickname) {
        return Optional.ofNullable(store.get().get(Nickname.normalize(nickname)));
    }

    public static boolean contains(@NotNull String nickname) {
        return store.get().containsKey(Nickname.normalize(nickname));
    }

    public static int size() {
        return store.get().size();
    }

    public static @NotNull List<PlayerTierData> searchByNickname(@NotNull String query) {
        String needle = Nickname.normalize(query);
        return store.get().values().stream()
                .filter(data -> Nickname.normalize(data.nickname()).contains(needle))
                .sorted(Comparator
                        .comparing((PlayerTierData d) -> !Nickname.normalize(d.nickname()).startsWith(needle))
                        .thenComparing(d -> Nickname.normalize(d.nickname())))
                .toList();
    }

    public static @NotNull CompletableFuture<Void> refresh() {
        state = RefreshState.REFRESHING;
        return CisTiersClient.fetchDump()
                .thenAccept(DumpCache::onDumpReceived)
                .exceptionally(err -> {
                    state = RefreshState.FAILED;
                    log.warn("Dump refresh failed", err);
                    return null;
                });
    }

    private static void onDumpReceived(@NotNull DumpResponse dump) {
        applyDump(dump);
        lastRefresh = Instant.now();
        lastEntryCount = store.get().size();
        state = RefreshState.READY;
        log.info("Dump refreshed: {} players", lastEntryCount);
        disk.save(dump);
    }

    private static void refreshSilently() {
        try {
            refresh().get(60, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private static void applyDump(@NotNull DumpResponse dump) {
        Map<String, PlayerTierData> next = new HashMap<>(Math.max(16, dump.data().size()));
        for (Map.Entry<String, DumpResponse.RawPlayer> entry : dump.data().entrySet()) {
            String nickname = entry.getKey();
            DumpResponse.RawPlayer raw = entry.getValue();
            if (raw == null || raw.tiers() == null) continue;
            Map<Gamemode, Tier> tiers = new EnumMap<>(Gamemode.class);
            for (Map.Entry<String, String> tierEntry : raw.tiers().entrySet()) {
                Gamemode gm = Gamemode.fromApiKey(tierEntry.getKey()).orElse(null);
                Tier tier = Tier.fromApiKey(tierEntry.getValue()).orElse(null);
                if (gm != null && tier != null) {
                    tiers.put(gm, tier);
                }
            }
            next.put(Nickname.normalize(nickname), new PlayerTierData(
                    nickname,
                    tiers,
                    raw.skinHash(),
                    raw.renderType() != null ? raw.renderType() : "default"
            ));
        }
        store.set(Map.copyOf(next));
        Badge.bumpGeneration();
    }

    private static void stopRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
    }

    public enum RefreshState {
        IDLE, REFRESHING, READY, FAILED
    }
}
