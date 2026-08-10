package ru.kirushkinx.cistiertagger.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.api.dto.ProfileResponse;
import ru.kirushkinx.cistiertagger.cache.DumpCache;
import ru.kirushkinx.cistiertagger.cache.ProfileCache;
import ru.kirushkinx.cistiertagger.cache.SkinCache;
import ru.kirushkinx.cistiertagger.config.ConfigManager;
import ru.kirushkinx.cistiertagger.decorate.Badge;
import ru.kirushkinx.cistiertagger.gui.Layout;
import ru.kirushkinx.cistiertagger.model.Gamemode;
import ru.kirushkinx.cistiertagger.model.PlayerTierData;
import ru.kirushkinx.cistiertagger.model.Tier;

import java.util.List;
import java.util.Map;

import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;

public class ProfileScreen extends ModScreen {

    private static final Component TITLE = Component.translatable("cistiertagger.screen.profile.title");
    private static final Component BACK = Component.translatable("cistiertagger.button.back");
    private static final Component CLOSE = Component.translatable("cistiertagger.button.close");
    private static final Component NOT_FOUND = Component.translatable("cistiertagger.status.player_not_found");
    private static final Component LOADING = Component.translatable("cistiertagger.status.loading");
    private static final Component TIERS_HEADER = Component.translatable("cistiertagger.profile.tiers_header");
    private static final Component HISTORY_HEADER = Component.translatable("cistiertagger.profile.history_header");
    private static final Component TIP_OPEN_PROFILE = Component.translatable("cistiertagger.tooltip.open_profile");

    private static final int SKIN_WIDTH = 80;
    private static final int SKIN_HEIGHT = 168;
    private static final int PANE_W = 200;

    private final @NotNull String nickname;
    private final @NotNull PlayerSkinWidget skinWidget;

    private @Nullable ProfileResponse profile;
    private @NotNull LoadState state = LoadState.LOADING;
    private @Nullable String errorMessage;

    private ProfileScreen(@NotNull String nickname, @NotNull PlayerSkinWidget skinWidget, @Nullable Screen parent) {
        super(TITLE, parent);
        this.nickname = nickname;
        this.skinWidget = skinWidget;
    }

    public static void openFor(@NotNull String nickname) {
        openFor(nickname, null);
    }

    public static void openFor(@NotNull String nickname, @Nullable Screen parent) {
        PlayerSkinWidget widget = new PlayerSkinWidget(SKIN_WIDTH, SKIN_HEIGHT, mc.getEntityModels(),
                () -> SkinCache.forNickname(nickname).get().get());
        ProfileScreen screen = new ProfileScreen(nickname, widget, parent);
        mc.gui.setScreen(screen);

        ProfileCache.fetch(nickname).whenComplete((response, error) ->
                mc.execute(() -> screen.applyProfile(response, error)));
    }

    private void applyProfile(@Nullable ProfileResponse response, @Nullable Throwable error) {
        if (error != null) {
            this.state = LoadState.ERROR;
            this.errorMessage = error.getMessage();
        } else if (response == null) {
            this.state = LoadState.ERROR;
            this.errorMessage = "empty response";
        } else {
            this.profile = response;
            this.state = LoadState.LOADED;
        }
        rebuildBody();
    }

    @Override
    protected void init() {
        rebuildBody();
    }

    private void rebuildBody() {
        this.clearWidgets();

        int comboLeft = Layout.comboLeft(this.width, SKIN_WIDTH, PANE_W);
        int skinX = comboLeft;
        int skinY = Math.max(40, (this.height - SKIN_HEIGHT) / 2);
        skinWidget.setPosition(skinX, skinY);
        this.addRenderableWidget(skinWidget);

        Component backCloseLabel = parent != null ? BACK : CLOSE;
        int btnX = this.width / 2 - Layout.FOOTER_BUTTON_WIDTH / 2;
        int btnY = this.height - Layout.BUTTON_HEIGHT - Layout.FOOTER_BUTTON_BOTTOM_INSET;
        this.addRenderableWidget(
                Button.builder(backCloseLabel, btn -> returnToParent())
                        .bounds(btnX, btnY, Layout.FOOTER_BUTTON_WIDTH, Layout.BUTTON_HEIGHT)
                        .build()
        );

        addCornerSiteButton(this.width - Layout.CORNER_BUTTON_INSET,
                this.height - Layout.CORNER_BUTTON_INSET,
                CisTierTagger.URL + "?profile=" + nickname, TIP_OPEN_PROFILE);

        int paneX = comboLeft + SKIN_WIDTH + Layout.INNER_GAP;
        int paneY = skinY;

        StringWidget header = new StringWidget(
                Component.literal(nickname).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                this.font);
        header.setX(paneX);
        header.setY(paneY);
        this.addRenderableWidget(header);

        switch (state) {
            case LOADING -> {
                StringWidget loadingWidget = new StringWidget(
                        LOADING.copy().withStyle(ChatFormatting.YELLOW), this.font);
                loadingWidget.setX(paneX);
                loadingWidget.setY(paneY + 16);
                this.addRenderableWidget(loadingWidget);
            }
            case ERROR -> {
                Component msg = errorMessage != null && errorMessage.contains("404")
                        ? NOT_FOUND
                        : Component.literal(errorMessage != null ? errorMessage : "error")
                            .withStyle(ChatFormatting.RED);
                StringWidget errorWidget = new StringWidget(msg, this.font);
                errorWidget.setX(paneX);
                errorWidget.setY(paneY + 16);
                this.addRenderableWidget(errorWidget);
            }
            case LOADED -> renderProfileBody(paneX, paneY);
        }
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    private void renderProfileBody(int paneX, int paneY) {
        if (profile == null) return;

        int rowY = paneY + 14;

        if (profile.rankPosition() != null) {
            Component rankLine = Component.translatable("cistiertagger.profile.rank", profile.rankPosition())
                    .withStyle(ChatFormatting.GRAY);
            StringWidget rankWidget = new StringWidget(rankLine, this.font);
            rankWidget.setX(paneX);
            rankWidget.setY(rowY);
            this.addRenderableWidget(rankWidget);
            rowY += 12;
        }

        int totalPoints = profile.tierStats() == null ? 0 : profile.tierStats().totalPoints();
        Component pointsLine = Component.translatable("cistiertagger.profile.points", totalPoints)
                .withStyle(ChatFormatting.GRAY);
        StringWidget pointsWidget = new StringWidget(pointsLine, this.font);
        pointsWidget.setX(paneX);
        pointsWidget.setY(rowY);
        this.addRenderableWidget(pointsWidget);
        rowY += 18;

        PlayerTierData cached = DumpCache.lookup(nickname).orElse(null);

        if (cached != null && !cached.tiers().isEmpty()) {
            StringWidget tiersHeader = new StringWidget(
                    TIERS_HEADER.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.UNDERLINE),
                    this.font);
            tiersHeader.setX(paneX);
            tiersHeader.setY(rowY);
            this.addRenderableWidget(tiersHeader);
            rowY += 12;

            for (Map.Entry<Gamemode, Tier> entry : cached.tiers().entrySet()) {
                Component badge = Badge.build(entry.getKey(), entry.getValue(), true,
                        ConfigManager.get().getBadgeMode());
                Component tierLine = badge.copy()
                        .append(Component.literal(" +" + entry.getValue().getPoints())
                                .withStyle(ChatFormatting.DARK_GRAY));
                StringWidget tierWidget = new StringWidget(tierLine, this.font);
                tierWidget.setX(paneX);
                tierWidget.setY(rowY);
                this.addRenderableWidget(tierWidget);
                rowY += 11;
            }
            rowY += 8;
        }

        if (profile.tierStats() != null && profile.tierStats().tierHistory() != null
                && !profile.tierStats().tierHistory().isEmpty()) {
            StringWidget historyHeader = new StringWidget(
                    HISTORY_HEADER.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.UNDERLINE),
                    this.font);
            historyHeader.setX(paneX);
            historyHeader.setY(rowY);
            this.addRenderableWidget(historyHeader);
            rowY += 12;

            List<ProfileResponse.TierHistoryEntry> history = profile.tierStats().tierHistory();
            int limit = Math.min(history.size(), 5);
            for (int i = 0; i < limit; i++) {
                ProfileResponse.TierHistoryEntry h = history.get(i);
                Gamemode gm = Gamemode.fromApiKey(h.kit()).orElse(null);
                Tier tier = Tier.fromApiKey(h.tier()).orElse(null);

                Component line;
                if (gm != null && tier != null) {
                    line = Badge.build(gm, tier, true,
                                ConfigManager.get().getBadgeMode())
                            .copy()
                            .append(Component.literal(" " + shortDate(h.date()))
                                    .withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    line = Component.literal(h.kit() + " " + h.tier() + " " + shortDate(h.date()))
                            .withStyle(ChatFormatting.GRAY);
                }
                StringWidget historyWidget = new StringWidget(line, this.font);
                historyWidget.setX(paneX);
                historyWidget.setY(rowY);

                if (h.comment() != null && !h.comment().isBlank()) {
                    historyWidget.setTooltip(Tooltip.create(
                            Component.literal(h.comment()).withStyle(ChatFormatting.GRAY)));
                }
                this.addRenderableWidget(historyWidget);
                rowY += 11;
            }
        }
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, TITLE, this.width / 2, 16, Layout.COLOR_TEXT);
        graphics.fill(20, 38, this.width - 20, 39, Layout.COLOR_ROW_HOVER);
    }

    private static @NotNull String shortDate(@Nullable String iso) {
        if (iso == null) return "";
        int t = iso.indexOf('T');
        return t > 0 ? iso.substring(0, t) : iso;
    }

    private enum LoadState {
        LOADING, LOADED, ERROR
    }
}
