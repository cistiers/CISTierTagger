package ru.kirushkinx.cistiertagger.gui.button;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;

public class IconTextButton extends IconButton {

    private static final int ICON = 14;
    private static final int GAP = 4;

    private final int textColor;

    public IconTextButton(int x, int y, int width, int height, @NotNull Component label, int textColor,
                          @NotNull Identifier texture, @NotNull OnPress onPress) {
        super(x, y, width, height, label, texture, onPress);
        this.textColor = textColor;
    }

    @Override
    protected void renderContents(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderDefaultSprite(graphics);
        Font font = mc.font;
        int total = ICON + GAP + font.width(getMessage());
        int startX = getX() + (getWidth() - total) / 2;
        int iconY = getY() + (getHeight() - ICON) / 2;
        blitIcon(graphics, startX, iconY, ICON);
        graphics.drawString(font, getMessage(), startX + ICON + GAP, getY() + (getHeight() - font.lineHeight) / 2 + 1, textColor, true);
    }
}
