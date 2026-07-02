package ru.kirushkinx.cistiertagger.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirushkinx.cistiertagger.config.ConfigManager;
import ru.kirushkinx.cistiertagger.config.ModConfig;
import ru.kirushkinx.cistiertagger.decorate.Badge;

@Mixin(Display.TextDisplay.class)
public abstract class TextDisplayRendererMixin {

    @Shadow
    private Display.TextDisplay.TextRenderState textRenderState;

    @Inject(method = "updateRenderSubState", at = @At("TAIL"))
    private void cistiertagger$decorateNametag(boolean interpolate, float partialTick, CallbackInfo ci) {
        if (textRenderState == null) return;
        ModConfig cfg = ConfigManager.get();
        if (!cfg.isEnabled() || !cfg.isShowInNametag()) return;
        if (!(((Display.TextDisplay) (Object) this).getVehicle() instanceof Player player)) return;

        String playerName = player.getScoreboardName();
        if (playerName == null || playerName.isBlank()) return;

        Component text = textRenderState.text();
        if (text == null || !text.getString().contains(playerName)) return;

        Component decorated = Badge.decorate(playerName, text, Badge.DisplaySurface.NAMETAG);
        if (decorated == null) return;

        textRenderState = new Display.TextDisplay.TextRenderState(decorated, textRenderState.lineWidth(),
                textRenderState.textOpacity(), textRenderState.backgroundColor(), textRenderState.flags());
    }
}
