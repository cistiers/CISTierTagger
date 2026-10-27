package ru.kirushkinx.cistiertagger.modmenu.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
//? if =26.2 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
import net.minecraft.client.Minecraft;
//? if >=1.21.6 <26.2 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?}
//? if >=1.21.2 <1.21.6 {
/*import java.util.function.Function;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?}
//? if <1.21.2 {
/*import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?}
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

//? if =26.2 {
    @ModifyExpressionValue(method = "extractContent(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIZF)V",
//?}
//? if =26.1 {
    /*@ModifyExpressionValue(method = "extractContent",
*///?}
//? if >=1.21.9 <26.1 {
    /*@ModifyExpressionValue(method = "renderContent",
*///?}
//? if <1.21.9 {
    /*@ModifyExpressionValue(method = "render",
*///?}
            at = @At(value = "INVOKE", target = "Lcom/terraformersmc/modmenu/util/mod/Mod;getId()Ljava/lang/String;"),
            require = 0, expect = 0)
    private String cistiertagger$identifyEntry(String id) {
        cistiertagger$ownEntry = CisTierTagger.MOD_ID.equals(id);
        return id;
    }

//? if =26.2 {
    @WrapOperation(method = "extractContent(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIZF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIII)V"),
//?}
//? if =26.1 {
    /*@WrapOperation(method = "extractContent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIII)V"),
*///?}
//? if >=1.21.9 <26.1 {
    /*@WrapOperation(method = "renderContent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIII)V", remap = true),
*///?}
//? if =1.21.6 {
    /*@WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIII)V", remap = true),
*///?}
//? if >=1.21.2 <1.21.6 {
    /*@WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Ljava/util/function/Function;Lnet/minecraft/resources/Identifier;IIFFIIII)V", remap = true),
*///?}
//? if <1.21.2 {
    /*@WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lnet/minecraft/resources/Identifier;IIFFIIII)V", remap = true),
*///?}
            require = 0, expect = 0)
//? if >=1.21.6 {
    private void cistiertagger$showAssetWarning(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
//?}
//? if >=1.21.2 <1.21.6 {
    /*private void cistiertagger$showAssetWarning(GuiGraphicsExtractor graphics, Function<Identifier, RenderType> pipeline, Identifier texture,
*///?}
//? if <1.21.2 {
    /*private void cistiertagger$showAssetWarning(GuiGraphicsExtractor graphics, Identifier texture,
*///?}
                                               int x, int y, float u, float v, int width, int height,
//? if >=1.21.9 {
                                               int textureWidth, int textureHeight, int color, Operation<Void> original,
                                               @Local(argsOnly = true, ordinal = 0) int mouseX,
                                               @Local(argsOnly = true, ordinal = 1) int mouseY) {
//?}
//? if =1.21.6 {
                                               /*int textureWidth, int textureHeight, int color, Operation<Void> original,
                                               @Local(argsOnly = true, ordinal = 5) int mouseX,
                                               @Local(argsOnly = true, ordinal = 6) int mouseY) {
*///?}
//? if <1.21.6 {
                                               /*int textureWidth, int textureHeight, Operation<Void> original,
                                               @Local(argsOnly = true, ordinal = 5) int mouseX,
                                               @Local(argsOnly = true, ordinal = 6) int mouseY) {
*///?}
        if (!cistiertagger$ownEntry || CatalogService.isAvailable()
                || !texture.getNamespace().equals("modmenu") || !texture.getPath().equals("textures/gui/mod_configuration.png")) {
//? if >=1.21.6 {
            original.call(graphics, pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight, color);
//?}
//? if >=1.21.2 <1.21.6 {
            /*original.call(graphics, pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
*///?}
//? if <1.21.2 {
            /*original.call(graphics, texture, x, y, u, v, width, height, textureWidth, textureHeight);
*///?}
            return;
        }
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
//? if >=1.21.2 {
        Identifier icon = Identifier.withDefaultNamespace(hovered ? "world_list/error_highlighted" : "world_list/error");
        graphics.blitSprite(pipeline, icon, x, y, width, height);
//?}
//? if =1.21 {
        /*Identifier icon = Identifier.withDefaultNamespace(hovered ? "world_list/error_highlighted" : "world_list/error");
        graphics.blitSprite(icon, x, y, width, height);
*///?}
//? if >=1.20.2 <1.21 {
        /*Identifier icon = new Identifier(hovered ? "world_list/error_highlighted" : "world_list/error");
        graphics.blitSprite(icon, x, y, width, height);
*///?}
//? if =1.20.1 {
        /*Identifier icon = new Identifier("minecraft", "textures/gui/world_selection.png");
        graphics.blit(icon, x, y, 96f, hovered ? 32f : 0f, width, height, 256, 256);
*///?}
        if (hovered) {
            Minecraft client = Minecraft.getInstance();
//? if =26.2 {
            int textWidth = Math.clamp(client.getWindow().getGuiScaledWidth() - 24, 1, 240);
//?}
//? if <26.2 {
            /*int textWidth = Math.max(1, Math.min(240, client.getWindow().getGuiScaledWidth() - 24));
*///?}
            var lines = client.font.split(Component.translatable("cistiertagger.unavailable.description")
                    .withStyle(ChatFormatting.RED), textWidth);
//? if >=1.21.6 {
            graphics.setTooltipForNextFrame(lines, mouseX, mouseY);
//?}
//? if <1.21.6 {
            /*if (client.screen != null) client.screen.setTooltipForNextRenderPass(lines);
*///?}
        }
    }
}
