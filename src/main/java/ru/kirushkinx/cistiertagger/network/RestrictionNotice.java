package ru.kirushkinx.cistiertagger.network;

import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.util.TextCompat;
import ru.kirushkinx.cistiertagger.gui.Layout;

@UtilityClass
public class RestrictionNotice {

    private static final int ACCENT = Layout.COLOR_WARN & 0xFFFFFF;
    private static final int DIM = Layout.COLOR_DIM & 0xFFFFFF;

    public static @NotNull Component message() {
        MutableComponent hover = Component.translatable("cistiertagger.restriction.hover")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(DIM)));
        if (ServerRestrictions.isNametagRestricted()) hover.append(surface("nametag"));
        if (ServerRestrictions.isTabRestricted()) hover.append(surface("tab"));
        if (ServerRestrictions.isChatRestricted()) hover.append(surface("chat"));

        return Component.literal("ℹ ")
                .append(Component.translatable("cistiertagger.restriction.chat"))
                .setStyle(Style.EMPTY
                        .withColor(TextColor.fromRgb(ACCENT))
                        .withHoverEvent(TextCompat.showText(hover)));
    }

    private static @NotNull Component surface(@NotNull String key) {
        return Component.literal("\n- ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(DIM)))
                .append(Component.translatable("cistiertagger.button." + key)
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(ACCENT))));
    }
}
