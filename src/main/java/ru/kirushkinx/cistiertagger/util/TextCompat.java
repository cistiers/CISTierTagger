package ru.kirushkinx.cistiertagger.util;

import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/** Text styling calls that shift across Minecraft versions, isolated for multi-version ports. */
@UtilityClass
public class TextCompat {

    public static @NotNull Style applyFont(@NotNull Style base, @NotNull Identifier font) {
        return base.withFont(new FontDescription.Resource(font));
    }

    public static @NotNull Style noShadow(@NotNull Style base) {
        return base.withShadowColor(0);
    }

    public static @NotNull HoverEvent showText(@NotNull Component text) {
        return new HoverEvent.ShowText(text);
    }
}
