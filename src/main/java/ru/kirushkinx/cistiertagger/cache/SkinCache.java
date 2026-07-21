package ru.kirushkinx.cistiertagger.cache;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
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
import java.util.function.Supplier;

import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;

/** Skins and head icons from mc-heads.net. */
@UtilityClass
public class SkinCache {

    public static final int HEAD_SIZE = 16;

    private static final String SKIN_URL = "https://mc-heads.net/skin/";
    private static final String HEAD_URL = "https://mc-heads.net/avatar/";
    private static final Duration DISK_TTL = Duration.ofHours(6); // cache-control: public, max-age=21600
    private static final String FALLBACK_PROBE = "00000000-0000-0000-0000-000000000000"; // nil uuid

    private static final @NotNull ConcurrentHashMap<String, SkinEntry> skins = new ConcurrentHashMap<>();
    private static final @NotNull ConcurrentHashMap<String, HeadEntry> heads = new ConcurrentHashMap<>();
    private static final @NotNull ExecutorService io = Async.daemonExecutor(4, "cistiers-skin");
    private volatile @Nullable SkinTextureDownloader downloader;
    private volatile @Nullable CompletableFuture<@Nullable Long> fallbackSkinHash;
    private volatile @Nullable CompletableFuture<@Nullable Long> fallbackHeadHash;

    public static @NotNull AtomicReference<Supplier<PlayerSkin>> forNickname(@NotNull String nickname) {
        if (mc.getConnection() != null) {
            var info = mc.getConnection().getPlayerInfo(nickname);
            if (info != null) {
                return new AtomicReference<>(info::getSkin);
            }
        }
        String key = Nickname.normalize(nickname);
        SkinEntry entry = skins.computeIfAbsent(key, k -> new SkinEntry(offlineDefault(k)));
        maybeDownloadSkin(entry, key, nickname);
        return entry.ref;
    }

    public static @NotNull AtomicReference<Supplier<PlayerSkin>> forClientPlayer() {
        GameProfile profile = mc.getGameProfile();
        if (hasTextures(profile)) {
            return new AtomicReference<>(mc.getSkinManager().createLookup(profile, true));
        }
        return forNickname(profile.name());
    }

    /** 16x16 head texture id, or null while still loading. */
    public static @NotNull AtomicReference<@Nullable ResourceLocation> headFor(@NotNull String nickname) {
        String key = Nickname.normalize(nickname);
        HeadEntry entry = heads.computeIfAbsent(key, k -> new HeadEntry());
        maybeDownloadHead(entry, key, nickname);
        return entry.id;
    }

    /** Default skin to render via PlayerFaceRenderer while {@link #headFor} is loading. */
    public static @NotNull PlayerSkin defaultSkinFor(@NotNull String nickname) {
        return DefaultPlayerSkin.get(offlineUuidFor(Nickname.normalize(nickname)));
    }

    private static void maybeDownloadSkin(@NotNull SkinEntry entry, @NotNull String key, @NotNull String nickname) {
        if (!entry.dispatched.compareAndSet(false, true)) return;

        Path cachePath = CacheDir.skins().resolve(key + ".png");
        expireIfStale(cachePath);

        String url = SKIN_URL + URLEncoder.encode(nickname, StandardCharsets.UTF_8);
        ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath(CisTierTagger.MOD_ID, "skin/" + key);

        getDownloader().downloadAndRegisterSkin(textureId, cachePath, url, true)
                .whenCompleteAsync((texture, err) -> {
                    if (err != null) {
                        entry.dispatched.set(false);
                        return;
                    }
                    if (isFallbackSkin(cachePath)) {
                        deleteQuietly(cachePath);
                        return;
                    }
                    PlayerModelType model = detectModel(cachePath);
                    PlayerSkin skin = new PlayerSkin(texture, null, null, model, false);
                    entry.ref.set(() -> skin);
                }, io);
    }

    /** Detects slim/wide by sampling back-face left-arm pixels (x=46-47, y=52-53). */
    private static @NotNull PlayerModelType detectModel(@NotNull Path cachePath) {
        try (InputStream in = Files.newInputStream(cachePath);
             NativeImage img = NativeImage.read(in)) {
            if (img.getWidth() != 64 || img.getHeight() != 64) return PlayerModelType.WIDE;
            return isTransparent(img, 46, 52) && isTransparent(img, 47, 52)
                    && isTransparent(img, 46, 53) && isTransparent(img, 47, 53)
                    ? PlayerModelType.SLIM
                    : PlayerModelType.WIDE;
        } catch (IOException e) {
            return PlayerModelType.WIDE;
        }
    }

    private static boolean isTransparent(@NotNull NativeImage img, int x, int y) {
        return ((img.getPixel(x, y) >>> 24) & 0xFF) == 0;
    }

    private static @NotNull SkinTextureDownloader getDownloader() {
        SkinTextureDownloader local = downloader;
        if (local != null) return local;
        synchronized (SkinCache.class) {
            if (downloader == null) {
                downloader = new SkinTextureDownloader(mc.getProxy(), mc.getTextureManager(), mc::execute);
            }
            return downloader;
        }
    }

    private static void maybeDownloadHead(@NotNull HeadEntry entry, @NotNull String key, @NotNull String nickname) {
        if (!entry.dispatched.compareAndSet(false, true)) return;

        Path cachePath = CacheDir.heads().resolve(key + ".png");
        expireIfStale(cachePath);

        String url = HEAD_URL + URLEncoder.encode(nickname, StandardCharsets.UTF_8) + "/" + HEAD_SIZE;
        ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath(CisTierTagger.MOD_ID, "head/" + key);

        CompletableFuture.runAsync(() -> {
            NativeImage image = fetchHeadImage(url, cachePath);
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
                mc.getTextureManager().register(textureId, new DynamicTexture(textureId::toString, image));
                entry.id.set(textureId);
            });
        }, io);
    }

    private static @Nullable NativeImage fetchHeadImage(@NotNull String url, @NotNull Path cachePath) {
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

    private static boolean isFallbackSkin(@NotNull Path file) {
        Long fallback = fallbackSkinSignature().join();
        return fallback != null && fallback.equals(hashImageFile(file));
    }

    private static boolean isFallbackHead(@NotNull NativeImage head) {
        Long fallback = fallbackHeadSignature().join();
        return fallback != null && fallback.equals(pixelHash(head));
    }

    /** Pixel hash of the Steve skin mc-heads serves for unknown players, captured once. */
    private static @NotNull CompletableFuture<@Nullable Long> fallbackSkinSignature() {
        var local = fallbackSkinHash;
        if (local != null) return local;
        synchronized (SkinCache.class) {
            if (fallbackSkinHash == null) {
                Path probe = CacheDir.skins().resolve("__cistier_fallback__.png");
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CisTierTagger.MOD_ID, "skin/__cistier_fallback__");
                fallbackSkinHash = getDownloader()
                        .downloadAndRegisterSkin(id, probe, SKIN_URL + FALLBACK_PROBE, true)
                        .handle((tex, err) -> {
                            Long hash = err != null ? null : hashImageFile(probe);
                            deleteQuietly(probe);
                            return hash;
                        });
            }
            return fallbackSkinHash;
        }
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

    private static @Nullable Long hashImageFile(@NotNull Path file) {
        try (InputStream in = Files.newInputStream(file);
             NativeImage img = NativeImage.read(in)) {
            return pixelHash(img);
        } catch (IOException e) {
            return null;
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
                hash = hash * 31 + img.getPixel(x, y);
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

    private static @NotNull Supplier<PlayerSkin> offlineDefault(@NotNull String key) {
        PlayerSkin defaultSkin = DefaultPlayerSkin.get(offlineUuidFor(key));
        return () -> defaultSkin;
    }

    private static @NotNull UUID offlineUuidFor(@NotNull String key) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + key).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean hasTextures(@NotNull GameProfile profile) {
        return profile.properties() != null && !profile.properties().get("textures").isEmpty();
    }

    private static final class SkinEntry {
        final AtomicReference<Supplier<PlayerSkin>> ref;
        final AtomicBoolean dispatched = new AtomicBoolean(false);

        SkinEntry(@NotNull Supplier<PlayerSkin> initial) {
            this.ref = new AtomicReference<>(initial);
        }
    }

    private static final class HeadEntry {
        final AtomicReference<@Nullable ResourceLocation> id = new AtomicReference<>(null);
        final AtomicBoolean dispatched = new AtomicBoolean(false);
    }
}
