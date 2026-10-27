package ru.kirushkinx.cistiertagger.mixin;

//? if <1.21.2 {
//import com.llamalad7.mixinextras.sugar.Local;
//?}
//? if <1.21.9 {
//import net.minecraft.client.player.AbstractClientPlayer;
//?}
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//?}
import net.minecraft.network.chat.Component;
//? if >=1.21.9 {
import net.minecraft.world.entity.Avatar;
//?}
//? if <1.21.2 {
//import org.jetbrains.annotations.Nullable;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.2 {
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}
//? if <1.21.2 {
//import org.spongepowered.asm.mixin.injection.ModifyVariable;
//?}
import ru.kirushkinx.cistiertagger.decorate.Badge;

@Mixin(AvatarRenderer.class)
public abstract class NametagRendererMixin {

//? if >=1.21.9 {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void cistiertagger$decorateNametag(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
//?}
//? if >=1.21.2 <1.21.9 {
    /*@Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void cistiertagger$decorateNametag(AbstractClientPlayer entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
*///?}
//? if >=1.21.2 {
        Component current = state.nameTag;
        if (current == null) return;
        Component decorated = Badge.decorate(entity.getName().getString(), current, Badge.DisplaySurface.NAMETAG);
        if (decorated != null) state.nameTag = decorated;
    }
//?}
//? if <1.21.2 {
    /*@ModifyVariable(method = "renderNameTag*", at = @At("HEAD"), argsOnly = true)
    private Component cistiertagger$decorateNametag(@Nullable Component displayName, @Local(argsOnly = true) AbstractClientPlayer entity) {
        if (displayName == null) return null;
        Component decorated = Badge.decorate(entity.getName().getString(), displayName, Badge.DisplaySurface.NAMETAG);
        return decorated != null ? decorated : displayName;
    }
*///?}
}
