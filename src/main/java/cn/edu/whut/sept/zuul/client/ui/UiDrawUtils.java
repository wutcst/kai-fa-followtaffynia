package cn.edu.whut.sept.zuul.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

/**
 * UI 绘制工具类 —— 从 GameScreen 提取的纯工具方法（无状态依赖）。
 */
public class UiDrawUtils
{
    public static final int ICON_MOVE = 0;
    public static final int ICON_ROOM = 1;
    public static final int ICON_LOOK = 2;
    public static final int ICON_TAKE = 3;
    public static final int ICON_INVENTORY = 4;
    public static final int ICON_BACK = 5;
    public static final int ICON_SAVE = 6;
    public static final int ICON_LOAD = 7;
    public static final int ICON_MENU = 8;
    public static final int ICON_TITLE = 9;
    public static final int ICON_USE = 10;
    private static final float UI_HEADER_HEIGHT = 32f;

    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final GameUiSkin uiSkin;
    private final GlyphLayout layout;
    private final Color uiLightText;
    private final Color uiDarkText;
    private final float uiGrid;

    public UiDrawUtils(BitmapFont font, BitmapFont smallFont, GameUiSkin uiSkin,
                       GlyphLayout layout, Color uiLightText, Color uiDarkText, float uiGrid)
    {
        this.font = font;
        this.smallFont = smallFont;
        this.uiSkin = uiSkin;
        this.layout = layout;
        this.uiLightText = uiLightText;
        this.uiDarkText = uiDarkText;
        this.uiGrid = uiGrid;
    }

    public Color getUiLightText() { return uiLightText; }
    public Color getUiDarkText() { return uiDarkText; }
    public BitmapFont getFont() { return font; }
    public BitmapFont getSmallFont() { return smallFont; }
    public GameUiSkin getUiSkin() { return uiSkin; }
    public GlyphLayout getLayout() { return layout; }
    public float getUiGrid() { return uiGrid; }

    public float grid(float value)
    {
        return Math.round(value / uiGrid) * uiGrid;
    }

    public void drawIcon(SpriteBatch batch, int icon, float x, float y, float size)
    {
        if (icon == ICON_MOVE) {
            uiSkin.drawMoveIcon(batch, x, y, size);
        } else if (icon == ICON_ROOM) {
            uiSkin.drawRoomIcon(batch, x, y, size);
        } else if (icon == ICON_LOOK) {
            uiSkin.drawLookIcon(batch, x, y, size);
        } else if (icon == ICON_TAKE) {
            uiSkin.drawHandIcon(batch, x, y, size);
        } else if (icon == ICON_INVENTORY) {
            uiSkin.drawInventoryIcon(batch, x, y, size);
        } else if (icon == ICON_BACK) {
            uiSkin.drawBackIcon(batch, x, y, size);
        } else if (icon == ICON_SAVE) {
            uiSkin.drawSaveIcon(batch, x, y, size);
        } else if (icon == ICON_LOAD) {
            uiSkin.drawLoadIcon(batch, x, y, size);
        } else if (icon == ICON_MENU) {
            uiSkin.drawMenuIcon(batch, x, y, size);
        } else if (icon == ICON_TITLE) {
            uiSkin.drawTitleIcon(batch, x, y, size);
        } else if (icon == ICON_USE) {
            uiSkin.drawUseIcon(batch, x, y, size);
        } else {
            uiSkin.drawCircleIcon(batch, x, y, size);
        }
    }

    public void drawCentered(SpriteBatch batch, String text, float centerX, float y)
    {
        layout.setText(font, text);
        font.draw(batch, text, Math.round(centerX - layout.width / 2f), Math.round(y));
    }

    public void drawCenteredWithSmallFont(SpriteBatch batch, String text, float centerX, float y)
    {
        layout.setText(smallFont, text);
        smallFont.draw(batch, text, Math.round(centerX - layout.width / 2f), Math.round(y));
    }

    public void drawCenteredInBoxWithSmallFont(SpriteBatch batch, String text, float x, float y,
                                                float width, float height)
    {
        layout.setText(smallFont, text);
        smallFont.draw(batch, text, Math.round(x + (width - layout.width) / 2f),
            Math.round(y + (height + layout.height) / 2f + 1f));
    }

    public void drawShortcutRow(SpriteBatch batch, String key, String label, int icon,
                                 float x, float y)
    {
        drawIcon(batch, icon, x, y + 3, 24);
        float keyWidth = key.length() >= 4 ? 54f : key.length() >= 3 ? 42f : 34f;
        uiSkin.drawButton(batch, x + 34, y, keyWidth, 28);
        smallFont.setColor(Color.WHITE);
        drawCenteredInBoxWithSmallFont(batch, key, x + 34, y, keyWidth, 28);
        smallFont.setColor(uiDarkText);
        drawClampedLine(batch, smallFont, label, x + keyWidth + 78, y + 20, 210f);
    }

    public void drawClampedLine(SpriteBatch batch, BitmapFont activeFont, String text,
                                 float x, float y, float maxWidth)
    {
        String line = text == null ? "" : text.replace('\n', ' ');
        layout.setText(activeFont, line);
        if (layout.width <= maxWidth) {
            activeFont.draw(batch, line, x, y);
            return;
        }

        String suffix = "...";
        while (line.length() > 1) {
            line = line.substring(0, line.length() - 1);
            layout.setText(activeFont, line + suffix);
            if (layout.width <= maxWidth) {
                activeFont.draw(batch, line + suffix, x, y);
                return;
            }
        }
        activeFont.draw(batch, suffix, x, y);
    }

    public void drawMultilineClamped(SpriteBatch batch, BitmapFont activeFont, String text,
                                      float x, float y, float maxWidth, float lineHeight,
                                      int maxLines)
    {
        String source = text == null ? "" : text.replace('\n', ' ');
        int start = 0;
        int lineCount = 0;
        while (start < source.length() && lineCount < maxLines) {
            int end = source.length();
            String line = source.substring(start, end);
            layout.setText(activeFont, line);
            while (line.length() > 1 && layout.width > maxWidth) {
                end--;
                line = source.substring(start, end);
                layout.setText(activeFont, line + (lineCount == maxLines - 1 ? "..." : ""));
            }
            if (lineCount == maxLines - 1 && end < source.length()) {
                activeFont.draw(batch, line + "...", x, y - lineHeight * lineCount);
                return;
            }
            activeFont.draw(batch, line, x, y - lineHeight * lineCount);
            start = end;
            while (start < source.length() && source.charAt(start) == ' ') {
                start++;
            }
            lineCount++;
        }
    }

    public void drawRightAligned(SpriteBatch batch, String text, float rightX, float y)
    {
        layout.setText(smallFont, text);
        smallFont.draw(batch, text, rightX - layout.width, y);
    }

    public void drawMultiline(SpriteBatch batch, String text, float x, float y, float lineHeight)
    {
        String[] lines = text.split("\\n");
        float lineY = y;
        for (String line : lines) {
            font.draw(batch, line, x, lineY);
            lineY -= lineHeight;
        }
    }

    public float drawHintChip(SpriteBatch batch, String key, float x, float y, float width,
                               float height, int icon, float keyWidth)
    {
        x = grid(x);
        y = grid(y);
        width = grid(width);
        height = grid(height);
        keyWidth = grid(keyWidth);
        uiSkin.drawLightButton(batch, x, y, width, height);
        uiSkin.drawButton(batch, x + 8f, y + 8f, keyWidth, height - 16f);
        drawIcon(batch, icon, x + keyWidth + 14f, y + 8f, height - 16f);

        smallFont.setColor(Color.WHITE);
        drawCenteredInBoxWithSmallFont(batch, key, x + 8f, y + 8f, keyWidth, height - 16f);
        return x + width;
    }

    public void drawPanelHeader(SpriteBatch batch, String title, int icon, float x, float y,
                                 float width)
    {
        if (width <= 42f) {
            return;
        }
        uiSkin.drawButton(batch, grid(x), grid(y), grid(width), UI_HEADER_HEIGHT);
        drawIcon(batch, icon, x + 8f, y + 8f, 16f);
        smallFont.setColor(uiLightText);
        drawClampedLine(batch, smallFont, title, x + 32f, y + 22f, width - 40f);
    }

    public void drawSideShortcut(SpriteBatch batch, String key, String label, int icon,
                                  float x, float y, float width)
    {
        y = grid(y);
        drawIcon(batch, icon, x, y + 4f, 16f);
        uiSkin.drawButton(batch, x + 24f, y, 40f, 24f);
        smallFont.setColor(Color.WHITE);
        drawCenteredInBoxWithSmallFont(batch, key, x + 24f, y, 40f, 24f);
        smallFont.setColor(uiDarkText);
        drawClampedLine(batch, smallFont, label, x + 72f, y + 18f, Math.max(24f, width - 72f));
    }

    public void drawCompactStatusBar(SpriteBatch batch, String label, int value, int maxValue,
                                      float x, float y, float width, boolean hpBar)
    {
        smallFont.setColor(uiDarkText);
        drawClampedLine(batch, smallFont, label + " " + value + "/" + maxValue, x, y + 26f, width);
        float ratio = maxValue <= 0 ? 0f : (float) value / maxValue;
        if (hpBar) {
            uiSkin.drawRedBar(batch, x, y, width, 14f, ratio);
        } else {
            uiSkin.drawYellowBar(batch, x, y, width, 14f, ratio);
        }
    }

    public void drawStatusBar(SpriteBatch batch, String label, int value, int maxValue,
                               float x, float y, float width, boolean hpBar)
    {
        smallFont.setColor(uiLightText);
        smallFont.draw(batch, label, x, y + 16);
        float barX = x + 42;
        float ratio = maxValue <= 0 ? 0f : (float) value / maxValue;
        if (hpBar) {
            uiSkin.drawRedBar(batch, barX, y, width, 18, ratio);
        } else {
            uiSkin.drawYellowBar(batch, barX, y, width, 18, ratio);
        }
        smallFont.draw(batch, value + "/" + maxValue, barX + width + 10, y + 16);
    }
}
