package ru.kirushkinx.cistiertagger.gui.button;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class TextureIconButton extends Button {

    private static final int FALLBACK_SIZE = 16;

    private final @NotNull Identifier texture;
    private final int iconSize;
    private int texW = -1;
    private int texH = -1;

    public TextureIconButton(int x, int y, int size, int iconSize,
                             @NotNull Identifier texture,
                             @NotNull OnPress onPress, @NotNull Component narration) {
        super(x, y, size, size, narration, onPress, DEFAULT_NARRATION);
        this.texture = texture;
        this.iconSize = iconSize;
    }

    @Override
    protected void renderContents(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderDefaultSprite(graphics);
        ensureTextureSize();
        int iconX = getX() + (getWidth() - iconSize) / 2;
        int iconY = getY() + (getHeight() - iconSize) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                iconX, iconY, 0f, 0f,
                iconSize, iconSize,
                texW, texH,
                texW, texH);
    }

    private void ensureTextureSize() {
        if (texW > 0) return;
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        Optional<Resource> resource = rm.getResource(texture);
        if (resource.isEmpty()) {
            texW = FALLBACK_SIZE;
            texH = FALLBACK_SIZE;
            return;
        }
        try (InputStream is = resource.get().open();
             NativeImage img = NativeImage.read(is)) {
            texW = img.getWidth();
            texH = img.getHeight();
        } catch (IOException e) {
            texW = FALLBACK_SIZE;
            texH = FALLBACK_SIZE;
        }
    }
}
