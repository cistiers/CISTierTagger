package ru.kirushkinx.cistiertagger.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
public class DumpCache {

    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofMinutes(2);

    private final @NotNull CisTiersClient client;
    private final @NotNull PersistentCache disk;
    private final @NotNull ScheduledExecutorService scheduler;

    private final @NotNull AtomicReference<Map<String, PlayerTierData>> store = new AtomicReference<>(Map.of());

    @Getter
    private volatile @NotNull RefreshState state = RefreshState.IDLE;

    @Getter
    private volatile Instant lastRefresh;

    @Getter
    private volatile int lastEntryCount;

    private @org.jetbrains.annotations.Nullable ScheduledFuture<?> refreshTask;

    public DumpCache(@NotNull CisTiersClient client) {
        this.client = client;
        this.disk = new PersistentCache();
        this.scheduler = Async.daemonScheduler(1, "cistiers-cache");
    }

    public synchronized void start(@NotNull Duration refreshInterval) {
        Duration clamped = refreshInterval.compareTo(MIN_REFRESH_INTERVAL) < 0
                ? MIN_REFRESH_INTERVAL : refreshInterval;
        disk.load().ifPresent(dump -> {
            applyDump(dump);
            log.info("Loaded persistent cache: {} players", store.get().size());
        });
        stopRefresh();
        long seconds = Math.max(1, clamped.toSeconds());
        refreshTask = scheduler.scheduleWithFixedDelay(
                this::refreshSilently, 0L, seconds, TimeUnit.SECONDS);
    }

    public synchronized void shutdown() {
        stopRefresh();
        scheduler.shutdownNow();
    }

    public @NotNull Optional<PlayerTierData> lookup(@NotNull String nickname) {
        return Optional.ofNullable(store.get().get(Nickname.normalize(nickname)));
    }

    public boolean contains(@NotNull String nickname) {
        return store.get().containsKey(Nickname.normalize(nickname));
    }

    public int size() {
        return store.get().size();
    }

    public @NotNull List<PlayerTierData> searchByNickname(@NotNull String query) {
        String needle = Nickname.normalize(query);
        return store.get().values().stream()
                .filter(data -> Nickname.normalize(data.nickname()).contains(needle))
                .sorted(Comparator
                        .comparing((PlayerTierData d) -> !Nickname.normalize(d.nickname()).startsWith(needle))
                        .thenComparing(d -> Nickname.normalize(d.nickname())))
                .toList();
    }

    public @NotNull CompletableFuture<Void> refresh() {
        state = RefreshState.REFRESHING;
        return client.fetchDump()
                .thenAccept(this::onDumpReceived)
                .exceptionally(err -> {
                    state = RefreshState.FAILED;
                    log.warn("Dump refresh failed", err);
                    return null;
                });
    }

    private void onDumpReceived(@NotNull DumpResponse dump) {
        applyDump(dump);
        lastRefresh = Instant.now();
        lastEntryCount = store.get().size();
        state = RefreshState.READY;
        log.info("Dump refreshed: {} players", lastEntryCount);
        disk.save(dump);
    }

    private void refreshSilently() {
        try {
            refresh().get(60, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private void applyDump(@NotNull DumpResponse dump) {
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

    private void stopRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
    }

    public enum RefreshState {
        IDLE, REFRESHING, READY, FAILED
    }
}
