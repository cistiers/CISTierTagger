package ru.kirushkinx.cistiertagger.modmenu.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

    @ModifyExpressionValue(method = "extractContent",
            at = @At(value = "INVOKE", target = "Lcom/terraformersmc/modmenu/util/mod/Mod;getId()Ljava/lang/String;"),
            require = 0, expect = 0)
    private String cistiertagger$identifyEntry(String id) {
        cistiertagger$ownEntry = CisTierTagger.MOD_ID.equals(id);
        return id;
    }

    @WrapOperation(method = "extractContent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIII)V"),
            require = 0, expect = 0)
    private void cistiertagger$showAssetWarning(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
                                               int x, int y, float u, float v, int width, int height,
                                               int textureWidth, int textureHeight, int color, Operation<Void> original,
                                               @Local(argsOnly = true, ordinal = 0) int mouseX,
                                               @Local(argsOnly = true, ordinal = 1) int mouseY) {
        if (!cistiertagger$ownEntry || CatalogService.isAvailable()
                || !texture.getNamespace().equals("modmenu") || !texture.getPath().equals("textures/gui/mod_configuration.png")) {
            original.call(graphics, pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight, color);
            return;
        }
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        Identifier icon = Identifier.withDefaultNamespace(hovered ? "world_list/error_highlighted" : "world_list/error");
        graphics.blitSprite(pipeline, icon, x, y, width, height);
        if (hovered) {
            Minecraft client = Minecraft.getInstance();
            int textWidth = Math.max(1, Math.min(240, client.getWindow().getGuiScaledWidth() - 24));
            var lines = client.font.split(Component.translatable("cistiertagger.unavailable.description")
                    .withStyle(ChatFormatting.RED), textWidth);
            graphics.setTooltipForNextFrame(lines, mouseX, mouseY);
        }
    }
}
