package ru.kirushkinx.cistiertagger.api;

import com.google.gson.reflect.TypeToken;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.api.dto.DumpResponse;
import ru.kirushkinx.cistiertagger.api.dto.LeaderboardEntry;
import ru.kirushkinx.cistiertagger.api.dto.ProfileResponse;
import ru.kirushkinx.cistiertagger.api.dto.SearchResponse;
import ru.kirushkinx.cistiertagger.catalog.CatalogService;
import ru.kirushkinx.cistiertagger.catalog.CatalogSettings;
import ru.kirushkinx.cistiertagger.util.Async;
import ru.kirushkinx.cistiertagger.util.Json;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@UtilityClass
public class CisTiersClient {

    private static final ExecutorService executor = Async.daemonExecutor(4, "cistiers-http");

    public static CompletableFuture<DumpResponse> fetchDump() {
        return fetchDump(CatalogService.settings());
    }

    static CompletableFuture<DumpResponse> fetchDump(CatalogSettings options) {
        return CompletableFuture.supplyAsync(() -> {
            Exception last = null;
            for (URI uri : options.dumps()) {
                try {
                    var response = HttpTransport.get(uri, options.timeoutMs(), 32 * 1024 * 1024, null);
                    DumpResponse dump = Json.GSON.fromJson(new String(HttpTransport.body(response, 32 * 1024 * 1024), StandardCharsets.UTF_8), DumpResponse.class);
                    if (dump == null || dump.data() == null || dump.data().isEmpty()) throw new IllegalArgumentException("Invalid tier dump");
                    return dump;
                } catch (Exception e) { last = e; }
            }
            throw new CompletionException(last);
        }, executor);
    }

    public static @NotNull CompletableFuture<ProfileResponse> fetchProfile(@NotNull String nickname) {
        return get("api/profile/" + URLEncoder.encode(nickname, StandardCharsets.UTF_8), ProfileResponse.class);
    }

    public static @NotNull CompletableFuture<SearchResponse> searchPlayers(@NotNull String query) {
        return searchPlayers(query, 100);
    }

    public static @NotNull CompletableFuture<SearchResponse> searchPlayers(@NotNull String query, int limit) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return get("api/search/players?q=" + encoded + "&limit=" + limit, SearchResponse.class);
    }

    public static @NotNull CompletableFuture<Map<Integer, List<LeaderboardEntry>>> fetchLeaderboard(int page) {
        Type type = new TypeToken<LinkedHashMap<Integer, List<LeaderboardEntry>>>() {}.getType();
        return send(URI.create(CisTierTagger.URL + "api/leaderboard?page=" + page))
                .thenApply(bytes -> Json.GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), type));
    }

    public static void shutdown() {
        executor.shutdownNow();
    }

    private static <T> @NotNull CompletableFuture<T> get(@NotNull String path, @NotNull Class<T> type) {
        return send(URI.create(CisTierTagger.URL + path))
                .thenApply(bytes -> Json.GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), type));
    }

    private static @NotNull CompletableFuture<byte[]> send(@NotNull URI uri) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var response = HttpTransport.get(uri, 10000, 16 * 1024 * 1024, null);
                return HttpTransport.body(response, 16 * 1024 * 1024);
            } catch (HttpTransport.HttpStatusException e) {
                throw new CisTiersHttpException(e.getStatus(), uri.toString());
            } catch (Exception e) {
                throw new CisTiersIoException("Failed to read " + uri, e);
            }
        }, executor);
    }

    public static final class CisTiersHttpException extends RuntimeException {
        private final int status;
        private final String path;

        public CisTiersHttpException(int status, @NotNull String path) {
            super("CisTiers responded with HTTP " + status + " for " + path);
            this.status = status;
            this.path = path;
        }

        public int status() { return status; }

        public @NotNull String path() { return path; }

        public boolean isNotFound() { return status == 404; }
    }

    public static final class CisTiersIoException extends RuntimeException {
        public CisTiersIoException(@NotNull String message, @NotNull Throwable cause) {
            super(message, cause);
        }
    }
}
