package ru.kirushkinx.cistiertagger.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.kirushkinx.cistiertagger.decorate.Badge;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @ModifyVariable(method = "renderNameTag*", at = @At("HEAD"), argsOnly = true)
    private Component cistier$decorateNametag(@Nullable Component displayName, @Local(argsOnly = true) AbstractClientPlayer entity) {
        if (displayName == null) return null;
        Component decorated = Badge.decorate(entity.getName().getString(), displayName, Badge.DisplaySurface.NAMETAG);
        return decorated != null ? decorated : displayName;
    }
}
