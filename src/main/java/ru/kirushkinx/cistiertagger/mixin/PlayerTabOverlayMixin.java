package ru.kirushkinx.cistiertagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import ru.kirushkinx.cistiertagger.decorate.Badge;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @ModifyReturnValue(method = "getNameForDisplay", at = @At("RETURN"))
    private Component cistier$decorateTabName(Component original, @Local(argsOnly = true) PlayerInfo entry) {
        String nickname = nicknameOf(entry);
        if (nickname == null) return original;
        Component decorated = Badge.decorate(nickname, original, Badge.DisplaySurface.TAB);
        return decorated != null ? decorated : original;
    }

    @Unique
    private static String nicknameOf(@NotNull PlayerInfo entry) {
        if (entry.getProfile() != null && entry.getProfile().getName() != null) {
            return entry.getProfile().getName();
        }
        return null;
    }
}
