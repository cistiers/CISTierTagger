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
import ru.kirushkinx.cistiertagger.config.ModConfig;
import ru.kirushkinx.cistiertagger.model.Gamemode;
import ru.kirushkinx.cistiertagger.model.PlayerTierData;
import ru.kirushkinx.cistiertagger.model.Tier;
import ru.kirushkinx.cistiertagger.util.Nickname;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@UtilityClass
public class Badge {

    public static final @NotNull FontDescription ICON_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("minecraft", "cistiers"));

    /** Per-frame selection cache for the hot mixin render path. */
    private static final @NotNull ConcurrentHashMap<String, CachedSelection> SELECTION_CACHE = new ConcurrentHashMap<>();
    private static final @NotNull AtomicLong GENERATION = new AtomicLong(0);

    public static void bumpGeneration() {
        GENERATION.incrementAndGet();
        SELECTION_CACHE.clear();
    }

    private record CachedSelection(long generation, @Nullable Map.Entry<Gamemode, Tier> selection) {}

    public static @NotNull Component gamemodeLabel(@NotNull Gamemode gamemode) {
        return gamemodeLabel(gamemode, ModConfig.IconMode.IMAGES);
    }

    public static @NotNull Component gamemodeLabel(@NotNull Gamemode gamemode, @NotNull ModConfig.IconMode iconMode) {
        Component name = Component.literal(gamemode.getDisplayName())
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(gamemode.getColor())));
        Component icon = renderIcon(gamemode, iconMode);
        if (icon == null) return name;
        return Component.empty().append(icon).append(Component.literal(" ")).append(name);
    }

    public static @Nullable Component decorate(@NotNull String nickname, @NotNull Component original) {
        return decorate(nickname, original, DisplaySurface.NAMETAG);
    }

    public static @Nullable Component decorate(@NotNull String nickname, @NotNull Component original, @NotNull DisplaySurface surface) {
        ModConfig cfg = configOrNull();
        if (cfg == null) return null;
        if (!cfg.isEnabled()) return null;
        if (!surfaceEnabled(cfg, surface)) return null;

        DumpCache cache = CisTierTagger.getDumpCache();
        if (cache == null) return null;

        Map.Entry<Gamemode, Tier> selection = resolveSelection(cfg, cache, nickname);
        if (selection == null) return null;

        Gamemode gamemode = selection.getKey();
        Tier tier = selection.getValue();

        return cfg.getPosition() == ModConfig.Position.LEFT
                ? assembleLeft(gamemode, tier, cfg, original)
                : assembleRight(gamemode, tier, cfg, original);
    }

    private static @Nullable Map.Entry<Gamemode, Tier> resolveSelection(@NotNull ModConfig cfg,
                                                                        @NotNull DumpCache cache,
                                                                        @NotNull String nickname) {
        String key = Nickname.normalize(nickname);
        long gen = GENERATION.get();
        CachedSelection cached = SELECTION_CACHE.get(key);
        if (cached != null && cached.generation == gen) return cached.selection;

        Map.Entry<Gamemode, Tier> computed = computeSelection(cfg, cache, nickname);
        SELECTION_CACHE.put(key, new CachedSelection(gen, computed));
        return computed;
    }

    private static @Nullable Map.Entry<Gamemode, Tier> computeSelection(@NotNull ModConfig cfg,
                                                                        @NotNull DumpCache cache,
                                                                        @NotNull String nickname) {
        Optional<PlayerTierData> dataOpt = cache.lookup(nickname);
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
                                           boolean iconFirst, @NotNull ModConfig.IconMode iconMode) {
        MutableComponent out = Component.empty();
        append(out, gamemode, tier, iconMode, iconFirst);
        return out;
    }

    public static @NotNull Component preview(@NotNull Gamemode gamemode, @NotNull Tier tier,
                                             boolean leftSide, @NotNull ModConfig.IconMode iconMode,
                                             @NotNull Component name) {
        MutableComponent out = Component.empty();
        if (leftSide) {
            append(out, gamemode, tier, iconMode, true);
            out.append(separator(true, true));
            out.append(name);
        } else {
            out.append(name);
            out.append(separator(true, true));
            append(out, gamemode, tier, iconMode, false);
        }
        return out;
    }

    private static @NotNull Component assembleLeft(@NotNull Gamemode gamemode, @NotNull Tier tier,
                                                   @NotNull ModConfig cfg, @NotNull Component original) {
        MutableComponent out = Component.empty();
        append(out, gamemode, tier, cfg.getIconMode(), true);
        boolean leadingSpace = original.getString().startsWith(" ");
        out.append(separator(true, !leadingSpace));
        out.append(original);
        return out;
    }

    private static @NotNull Component assembleRight(@NotNull Gamemode gamemode, @NotNull Tier tier,
                                                    @NotNull ModConfig cfg, @NotNull Component original) {
        MutableComponent out = Component.empty();
        out.append(original);
        boolean trailingSpace = original.getString().endsWith(" ");
        out.append(separator(!trailingSpace, true));
        append(out, gamemode, tier, cfg.getIconMode(), false);
        return out;
    }

    private static void append(@NotNull MutableComponent target, @NotNull Gamemode gamemode,
                               @NotNull Tier tier, @NotNull ModConfig.IconMode iconMode, boolean iconFirst) {
        Style tierStyle = Style.EMPTY.withColor(TextColor.fromRgb(tier.getColor()));
        Component icon = renderIcon(gamemode, iconMode);
        Component tierText = Component.literal(tier.displayName()).setStyle(tierStyle);

        if (iconFirst) {
            if (icon != null) {
                target.append(icon);
                target.append(Component.literal(" "));
            }
            target.append(tierText);
        } else {
            target.append(tierText);
            if (icon != null) {
                target.append(Component.literal(" "));
                target.append(icon);
            }
        }
    }

    private static @Nullable Component renderIcon(@NotNull Gamemode gamemode, @NotNull ModConfig.IconMode mode) {
        return switch (mode) {
            case IMAGES -> Component.literal(gamemode.getIconString())
                    .setStyle(Style.EMPTY.withFont(ICON_FONT));
            case SYMBOLS -> Component.literal(gamemode.getIconString())
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(gamemode.getColor())));
            case OFF -> null;
        };
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

    private static @Nullable ModConfig configOrNull() {
        var manager = CisTierTagger.getConfigManager();
        return manager == null ? null : manager.get();
    }

    public enum DisplaySurface {
        NAMETAG, TAB, CHAT
    }
}
