package ru.kirushkinx.cistiertagger.gui.button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class TextureIconButton extends IconButton {

    private final int iconSize;

    public TextureIconButton(int x, int y, int size, int iconSize,
                             @NotNull Identifier texture,
                             @NotNull OnPress onPress, @NotNull Component narration) {
        super(x, y, size, size, narration, texture, onPress);
        this.iconSize = iconSize;
    }

    @Override
    protected void extractContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractDefaultSprite(graphics);
        int iconX = getX() + (getWidth() - iconSize) / 2;
        int iconY = getY() + (getHeight() - iconSize) / 2;
        blitIcon(graphics, iconX, iconY, iconSize);
    }
}
