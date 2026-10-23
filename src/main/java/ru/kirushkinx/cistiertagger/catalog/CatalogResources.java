package ru.kirushkinx.cistiertagger.catalog;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class CatalogResources extends AbstractPackResources {
    public static volatile Path archive;
    @Getter
    private static volatile Path attachedArchive;
    private static PendingReload pendingReload;

    private final PackResources bundled;
    private final PackResources assets;

    private record PendingReload(CompletableFuture<Void> result, boolean restoring) {}

    private CatalogResources(PackResources bundled, Path path) {
        super(bundled.location());
        this.bundled = bundled;
        assets = new FilePackResources.FileResourcesSupplier(path).openPrimary(bundled.location());
    }

    public static List<PackResources> overlay(List<PackResources> packs) {
        Path path = archive;
        attachedArchive = null;
        if (path == null) return packs;
        return packs.stream().map(pack -> {
            if (!pack.packId().equals("cistiertagger")) return pack;
            attachedArchive = path;
            return (PackResources) new CatalogResources(pack, path);
        }).toList();
    }

    static CompletableFuture<Void> replace(Path staged, Supplier<CompletableFuture<Void>> reload,
                                           Runnable apply, Runnable restore, Executor game) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        game.execute(() -> {
            Path previous = archive;
            archive = staged;
            try {
                reload(reload, false).whenCompleteAsync((unused, error) -> {
                    try {
                        if (error != null) throw new CompletionException(error);
                        apply.run();
                        result.complete(null);
                    } catch (Exception failure) {
                        restore(previous, reload, restore, failure, result);
                    }
                }, game);
            } catch (Exception failure) {
                restore(previous, reload, restore, failure, result);
            }
        });
        return result;
    }

    private static CompletableFuture<Void> reload(Supplier<CompletableFuture<Void>> action, boolean restoring) {
        PendingReload pending = new PendingReload(action.get(), restoring);
        pendingReload = pending;
        pending.result().whenComplete((unused, error) -> {
            if (pendingReload == pending) pendingReload = null;
        });
        return pending.result();
    }

    public static boolean failReload(Throwable error) {
        PendingReload pending = pendingReload;
        if (pending == null || !Objects.equals(archive, attachedArchive)) return false;
        pendingReload = null;
        pending.result().completeExceptionally(error); // finish failed vanilla reload
        return !pending.restoring(); // retain vanilla recovery behavior
    }

    private static void restore(Path previous, Supplier<CompletableFuture<Void>> reload, Runnable restore,
                                Throwable failure, CompletableFuture<Void> result) {
        archive = previous; // restore previous resource set
        try {
            restore.run();
            reload(reload, true).whenComplete((unused, error) -> {
                if (error != null) failure.addSuppressed(error);
                result.completeExceptionally(failure);
            });
        } catch (Exception error) {
            failure.addSuppressed(error);
            result.completeExceptionally(failure);
        }
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        IoSupplier<InputStream> asset = assets.getResource(type, id);
        return asset != null ? asset : bundled.getResource(type, id);
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        var entries = new LinkedHashMap<ResourceLocation, IoSupplier<InputStream>>();
        bundled.listResources(type, namespace, prefix, entries::put);
        assets.listResources(type, namespace, prefix, entries::put);
        entries.forEach(output::accept);
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return bundled.getNamespaces(type);
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        return bundled.getRootResource(path);
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionType<T> type) throws IOException {
        return bundled.getMetadataSection(type);
    }

    @Override
    public void close() {
        try {
            assets.close();
        } finally {
            bundled.close();
        }
    }
}
