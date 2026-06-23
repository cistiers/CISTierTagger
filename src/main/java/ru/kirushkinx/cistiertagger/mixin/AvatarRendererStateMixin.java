package ru.kirushkinx.cistiertagger.mixin;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirushkinx.cistiertagger.decorate.Badge;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererStateMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void cistier$decorateNametag(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        Component current = state.nameTag;
        if (current == null) return;
        Component decorated = Badge.decorate(entity.getName().getString(), current, Badge.DisplaySurface.NAMETAG);
        if (decorated != null) state.nameTag = decorated;
    }
}
