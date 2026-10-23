package ru.kirushkinx.cistiertagger.catalog;

import com.google.gson.JsonObject;
import ru.kirushkinx.cistiertagger.util.Json;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public record CatalogSettings(List<URI> mirrors, List<URI> dumps, int timeoutMs, int refreshSeconds,
                              String manifestPath, List<String> tierlistOrder, List<String> tierOrder,
                              List<String> retiredTiers, Map<String, Integer> tierPoints) {

    public static CatalogSettings defaults() {
        return new CatalogSettings(
                List.of(URI.create("https://gcore-cdn.cistiers.com/"),
                        URI.create("https://cdn.cistiers.com/")),
                List.of(URI.create("https://gcore-cdn.cistiers.com/api/dump.json"),
                        URI.create("https://cdn.cistiers.com/api/dump.json"),
                        URI.create("https://cistiers.com/api/dump")),
                10000, 300, "tiertagger/v1/manifest.json", List.of(), List.of(), List.of(), Map.of());
    }

    public static CatalogSettings parse(byte[] bytes) {
        JsonObject root = Json.GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
        if (root == null || !root.has("tierlists")) throw new IllegalArgumentException("Invalid site config");
        CatalogSettings fallback = defaults();
        JsonObject cdn = root.has("cdn") ? root.getAsJsonObject("cdn") : new JsonObject();
        JsonObject mod = root.has("tiertagger") ? root.getAsJsonObject("tiertagger") : new JsonObject();
        if (mod.has("schema_version") && mod.get("schema_version").getAsInt() != 1) {
            throw new IllegalArgumentException("Unsupported config schema");
        }
        String path = mod.has("assets") ? mod.getAsJsonObject("assets").get("manifest_path").getAsString() : fallback.manifestPath;
        if (!path.matches("[a-zA-Z0-9_/-]+\\.json") || path.contains("..") || path.startsWith("/")) {
            throw new IllegalArgumentException("Invalid manifest path");
        }
        List<String> order = new ArrayList<>();
        for (var mode : root.getAsJsonArray("tierlists")) {
            String id = mode.getAsJsonObject().get("name").getAsString();
            if (!id.matches("[a-z0-9][a-z0-9_]{0,63}")) throw new IllegalArgumentException("Invalid tier list id");
            if (!order.contains(id)) order.add(id);
        }

        Map<String, Integer> points = new LinkedHashMap<>();
        if (root.has("tier_points")) {
            for (var entry : root.getAsJsonObject("tier_points").entrySet()) {
                if (!entry.getKey().matches("[a-z0-9][a-z0-9_]{0,31}")) throw new IllegalArgumentException("Invalid tier id");
                points.put(entry.getKey(), entry.getValue().getAsInt());
            }
        }

        return new CatalogSettings(urls(cdn, "base_urls", fallback.mirrors, true),
                urls(mod, "dump_urls", fallback.dumps, false),
                bounded(cdn, "request_timeout_ms", 10000, 1000, 10000),
                bounded(mod, "refresh_interval_seconds", 300, 30, 86400), path, List.copyOf(order), tiers(root, "tier_order"), tiers(root, "retired_tiers"), Map.copyOf(points));
    }

    private static List<String> tiers(JsonObject root, String key) {
        if (!root.has(key)) return List.of();
        List<String> result = new ArrayList<>();
        for (var value : root.getAsJsonArray(key)) {
            String id = value.getAsString();
            if (!id.matches("[a-z0-9][a-z0-9_]{0,31}") || result.contains(id)) throw new IllegalArgumentException("Invalid tier order");
            result.add(id);
        }
        return List.copyOf(result);
    }

    private static int bounded(JsonObject value, String key, int fallback, int min, int max) {
        return value.has(key) ? Math.max(min, Math.min(max, value.get(key).getAsInt())) : fallback;
    }

    private static List<URI> urls(JsonObject object, String key, List<URI> fallback, boolean base) {
        if (!object.has(key)) return fallback;
        List<URI> result = new ArrayList<>();
        for (var value : object.getAsJsonArray(key)) {
            String text = value.getAsString();
            if (text.contains("cistiers.b-cdn.net")) text = text.replace("cistiers.b-cdn.net", "gcore-cdn.cistiers.com");
            URI uri = URI.create(base && !text.endsWith("/") ? text + "/" : text);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getFragment() != null || result.size() >= 8) throw new IllegalArgumentException("Invalid CDN URL");
            if (!result.contains(uri)) result.add(uri);
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Empty mirror list");
        URI gcore = URI.create(base ? "https://gcore-cdn.cistiers.com/" : "https://gcore-cdn.cistiers.com/api/dump.json");
        result.remove(gcore);
        result.add(0, gcore);
        return List.copyOf(result);
    }
}
