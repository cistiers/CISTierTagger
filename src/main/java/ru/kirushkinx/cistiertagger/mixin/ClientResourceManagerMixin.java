package ru.kirushkinx.cistiertagger.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.kirushkinx.cistiertagger.catalog.CatalogResources;
import ru.kirushkinx.cistiertagger.catalog.CatalogService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableResourceManager.class)
public abstract class ClientResourceManagerMixin {

    @Shadow @Final private PackType type;

    @ModifyVariable(method = "createReload", at = @At("HEAD"), argsOnly = true)
    private List<PackResources> cistiertagger$includeCatalogAssets(List<PackResources> packs) {
        return type == PackType.CLIENT_RESOURCES ? CatalogResources.overlay(packs) : packs;
    }

    @WrapMethod(method = "createReload")
    private ReloadInstance cistiertagger$prepareCatalogAssets(Executor background, Executor game, CompletableFuture<Unit> pending,
                                                List<PackResources> packs, Operation<ReloadInstance> original) {
        if (type != PackType.CLIENT_RESOURCES) return original.call(background, game, pending, packs);

        ReloadInstance result;
        if (CatalogService.getInitialLoad().isDone()) result = original.call(background, game, pending, packs);
        else {
            CompletableFuture<ReloadInstance> reload = CatalogService.getInitialLoad()
                    .thenApplyAsync(unused -> original.call(background, game, pending, packs), game);
            CompletableFuture<?> completion = reload.thenCompose(ReloadInstance::done); // initial reload barrier
            result = new ReloadInstance() {
                @Override
                public CompletableFuture<?> done() {
                    return completion;
                }

                @Override
                public float getActualProgress() {
                    ReloadInstance current = reload.getNow(null);
                    return current == null ? 0f : current.getActualProgress();
                }
            };
        }
        result.done().thenRun(CatalogService::resourcesLoaded);
        return result;
    }
}
