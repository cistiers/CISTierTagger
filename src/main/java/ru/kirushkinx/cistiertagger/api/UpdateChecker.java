package ru.kirushkinx.cistiertagger.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.config.ConfigManager;
import ru.kirushkinx.cistiertagger.util.Async;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Checking for mod updates by parsing github releases.*/
@UtilityClass
public class UpdateChecker {

    public record Update(@NotNull String version, @NotNull String url) {}

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateChecker.class);
    private static final String RELEASES = "https://api.github.com/repos/cistiers/CISTierTagger/releases";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    private static final Pattern ASSET = Pattern.compile("^" + Pattern.quote(CisTierTagger.MOD_ID) + "-(.+)\\+(.+)\\.jar$");

    private static final ExecutorService executor = Async.daemonExecutor(1, "cistiers-update");
    private static final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .executor(executor)
            .build();

    private volatile @Nullable Update available;

    public static @Nullable Update available() {
        return available;
    }

    public static void checkAsync() {
        if (!ConfigManager.get().isCheckForUpdates()) return; // skip if false
        String[] current = currentVersion();
        if (current == null) return;
        String currentMod = current[0];
        String currentMc = current[1];

        HttpRequest request = HttpRequest.newBuilder(URI.create(RELEASES))
                .GET()
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "CisTierTagger/" + currentMod)
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> findUpdate(response, currentMod, currentMc))
                .whenComplete((update, err) -> {
                    if (err != null) {
                        LOGGER.debug("Update check failed", err);
                        return;
                    }
                    available = update;
                });
    }

    public static void shutdown() {
        executor.shutdownNow();
    }

    private static @Nullable Update findUpdate(@NotNull HttpResponse<InputStream> response, @NotNull String currentMod, @NotNull String currentMc) {
        if (response.statusCode() / 100 != 2) {
            LOGGER.debug("GitHub releases returned HTTP {}", response.statusCode());
            return null;
        }
        try (Reader reader = new InputStreamReader(response.body(), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!(root instanceof JsonArray releases)) return null;

            String bestMod = currentMod;
            String bestUrl = null;
            for (JsonElement el : releases) {
                if (!(el instanceof JsonObject release)) continue;
                if (boolField(release, "draft") || boolField(release, "prerelease")) continue;

                String url = release.has("html_url") ? release.get("html_url").getAsString() : null;
                JsonElement assetsEl = release.get("assets");
                if (url == null || !(assetsEl instanceof JsonArray assets)) continue;

                for (JsonElement assetEl : assets) {
                    if (!(assetEl instanceof JsonObject asset) || !asset.has("name")) continue;
                    Matcher m = ASSET.matcher(asset.get("name").getAsString());
                    if (!m.matches() || !m.group(2).equals(currentMc)) continue;
                    if (compare(m.group(1), bestMod) > 0) {
                        bestMod = m.group(1);
                        bestUrl = url;
                    }
                }
            }
            return bestUrl == null ? null : new Update(bestMod, bestUrl);
        } catch (Exception e) {
            LOGGER.debug("Failed to parse GitHub releases", e);
            return null;
        }
    }

    private static boolean boolField(@NotNull JsonObject obj, @NotNull String name) {
        return obj.has(name) && obj.get(name).getAsBoolean();
    }

    private static @Nullable String[] currentVersion() {
        String full = FabricLoader.getInstance().getModContainer(CisTierTagger.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
        if (full == null) return null;
        int plus = full.indexOf('+');
        if (plus <= 0 || plus == full.length() - 1) return null;
        return new String[]{full.substring(0, plus), full.substring(plus + 1)};
    }

    private static int compare(@NotNull String a, @NotNull String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int x = i < pa.length ? digits(pa[i]) : 0;
            int y = i < pb.length ? digits(pb[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    private static int digits(@NotNull String segment) {
        int value = 0;
        for (int i = 0; i < segment.length() && Character.isDigit(segment.charAt(i)); i++) {
            value = value * 10 + (segment.charAt(i) - '0');
        }
        return value;
    }
}
