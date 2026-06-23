package ru.kirushkinx.cistiertagger.cache;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kirushkinx.cistiertagger.CisTierTagger;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** Skins and head icons from mc-heads.net. */
public class SkinCache {

    private static final String SKIN_URL = "https://mc-heads.net/skin/";
    private static final String HEAD_URL = "https://mc-heads.net/avatar/";
    private static final int HEAD_SIZE = 16;
    private static final Duration DISK_TTL = Duration.ofHours(6); // cache-control: public, max-age=21600

    private final @NotNull ConcurrentHashMap<String, SkinEntry> skins = new ConcurrentHashMap<>();
    private final @NotNull ConcurrentHashMap<String, HeadEntry> heads = new ConcurrentHashMap<>();
    private volatile @Nullable SkinTextureDownloader downloader;

    public @NotNull AtomicReference<Supplier<PlayerSkin>> forNickname(@NotNull String nickname) {
        Minecraft mc = Minecraft.getInstance();
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

    public @NotNull AtomicReference<Supplier<PlayerSkin>> forClientPlayer() {
        Minecraft mc = Minecraft.getInstance();
        GameProfile profile = mc.getGameProfile();
        if (hasTextures(profile)) {
            return new AtomicReference<>(mc.getSkinManager().createLookup(profile, true));
        }
        return forNickname(profile.name());
    }

    /** 16x16 head texture id, or null while still loading. */
    public @NotNull AtomicReference<@Nullable Identifier> headFor(@NotNull String nickname) {
        String key = Nickname.normalize(nickname);
        HeadEntry entry = heads.computeIfAbsent(key, k -> new HeadEntry());
        maybeDownloadHead(entry, key, nickname);
        return entry.id;
    }

    /** Default skin to render via PlayerFaceRenderer while {@link #headFor} is loading. */
    public @NotNull PlayerSkin defaultSkinFor(@NotNull String nickname) {
        return DefaultPlayerSkin.get(offlineUuidFor(Nickname.normalize(nickname)));
    }

    private void maybeDownloadSkin(@NotNull SkinEntry entry, @NotNull String key, @NotNull String nickname) {
        if (!entry.dispatched.compareAndSet(false, true)) return;

        Path cachePath = CisTierTagger.cacheDir().resolve("skins").resolve(key + ".png");
        expireIfStale(cachePath);

        String url = SKIN_URL + URLEncoder.encode(nickname, StandardCharsets.UTF_8);
        Identifier textureId = Identifier.fromNamespaceAndPath(CisTierTagger.MOD_ID, "skin/" + key);

        Minecraft mc = Minecraft.getInstance();
        getDownloader().downloadAndRegisterSkin(textureId, cachePath, url, true)
                .whenCompleteAsync((texture, err) -> {
                    if (err != null) {
                        entry.dispatched.set(false);
                        return;
                    }
                    PlayerModelType model = detectModel(cachePath);
                    PlayerSkin skin = new PlayerSkin(texture, null, null, model, false);
                    entry.ref.set(() -> skin);
                }, mc::execute);
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

    private @NotNull SkinTextureDownloader getDownloader() {
        SkinTextureDownloader local = downloader;
        if (local != null) return local;
        synchronized (this) {
            if (downloader == null) {
                Minecraft mc = Minecraft.getInstance();
                downloader = new SkinTextureDownloader(mc.getProxy(), mc.getTextureManager(), mc::execute);
            }
            return downloader;
        }
    }

    private void maybeDownloadHead(@NotNull HeadEntry entry, @NotNull String key, @NotNull String nickname) {
        if (!entry.dispatched.compareAndSet(false, true)) return;

        Path cachePath = CisTierTagger.cacheDir().resolve("heads").resolve(key + ".png");
        expireIfStale(cachePath);

        String url = HEAD_URL + URLEncoder.encode(nickname, StandardCharsets.UTF_8) + "/" + HEAD_SIZE;
        Identifier textureId = Identifier.fromNamespaceAndPath(CisTierTagger.MOD_ID, "head/" + key);

        Minecraft mc = Minecraft.getInstance();
        CompletableFuture.supplyAsync(() -> fetchHeadImage(url, cachePath))
                .whenCompleteAsync((image, err) -> {
                    if (err != null || image == null) {
                        entry.dispatched.set(false);
                        return;
                    }
                    mc.getTextureManager().register(textureId, new DynamicTexture(textureId::toString, image));
                    entry.id.set(textureId);
                }, mc::execute);
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
                .openConnection(Minecraft.getInstance().getProxy());
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        try (InputStream is = conn.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            Files.createDirectories(cachePath.getParent());
            Files.write(cachePath, bytes);
            return bytes;
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
        final AtomicReference<@Nullable Identifier> id = new AtomicReference<>(null);
        final AtomicBoolean dispatched = new AtomicBoolean(false);
    }
}
