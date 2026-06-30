package ru.kirushkinx.cistiertagger.api;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.api.dto.DumpResponse;
import ru.kirushkinx.cistiertagger.api.dto.LeaderboardEntry;
import ru.kirushkinx.cistiertagger.api.dto.ProfileResponse;
import ru.kirushkinx.cistiertagger.api.dto.SearchResponse;
import ru.kirushkinx.cistiertagger.util.Async;
import ru.kirushkinx.cistiertagger.util.Json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.zip.GZIPInputStream;

@UtilityClass
public class CisTiersClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);
    private static final String USER_AGENT = "CisTierTagger/" + CisTierTagger.MOD_ID;

    private static final ExecutorService executor = Async.daemonExecutor(4, "cistiers-http");
    private static final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .executor(executor)
            .build();

    public static @NotNull CompletableFuture<DumpResponse> fetchDump() {
        return get("api/dump", DumpResponse.class);
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
        return send("api/leaderboard?page=" + page).thenApply(stream -> {
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return Json.GSON.<Map<Integer, List<LeaderboardEntry>>>fromJson(reader, type);
            } catch (IOException | JsonSyntaxException e) {
                throw new CisTiersIoException("Failed to parse leaderboard", e);
            }
        });
    }

    public static void shutdown() {
        executor.shutdownNow();
    }

    private static <T> @NotNull CompletableFuture<T> get(@NotNull String path, @NotNull Class<T> type) {
        return send(path).thenApply(stream -> readJson(stream, type));
    }

    private static @NotNull CompletableFuture<InputStream> send(@NotNull String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(CisTierTagger.URL + path))
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Accept-Encoding", "gzip")
                .header("User-Agent", USER_AGENT)
                .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    int status = response.statusCode();
                    if (status / 100 != 2) {
                        drain(response.body());
                        throw new CisTiersHttpException(status, path);
                    }
                    return wrapGzipIfNeeded(response);
                });
    }

    private static @NotNull InputStream wrapGzipIfNeeded(@NotNull HttpResponse<InputStream> response) {
        boolean gzipped = response.headers()
                .firstValue("Content-Encoding")
                .map(s -> s.equalsIgnoreCase("gzip"))
                .orElse(false);
        InputStream raw = response.body();
        if (!gzipped) return raw;
        try {
            return new GZIPInputStream(raw);
        } catch (IOException e) {
            drain(raw);
            throw new CisTiersIoException("Failed to open gzip stream", e);
        }
    }

    private static <T> T readJson(@NotNull InputStream stream, @NotNull Class<T> type) {
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return Json.GSON.fromJson(reader, type);
        } catch (IOException | JsonSyntaxException e) {
            throw new CisTiersIoException("Failed to parse JSON of " + type.getSimpleName(), e);
        }
    }

    private static void drain(@NotNull InputStream stream) {
        try (stream) {
            stream.transferTo(OutputStream.nullOutputStream());
        } catch (IOException ignored) {
        }
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
