package ru.kirushkinx.cistiertagger.modmenu.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.catalog.CatalogService;

@Pseudo
@Mixin(targets = "com.terraformersmc.modmenu.gui.widget.entries.ModListEntry", remap = false)
public abstract class ModMenuEntryMixin {

    @Unique
    private boolean cistiertagger$ownEntry;

    @ModifyExpressionValue(method = "render",
            at = @At(value = "INVOKE", target = "Lcom/terraformersmc/modmenu/util/mod/Mod;getId()Ljava/lang/String;"),
            require = 0, expect = 0)
    private String cistiertagger$identifyEntry(String id) {
        cistiertagger$ownEntry = CisTierTagger.MOD_ID.equals(id);
        return id;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V", remap = true),
            require = 0, expect = 0)
    private void cistiertagger$showAssetWarning(GuiGraphics graphics, ResourceLocation texture,
                                               int x, int y, float u, float v, int width, int height,
                                               int textureWidth, int textureHeight, Operation<Void> original,
                                               @Local(argsOnly = true, ordinal = 5) int mouseX,
                                               @Local(argsOnly = true, ordinal = 6) int mouseY) {
        if (!cistiertagger$ownEntry || CatalogService.isAvailable()
                || !texture.getNamespace().equals("modmenu") || !texture.getPath().equals("textures/gui/mod_configuration.png")) {
            original.call(graphics, texture, x, y, u, v, width, height, textureWidth, textureHeight);
            return;
        }
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        ResourceLocation icon = new ResourceLocation(hovered ? "world_list/error_highlighted" : "world_list/error");
        graphics.blitSprite(icon, x, y, width, height);
        if (hovered) {
            Minecraft client = Minecraft.getInstance();
            int textWidth = Math.max(1, Math.min(240, client.getWindow().getGuiScaledWidth() - 24));
            var lines = client.font.split(Component.translatable("cistiertagger.assets.unavailable.description")
                    .withStyle(ChatFormatting.RED), textWidth);
            if (client.screen != null) client.screen.setTooltipForNextRenderPass(lines);
        }
    }
}
