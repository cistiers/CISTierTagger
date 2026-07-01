package ru.kirushkinx.cistiertagger.util;

import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class FormattedChars {

    public static @NotNull String plainText(@NotNull FormattedCharSequence sequence) {
        StringBuilder buffer = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            buffer.appendCodePoint(codePoint);
            return true;
        });
        return buffer.toString();
    }

    public static @NotNull Component toComponent(@NotNull FormattedCharSequence sequence) {
        MutableComponent result = Component.empty();
        StringBuilder buffer = new StringBuilder();
        Style[] currentStyle = { Style.EMPTY };
        boolean[] hasStyle = { false };
        sequence.accept((index, style, codePoint) -> {
            if (hasStyle[0] && !style.equals(currentStyle[0])) {
                if (buffer.length() > 0) {
                    result.append(Component.literal(buffer.toString()).setStyle(currentStyle[0]));
                    buffer.setLength(0);
                }
            }
            currentStyle[0] = style;
            hasStyle[0] = true;
            buffer.appendCodePoint(codePoint);
            return true;
        });
        if (buffer.length() > 0) {
            result.append(Component.literal(buffer.toString()).setStyle(currentStyle[0]));
        }
        return result;
    }
}
