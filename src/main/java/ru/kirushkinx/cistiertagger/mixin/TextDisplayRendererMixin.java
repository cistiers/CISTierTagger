package ru.kirushkinx.cistiertagger.mixin;

//? if >=1.21.4 {
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
//?}
import net.minecraft.network.chat.Component;
//? if >=1.21.4 {
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
//?}
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
//? if <1.21.4 {
//import org.spongepowered.asm.mixin.Shadow;
//?}
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirushkinx.cistiertagger.config.ConfigManager;
import ru.kirushkinx.cistiertagger.config.ModConfig;
import ru.kirushkinx.cistiertagger.decorate.Badge;

//? if >=1.21.4 {
import java.util.ArrayList;
import java.util.List;
//?}
//? if <1.21.4 {
/*@Mixin(Display.TextDisplay.class)
public abstract class TextDisplayRendererMixin {
*///?}

//? if >=1.21.4 {
import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;
//?}
//? if <1.21.4 {
    /*@Shadow
    private Display.TextDisplay.TextRenderState textRenderState;
*///?}

//? if >=1.21.4 {
@Mixin(DisplayRenderer.TextDisplayRenderer.class)
public class TextDisplayRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Display$TextDisplay;Lnet/minecraft/client/renderer/entity/state/TextDisplayEntityRenderState;F)V",
            at = @At("RETURN"))
    private void cistiertagger$injectTier(Display.TextDisplay entity,
                                          TextDisplayEntityRenderState state,
                                          float partialTick,
                                          CallbackInfo ci) {
//?}
//? if <1.21.4 {
    /*@Inject(method = "updateRenderSubState", at = @At("TAIL"))
    private void cistiertagger$decorateNametag(boolean interpolate, float partialTick, CallbackInfo ci) {
        if (textRenderState == null) return;
*///?}
        ModConfig cfg = ConfigManager.get();
        if (!cfg.isEnabled() || !cfg.isShowInNametag()) return;
//? if >=1.21.4 {
        if (state.cachedInfo == null) return;
        if (!(entity.getVehicle() instanceof Player player)) return;
//?}
//? if <1.21.4 {
        //if (!(((Display.TextDisplay) (Object) this).getVehicle() instanceof Player player)) return;
//?}

        String playerName = player.getScoreboardName();
        if (playerName == null || playerName.isBlank()) return;

//? if >=1.21.4 {
        List<Display.TextDisplay.CachedLine> lines = state.cachedInfo.lines();
        for (int i = 0; i < lines.size(); i++) {
            Display.TextDisplay.CachedLine line = lines.get(i);
            StringBuilder plainText = new StringBuilder();
            line.contents().accept((index, style, codePoint) -> {
                plainText.appendCodePoint(codePoint);
                return true;
            });
            String plain = plainText.toString();
            if (plain.isBlank() || !plain.contains(playerName)) continue;
//?}
//? if <1.21.4 {
        /*Component text = textRenderState.text();
        if (text == null || !text.getString().contains(playerName)) return;
*///?}

//? if >=1.21.4 {
            MutableComponent lineComponent = Component.empty();
            StringBuilder buffer = new StringBuilder();
            Style[] currentStyle = { Style.EMPTY };
            boolean[] hasStyle = { false };
            line.contents().accept((index, style, codePoint) -> {
                if (hasStyle[0] && !style.equals(currentStyle[0])) {
                    if (buffer.length() > 0) {
                        lineComponent.append(Component.literal(buffer.toString()).setStyle(currentStyle[0]));
                        buffer.setLength(0);
                    }
                }
                currentStyle[0] = style;
                hasStyle[0] = true;
                buffer.appendCodePoint(codePoint);
                return true;
            });
            if (buffer.length() > 0) {
                lineComponent.append(Component.literal(buffer.toString()).setStyle(currentStyle[0]));
            }
            Component decorated = Badge.decorate(playerName, lineComponent, Badge.DisplaySurface.NAMETAG);
            if (decorated == null) return;
//?}
//? if <1.21.4 {
        /*Component decorated = Badge.decorate(playerName, text, Badge.DisplaySurface.NAMETAG);
        if (decorated == null) return;
*///?}

//? if >=1.21.4 {
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
//?}
//? if <1.21.4 {
        /*textRenderState = new Display.TextDisplay.TextRenderState(decorated, textRenderState.lineWidth(),
                textRenderState.textOpacity(), textRenderState.backgroundColor(), textRenderState.flags());
*///?}
    }
}
