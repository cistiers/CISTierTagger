package ru.kirushkinx.cistiertagger.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirushkinx.cistiertagger.api.UpdateChecker;
import ru.kirushkinx.cistiertagger.config.ConfigManager;
import ru.kirushkinx.cistiertagger.gui.Layout;
import ru.kirushkinx.cistiertagger.gui.button.IconTextButton;

import java.net.URI;

import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique
    private static final Component UPDATE_BUTTON = Component.translatable("cistiertagger.update.button");
    @Unique
    private static final Component INFO_TITLE = Component.translatable("cistiertagger.update.info.title");
    @Unique
    private static final Component INFO_SUBTITLE = Component.translatable("cistiertagger.update.info.subtitle");
    @Unique
    private int cistiertagger$infoY;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void cistiertagger$addUpdateButton(CallbackInfo ci) {
        UpdateChecker.Update update = cistiertagger$update();
        if (update == null) return;

        int top = this.height - 45;
        this.addRenderableWidget(new IconTextButton(this.width / 2 - 100, top, 200, 20,
                UPDATE_BUTTON, 0xFFFFFFFF, Layout.LOGO_TEXTURE,
                b -> ConfirmLinkScreen.confirmLinkNow(this, URI.create(update.url()))));
        cistiertagger$infoY = top - 20;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void cistiertagger$renderInfo(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (cistiertagger$infoY <= 0 || mc.getWindow().getGuiScale() >= 4) return;
        graphics.drawCenteredString(this.font, INFO_TITLE, this.width / 2, cistiertagger$infoY, Layout.COLOR_WARN);
        graphics.drawCenteredString(this.font, INFO_SUBTITLE, this.width / 2, cistiertagger$infoY + 11, Layout.COLOR_DIM);
    }

    @Unique
    private static UpdateChecker.@Nullable Update cistiertagger$update() {
        if (!ConfigManager.get().isCheckForUpdates()) return null;
        return UpdateChecker.available();
    }
}
