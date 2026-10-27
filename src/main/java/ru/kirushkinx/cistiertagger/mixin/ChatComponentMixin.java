package ru.kirushkinx.cistiertagger.mixin;

//? if <26.1 {
/*import net.minecraft.client.GuiMessageTag;
*///?}
import net.minecraft.client.gui.components.ChatComponent;
//? if >=26.1 {
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.kirushkinx.cistiertagger.decorate.Chat;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

//? if >=26.1 {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
//?}
//? if <26.1 {
    /*@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
*///?}
            at = @At("HEAD"), argsOnly = true)
    private Component cistiertagger$decorateChatMessage(Component original,
                                                  Component originalArg,
                                                  @Nullable MessageSignature signature,
//? if >=26.1 {
                                                  GuiMessageSource source,
//?}
                                                  @Nullable GuiMessageTag tag) {
        return Chat.decorate(original);
    }
}
