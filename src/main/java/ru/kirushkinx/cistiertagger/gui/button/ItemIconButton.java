//? if <26.1 {
/*package ru.kirushkinx.cistiertagger.gui.button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemIconButton extends Button {

    private final @NotNull ItemStack icon;

    public ItemIconButton(int x, int y, int size, @NotNull ItemStack icon, @NotNull OnPress onPress, @NotNull Component narration) {
        super(x, y, size, size, narration, onPress, DEFAULT_NARRATION);
        this.icon = icon;
    }

    @Override
//? if =1.21.11 {
    /^protected void renderContents(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderDefaultSprite(graphics);
^///?}
//? if <1.21.11 {
    /^protected void renderWidget(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        IconButton.renderButtonBackground(graphics, this);
^///?}
        int iconSize = 16;
        int iconX = getX() + (getWidth() - iconSize) / 2;
        int iconY = getY() + (getHeight() - iconSize) / 2;
        graphics.renderItem(icon, iconX, iconY);
    }
}
*///?}
