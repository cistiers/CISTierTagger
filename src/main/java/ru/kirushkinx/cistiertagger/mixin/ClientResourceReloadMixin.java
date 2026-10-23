package ru.kirushkinx.cistiertagger.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirushkinx.cistiertagger.catalog.CatalogResources;

@Mixin(Minecraft.class)
public abstract class ClientResourceReloadMixin {

    @Inject(method = "rollbackResourcePacks", at = @At("HEAD"), cancellable = true)
    private void cistiertagger$restoreCatalogAssets(CallbackInfo ci, @Local(argsOnly = true) Throwable error) {
        if (CatalogResources.failReload(error)) ci.cancel();
    }
}
