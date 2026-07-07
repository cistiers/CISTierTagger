package ru.kirushkinx.cistiertagger.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kirushkinx.cistiertagger.cache.DumpCache;
import ru.kirushkinx.cistiertagger.cache.SkinCache;
import ru.kirushkinx.cistiertagger.config.ConfigManager;
import ru.kirushkinx.cistiertagger.config.ModConfig;
import ru.kirushkinx.cistiertagger.gui.Layout;
import ru.kirushkinx.cistiertagger.model.Gamemode;
import ru.kirushkinx.cistiertagger.model.PlayerTierData;
import ru.kirushkinx.cistiertagger.model.Tier;
import ru.kirushkinx.cistiertagger.decorate.Badge;
import ru.kirushkinx.cistiertagger.network.ServerRestrictions;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;

public class ConfigScreen extends ModScreen {

    private static final Component TITLE = Component.translatable("cistiertagger.screen.config.title");
    private static final Component RESTRICTED = Component.translatable("cistiertagger.restriction.config");
    private static final Component HEADER_VISIBILITY = Component.translatable("cistiertagger.section.visibility");
    private static final Component HEADER_GAMEMODES = Component.translatable("cistiertagger.section.gamemodes");
    private static final Component HEADER_PRIORITY_SUB = Component.translatable("cistiertagger.subview.priority");
    private static final Component HEADER_GAMEMODES_SUB = Component.translatable("cistiertagger.subview.gamemodes");

    private static final Component LBL_ENABLED = Component.translatable("cistiertagger.button.enabled");
    private static final Component LBL_NAMETAG = Component.translatable("cistiertagger.button.nametag");
    private static final Component LBL_TAB = Component.translatable("cistiertagger.button.tab");
    private static final Component LBL_CHAT = Component.translatable("cistiertagger.button.chat");
    private static final Component LBL_BADGE = Component.translatable("cistiertagger.button.badge");
    private static final Component LBL_POSITION = Component.translatable("cistiertagger.button.position");
    private static final Component LBL_MODE = Component.translatable("cistiertagger.button.mode");
    private static final Component LBL_PRIORITY = Component.translatable("cistiertagger.button.priority");
    private static final Component LBL_GAMEMODES = Component.translatable("cistiertagger.button.gamemodes");
    private static final Component LBL_SAVE = Component.translatable("cistiertagger.button.save");
    private static final Component LBL_DONE = Component.translatable("cistiertagger.button.done");

    private static final Component TIP_ENABLED = Component.translatable("cistiertagger.tooltip.enabled");
    private static final Component TIP_NAMETAG = Component.translatable("cistiertagger.tooltip.nametag");
    private static final Component TIP_TAB = Component.translatable("cistiertagger.tooltip.tab");
    private static final Component TIP_CHAT = Component.translatable("cistiertagger.tooltip.chat");
    private static final Component TIP_BADGE = Component.translatable("cistiertagger.tooltip.badge");
    private static final Component TIP_POSITION = Component.translatable("cistiertagger.tooltip.position");
    private static final Component TIP_MODE = Component.translatable("cistiertagger.tooltip.mode");
    private static final Component TIP_PRIORITY = Component.translatable("cistiertagger.tooltip.priority");
    private static final Component TIP_GAMEMODES = Component.translatable("cistiertagger.tooltip.gamemodes");
    private static final Component TIP_OPEN_SEARCH = Component.translatable("cistiertagger.tooltip.open_search");

    private static final Component ON = Component.translatable("cistiertagger.toggle.on");
    private static final Component OFF = Component.translatable("cistiertagger.toggle.off");
    private static final Component UP = Component.literal("▲");
    private static final Component DOWN = Component.literal("▼");

    private static final int PANE_W = 240;
    private static final int SKIN_W = 96;
    private static final int SKIN_H = 180;

    private @Nullable PlayerSkinWidget skinWidget;
    private @NotNull AtomicReference<Supplier<PlayerSkin>> skinSupplierRef = new AtomicReference<>();
    private int skinX;
    private int skinY;
    private int paneX;
    private int paneTop;
    private @NotNull View view = View.MAIN;
    private @NotNull List<Gamemode> editingOrder = new ArrayList<>();

    public ConfigScreen(@Nullable Screen parent) {
        super(TITLE, parent);
    }

    @Override
    protected void init() {
        if (skinWidget == null) {
            skinSupplierRef = SkinCache.forClientPlayer();
            skinWidget = new PlayerSkinWidget(SKIN_W, SKIN_H, mc.getEntityModels(),
                    () -> skinSupplierRef.get().get());
        }
        recomputeLayout();
        rebuild();
    }

    private void recomputeLayout() {
        int paneTotalHeight = Layout.BUTTON_HEIGHT
                + Layout.SECTION_GAP + Layout.HEADER_OFFSET + Layout.BUTTON_HEIGHT + Layout.GAP_SMALL + Layout.BUTTON_HEIGHT + Layout.GAP_SMALL + Layout.BUTTON_HEIGHT
                + Layout.SECTION_GAP + Layout.HEADER_OFFSET + Layout.BUTTON_HEIGHT + Layout.GAP_SMALL + Layout.BUTTON_HEIGHT;
        int comboLeft = Layout.comboLeft(this.width, SKIN_W, PANE_W);
        skinX = comboLeft;
        paneX = comboLeft + SKIN_W + Layout.INNER_GAP;

        paneTop = Math.max(40, (this.height - paneTotalHeight) / 2);
        skinY = Math.max(40, (this.height - SKIN_H) / 2);
    }

    private void rebuild() {
        if (view == View.PRIORITY) {
            buildPriority();
            return;
        }
        this.clearWidgets();
        skinWidget.setSize(SKIN_W, SKIN_H);
        skinWidget.setPosition(skinX, skinY);
        this.addRenderableWidget(skinWidget);

        switch (view) {
            case MAIN -> buildMain();
            case GAMEMODES -> buildGamemodes();
            default -> { /* unreachable */ }
        }

        addFooterButtons();
    }

    private void addFooterButtons() {
        int saveX = this.width / 2 - Layout.FOOTER_BUTTON_WIDTH / 2;
        int saveY = this.height - Layout.BUTTON_HEIGHT - Layout.FOOTER_BUTTON_BOTTOM_INSET;
        this.addRenderableWidget(Button.builder(LBL_SAVE, btn -> closeAndSave())
                .bounds(saveX, saveY, Layout.FOOTER_BUTTON_WIDTH, Layout.BUTTON_HEIGHT).build());

        addCornerItemButton(
                this.width - Layout.CORNER_BUTTON_INSET, this.height - Layout.CORNER_BUTTON_INSET,
                new ItemStack(Items.SPYGLASS),
                btn -> mc.setScreen(new SearchScreen()),
                TIP_OPEN_SEARCH);

        addCornerSiteButton(this.width - Layout.CORNER_BUTTON_INSET,
                this.height - Layout.CORNER_BUTTON_INSET - Layout.CORNER_BUTTON_STACK);
    }

    private void buildMain() {
        ModConfig cfg = ConfigManager.get();
        int y = paneTop;

        addToggle(paneX, y, PANE_W, LBL_ENABLED, TIP_ENABLED, cfg.isEnabled(), cfg::setEnabled);
        y += Layout.BUTTON_HEIGHT + Layout.SECTION_GAP + Layout.HEADER_OFFSET;

        int thirdW = (PANE_W - Layout.GAP_SMALL * 2) / 3;
        addToggle(paneX, y, thirdW, LBL_NAMETAG, TIP_NAMETAG, cfg.isShowInNametag(), cfg::setShowInNametag);
        addToggle(paneX + thirdW + Layout.GAP_SMALL, y, thirdW, LBL_TAB, TIP_TAB, cfg.isShowInTabList(), cfg::setShowInTabList);
        addToggle(paneX + (thirdW + Layout.GAP_SMALL) * 2, y, thirdW, LBL_CHAT, TIP_CHAT, cfg.isShowInChat(), cfg::setShowInChat);
        y += Layout.BUTTON_HEIGHT + Layout.GAP_SMALL;

        int halfW = (PANE_W - Layout.GAP_SMALL) / 2;
        addCycleEnum(paneX, y, halfW, LBL_BADGE, TIP_BADGE,
                ModConfig.BadgeMode.class, cfg.getBadgeMode(), cfg::setBadgeMode,
                mode -> Component.translatable("cistiertagger.enum.badge_mode." + mode.name().toLowerCase()));
        addCycleEnum(paneX + halfW + Layout.GAP_SMALL, y, halfW, LBL_POSITION, TIP_POSITION,
                ModConfig.Position.class, cfg.getPosition(), cfg::setPosition,
                pos -> Component.translatable("cistiertagger.enum.position." + pos.name().toLowerCase()));
        y += Layout.BUTTON_HEIGHT + Layout.GAP_SMALL;

        addCycleEnum(paneX, y, PANE_W, LBL_MODE, TIP_MODE,
                ModConfig.DisplayMode.class, cfg.getDisplayMode(), cfg::setDisplayMode,
                mode -> Component.translatable("cistiertagger.enum.display_mode." + mode.name().toLowerCase()));
        y += Layout.BUTTON_HEIGHT + Layout.SECTION_GAP + Layout.HEADER_OFFSET;

        this.addRenderableWidget(Button.builder(LBL_PRIORITY, btn -> switchTo(View.PRIORITY))
                .bounds(paneX, y, PANE_W, Layout.BUTTON_HEIGHT)
                .tooltip(Tooltip.create(TIP_PRIORITY)).build());
        y += Layout.BUTTON_HEIGHT + Layout.GAP_SMALL;
        this.addRenderableWidget(Button.builder(LBL_GAMEMODES, btn -> switchTo(View.GAMEMODES))
                .bounds(paneX, y, PANE_W, Layout.BUTTON_HEIGHT)
                .tooltip(Tooltip.create(TIP_GAMEMODES)).build());
    }

    private void buildPriority() {
        editingOrder = new ArrayList<>(ConfigManager.get().getPriorityOrder());
        for (Gamemode gm : Gamemode.values()) {
            if (!editingOrder.contains(gm)) editingOrder.add(gm);
        }
        rebuildPriorityRows();
    }

    private void rebuildPriorityRows() {
        this.clearWidgets();
        skinWidget.setSize(SKIN_W, SKIN_H);
        skinWidget.setPosition(skinX, skinY);
        this.addRenderableWidget(skinWidget);

        int y = paneTop + Layout.HEADER_OFFSET;
        for (int i = 0; i < editingOrder.size(); i++) {
            int idx = i;
            int rowY = y + i * (Layout.BUTTON_HEIGHT + Layout.GAP_SMALL);

            Button up = Button.builder(UP, b -> {
                if (idx <= 0) return;
                Gamemode gm = editingOrder.remove(idx);
                editingOrder.add(idx - 1, gm);
                rebuildPriorityRows();
            }).bounds(paneX, rowY, 24, Layout.BUTTON_HEIGHT).build();
            up.active = i > 0;
            this.addRenderableWidget(up);

            Button down = Button.builder(DOWN, b -> {
                if (idx >= editingOrder.size() - 1) return;
                Gamemode gm = editingOrder.remove(idx);
                editingOrder.add(idx + 1, gm);
                rebuildPriorityRows();
            }).bounds(paneX + 28, rowY, 24, Layout.BUTTON_HEIGHT).build();
            down.active = i < editingOrder.size() - 1;
            this.addRenderableWidget(down);
        }

        int doneY = y + editingOrder.size() * (Layout.BUTTON_HEIGHT + Layout.GAP_SMALL) + Layout.GAP_MEDIUM;
        this.addRenderableWidget(Button.builder(LBL_DONE, b -> {
            ConfigManager.get().setPriorityOrder(new ArrayList<>(editingOrder));
            Badge.bumpGeneration();
            switchTo(View.MAIN);
        }).bounds(paneX, doneY, PANE_W, Layout.BUTTON_HEIGHT).build());

        addFooterButtons();
    }

    private void buildGamemodes() {
        ModConfig cfg = ConfigManager.get();
        Gamemode[] all = Gamemode.values();
        int y = paneTop + Layout.HEADER_OFFSET;
        int toggleW = 80;

        Function<Boolean, Component> formatter = value ->
                value ? ON.copy().withStyle(ChatFormatting.GREEN)
                      : OFF.copy().withStyle(ChatFormatting.RED);

        for (int i = 0; i < all.length; i++) {
            Gamemode gm = all[i];
            int rowY = y + i * (Layout.BUTTON_HEIGHT + Layout.GAP_SMALL);
            CycleButton<Boolean> toggle = CycleButton.<Boolean>builder(formatter).withInitialValue(cfg.isGamemodeEnabled(gm))
                    .withValues(true, false)
                    .displayOnlyValue()
                    .create(paneX + PANE_W - toggleW, rowY, toggleW, Layout.BUTTON_HEIGHT,
                            Component.literal(gm.getDisplayName()),
                            (cycle, value) -> applyEnabled(gm, value));
            this.addRenderableWidget(toggle);
        }

        int doneY = y + all.length * (Layout.BUTTON_HEIGHT + Layout.GAP_SMALL) + Layout.GAP_MEDIUM;
        this.addRenderableWidget(Button.builder(LBL_DONE, b -> switchTo(View.MAIN))
                .bounds(paneX, doneY, PANE_W, Layout.BUTTON_HEIGHT).build());
    }

    private void applyEnabled(@NotNull Gamemode gm, boolean value) {
        ModConfig config = ConfigManager.get();
        Set<Gamemode> current = config.getEnabledGamemodes();
        Set<Gamemode> next = current.isEmpty() ? EnumSet.noneOf(Gamemode.class) : EnumSet.copyOf(current);
        if (value) next.add(gm); else next.remove(gm);
        config.setEnabledGamemodes(next);
        Badge.bumpGeneration();
    }

    private void switchTo(@NotNull View target) {
        this.view = target;
        rebuild();
    }

    private void addToggle(int x, int y, int width,
                           @NotNull Component label,
                           @NotNull Component tooltip,
                           boolean initial,
                           @NotNull Consumer<Boolean> setter) {
        Function<Boolean, Component> formatter = value ->
                Component.empty()
                        .append(label.copy().withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(": "))
                        .append(value ? ON.copy().withStyle(ChatFormatting.GREEN)
                                      : OFF.copy().withStyle(ChatFormatting.RED));
        Tooltip tip = Tooltip.create(tooltip);
        CycleButton<Boolean> button = CycleButton.<Boolean>builder(formatter).withInitialValue(initial)
                .withValues(true, false)
                .withTooltip(value -> tip)
                .displayOnlyValue()
                .create(x, y, width, Layout.BUTTON_HEIGHT, label, (cycle, value) -> {
                    setter.accept(value);
                    Badge.bumpGeneration();
                });
        this.addRenderableWidget(button);
    }

    private <E extends Enum<E>> void addCycleEnum(int x, int y, int width,
                                                  @NotNull Component label,
                                                  @NotNull Component tooltip,
                                                  @NotNull Class<E> enumClass,
                                                  @NotNull E initial,
                                                  @NotNull Consumer<E> setter,
                                                  @NotNull Function<E, Component> valueFormatter) {
        E[] constants = enumClass.getEnumConstants();
        Style whiteStyle = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
        Function<E, Component> formatter = value -> {
            MutableComponent valueComp = valueFormatter.apply(value).copy();
            Style valueStyle = valueComp.getStyle();
            if (valueStyle.getColor() == null) {
                valueComp.setStyle(valueStyle.withColor(TextColor.fromRgb(0xFFFFFF)));
            }
            return Component.empty()
                    .append(label.copy().setStyle(whiteStyle))
                    .append(Component.literal(": ").setStyle(whiteStyle))
                    .append(valueComp);
        };
        Tooltip tip = Tooltip.create(tooltip);
        CycleButton<E> button = CycleButton.<E>builder(formatter).withInitialValue(initial)
                .withValues(constants)
                .withTooltip(value -> tip)
                .displayOnlyValue()
                .create(x, y, width, Layout.BUTTON_HEIGHT, label, (cycle, value) -> {
                    setter.accept(value);
                    Badge.bumpGeneration();
                });
        this.addRenderableWidget(button);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, TITLE, this.width / 2, 14, Layout.COLOR_TEXT);
        if (ServerRestrictions.isAnyRestricted()) {
            int noticeY = (14 + this.font.lineHeight + paneTop) / 2 - this.font.lineHeight / 2;
            graphics.drawCenteredString(this.font, RESTRICTED, this.width / 2, noticeY, Layout.COLOR_WARN);
        }

        renderPreviewNametag(graphics);
        switch (view) {
            case MAIN -> renderMainHeaders(graphics);
            case PRIORITY -> renderPriorityList(graphics);
            case GAMEMODES -> renderGamemodesList(graphics);
        }
    }

    private void renderMainHeaders(@NotNull GuiGraphics graphics) {
        int y = paneTop + Layout.BUTTON_HEIGHT + Layout.SECTION_GAP - 2;
        graphics.drawString(this.font,
                HEADER_VISIBILITY.copy().withStyle(ChatFormatting.GRAY),
                paneX, y, Layout.COLOR_HEADER, false);

        y += Layout.HEADER_OFFSET + (Layout.BUTTON_HEIGHT + Layout.GAP_SMALL) * 3 + Layout.SECTION_GAP - 2;
        graphics.drawString(this.font,
                HEADER_GAMEMODES.copy().withStyle(ChatFormatting.GRAY),
                paneX, y, Layout.COLOR_HEADER, false);
    }

    private void renderPriorityList(@NotNull GuiGraphics graphics) {
        graphics.drawString(this.font,
                HEADER_PRIORITY_SUB.copy().withStyle(ChatFormatting.GRAY),
                paneX, paneTop, Layout.COLOR_HEADER, false);

        int labelX = paneX + 24 + 4 + 24 + 8;
        int y = paneTop + Layout.HEADER_OFFSET;
        for (int i = 0; i < editingOrder.size(); i++) {
            int rowY = y + i * (Layout.BUTTON_HEIGHT + Layout.GAP_SMALL);
            Gamemode gm = editingOrder.get(i);
            Component idx = Component.literal((i + 1) + ".").withStyle(ChatFormatting.DARK_GRAY);
            graphics.drawString(this.font, idx, labelX, rowY + 7, 0xFFAAAAAA, false);
            graphics.drawString(this.font,
                    Badge.gamemodeLabel(gm, ConfigManager.get().getBadgeMode()),
                    labelX + 16, rowY + 7, Layout.COLOR_TEXT, false);
        }
    }

    private void renderGamemodesList(@NotNull GuiGraphics graphics) {
        graphics.drawString(this.font,
                HEADER_GAMEMODES_SUB.copy().withStyle(ChatFormatting.GRAY),
                paneX, paneTop, Layout.COLOR_HEADER, false);

        Gamemode[] all = Gamemode.values();
        int y = paneTop + Layout.HEADER_OFFSET;
        for (int i = 0; i < all.length; i++) {
            int rowY = y + i * (Layout.BUTTON_HEIGHT + Layout.GAP_SMALL);
            graphics.drawString(this.font,
                    Badge.gamemodeLabel(all[i], ConfigManager.get().getBadgeMode()),
                    paneX + 6, rowY + 7, Layout.COLOR_TEXT, false);
        }
    }

    private void renderPreviewNametag(@NotNull GuiGraphics graphics) {
        if (skinWidget == null) return;
        Component preview = buildPreviewName();
        int textWidth = this.font.width(preview);
        int previewCenterX = skinX + SKIN_W / 2;
        int previewY = skinY - 22;
        float scale = 1.4f;

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(previewCenterX, previewY);
        pose.scale(scale, scale);
        graphics.drawString(this.font, preview, -textWidth / 2, 0, Layout.COLOR_TEXT, true);
        pose.popMatrix();
    }

    private @NotNull Component buildPreviewName() {
        String selfName = mc.getGameProfile().getName();
        Component base = Component.literal(selfName).withStyle(ChatFormatting.WHITE);

        ModConfig cfg = ConfigManager.get();
        if (!cfg.isEnabled() || !cfg.isShowInNametag()) return base;

        PlayerTierData realData = DumpCache.lookup(selfName).orElse(null);

        if (realData != null) {
            Component decorated = Badge.decorate(selfName, base, Badge.DisplaySurface.NAMETAG, false);
            return decorated != null ? decorated : base;
        }

        return Badge.preview(Gamemode.VANILLA, Tier.LT3,
                cfg.getPosition() == ModConfig.Position.LEFT,
                cfg.getBadgeMode(), base);
    }

    private void closeAndSave() {
        ConfigManager.save();
        returnToParent();
    }

    @Override
    public void onClose() {
        if (view != View.MAIN) {
            switchTo(View.MAIN);
            return;
        }
        closeAndSave();
    }

    public static @NotNull Screen create(@Nullable Screen parent) {
        return new ConfigScreen(parent);
    }

    private enum View {
        MAIN, PRIORITY, GAMEMODES
    }
}
