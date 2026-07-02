package ru.kirushkinx.cistiertagger.gui.button;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class TextureIconButton extends IconButton {

    private final int iconSize;

    public TextureIconButton(int x, int y, int size, int iconSize,
                             @NotNull ResourceLocation texture,
                             @NotNull OnPress onPress, @NotNull Component narration) {
        super(x, y, size, size, narration, texture, onPress);
        this.iconSize = iconSize;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        int iconX = getX() + (getWidth() - iconSize) / 2;
        int iconY = getY() + (getHeight() - iconSize) / 2;
        blitIcon(graphics, iconX, iconY, iconSize);
    }
}
