package ru.kirushkinx.cistiertagger.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** Full-body player image (mc-heads.net), scaled to fit while preserving aspect. */
public class PlayerBodyWidget extends AbstractWidget {

    private static final int TEX_W = 180;
    private static final int TEX_H = 432;

    private final @NotNull Supplier<@Nullable ResourceLocation> body;

    public PlayerBodyWidget(int width, int height, @NotNull Supplier<@Nullable ResourceLocation> body) {
        super(0, 0, width, height, CommonComponents.EMPTY);
        this.body = body;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation id = body.get();
        if (id == null) return;

        int drawH = this.height;
        int drawW = Math.round(drawH * (float) TEX_W / TEX_H);
        if (drawW > this.width) {
            drawW = this.width;
            drawH = Math.round(drawW * (float) TEX_H / TEX_W);
        }
        int x = this.getX() + (this.width - drawW) / 2;
        int y = this.getY() + (this.height - drawH) / 2;
        graphics.blit(id, x, y, drawW, drawH, 0.0F, 0.0F, TEX_W, TEX_H, TEX_W, TEX_H);
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
    }
}
