package ru.kirushkinx.cistiertagger.gui.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?}
//? if <26.1 {
//import net.minecraft.world.item.ItemStack;
//?}
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kirushkinx.cistiertagger.CisTierTagger;
import ru.kirushkinx.cistiertagger.gui.Layout;
//? if <26.1 {
//import ru.kirushkinx.cistiertagger.gui.button.ItemIconButton;
//?}
import ru.kirushkinx.cistiertagger.gui.button.TextureIconButton;

//? if >=1.21 {
import java.net.URI;
//?}

import static ru.kirushkinx.cistiertagger.CisTierTagger.mc;

/** Base screen with shared helpers for corner buttons and parent navigation. */
public abstract class ModScreen extends Screen {

    protected static final Component TIP_OPEN_SITE = Component.translatable("cistiertagger.tooltip.open_site");

    protected final @Nullable Screen parent;

    protected ModScreen(@NotNull Component title) {
        this(title, null);
    }

    protected ModScreen(@NotNull Component title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    public static Screen assetsUnavailable(@Nullable Screen parent) {
        return new AlertScreen(() -> mc.gui.setScreen(parent),
                Component.translatable("cistiertagger.unavailable.title").withStyle(ChatFormatting.RED),
                Component.translatable("cistiertagger.unavailable.description").withStyle(ChatFormatting.GRAY));
    }

    protected void returnToParent() {
        mc.gui.setScreen(parent);
    }

    /** Adds the corner logo button that opens cistiers.com through the link-confirm screen. */
    protected void addCornerSiteButton(int x, int y) {
        addCornerSiteButton(x, y, CisTierTagger.URL, TIP_OPEN_SITE);
    }

    protected void addCornerSiteButton(int x, int y, @NotNull String url, @NotNull Component tooltip) {
//? if >=26.1 {
        addCornerIconButton(x, y, Layout.LOGO_TEXTURE,
                btn -> ConfirmLinkScreen.confirmLinkNow(this, URI.create(url)),
                tooltip);
    }

    protected void addCornerIconButton(int x, int y,
                                       @NotNull Identifier icon,
                                       @NotNull Button.OnPress onPress,
                                       @NotNull Component tooltip) {
//?}
        TextureIconButton button = new TextureIconButton(
                x, y,
                Layout.CORNER_BUTTON_SIZE, Layout.CORNER_BUTTON_ICON_SIZE,
//? if >=26.1 {
                icon, onPress, tooltip);
//?}
//? if >=1.21 <26.1 {
                /*Layout.LOGO_TEXTURE,
                btn -> ConfirmLinkScreen.confirmLinkNow(this, URI.create(url)),
                tooltip);
        button.setTooltip(Tooltip.create(tooltip));
        this.addRenderableWidget(button);
    }

    protected void addCornerItemButton(int x, int y,
                                       @NotNull ItemStack icon,
                                       @NotNull Button.OnPress onPress,
                                       @NotNull Component tooltip) {
        ItemIconButton button = new ItemIconButton(
                x, y, Layout.CORNER_BUTTON_SIZE, icon, onPress, tooltip);
*///?}
//? if >=1.20.3 <1.21 {
                /*Layout.LOGO_TEXTURE,
                btn -> ConfirmLinkScreen.confirmLinkNow(this, url),
                tooltip);
        button.setTooltip(Tooltip.create(tooltip));
        this.addRenderableWidget(button);
    }

    protected void addCornerItemButton(int x, int y,
                                       @NotNull ItemStack icon,
                                       @NotNull Button.OnPress onPress,
                                       @NotNull Component tooltip) {
        ItemIconButton button = new ItemIconButton(
                x, y, Layout.CORNER_BUTTON_SIZE, icon, onPress, tooltip);
*///?}
//? if <1.20.3 {
                /*Layout.LOGO_TEXTURE,
                btn -> ConfirmLinkScreen.confirmLinkNow(url, this, false),
                tooltip);
        button.setTooltip(Tooltip.create(tooltip));
        this.addRenderableWidget(button);
    }

    protected void addCornerItemButton(int x, int y,
                                       @NotNull ItemStack icon,
                                       @NotNull Button.OnPress onPress,
                                       @NotNull Component tooltip) {
        ItemIconButton button = new ItemIconButton(
                x, y, Layout.CORNER_BUTTON_SIZE, icon, onPress, tooltip);
*///?}
        button.setTooltip(Tooltip.create(tooltip));
        this.addRenderableWidget(button);
    }
}
