package ru.kirushkinx.cistiertagger.catalog;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.gui.screen.ConfigScreen;
import ru.kirushkinx.cistiertagger.gui.screen.ProfileScreen;
import ru.kirushkinx.cistiertagger.api.HttpTransport;
import ru.kirushkinx.cistiertagger.cache.CacheDir;
import ru.kirushkinx.cistiertagger.cache.CatalogCache;
import ru.kirushkinx.cistiertagger.config.ConfigManager;
import ru.kirushkinx.cistiertagger.decorate.Badge;
import ru.kirushkinx.cistiertagger.model.Gamemode;
import ru.kirushkinx.cistiertagger.util.Async;
import ru.kirushkinx.cistiertagger.util.Json;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class CatalogService {

    private static final Path root = CacheDir.root().resolve("catalog");
    private static final URI CONFIG_URI = URI.create(CisTierTagger.URL + "api/config");
    private static final CatalogCache cache = new CatalogCache();
    private static final ScheduledExecutorService worker = Async.daemonScheduler(1, "cistiers-assets");
    private static volatile CatalogSettings settings = CatalogSettings.defaults();
    @Getter
    private static CompletableFuture<Void> initialLoad = CompletableFuture.completedFuture(null);
    private static final CompletableFuture<Void> resourcesReady = new CompletableFuture<>();
    private static TierCatalog initialCatalog;
    private static Minecraft client;

    public static CatalogSettings settings() {
        return settings;
    }

    public static boolean isAvailable() {
        return !Gamemode.catalog().tierlists().isEmpty();
    }

    public static int refreshSeconds() {
        return settings.refreshSeconds();
    }

    private record Assets(TierCatalog catalog, Path archive) {}

    public static void prepare() {
        initialLoad = CompletableFuture.runAsync(() -> {
            try {
                Path oldCache = root.resolve("http");
                if (Files.exists(oldCache)) removeTree(oldCache);
            } catch (IOException e) {
                log.warn("Cannot remove obsolete HTTP cache", e);
            }
            try {
                Path config = root.resolve("settings.json");
                if (Files.exists(config)) settings = CatalogSettings.parse(Files.readAllBytes(config));
            } catch (Exception e) {
                log.warn("Cannot restore CDN settings", e);
            }
            refreshSettings(true);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10); // separate asset request budget
            try {
                Assets assets = download(true, deadline);
                initialCatalog = assets.catalog();
                CatalogResources.archive = assets.archive();
                return;
            } catch (Exception e) {
                log.warn("Cannot refresh tier assets; trying last working cache", e);
            }
            try {
                TierCatalog saved = TierCatalog.parse(Files.readAllBytes(root.resolve("current.json")));
                Path cached = findArchive(saved);
                if (cached == null) throw new IOException("No working cached archive");
                CatalogResources.archive = cached;
                initialCatalog = saved;
            } catch (Exception e) {
                log.warn("Tier assets unavailable; waiting for a successful refresh", e);
            }
        }, worker);
    }

    public static void start(Minecraft value) {
        client = value;
        resourcesReady.thenRunAsync(() -> {
            if (initialCatalog != null && CatalogResources.archive.equals(CatalogResources.getAttachedArchive())) {
                applyCatalog(initialCatalog);
                worker.execute(() -> persist(initialCatalog));
//            } else {
//                SystemToast.add(client.gui.toastManager(), new SystemToast.SystemToastId(),
//                        Component.translatable("cistiertagger.assets.unavailable.title"),
//                        Component.translatable("cistiertagger.assets.unavailable.description"));
            }
            worker.schedule(CatalogService::update, settings.refreshSeconds(), TimeUnit.SECONDS);
        }, client);
    }

    public static void resourcesLoaded() {
        resourcesReady.complete(null);
    }

    private static void refreshSettings(boolean revalidate) {
        try {
            byte[] config = cache.get(CONFIG_URI, settings.timeoutMs(), CatalogSettings::parse, revalidate);
            settings = CatalogSettings.parse(config);
            var urls = settings.mirrors().stream().map(base -> base.resolve(settings.manifestPath())).collect(Collectors.toSet());
            urls.add(CONFIG_URI);
            cache.retain(urls);
            CatalogCache.write(root.resolve("settings.json"), config);
        } catch (Exception e) {
            log.warn("Using last known CDN settings", e);
        }
    }

    private static Assets download(boolean revalidate, long deadline) throws Exception {
        Exception last = null;
        TierCatalog next = null;
        var mirrors = settings.mirrors();
        for (int i = 0; i < mirrors.size(); i++) {
            URI base = mirrors.get(i);
            try {
                int timeout = requestTimeout(deadline, mirrors.size() - i + 1); // reserve archive request time
                byte[] manifest = cache.get(base.resolve(settings.manifestPath()), timeout, TierCatalog::parse, revalidate);
                next = TierCatalog.parse(manifest);
                break;
            } catch (Exception e) {
                last = e;
            }
        }
        if (next == null) throw new IOException("No valid tier catalog mirror", last);
        Path active = CatalogResources.archive;
        boolean unchanged = active != null && next.version().equals(Gamemode.catalog().version())
                && next.archive().equals(Gamemode.catalog().archive()) && Files.isRegularFile(active)
                && Files.size(active) == next.archive().size();
        return new Assets(next, unchanged ? active : loadArchive(next, deadline));
    }

    private static int requestTimeout(long deadline, int attempts) throws IOException {
        if (deadline == Long.MAX_VALUE) return settings.timeoutMs();
        long remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
        if (remaining <= 0) throw new IOException("Catalog request budget exhausted");
        return (int) Math.min(settings.timeoutMs(), Math.max(1, remaining / attempts));
    }

    private static void update() {
        try {
            refreshSettings(false);
            Assets assets = download(false, Long.MAX_VALUE);
            TierCatalog next = assets.catalog();
            if (assets.archive().equals(CatalogResources.archive)) {
                client.submit(() -> applyCatalog(next)).join();
            } else {
                try {
                    mount(next, assets.archive()).join();
                } catch (Exception e) {
                    if (!assets.archive().equals(CatalogResources.archive)) Files.deleteIfExists(assets.archive());
                    throw e;
                }
            }
            persist(next);
        } catch (Exception e) {
            log.warn("Keeping last working tier assets", e);
        } finally {
            if (!worker.isShutdown()) worker.schedule(CatalogService::update, settings.refreshSeconds(), TimeUnit.SECONDS);
        }
    }

    private static void persist(TierCatalog catalog) {
        try {
            CatalogCache.write(root.resolve("current.json"), Json.GSON.toJson(catalog).getBytes(StandardCharsets.UTF_8));
            prune();
        } catch (IOException e) {
            log.warn("Cannot persist tier catalog", e);
        }
    }

    private static void prune() throws IOException {
        Path active = CatalogResources.archive;
        try (var files = Files.list(root)) {
            for (Path path : files.toList()) {
                String name = path.getFileName().toString();
                if (path.equals(active)) continue;
                if (Files.isDirectory(path) && (name.startsWith("assets-") || name.startsWith("pack-"))) removeTree(path);
                else if (name.matches("[a-f0-9]{64}(-[0-9]+)?\\.zip")) Files.deleteIfExists(path);
            }
        }
    }

    private static Path findArchive(TierCatalog next) throws IOException {
        TierCatalog.Archive archive = next.archive();
        if (!Files.isDirectory(root)) return null;
        try (var files = Files.list(root)) {
            for (Path file : files.filter(path -> matchesArchive(path, archive.sha256())).toList()) {
                try {
                    AssetArchive.validate(file, next);
                    return file;
                } catch (Exception e) {
                    log.debug("Cached archive does not match the requested catalog", e);
                }
            }
        }
        return null;
    }

    private static Path loadArchive(TierCatalog next, long deadline) throws Exception {
        Path cached = findArchive(next);
        if (cached != null) return cached;
        TierCatalog.Archive archive = next.archive();
        Files.createDirectories(root);
        Exception last = null;
        var mirrors = settings.mirrors();
        for (int i = 0; i < mirrors.size(); i++) {
            URI base = mirrors.get(i);
            try {
                var response = HttpTransport.get(base.resolve(archive.path()), requestTimeout(deadline, mirrors.size() - i), AssetArchive.MAX_ARCHIVE, null);
                byte[] bytes = HttpTransport.body(response, AssetArchive.MAX_ARCHIVE);
                AssetArchive.validate(bytes, next);
                Path zip = root.resolve(archive.sha256() + ".zip");
                if (Files.exists(zip)) zip = Files.createTempFile(root, archive.sha256() + "-", ".zip"); // preserve open zip handles
                try {
                    CatalogCache.write(zip, bytes);
                    return zip;
                } catch (Exception e) {
                    Files.deleteIfExists(zip);
                    throw e;
                }
            } catch (Exception e) {
                last = e;
            }
        }
        throw new IOException("No valid asset mirror", last);
    }

    private static boolean matchesArchive(Path path, String hash) {
        String name = path.getFileName().toString();
        return Files.isRegularFile(path) && (name.equals(hash + ".zip") || name.matches(hash + "-[0-9]+\\.zip"));
    }

    private static CompletableFuture<Void> mount(TierCatalog next, Path staged) {
        TierCatalog previous = Gamemode.catalog();
        return CatalogResources.replace(staged, client::reloadResourcePacks, () -> {
            if (!staged.equals(CatalogResources.getAttachedArchive())) {
                throw new IllegalStateException("Catalog archive was not attached");
            }
            applyCatalog(next);
        }, () -> Gamemode.install(previous), client);
    }

    private static void applyCatalog(TierCatalog catalog) {
        Gamemode.install(catalog.inOrder(settings.tierlistOrder()));
        ConfigManager.syncCatalog();
        log.info("Loaded tier catalog {}: {} modes", catalog.version(), catalog.tierlists().size());
        Badge.bumpGeneration();
        if (client.gui.screen() instanceof ConfigScreen screen) screen.onCatalogChanged();
        else if (client.gui.screen() instanceof ProfileScreen screen) screen.onCatalogChanged();
    }

    public static void shutdown() {
        worker.shutdownNow();
    }

    private static void removeTree(Path path) throws IOException {
        try (var files = Files.walk(path)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
        }
    }
}
