package ru.kirushkinx.cistiertagger.cache;

import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.api.CisTiersClient;
import ru.kirushkinx.cistiertagger.api.dto.ProfileResponse;
import ru.kirushkinx.cistiertagger.util.Nickname;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileCache {

    private static final Duration POSITIVE_TTL = Duration.ofMinutes(5);
    private static final Duration NEGATIVE_TTL = Duration.ofMinutes(15);
    private static final int CACHE_CAPACITY = 256;

    private final @NotNull CisTiersClient client;
    private final @NotNull Map<String, Entry> positives = boundedLru();
    private final @NotNull Map<String, Instant> negatives = boundedLru();
    private final @NotNull Map<String, CompletableFuture<ProfileResponse>> inflight = new ConcurrentHashMap<>();

    public ProfileCache(@NotNull CisTiersClient client) {
        this.client = client;
    }

    private static <V> @NotNull Map<String, V> boundedLru() {
        return Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                return size() > CACHE_CAPACITY;
            }
        });
    }

    public @NotNull CompletableFuture<ProfileResponse> fetch(@NotNull String nickname) {
        String key = Nickname.normalize(nickname);
        Entry cached = positives.get(key);
        if (cached != null && cached.expiresAt.isAfter(Instant.now())) {
            return CompletableFuture.completedFuture(cached.value);
        }
        Instant negativeUntil = negatives.get(key);
        if (negativeUntil != null && negativeUntil.isAfter(Instant.now())) {
            return CompletableFuture.failedFuture(new ProfileNotFoundException(nickname));
        }
        return inflight.computeIfAbsent(key, k -> client.fetchProfile(nickname)
                .whenComplete((response, error) -> {
                    inflight.remove(k);
                    if (error != null) {
                        if (error instanceof CisTiersClient.CisTiersHttpException http && http.isNotFound()) {
                            negatives.put(k, Instant.now().plus(NEGATIVE_TTL));
                        }
                        return;
                    }
                    positives.put(k, new Entry(response, Instant.now().plus(POSITIVE_TTL)));
                    negatives.remove(k);
                }));
    }

    public void invalidate(@NotNull String nickname) {
        String key = Nickname.normalize(nickname);
        positives.remove(key);
        negatives.remove(key);
    }

    private record Entry(@NotNull ProfileResponse value, @NotNull Instant expiresAt) {}

    public static final class ProfileNotFoundException extends RuntimeException {
        public ProfileNotFoundException(@NotNull String nickname) {
            super("Profile not found: " + nickname);
        }
    }
}
