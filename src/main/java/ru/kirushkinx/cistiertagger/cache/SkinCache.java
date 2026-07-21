package ru.kirushkinx.cistiertagger.cache;

import com.mojang.blaze3d.platform.NativeImage;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.util.Async;
import ru.kirushkinx.cistiertagger.util.Nickname;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;

/** Skins and head icons from mc-heads.net. */
@UtilityClass
public class SkinCache {

    public static final int HEAD_SIZE = 16;

    private static final String BODY_URL = "https://mc-heads.net/body/";
    private static final String HEAD_URL = "https://mc-heads.net/avatar/";
    private static final Duration DISK_TTL = Duration.ofHours(6); // cache-control: public, max-age=21600
    private static final String FALLBACK_PROBE = "00000000-0000-0000-0000-000000000000"; // nil uuid

    private static final @NotNull ConcurrentHashMap<String, BodyEntry> bodies = new ConcurrentHashMap<>();
    private static final @NotNull ConcurrentHashMap<String, HeadEntry> heads = new ConcurrentHashMap<>();
    private static final @NotNull ExecutorService io = Async.daemonExecutor(4, "cistiers-skin");
    private volatile @Nullable CompletableFuture<@Nullable Long> fallbackHeadHash;

    /** Full-body render, pre-1.20.2 only. */
    public static @NotNull AtomicReference<@Nullable ResourceLocation> bodyFor(@NotNull String nickname) {
        String key = Nickname.normalize(nickname);
        BodyEntry entry = bodies.computeIfAbsent(key, k -> new BodyEntry());
        maybeDownloadBody(entry, key, nickname);
        return entry.id;
    }

    /** 16x16 head texture id, or null while still loading. */
    public static @NotNull AtomicReference<@Nullable ResourceLocation> headFor(@NotNull String nickname) {
        String key = Nickname.normalize(nickname);
        HeadEntry entry = heads.computeIfAbsent(key, k -> new HeadEntry());
        maybeDownloadHead(entry, key, nickname);
        return entry.id;
    }

    /** Default skin texture to render via PlayerFaceRenderer while {@link #headFor} is loading. */
    public static @NotNull ResourceLocation defaultSkinFor(@NotNull String nickname) {
        return DefaultPlayerSkin.getDefaultSkin(offlineUuidFor(Nickname.normalize(nickname)));
    }

    private static void maybeDownloadBody(@NotNull BodyEntry entry, @NotNull String key, @NotNull String nickname) {
        if (!entry.dispatched.compareAndSet(false, true)) return;

        Path cachePath = CacheDir.bodies().resolve(key + ".png");
        expireIfStale(cachePath);

        String url = BODY_URL + URLEncoder.encode(nickname, StandardCharsets.UTF_8) + "/right";
        ResourceLocation textureId = new ResourceLocation(CisTierTagger.MOD_ID, "body/" + key);

        CompletableFuture.runAsync(() -> {
            NativeImage image = fetchImage(url, cachePath);
            if (image == null) {
                entry.dispatched.set(false);
                return;
            }
            mc.execute(() -> {
                mc.getTextureManager().register(textureId, new DynamicTexture(image));
                entry.id.set(textureId);
            });
        }, io);
    }

    private static void maybeDownloadHead(@NotNull HeadEntry entry, @NotNull String key, @NotNull String nickname) {
        if (!entry.dispatched.compareAndSet(false, true)) return;

        Path cachePath = CacheDir.heads().resolve(key + ".png");
        expireIfStale(cachePath);

        String url = HEAD_URL + URLEncoder.encode(nickname, StandardCharsets.UTF_8) + "/" + HEAD_SIZE;
        ResourceLocation textureId = new ResourceLocation(CisTierTagger.MOD_ID, "head/" + key);

        CompletableFuture.runAsync(() -> {
            NativeImage image = fetchImage(url, cachePath);
            if (image == null) {
                entry.dispatched.set(false);
                return;
            }
            if (isFallbackHead(image)) {
                image.close();
                deleteQuietly(cachePath);
                return;
            }
            mc.execute(() -> {
                mc.getTextureManager().register(textureId, new DynamicTexture(image));
                entry.id.set(textureId);
            });
        }, io);
    }

    private static @Nullable NativeImage fetchImage(@NotNull String url, @NotNull Path cachePath) {
        try {
            byte[] bytes = Files.exists(cachePath)
                    ? Files.readAllBytes(cachePath)
                    : downloadBytes(url, cachePath);
            return NativeImage.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return null;
        }
    }

    private static byte @NotNull [] downloadBytes(@NotNull String url, @NotNull Path cachePath) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL()
                .openConnection(mc.getProxy());
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        try (InputStream is = conn.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            Files.createDirectories(cachePath.getParent());
            Files.write(cachePath, bytes);
            return bytes;
        }
    }

    private static boolean isFallbackHead(@NotNull NativeImage head) {
        Long fallback = fallbackHeadSignature().join();
        return fallback != null && fallback.equals(pixelHash(head));
    }

    /** Pixel hash of the Steve avatar mc-heads serves for unknown players, captured once. */
    private static @NotNull CompletableFuture<@Nullable Long> fallbackHeadSignature() {
        var local = fallbackHeadHash;
        if (local != null) return local;
        synchronized (SkinCache.class) {
            if (fallbackHeadHash == null) {
                String url = HEAD_URL + FALLBACK_PROBE + "/" + HEAD_SIZE;
                fallbackHeadHash = CompletableFuture.supplyAsync(() -> hashImageBytes(httpGetBytes(url)));
            }
            return fallbackHeadHash;
        }
    }

    private static @Nullable Long hashImageBytes(byte @Nullable [] bytes) {
        if (bytes == null) return null;
        try (NativeImage img = NativeImage.read(new ByteArrayInputStream(bytes))) {
            return pixelHash(img);
        } catch (IOException e) {
            return null;
        }
    }

    private static long pixelHash(@NotNull NativeImage img) {
        long hash = img.getWidth() * 31L + img.getHeight();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                hash = hash * 31 + img.getPixelRGBA(x, y);
            }
        }
        return hash;
    }

    private static byte @Nullable [] httpGetBytes(@NotNull String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL()
                    .openConnection(mc.getProxy());
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(10_000);
            try (InputStream is = conn.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static void deleteQuietly(@NotNull Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private static void expireIfStale(@NotNull Path path) {
        try {
            if (!Files.exists(path)) return;
            FileTime modified = Files.getLastModifiedTime(path);
            if (modified.toMillis() < System.currentTimeMillis() - DISK_TTL.toMillis()) {
                Files.delete(path);
            }
        } catch (IOException ignored) {
        }
    }

    private static @NotNull UUID offlineUuidFor(@NotNull String key) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + key).getBytes(StandardCharsets.UTF_8));
    }

    private static final class BodyEntry {
        final AtomicReference<@Nullable ResourceLocation> id = new AtomicReference<>(null);
        final AtomicBoolean dispatched = new AtomicBoolean(false);
    }

    private static final class HeadEntry {
        final AtomicReference<@Nullable ResourceLocation> id = new AtomicReference<>(null);
        final AtomicBoolean dispatched = new AtomicBoolean(false);
    }
}
