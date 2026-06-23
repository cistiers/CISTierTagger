package ru.kirushkinx.cistiertagger.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.config.ModConfig;
import ru.kirushkinx.cistiertagger.decorate.Badge;

import java.util.ArrayList;
import java.util.List;

@Mixin(DisplayRenderer.TextDisplayRenderer.class)
public class TextDisplayRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Display$TextDisplay;Lnet/minecraft/client/renderer/entity/state/TextDisplayEntityRenderState;F)V",
            at = @At("RETURN"))
    private void cistiertagger$injectTier(Display.TextDisplay entity,
                                          TextDisplayEntityRenderState state,
                                          float partialTick,
                                          CallbackInfo ci) {
        ModConfig cfg = configOrNull();
        if (cfg == null || !cfg.isEnabled() || !cfg.isShowInNametag()) return;
        if (state.cachedInfo == null) return;
        if (!(entity.getVehicle() instanceof Player player)) return;

        String playerName = player.getScoreboardName();
        if (playerName == null || playerName.isBlank()) return;

        List<Display.TextDisplay.CachedLine> lines = state.cachedInfo.lines();
        for (int i = 0; i < lines.size(); i++) {
            Display.TextDisplay.CachedLine line = lines.get(i);
            String plain = plainText(line.contents());
            if (plain.isBlank() || !plain.contains(playerName)) continue;

            Component lineComponent = fromFormattedCharSequence(line.contents());
            Component decorated = Badge.decorate(playerName, lineComponent, Badge.DisplaySurface.NAMETAG);
            if (decorated == null) return;

            FormattedCharSequence newContents = decorated.getVisualOrderText();
            int newWidth = Minecraft.getInstance().font.width(decorated);

            List<Display.TextDisplay.CachedLine> newLines = new ArrayList<>(lines);
            newLines.set(i, new Display.TextDisplay.CachedLine(newContents, newWidth));

            int newMaxWidth = newLines.stream()
                    .mapToInt(Display.TextDisplay.CachedLine::width)
                    .max().orElse(state.cachedInfo.width());

            state.cachedInfo = new Display.TextDisplay.CachedInfo(newLines, newMaxWidth);
            return;
        }
    }

    @Unique
    private static @NotNull String plainText(@NotNull FormattedCharSequence sequence) {
        StringBuilder buffer = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            buffer.appendCodePoint(codePoint);
            return true;
        });
        return buffer.toString();
    }

    @Unique
    private static @NotNull Component fromFormattedCharSequence(@NotNull FormattedCharSequence sequence) {
        MutableComponent result = Component.empty();
        StringBuilder buffer = new StringBuilder();
        Style[] currentStyle = { Style.EMPTY };
        boolean[] hasStyle = { false };
        sequence.accept((index, style, codePoint) -> {
            if (hasStyle[0] && !style.equals(currentStyle[0])) {
                if (buffer.length() > 0) {
                    result.append(Component.literal(buffer.toString()).setStyle(currentStyle[0]));
                    buffer.setLength(0);
                }
            }
            currentStyle[0] = style;
            hasStyle[0] = true;
            buffer.appendCodePoint(codePoint);
            return true;
        });
        if (buffer.length() > 0) {
            result.append(Component.literal(buffer.toString()).setStyle(currentStyle[0]));
        }
        return result;
    }

    @Unique
    private static @Nullable ModConfig configOrNull() {
        var manager = CisTierTagger.getConfigManager();
        return manager == null ? null : manager.get();
    }
}
