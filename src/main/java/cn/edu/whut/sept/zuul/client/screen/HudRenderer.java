package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
import cn.edu.whut.sept.zuul.client.ui.UiDrawUtils;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * HUD 渲染器 —— 顶栏、底栏、暂停菜单。
 */
public class HudRenderer
{
    private static final float TOP_BAR_HEIGHT = 56f;
    private static final float FOOTER_HEIGHT = 96f;
    private static final float UI_EDGE = 8f;
    private static final float UI_CHIP_GAP = 8f;
    private static final float UI_CHIP_HEIGHT = 40f;

    private final GameEngine engine;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final GameUiSkin uiSkin;
    private final UiDrawUtils draw;

    public HudRenderer(GameEngine engine, SpriteBatch batch, BitmapFont font,
                        BitmapFont smallFont, GameUiSkin uiSkin, UiDrawUtils draw)
    {
        this.engine = engine;
        this.batch = batch;
        this.font = font;
        this.smallFont = smallFont;
        this.uiSkin = uiSkin;
        this.draw = draw;
    }

    public void drawUiPanels(float width, float height, boolean paused, boolean inventoryOpen,
                              float inventoryPanelHeight)
    {
        uiSkin.drawWindow(batch, UI_EDGE, height - TOP_BAR_HEIGHT - UI_EDGE,
            width - UI_EDGE * 2f, TOP_BAR_HEIGHT);
        uiSkin.drawWindow(batch, UI_EDGE, UI_EDGE, width - UI_EDGE * 2f, FOOTER_HEIGHT);
        uiSkin.drawInset(batch, UI_EDGE + 20f, UI_EDGE + 56f,
            width - (UI_EDGE + 20f) * 2f, 28f);

        if (paused) {
            float pw = draw.grid(Math.min(672f, width - 72f));
            float ph = draw.grid(Math.min(384f, height - 88f));
            float px = draw.grid((width - pw) / 2f);
            float py = draw.grid((height - ph) / 2f);
            uiSkin.drawWindow(batch, px, py, pw, ph);
            uiSkin.drawInset(batch, px + 32f, py + 72f, pw - 64f, ph - 144f);
            uiSkin.drawButton(batch, px + pw / 2f - 96f, py + 24f, 192f, 40f);
        }
        if (inventoryOpen) {
            float pw = draw.grid(Math.min(520f, width - 96f));
            float ph = draw.grid(inventoryPanelHeight);
            float px = draw.grid((width - pw) / 2f);
            float py = draw.grid(Math.max(112f, height - 64f - ph - 16f));
            uiSkin.drawWindow(batch, px, py, pw, ph);
            uiSkin.drawInset(batch, px + 24f, py + 48f, pw - 48f, ph - 84f);
        }
    }

    public void drawHud(float width, float height, boolean paused, String actionMessage)
    {
        float contentX = UI_EDGE + 20f;
        float contentRight = width - UI_EDGE - 20f;
        float topY = height - 24f;
        float barY = height - 58f;
        float barWidth = draw.grid(Math.max(72f, Math.min(176f, (width - 256f) / 2f)));
        float hpX = contentX;
        float weightX = draw.grid(Math.max(width / 2f + 16f, hpX + barWidth + 120f));

        smallFont.setColor(draw.getUiLightText());
        draw.drawClampedLine(batch, smallFont, "玩家 " + engine.getPlayer().getName(), contentX, topY, 180f);
        draw.drawClampedLine(batch, smallFont, "房间 " + engine.getCurrentRoom().getRoomId(),
            contentX + 210f, topY, Math.max(160f, contentRight - contentX - 370f));
        draw.drawRightAligned(batch, "声望 " + engine.getPlayer().getReputation(), contentRight, topY);

        draw.drawStatusBar(batch, "生命", engine.getPlayer().getHp(), engine.getPlayer().getMaxHp(),
            hpX, barY, barWidth, true);
        draw.drawStatusBar(batch, "负重", engine.getPlayer().totalWeight(), engine.getPlayer().getMaxWeight(),
            weightX, barY, barWidth, false);

        if (paused) drawPauseMenu(width, height);
    }

    public void drawFooter(float width, String actionMessage)
    {
        smallFont.setColor(draw.getUiDarkText());
        draw.drawClampedLine(batch, smallFont, "日志", UI_EDGE + 36f, 84f, 40f);
        draw.drawMultilineClamped(batch, smallFont, actionMessage, UI_EDGE + 80f, 84f,
            width - 128f, 16f, 2);

        float gap = UI_CHIP_GAP;
        float totalWidth = 80f + 56f * 3f + 72f + gap * 4f;
        float x = draw.grid(Math.max(20f, (width - totalWidth) / 2f));
        float y = 16f;
        x = draw.drawHintChip(batch, "WASD", x, y, 80f, UI_CHIP_HEIGHT, UiDrawUtils.ICON_MOVE, 40f) + gap;
        x = draw.drawHintChip(batch, "E", x, y, 56f, UI_CHIP_HEIGHT, UiDrawUtils.ICON_TAKE, 28f) + gap;
        x = draw.drawHintChip(batch, "Q", x, y, 56f, UI_CHIP_HEIGHT, UiDrawUtils.ICON_LOOK, 28f) + gap;
        x = draw.drawHintChip(batch, "I", x, y, 56f, UI_CHIP_HEIGHT, UiDrawUtils.ICON_INVENTORY, 28f) + gap;
        draw.drawHintChip(batch, "ESC", x, y, 72f, UI_CHIP_HEIGHT, UiDrawUtils.ICON_MENU, 40f);
    }

    private void drawPauseMenu(float width, float height)
    {
        float pw = draw.grid(Math.min(672f, width - 72f));
        float ph = draw.grid(Math.min(384f, height - 88f));
        float px = draw.grid((width - pw) / 2f);
        float py = draw.grid((height - ph) / 2f);
        float cx = px + pw / 2f;
        float leftX = px + 48f;
        float rightX = px + pw / 2f + 24f;
        float rowY = py + ph - 104f;
        float rowGap = 32f;

        font.setColor(draw.getUiLightText());
        draw.drawCentered(batch, "暂停菜单", cx, py + ph - 36);
        smallFont.setColor(draw.getUiLightText());
        draw.drawCenteredWithSmallFont(batch, "按键说明", cx, py + ph - 64);

        draw.drawShortcutRow(batch, "ESC", "继续探索 / 打开菜单", UiDrawUtils.ICON_MENU, leftX, rowY);
        draw.drawShortcutRow(batch, "WASD", "移动角色", UiDrawUtils.ICON_MOVE, leftX, rowY - rowGap);
        draw.drawShortcutRow(batch, "出口", "走入出口切换房间", UiDrawUtils.ICON_ROOM, leftX, rowY - rowGap * 2f);
        draw.drawShortcutRow(batch, "Q", "调查当前房间", UiDrawUtils.ICON_LOOK, leftX, rowY - rowGap * 3f);
        draw.drawShortcutRow(batch, "B", "回退上一个房间", UiDrawUtils.ICON_BACK, leftX, rowY - rowGap * 4f);
        draw.drawShortcutRow(batch, "T", "返回标题画面", UiDrawUtils.ICON_TITLE, leftX, rowY - rowGap * 5f);

        draw.drawShortcutRow(batch, "E", "拾取地面物品 / 与 NPC 互动", UiDrawUtils.ICON_TAKE, rightX, rowY);
        draw.drawShortcutRow(batch, "I", "打开 / 关闭背包", UiDrawUtils.ICON_INVENTORY, rightX, rowY - rowGap);
        draw.drawShortcutRow(batch, "U", "使用背包中选中的物品", UiDrawUtils.ICON_USE, rightX, rowY - rowGap * 2f);
        draw.drawShortcutRow(batch, "F5", "保存当前进度", UiDrawUtils.ICON_SAVE, rightX, rowY - rowGap * 3f);
        draw.drawShortcutRow(batch, "F9", "读取存档", UiDrawUtils.ICON_LOAD, rightX, rowY - rowGap * 4f);

        smallFont.setColor(draw.getUiDarkText());
        draw.drawCenteredInBoxWithSmallFont(batch, "ESC 继续", cx - 96f, py + 24f, 192f, 40f);
    }
}
