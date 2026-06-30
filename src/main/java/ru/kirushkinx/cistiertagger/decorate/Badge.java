package ru.kirushkinx.cistiertagger.decorate;

import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.cache.DumpCache;
import ru.kirushkinx.cistiertagger.config.ConfigManager;
import ru.kirushkinx.cistiertagger.config.ModConfig;
import ru.kirushkinx.cistiertagger.model.Gamemode;
import ru.kirushkinx.cistiertagger.model.PlayerTierData;
import ru.kirushkinx.cistiertagger.model.Tier;
import ru.kirushkinx.cistiertagger.network.ServerRestrictions;
import ru.kirushkinx.cistiertagger.util.Nickname;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@UtilityClass
public class Badge {

    public static final @NotNull FontDescription ICON_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(CisTierTagger.MOD_ID, "icons"));
    private static final @NotNull FontDescription BADGE_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(CisTierTagger.MOD_ID, "badges"));
    private static final int BADGE_CHAR_BASE = 0xE000; // PUA, range of U+E000 to U+F8FF

    /** Per-frame selection cache for the hot mixin render path. */
    private static final @NotNull ConcurrentHashMap<String, CachedSelection> SELECTION_CACHE = new ConcurrentHashMap<>();
    private static final @NotNull AtomicLong GENERATION = new AtomicLong(0);

    public static void bumpGeneration() {
        GENERATION.incrementAndGet();
        SELECTION_CACHE.clear();
    }

    private record CachedSelection(long generation, @Nullable Map.Entry<Gamemode, Tier> selection) {}

    public static @NotNull Component gamemodeLabel(@NotNull Gamemode gamemode) {
        return gamemodeLabel(gamemode, ModConfig.BadgeMode.IMAGE);
    }

    public static @NotNull Component gamemodeLabel(@NotNull Gamemode gamemode, @NotNull ModConfig.BadgeMode mode) {
        Component name = Component.literal(gamemode.getDisplayName())
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(gamemode.getColor())));
        return Component.empty().append(modeIcon(gamemode, mode)).append(Component.literal(" ")).append(name);
    }

    public static @Nullable Component decorate(@NotNull String nickname, @NotNull Component original) {
        return decorate(nickname, original, DisplaySurface.NAMETAG);
    }

    public static @Nullable Component decorate(@NotNull String nickname, @NotNull Component original, @NotNull DisplaySurface surface) {
        return decorate(nickname, original, surface, true);
    }

    public static @Nullable Component decorate(@NotNull String nickname, @NotNull Component original,
                                               @NotNull DisplaySurface surface, boolean serverRestrictable) {
        ModConfig cfg = ConfigManager.get();
        if (!cfg.isEnabled()) return null;
        if (!surfaceEnabled(cfg, surface)) return null;
        if (serverRestrictable && serverRestricted(surface)) return null;

        Map.Entry<Gamemode, Tier> selection = resolveSelection(cfg, nickname);
        if (selection == null) return null;

        Gamemode gamemode = selection.getKey();
        Tier tier = selection.getValue();

        return cfg.getPosition() == ModConfig.Position.LEFT
                ? assembleLeft(gamemode, tier, cfg, original)
                : assembleRight(gamemode, tier, cfg, original);
    }

    private static @Nullable Map.Entry<Gamemode, Tier> resolveSelection(@NotNull ModConfig cfg,
                                                                        @NotNull String nickname) {
        String key = Nickname.normalize(nickname);
        long gen = GENERATION.get();
        CachedSelection cached = SELECTION_CACHE.get(key);
        if (cached != null && cached.generation == gen) return cached.selection;

        Map.Entry<Gamemode, Tier> computed = computeSelection(cfg, nickname);
        SELECTION_CACHE.put(key, new CachedSelection(gen, computed));
        return computed;
    }

    private static @Nullable Map.Entry<Gamemode, Tier> computeSelection(@NotNull ModConfig cfg,
                                                                        @NotNull String nickname) {
        Optional<PlayerTierData> dataOpt = DumpCache.lookup(nickname);
        if (dataOpt.isEmpty()) return null;

        PlayerTierData data = dataOpt.get();
        List<Gamemode> activePriority = cfg.getPriorityOrder().stream()
                .filter(cfg::isGamemodeEnabled)
                .toList();
        if (activePriority.isEmpty()) return null;

        Optional<Map.Entry<Gamemode, Tier>> selection = switch (cfg.getDisplayMode()) {
            case PRIORITY -> data.selectByPriority(activePriority);
            case HIGHEST -> data.selectHighest(cfg.getEnabledGamemodes());
        };
        return selection.orElse(null);
    }

    public static @NotNull Component build(@NotNull Gamemode gamemode, @NotNull Tier tier,
                                           boolean iconFirst, @NotNull ModConfig.BadgeMode mode) {
        MutableComponent out = Component.empty();
        append(out, gamemode, tier, mode, iconFirst);
        return out;
    }

    public static @NotNull Component preview(@NotNull Gamemode gamemode, @NotNull Tier tier,
                                             boolean leftSide, @NotNull ModConfig.BadgeMode mode,
                                             @NotNull Component name) {
        MutableComponent out = Component.empty();
        if (leftSide) {
            append(out, gamemode, tier, mode, true);
            out.append(divider(mode, true, true));
            out.append(name);
        } else {
            out.append(name);
            out.append(divider(mode, true, true));
            append(out, gamemode, tier, mode, false);
        }
        return out;
    }

    private static @NotNull Component assembleLeft(@NotNull Gamemode gamemode, @NotNull Tier tier,
                                                   @NotNull ModConfig cfg, @NotNull Component original) {
        ModConfig.BadgeMode mode = cfg.getBadgeMode();
        MutableComponent out = Component.empty();
        append(out, gamemode, tier, mode, true);
        boolean leadingSpace = original.getString().startsWith(" ");
        out.append(divider(mode, true, !leadingSpace));
        out.append(original);
        return out;
    }

    private static @NotNull Component assembleRight(@NotNull Gamemode gamemode, @NotNull Tier tier,
                                                    @NotNull ModConfig cfg, @NotNull Component original) {
        ModConfig.BadgeMode mode = cfg.getBadgeMode();
        MutableComponent out = Component.empty();
        out.append(original);
        boolean trailingSpace = original.getString().endsWith(" ");
        out.append(divider(mode, !trailingSpace, true));
        append(out, gamemode, tier, mode, false);
        return out;
    }

    private static void append(@NotNull MutableComponent target, @NotNull Gamemode gamemode,
                               @NotNull Tier tier, @NotNull ModConfig.BadgeMode mode, boolean iconFirst) {
        if (mode == ModConfig.BadgeMode.IMAGE) {
            target.append(badgeImage(gamemode, tier).withStyle(style -> style.withShadowColor(0)));
            return;
        }
        Component icon = modeIcon(gamemode, mode);
        Component tierText = Component.literal(tier.displayName())
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(tier.getColor())));
        if (iconFirst) {
            target.append(icon).append(Component.literal(" ")).append(tierText);
        } else {
            target.append(tierText).append(Component.literal(" ")).append(icon);
        }
    }

    private static @NotNull MutableComponent badgeImage(@NotNull Gamemode gamemode, @NotNull Tier tier) {
        int code = BADGE_CHAR_BASE + gamemode.ordinal() * Tier.values().length + tier.ordinal();
        return Component.literal(new String(Character.toChars(code)))
                .setStyle(Style.EMPTY.withFont(BADGE_FONT).withColor(ChatFormatting.WHITE));
    }

    private static @NotNull Component modeIcon(@NotNull Gamemode gamemode, @NotNull ModConfig.BadgeMode mode) {
        return switch (mode) {
            case IMAGE -> Component.literal(gamemode.getIconString()).setStyle(Style.EMPTY.withFont(ICON_FONT));
            case TEXT -> Component.literal(gamemode.getIconString())
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(gamemode.getColor())));
        };
    }

    private static @NotNull Component divider(@NotNull ModConfig.BadgeMode mode, boolean leadingSpace, boolean trailingSpace) {
        return mode == ModConfig.BadgeMode.IMAGE
                ? Component.literal(" ")
                : separator(leadingSpace, trailingSpace);
    }

    private static @NotNull Component separator(boolean leadingSpace, boolean trailingSpace) {
        StringBuilder sb = new StringBuilder();
        if (leadingSpace) sb.append(' ');
        sb.append('|');
        if (trailingSpace) sb.append(' ');
        return Component.literal(sb.toString()).setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY));
    }

    private static boolean surfaceEnabled(@NotNull ModConfig cfg, @NotNull DisplaySurface surface) {
        return switch (surface) {
            case NAMETAG -> cfg.isShowInNametag();
            case TAB -> cfg.isShowInTabList();
            case CHAT -> cfg.isShowInChat();
        };
    }

    private static boolean serverRestricted(@NotNull DisplaySurface surface) {
        return switch (surface) {
            case NAMETAG -> ServerRestrictions.isNametagRestricted();
            case TAB -> ServerRestrictions.isTabRestricted();
            case CHAT -> ServerRestrictions.isChatRestricted();
        };
    }

    public enum DisplaySurface {
        NAMETAG, TAB, CHAT
    }
}
