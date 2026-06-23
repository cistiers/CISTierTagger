package ru.kirushkinx.cistiertagger.gui.button;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class IconTextButton extends Button {

    private static final int FALLBACK = 16;
    private static final int ICON = 14;
    private static final int GAP = 4;

    private final @NotNull Identifier texture;
    private final int textColor;
    private int texW = -1;
    private int texH = -1;

    public IconTextButton(int x, int y, int width, int height, @NotNull Component label, int textColor,
                          @NotNull Identifier texture, @NotNull OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.textColor = textColor;
    }

    @Override
    protected void renderContents(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderDefaultSprite(graphics);
        ensureTextureSize();
        Font font = Minecraft.getInstance().font;
        int total = ICON + GAP + font.width(getMessage());
        int startX = getX() + (getWidth() - total) / 2;
        int iconY = getY() + (getHeight() - ICON) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, startX, iconY, 0f, 0f, ICON, ICON, texW, texH, texW, texH);
        graphics.drawString(font, getMessage(), startX + ICON + GAP,
                getY() + (getHeight() - font.lineHeight) / 2 + 1, textColor, false);
    }

    private void ensureTextureSize() {
        if (texW > 0) return;
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
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
