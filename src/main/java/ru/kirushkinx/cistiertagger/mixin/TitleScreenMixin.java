package ru.kirushkinx.cistiertagger.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.config.ModConfig;
import ru.kirushkinx.cistiertagger.gui.Layout;
import ru.kirushkinx.cistiertagger.update.Update;
import ru.kirushkinx.cistiertagger.update.UpdateChecker;

import java.net.URI;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique
    private static final Component DOWNLOAD = Component.translatable("cistiertagger.update.download");

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void cistiertagger$addUpdateButton(CallbackInfo ci) {
        Update update = cistiertagger$update();
        if (update == null) return;
        this.addRenderableWidget(Button.builder(DOWNLOAD,
                        b -> ConfirmLinkScreen.confirmLinkNow(this, URI.create(update.url())))
                .bounds(this.width / 2 - 60, 16, 120, 20)
                .build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void cistiertagger$renderBanner(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Update update = cistiertagger$update();
        if (update == null) return;
        Component text = Component.translatable("cistiertagger.update.available", update.version());
        graphics.drawCenteredString(this.font, text, this.width / 2, 4, Layout.COLOR_WARN);
    }

    @Unique
    private static @Nullable Update cistiertagger$update() {
        ModConfig cfg = CisTierTagger.getConfigManager() == null ? null : CisTierTagger.config();
        if (cfg == null || !cfg.isCheckForUpdates()) return null;
        UpdateChecker checker = CisTierTagger.getUpdateChecker();
        return checker == null ? null : checker.available();
    }
}
