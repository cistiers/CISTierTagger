package ru.kirushkinx.cistiertagger.gui.button;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;

public abstract class IconButton extends Button {

    private static final int FALLBACK = 16;

    protected final @NotNull ResourceLocation texture;
    private int texW = -1;
    private int texH = -1;

    protected IconButton(int x, int y, int width, int height, @NotNull Component message,
                         @NotNull ResourceLocation texture, @NotNull OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.texture = texture;
    }

    protected void blitIcon(@NotNull GuiGraphics graphics, int x, int y, int size) {
        ensureTextureSize();
        graphics.blit(texture, x, y, size, size, 0.0F, 0.0F, texW, texH, texW, texH);
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
