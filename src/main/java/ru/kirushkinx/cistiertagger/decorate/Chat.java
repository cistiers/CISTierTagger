package ru.kirushkinx.cistiertagger.decorate;

import lombok.experimental.UtilityClass;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.cache.DumpCache;
import ru.kirushkinx.cistiertagger.config.ConfigManager;
import ru.kirushkinx.cistiertagger.config.ModConfig;
import ru.kirushkinx.cistiertagger.util.Nickname;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;

@UtilityClass
public class Chat {

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,16}");

    public static @NotNull Component decorate(@NotNull Component original) {
        ModConfig cfg = ConfigManager.get();
        if (!cfg.isEnabled() || !cfg.isShowInChat()) return original;
        if (DumpCache.size() == 0) return original;
        return processComponent(original);
    }

    private static @NotNull Component processComponent(@NotNull Component component) {
        Style style = component.getStyle();
        MutableComponent rebuilt;

        if (component.getContents() instanceof LiteralContents literal) {
            rebuilt = decorateLiteral(literal.text(), style);
        } else {
            rebuilt = MutableComponent.create(component.getContents()).setStyle(style);
        }

        for (Component sibling : component.getSiblings()) {
            rebuilt.append(processComponent(sibling));
        }
        return rebuilt;
    }

    private static @NotNull MutableComponent decorateLiteral(@NotNull String text, @NotNull Style baseStyle) {
        if (text.isEmpty()) {
            return Component.literal(text).setStyle(baseStyle);
        }

        Matcher matcher = NICKNAME_PATTERN.matcher(text);
        MutableComponent result = null;
        int cursor = 0;

        while (matcher.find()) {
            String word = matcher.group();
            if (!DumpCache.contains(word)) continue;
            if (!isOnlinePlayer(word)) continue;
            Component badge = Badge.decorate(word, Component.literal(word).setStyle(baseStyle),
                    Badge.DisplaySurface.CHAT);
            if (badge == null) continue;

            if (result == null) result = Component.empty().setStyle(Style.EMPTY);
            if (matcher.start() > cursor) {
                result.append(Component.literal(text.substring(cursor, matcher.start())).setStyle(baseStyle));
            }
            result.append(badge);
            cursor = matcher.end();
        }

        if (result == null) {
            return Component.literal(text).setStyle(baseStyle);
        }
        if (cursor < text.length()) {
            result.append(Component.literal(text.substring(cursor)).setStyle(baseStyle));
        }
        return result;
    }

    private static boolean isOnlinePlayer(@NotNull String word) {
        if (mc.getConnection() == null) return false;
        String key = Nickname.normalize(word);
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            String name = info.getProfile().getName();
            if (name != null && Nickname.normalize(name).equals(key)) return true;
        }
        return false;
    }
}
