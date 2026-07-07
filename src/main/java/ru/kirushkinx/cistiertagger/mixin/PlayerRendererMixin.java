package ru.kirushkinx.cistiertagger.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirushkinx.cistiertagger.decorate.Badge;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
            at = @At("TAIL"))
    private void cistier$decorateNametag(AbstractClientPlayer entity, PlayerRenderState state, float partialTick, CallbackInfo ci) {
        Component current = state.nameTag;
        if (current == null) return;
        Component decorated = Badge.decorate(entity.getName().getString(), current, Badge.DisplaySurface.NAMETAG);
        if (decorated != null) state.nameTag = decorated;
    }
}
