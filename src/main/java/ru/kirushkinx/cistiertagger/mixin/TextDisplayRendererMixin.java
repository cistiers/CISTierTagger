package ru.kirushkinx.cistiertagger.mixin;

import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirushkinx.cistiertagger.config.ConfigManager;
import ru.kirushkinx.cistiertagger.config.ModConfig;
import ru.kirushkinx.cistiertagger.decorate.Badge;
import ru.kirushkinx.cistiertagger.util.FormattedChars;

import java.util.ArrayList;
import java.util.List;

import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;

@Mixin(DisplayRenderer.TextDisplayRenderer.class)
public class TextDisplayRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Display$TextDisplay;Lnet/minecraft/client/renderer/entity/state/TextDisplayEntityRenderState;F)V",
            at = @At("RETURN"))
    private void cistiertagger$injectTier(Display.TextDisplay entity,
                                          TextDisplayEntityRenderState state,
                                          float partialTick,
                                          CallbackInfo ci) {
        ModConfig cfg = ConfigManager.get();
        if (!cfg.isEnabled() || !cfg.isShowInNametag()) return;
        if (state.cachedInfo == null) return;
        if (!(entity.getVehicle() instanceof Player player)) return;

        String playerName = player.getScoreboardName();
        if (playerName == null || playerName.isBlank()) return;

        List<Display.TextDisplay.CachedLine> lines = state.cachedInfo.lines();
        for (int i = 0; i < lines.size(); i++) {
            Display.TextDisplay.CachedLine line = lines.get(i);
            String plain = FormattedChars.plainText(line.contents());
            if (plain.isBlank() || !plain.contains(playerName)) continue;

            Component lineComponent = FormattedChars.toComponent(line.contents());
            Component decorated = Badge.decorate(playerName, lineComponent, Badge.DisplaySurface.NAMETAG);
            if (decorated == null) return;

            FormattedCharSequence newContents = decorated.getVisualOrderText();
            int newWidth = mc.font.width(decorated);

            List<Display.TextDisplay.CachedLine> newLines = new ArrayList<>(lines);
            newLines.set(i, new Display.TextDisplay.CachedLine(newContents, newWidth));

            int newMaxWidth = newLines.stream()
                    .mapToInt(Display.TextDisplay.CachedLine::width)
                    .max().orElse(state.cachedInfo.width());

            state.cachedInfo = new Display.TextDisplay.CachedInfo(newLines, newMaxWidth);
            return;
        }
    }
}
