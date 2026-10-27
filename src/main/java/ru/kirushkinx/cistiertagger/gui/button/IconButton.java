package ru.kirushkinx.cistiertagger.gui.button;

import com.mojang.blaze3d.platform.NativeImage;
//? if >=1.21.11 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
//? if <1.21.11 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
*///?}
import net.minecraft.client.gui.components.Button;
//? if >=1.21.6 {
import net.minecraft.client.renderer.RenderPipelines;
//?}
//? if >=1.21.2 <1.21.6 {
//import net.minecraft.client.renderer.RenderType;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;

public abstract class IconButton extends Button {

    private static final int FALLBACK = 16;

//? if >=1.21.11 {
    protected final @NotNull Identifier texture;
//?}
//? if >=1.21 <1.21.11 {
    /*private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier BUTTON_DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");
    private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");

    protected final @NotNull Identifier texture;
*///?}
//? if >=1.20.2 <1.21 {
    /*private static final Identifier BUTTON = new Identifier("widget/button");
    private static final Identifier BUTTON_DISABLED = new Identifier("widget/button_disabled");
    private static final Identifier BUTTON_HIGHLIGHTED = new Identifier("widget/button_highlighted");

    protected final @NotNull Identifier texture;
*///?}
//? if =1.20.1 {
    /*private static final Identifier WIDGETS = new Identifier("textures/gui/widgets.png");

    protected final @NotNull Identifier texture;
*///?}
    private int texW = -1;
    private int texH = -1;

    protected IconButton(int x, int y, int width, int height, @NotNull Component message,
                         @NotNull Identifier texture, @NotNull OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.texture = texture;
    }

    protected void blitIcon(@NotNull GuiGraphicsExtractor graphics, int x, int y, int size) {
        ensureTextureSize();
//? if >=1.21.11 {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, size, size, texW, texH, texW, texH);
//?}
//? if >=1.21.6 <1.21.11 {
        /*graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size, texW, texH, texW, texH);
    }

    static void renderButtonBackground(@NotNull GuiGraphicsExtractor graphics, @NotNull AbstractWidget widget) {
        Identifier sprite = !widget.active ? BUTTON_DISABLED
                : widget.isHoveredOrFocused() ? BUTTON_HIGHLIGHTED : BUTTON;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
*///?}
//? if >=1.21.2 <1.21.6 {
        /*graphics.blit(RenderType::guiTextured, texture, x, y, 0.0F, 0.0F, size, size, texW, texH, texW, texH);
    }

    static void renderButtonBackground(@NotNull GuiGraphicsExtractor graphics, @NotNull AbstractWidget widget) {
        Identifier sprite = !widget.active ? BUTTON_DISABLED
                : widget.isHoveredOrFocused() ? BUTTON_HIGHLIGHTED : BUTTON;
        graphics.blitSprite(RenderType::guiTextured, sprite, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
*///?}
//? if >=1.20.2 <1.21.2 {
        /*graphics.blit(texture, x, y, size, size, 0.0F, 0.0F, texW, texH, texW, texH);
    }

    static void renderButtonBackground(@NotNull GuiGraphicsExtractor graphics, @NotNull AbstractWidget widget) {
        Identifier sprite = !widget.active ? BUTTON_DISABLED
                : widget.isHoveredOrFocused() ? BUTTON_HIGHLIGHTED : BUTTON;
        graphics.blitSprite(sprite, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
*///?}
//? if =1.20.1 {
        /*graphics.blit(texture, x, y, size, size, 0.0F, 0.0F, texW, texH, texW, texH);
    }

    static void renderButtonBackground(@NotNull GuiGraphicsExtractor graphics, @NotNull AbstractWidget widget) {
        int state = !widget.active ? 0 : widget.isHoveredOrFocused() ? 2 : 1;
        graphics.blitNineSliced(WIDGETS, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(),
                20, 4, 200, 20, 0, 46 + state * 20);
*///?}
    }

    private void ensureTextureSize() {
        if (texW > 0) return;
        Optional<Resource> resource = mc.getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            texW = texH = FALLBACK;
            return;
        }
        try (InputStream is = resource.get().open();
             NativeImage img = NativeImage.read(is)) {
            texW = img.getWidth();
            texH = img.getHeight();
        } catch (IOException e) {
            texW = texH = FALLBACK;
        }
    }
}
