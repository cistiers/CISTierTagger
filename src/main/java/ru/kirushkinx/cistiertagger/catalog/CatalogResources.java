package ru.kirushkinx.cistiertagger.catalog;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.FilePackResources;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@UtilityClass
public class CatalogResources {

    public static volatile Path archive;
    @Getter
    private static volatile Path attachedArchive;
    private static PendingReload pendingReload;

    private record PendingReload(CompletableFuture<Void> result, boolean restoring) {}

    public static List<PackResources> overlay(List<PackResources> packs) {
        Path path = archive;
        attachedArchive = null;
        if (path == null) return packs;
        List<PackResources> combined = new ArrayList<>(packs);
        combined.add(new FilePackResources("cistiertagger", path.toFile(), false)); // grouped Fabric mod resources
        attachedArchive = path;
        return List.copyOf(combined);
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

}
