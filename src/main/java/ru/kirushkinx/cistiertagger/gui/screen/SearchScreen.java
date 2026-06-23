package ru.kirushkinx.cistiertagger.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.api.dto.LeaderboardEntry;
import ru.kirushkinx.cistiertagger.api.dto.SearchResultEntry;
import ru.kirushkinx.cistiertagger.cache.DumpCache;
import ru.kirushkinx.cistiertagger.config.ModConfig;
import ru.kirushkinx.cistiertagger.gui.CisTierScreen;
import ru.kirushkinx.cistiertagger.gui.Layout;
import ru.kirushkinx.cistiertagger.gui.button.ItemIconButton;
import ru.kirushkinx.cistiertagger.model.Gamemode;
import ru.kirushkinx.cistiertagger.model.PlayerTierData;
import ru.kirushkinx.cistiertagger.model.Tier;
import ru.kirushkinx.cistiertagger.decorate.Badge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;

public class SearchScreen extends CisTierScreen {

    private static final Component TITLE = Component.translatable("cistiertagger.screen.search.title");
    private static final Component HINT = Component.translatable("cistiertagger.screen.search.hint");
    private static final Component GO = Component.translatable("cistiertagger.button.search");
    private static final Component STATUS_SEARCHING = Component.translatable("cistiertagger.status.searching");
    private static final Component STATUS_NOT_FOUND = Component.translatable("cistiertagger.status.not_found");
    private static final Component STATUS_TOP_LEADERBOARD = Component.translatable("cistiertagger.status.top_leaderboard");
    private static final Component NO_TIER = Component.translatable("cistiertagger.status.no_tier");
    private static final Component TIP_SETTINGS = Component.translatable("cistiertagger.tooltip.open_settings");

    private static final long DEBOUNCE_MS = 250L;
    private static final int MAX_RESULTS = 100;
    private static final int HEAD_SIZE = 16;
    private static final int HEAD_PADDING = 4;

    private EditBox nicknameField;
    private final AtomicLong lastQueryAt = new AtomicLong();
    private volatile @NotNull List<SearchResultEntry> results = Collections.emptyList();
    private volatile int totalMatched;
    private volatile boolean loading;
    private volatile boolean showingTop;
    private volatile @org.jetbrains.annotations.Nullable Component errorMessage;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int dragGrabOffset;

    public SearchScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int boxWidth = 260;

        this.nicknameField = new EditBox(this.font, centerX - boxWidth / 2, 44, boxWidth, 22, TITLE);
        this.nicknameField.setMaxLength(32);
        this.nicknameField.setHint(HINT);
        this.nicknameField.setResponder(this::onQueryChanged);
        this.addRenderableWidget(this.nicknameField);

        this.addRenderableWidget(
                Button.builder(GO, btn -> submit())
                        .bounds(centerX + boxWidth / 2 + 8, 44, 60, 22)
                        .build());

        ItemIconButton settingsButton = new ItemIconButton(
                this.width - Layout.CORNER_BUTTON_INSET, this.height - Layout.CORNER_BUTTON_INSET, Layout.CORNER_BUTTON_SIZE,
                new ItemStack(Items.COMPARATOR),
                btn -> Minecraft.getInstance().setScreen(new ConfigScreen(this)),
                TIP_SETTINGS);
        settingsButton.setTooltip(Tooltip.create(TIP_SETTINGS));
        this.addRenderableWidget(settingsButton);

        addCornerSiteButton(this.width - Layout.CORNER_BUTTON_INSET,
                this.height - Layout.CORNER_BUTTON_INSET - Layout.CORNER_BUTTON_STACK);

        setInitialFocus(this.nicknameField);
        loadTopLeaderboard();
    }

    private void loadTopLeaderboard() {
        var client = CisTierTagger.getHttpClient();
        if (client == null) return;
        long requestedAt = System.currentTimeMillis();
        lastQueryAt.set(requestedAt);
        loading = true;
        showingTop = true;
        client.fetchLeaderboard(1).whenComplete((response, error) ->
                Minecraft.getInstance().execute(() -> applyTopLeaderboard(requestedAt, response, error)));
    }

    private void applyTopLeaderboard(long requestedAt, Map<Integer, List<LeaderboardEntry>> response, Throwable error) {
        if (lastQueryAt.get() != requestedAt) return;
        loading = false;
        if (error != null) {
            errorMessage = Component.translatable("cistiertagger.status.error", error.getMessage());
            results = Collections.emptyList();
            return;
        }
        if (response == null) {
            results = Collections.emptyList();
            return;
        }
        List<SearchResultEntry> next = new ArrayList<>();
        response.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    if (e.getValue() == null) return;
                    for (LeaderboardEntry lb : e.getValue()) {
                        next.add(new SearchResultEntry(
                                lb.nickname(),
                                lb.userId(),
                                null,
                                lb.points(),
                                lb.currentTiers(),
                                lb.customization()));
                    }
                });
        totalMatched = next.size();
        results = next.stream().limit(MAX_RESULTS).toList();
    }

    private void onQueryChanged(@NotNull String value) {
        long requestedAt = System.currentTimeMillis();
        lastQueryAt.set(requestedAt);
        scrollOffset = 0;

        if (value.isBlank()) {
            loadTopLeaderboard();
            errorMessage = null;
            return;
        }

        showingTop = false;
        errorMessage = null;
        loading = true;

        CompletableFuture.delayedExecutor(DEBOUNCE_MS, TimeUnit.MILLISECONDS).execute(() -> {
            if (lastQueryAt.get() != requestedAt) return;
            DumpCache cache = CisTierTagger.getDumpCache();
            if (cache == null || cache.size() == 0) {
                Minecraft.getInstance().execute(() -> applyDumpSearchResult(requestedAt, List.of()));
                return;
            }
            List<PlayerTierData> matches = cache.searchByNickname(value);
            Minecraft.getInstance().execute(() -> applyDumpSearchResult(requestedAt, matches));
        });
    }

    private void applyDumpSearchResult(long requestedAt, @NotNull List<PlayerTierData> matches) {
        if (lastQueryAt.get() != requestedAt) return;
        loading = false;
        totalMatched = matches.size();
        results = matches.stream()
                .limit(MAX_RESULTS)
                .map(SearchScreen::dumpEntryToResult)
                .toList();
    }

    private static @NotNull SearchResultEntry dumpEntryToResult(@NotNull PlayerTierData data) {
        Map<String, String> rawTiers = new LinkedHashMap<>();
        for (Map.Entry<Gamemode, Tier> e : data.tiers().entrySet()) {
            rawTiers.put(e.getKey().apiKey(), e.getValue().apiKey());
        }
        return new SearchResultEntry(data.nickname(), null, null, 0, rawTiers, null);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, TITLE, this.width / 2, 16, Layout.COLOR_TEXT);
        renderResults(graphics, mouseX, mouseY);
        renderStatus(graphics);
    }

    private void renderStatus(@NotNull GuiGraphics graphics) {
        Component statusComponent;
        int color;
        if (errorMessage != null) {
            statusComponent = errorMessage;
            color = Layout.COLOR_ERROR;
        } else if (loading) {
            statusComponent = STATUS_SEARCHING;
            color = Layout.COLOR_WARN;
        } else if (showingTop && !results.isEmpty()) {
            statusComponent = STATUS_TOP_LEADERBOARD;
            color = Layout.COLOR_DIM;
        } else {
            if (results.isEmpty()) {
                statusComponent = STATUS_NOT_FOUND;
            } else {
                statusComponent = Component.translatable("cistiertagger.status.found", totalMatched);
            }
            color = Layout.COLOR_DIM;
        }
        graphics.drawCenteredString(this.font, statusComponent, this.width / 2, this.height - 18, color);
    }

    private void renderResults(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        List<SearchResultEntry> snapshot = this.results;
        if (snapshot.isEmpty()) return;

        int x = Layout.LIST_HORIZONTAL_MARGIN;
        int rowWidth = this.width - Layout.LIST_HORIZONTAL_MARGIN * 2;
        int visible = Math.min(maxVisibleRows(), snapshot.size() - scrollOffset);
        int textOffset = HEAD_PADDING + HEAD_SIZE + 6;

        for (int i = 0; i < visible; i++) {
            SearchResultEntry entry = snapshot.get(scrollOffset + i);
            int rowY = Layout.LIST_TOP_OFFSET + i * Layout.LIST_ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX <= x + rowWidth
                    && mouseY >= rowY && mouseY <= rowY + Layout.LIST_ROW_HEIGHT - 2;
            int bg = hovered ? Layout.COLOR_ROW_HOVER : Layout.COLOR_ROW_BG;
            graphics.fill(x, rowY, x + rowWidth, rowY + Layout.LIST_ROW_HEIGHT - 2, bg);

            int headX = x + HEAD_PADDING;
            int headY = rowY + (Layout.LIST_ROW_HEIGHT - 2 - HEAD_SIZE) / 2;
            drawHead(graphics, entry.nickname(), headX, headY);

            Component nicknameComp = Component.literal(entry.nickname()).withStyle(ChatFormatting.WHITE);
            graphics.drawString(this.font, nicknameComp, x + textOffset, rowY + 7, Layout.COLOR_TEXT, false);

            Component tierLine = topTierComponent(entry);
            int tierX = x + rowWidth - this.font.width(tierLine) - 8;
            graphics.drawString(this.font, tierLine, tierX, rowY + 7, Layout.COLOR_TEXT, false);
        }

        renderScrollbar(graphics, mouseX, mouseY, snapshot);
    }

    private void renderScrollbar(@NotNull GuiGraphics graphics, int mouseX, int mouseY,
                                 @NotNull List<SearchResultEntry> snapshot) {
        if (snapshot.size() <= maxVisibleRows()) return;

        int trackX = scrollbarTrackX();
        int trackY = Layout.LIST_TOP_OFFSET;
        int trackH = scrollbarTrackHeight();
        graphics.fill(trackX, trackY, trackX + Layout.SCROLLBAR_WIDTH, trackY + trackH, Layout.COLOR_SCROLLBAR_TRACK);

        int thumbH = thumbHeight(snapshot.size(), trackH);
        int thumbY = thumbY(snapshot.size(), trackY, trackH, thumbH);
        boolean hovered = mouseX >= trackX && mouseX <= trackX + Layout.SCROLLBAR_WIDTH
                && mouseY >= thumbY && mouseY <= thumbY + thumbH;
        int thumbColor = draggingScrollbar
                ? Layout.COLOR_SCROLLBAR_THUMB_DRAG
                : (hovered ? Layout.COLOR_SCROLLBAR_THUMB_HOVER : Layout.COLOR_SCROLLBAR_THUMB);
        graphics.fill(trackX, thumbY, trackX + Layout.SCROLLBAR_WIDTH, thumbY + thumbH, thumbColor);
    }

    private int maxVisibleRows() {
        int available = this.height - Layout.LIST_TOP_OFFSET - Layout.LIST_BOTTOM_RESERVE;
        return Math.max(1, available / Layout.LIST_ROW_HEIGHT);
    }

    private int scrollbarTrackX() {
        return this.width - Layout.LIST_HORIZONTAL_MARGIN + Layout.SCROLLBAR_GAP;
    }

    private int scrollbarTrackHeight() {
        return maxVisibleRows() * Layout.LIST_ROW_HEIGHT - 2;
    }

    private int thumbHeight(int total, int trackH) {
        int raw = trackH * maxVisibleRows() / total;
        return Math.max(Layout.SCROLLBAR_MIN_THUMB_H, raw);
    }

    private int thumbY(int total, int trackY, int trackH, int thumbH) {
        int maxScroll = total - maxVisibleRows();
        if (maxScroll <= 0) return trackY;
        int travel = trackH - thumbH;
        return trackY + travel * scrollOffset / maxScroll;
    }

    private void setScrollFromY(double mouseY, @NotNull List<SearchResultEntry> snapshot) {
        int trackY = Layout.LIST_TOP_OFFSET;
        int trackH = scrollbarTrackHeight();
        int thumbH = thumbHeight(snapshot.size(), trackH);
        int travel = trackH - thumbH;
        if (travel <= 0) return;
        int maxScroll = snapshot.size() - maxVisibleRows();
        double targetThumbY = mouseY - dragGrabOffset;
        double rel = (targetThumbY - trackY) / travel;
        int next = (int) Math.round(rel * maxScroll);
        scrollOffset = Math.max(0, Math.min(maxScroll, next));
    }

    private void drawHead(@NotNull GuiGraphics graphics, @NotNull String nickname, int x, int y) {
        var skins = CisTierTagger.getSkinCache();
        Identifier id = skins.headFor(nickname).get();
        if (id != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f,
                    HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);
        } else {
            PlayerFaceRenderer.draw(graphics, skins.defaultSkinFor(nickname), x, y, HEAD_SIZE);
        }
    }

    private @NotNull Component topTierComponent(@NotNull SearchResultEntry entry) {
        Map<String, String> rawTiers = entry.currentTiers();
        if (rawTiers == null || rawTiers.isEmpty()) {
            return NO_TIER.copy().withStyle(ChatFormatting.DARK_GRAY);
        }

        ModConfig cfg = CisTierTagger.config();
        PlayerTierData data = toPlayerTierData(entry.nickname(), rawTiers);
        if (data.isEmpty()) {
            return NO_TIER.copy().withStyle(ChatFormatting.DARK_GRAY);
        }

        List<Gamemode> activePriority = cfg.getPriorityOrder().stream()
                .filter(cfg::isGamemodeEnabled)
                .toList();

        Optional<Map.Entry<Gamemode, Tier>> selection = switch (cfg.getDisplayMode()) {
            case PRIORITY -> activePriority.isEmpty()
                    ? Optional.empty()
                    : data.selectByPriority(activePriority);
            case HIGHEST -> data.selectHighest(cfg.getEnabledGamemodes());
        };

        if (selection.isEmpty()) {
            return NO_TIER.copy().withStyle(ChatFormatting.DARK_GRAY);
        }
        return Badge.build(selection.get().getKey(), selection.get().getValue(),
                true, cfg.getIconMode());
    }

    private static @NotNull PlayerTierData toPlayerTierData(@NotNull String nickname,
                                                            @NotNull Map<String, String> rawTiers) {
        EnumMap<Gamemode, Tier> tiers = new EnumMap<>(Gamemode.class);
        for (Map.Entry<String, String> e : rawTiers.entrySet()) {
            Optional<Gamemode> gm = Gamemode.fromApiKey(e.getKey());
            Optional<Tier> tier = Tier.fromApiKey(e.getValue());
            if (gm.isPresent() && tier.isPresent()) {
                tiers.put(gm.get(), tier.get());
            }
        }
        return new PlayerTierData(nickname, tiers, null, "default");
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClicked) {
        List<SearchResultEntry> snapshot = this.results;
        if (event.button() == 0 && !snapshot.isEmpty()) {
            if (handleScrollbarClick(event.x(), event.y(), snapshot)) return true;

            int x = Layout.LIST_HORIZONTAL_MARGIN;
            int rowWidth = this.width - Layout.LIST_HORIZONTAL_MARGIN * 2;
            int visible = Math.min(maxVisibleRows(), snapshot.size() - scrollOffset);
            for (int i = 0; i < visible; i++) {
                int rowY = Layout.LIST_TOP_OFFSET + i * Layout.LIST_ROW_HEIGHT;
                if (event.x() >= x && event.x() <= x + rowWidth
                        && event.y() >= rowY && event.y() <= rowY + Layout.LIST_ROW_HEIGHT - 2) {
                    openProfile(snapshot.get(scrollOffset + i).nickname());
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClicked);
    }

    private boolean handleScrollbarClick(double mouseX, double mouseY,
                                         @NotNull List<SearchResultEntry> snapshot) {
        if (snapshot.size() <= maxVisibleRows()) return false;

        int trackX = scrollbarTrackX();
        int trackY = Layout.LIST_TOP_OFFSET;
        int trackH = scrollbarTrackHeight();
        if (mouseX < trackX || mouseX > trackX + Layout.SCROLLBAR_WIDTH
                || mouseY < trackY || mouseY > trackY + trackH) {
            return false;
        }

        int thumbH = thumbHeight(snapshot.size(), trackH);
        int thumbY = thumbY(snapshot.size(), trackY, trackH, thumbH);

        draggingScrollbar = true;
        if (mouseY >= thumbY && mouseY <= thumbY + thumbH) {
            dragGrabOffset = (int) (mouseY - thumbY);
        } else {
            dragGrabOffset = thumbH / 2;
            setScrollFromY(mouseY, snapshot);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollbar) {
            setScrollFromY(event.y(), this.results);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (draggingScrollbar && event.button() == 0) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<SearchResultEntry> snapshot = this.results;
        if (snapshot.size() > maxVisibleRows()) {
            int delta = scrollY > 0 ? -1 : 1;
            scrollOffset = Math.max(0, Math.min(snapshot.size() - maxVisibleRows(), scrollOffset + delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW_KEY_ENTER || keyCode == GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        return super.keyPressed(event);
    }

    private void submit() {
        List<SearchResultEntry> snapshot = this.results;
        if (!showingTop && !snapshot.isEmpty()) {
            openProfile(snapshot.get(0).nickname());
            return;
        }
        String value = nicknameField.getValue().trim();
        if (!value.isEmpty()) openProfile(value);
    }

    private void openProfile(@NotNull String nickname) {
        errorMessage = null;
        ProfileScreen.openFor(nickname, this);
    }
}
