package ru.kirushkinx.cistiertagger.catalog;

import lombok.Getter;
//? if >=1.20.5 {
import net.minecraft.resources.Identifier;
//?}
//? if <1.20.5 {
//import lombok.experimental.UtilityClass;
//?}
import net.minecraft.server.packs.PackResources;
//? if >=1.20.5 {
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.AbstractPackResources;
//?}
import net.minecraft.server.packs.FilePackResources;
//? if >=1.21.4 {
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
//?}
//? if >=1.20.5 <1.21.4 {
/*import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
*///?}

//? if >=1.20.5 {
import java.io.IOException;
import java.io.InputStream;
//?}
import java.nio.file.Path;
//? if >=1.20.5 {
import java.util.LinkedHashMap;
//?}
//? if <1.20.5 {
//import java.util.ArrayList;
//?}
import java.util.List;
import java.util.Objects;
//? if >=1.20.5 {
import java.util.Set;
//?}
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

//? if >=1.20.5 {
public final class CatalogResources extends AbstractPackResources {
//?}
//? if <1.20.5 {
/*@UtilityClass
public class CatalogResources {

*///?}
    public static volatile Path archive;
    @Getter
    private static volatile Path attachedArchive;
    private static PendingReload pendingReload;

//? if >=1.20.5 {
    private final PackResources bundled;
    private final PackResources assets;

//?}
    private record PendingReload(CompletableFuture<Void> result, boolean restoring) {}
//? if >=1.20.5 {

    private CatalogResources(PackResources bundled, Path path) {
        super(bundled.location());
        this.bundled = bundled;
        assets = new FilePackResources.FileResourcesSupplier(path).openPrimary(bundled.location());
    }
//?}

    public static List<PackResources> overlay(List<PackResources> packs) {
        Path path = archive;
        attachedArchive = null;
        if (path == null) return packs;
//? if >=1.20.5 {
        return packs.stream().map(pack -> {
            if (!pack.packId().equals("cistiertagger")) return pack;
            attachedArchive = path;
            return (PackResources) new CatalogResources(pack, path);
        }).toList();
//?}
//? if >=1.20.2 <1.20.5 {
        /*List<PackResources> combined = new ArrayList<>(packs);
        combined.add(new FilePackResources.FileResourcesSupplier(path, false).openPrimary("cistiertagger")); // grouped Fabric mod resources
        attachedArchive = path;
        return List.copyOf(combined);
*///?}
//? if =1.20.1 {
        /*List<PackResources> combined = new ArrayList<>(packs);
        combined.add(new FilePackResources("cistiertagger", path.toFile(), false)); // grouped Fabric mod resources
        attachedArchive = path;
        return List.copyOf(combined);
*///?}
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

//? if >=1.21.4 {
    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        IoSupplier<InputStream> asset = assets.getResource(type, id);
        return asset != null ? asset : bundled.getResource(type, id);
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        var entries = new LinkedHashMap<Identifier, IoSupplier<InputStream>>();
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
//?}
//? if >=1.20.5 <1.21.4 {
    /*@Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        IoSupplier<InputStream> asset = assets.getResource(type, id);
        return asset != null ? asset : bundled.getResource(type, id);
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        var entries = new LinkedHashMap<Identifier, IoSupplier<InputStream>>();
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
    public <T> T getMetadataSection(MetadataSectionSerializer<T> type) throws IOException {
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
*///?}
}
