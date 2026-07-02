package ru.kirushkinx.cistiertagger.gui;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceLocation;
import ru.kirushkinx.cistiertagger.CisTierTagger;

/** Shared spacing, size and color constants to stay screens visually consistent. */
@UtilityClass
public class Layout {

    public static final int SCREEN_EDGE_MARGIN = 20;
    public static final int INNER_GAP = 30;
    public static final int GAP_SMALL = 4;
    public static final int GAP_MEDIUM = 8;
    public static final int SECTION_GAP = 14;
    public static final int HEADER_OFFSET = 12;

    public static final int BUTTON_HEIGHT = 22;
    public static final int CORNER_BUTTON_SIZE = 22;
    public static final int CORNER_BUTTON_ICON_SIZE = 16;
    public static final int CORNER_BUTTON_INSET = 30;
    public static final int CORNER_BUTTON_STACK = 26;

    public static final int FOOTER_BUTTON_WIDTH = 220;
    public static final int FOOTER_BUTTON_BOTTOM_INSET = 12;

    public static final int LIST_BOTTOM_RESERVE = 60;
    public static final int LIST_TOP_OFFSET = 80;
    public static final int LIST_HORIZONTAL_MARGIN = 60;
    public static final int LIST_ROW_HEIGHT = 22;

    public static final int SCROLLBAR_WIDTH = 6;
    public static final int SCROLLBAR_GAP = 4;
    public static final int SCROLLBAR_MIN_THUMB_H = 16;

    public static final ResourceLocation LOGO_TEXTURE = ResourceLocation.fromNamespaceAndPath(CisTierTagger.MOD_ID, "textures/logo.png");

    public static final int COLOR_TEXT = 0xFFFFFFFF;
    public static final int COLOR_HEADER = 0xFFCCCCCC;
    public static final int COLOR_DIM = 0xFF888888;
    public static final int COLOR_WARN = 0xFFFFAA00;
    public static final int COLOR_ERROR = 0xFFFF5555;
    public static final int COLOR_ROW_BG = 0x55000000;
    public static final int COLOR_ROW_HOVER = 0x66FFFFFF;
    public static final int COLOR_SCROLLBAR_TRACK = 0x55000000;
    public static final int COLOR_SCROLLBAR_THUMB = 0x88AAAAAA;
    public static final int COLOR_SCROLLBAR_THUMB_HOVER = 0xAAFFFFFF;
    public static final int COLOR_SCROLLBAR_THUMB_DRAG = 0xCCFFFFFF;

    /** Left X for a centered "skin + gap + pane" block, clamped to the screen edges. */
    public static int comboLeft(int screenWidth, int skinWidth, int paneWidth) {
        int comboWidth = skinWidth + INNER_GAP + paneWidth;
        int centerX = screenWidth / 2;
        int left = Math.max(SCREEN_EDGE_MARGIN, centerX - comboWidth / 2);
        if (left + comboWidth > screenWidth - SCREEN_EDGE_MARGIN) {
            left = Math.max(SCREEN_EDGE_MARGIN, screenWidth - SCREEN_EDGE_MARGIN - comboWidth);
        }
        return left;
    }
}
