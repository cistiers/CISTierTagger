package ru.kirushkinx.cistiertagger.mixin;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.kirushkinx.cistiertagger.decorate.Chat;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true)
    private Component cistier$decorateChatMessage(Component original,
                                                  Component originalArg,
                                                  @Nullable MessageSignature signature,
                                                  @Nullable GuiMessageTag tag) {
        return Chat.decorate(original);
    }
}
