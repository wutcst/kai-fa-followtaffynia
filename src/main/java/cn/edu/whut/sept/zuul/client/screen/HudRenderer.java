package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
import cn.edu.whut.sept.zuul.client.ui.UiDrawUtils;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.QuestManager;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * HUD 渲染器 —— 顶栏（5 区布局）、底栏、暂停菜单。
 * 坐标基于设计分辨率 1280×720，通过 CameraController 等比缩放。
 */
public class HudRenderer
{
    /** 顶栏高度（设计分辨率下，10% 屏高） */
    public static final float TOP_BAR_HEIGHT = 72f;
    /** 底栏高度（10% 屏高） */
    static final float FOOTER_HEIGHT = 72f;
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

    /** 画 UI 背景面板 */
    public void drawUiPanels(float width, float height, boolean paused, boolean inventoryOpen,
                              float inventoryPanelHeight)
    {
        // 顶栏面板 — 全宽
        uiSkin.drawWindow(batch, 0, height - TOP_BAR_HEIGHT, width, TOP_BAR_HEIGHT);
        // 底栏面板 — 全宽
        uiSkin.drawWindow(batch, 0, 0, width, FOOTER_HEIGHT);

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

    /** 画顶栏 HUD —— 5 区单行布局 */
    public void drawHud(float width, float height, boolean paused, String actionMessage)
    {
        float padX = 20f;
        float availW = width - padX * 2f;
        float barCenterY = height - TOP_BAR_HEIGHT / 2f;     // 72px 条垂直中心
        float barH = 16f;
        float barY = barCenterY - barH / 2f;
        float lineY = barCenterY + 9f;                        // 统一文字基线

        // ---- 5 区均分 ----
        float zoneW = draw.grid(availW / 5f);

        float x1 = padX;
        float x2 = x1 + zoneW;
        float x3 = x2 + zoneW;
        float x4 = x3 + zoneW;
        float x5 = x4 + zoneW;
        float divH = 32f;

        // ---- 区 1：玩家名 ----
        smallFont.setColor(draw.getUiLightText());
        draw.drawClampedLine(batch, smallFont, "玩家 " + engine.getPlayer().getName(),
            x1 + 8f, lineY, zoneW - 16f);
        drawHudDivider(x2, barCenterY - divH / 2f, divH);

        // ---- 区 2：生命条（标签 + 条 + 数值，同行） ----
        smallFont.setColor(draw.getUiLightText());
        float hpPad = 6f;
        smallFont.draw(batch, "生命", x2 + hpPad, lineY);
        float hpBarX = x2 + hpPad + 50f;
        float hpBarW = zoneW - 50f - hpPad - 12f - 58f;
        draw.drawStatusBarCompact(batch, engine.getPlayer().getHp(), engine.getPlayer().getMaxHp(),
            hpBarX, barY, hpBarW, barH, true);
        smallFont.draw(batch,
            engine.getPlayer().getHp() + "/" + engine.getPlayer().getMaxHp(),
            hpBarX + hpBarW + 8f, lineY);
        drawHudDivider(x3, barCenterY - divH / 2f, divH);

        // ---- 区 3：房间名（单行） ----
        smallFont.setColor(draw.getUiLightText());
        String roomText = "房间 " + engine.getCurrentRoom().getRoomId();
        draw.drawClampedLine(batch, smallFont, roomText, x3 + 8f, lineY, zoneW - 16f);
        drawHudDivider(x4, barCenterY - divH / 2f, divH);

        // ---- 区 4：负重条（标签 + 条 + 数值，同行） ----
        smallFont.setColor(draw.getUiLightText());
        float wtPad = 6f;
        smallFont.draw(batch, "负重", x4 + wtPad, lineY);
        float wtBarX = x4 + wtPad + 50f;
        float wtBarW = zoneW - 50f - wtPad - 12f - 70f;
        draw.drawStatusBarCompact(batch, engine.getPlayer().totalWeight(),
            (double) engine.getPlayer().getMaxWeight(),
            wtBarX, barY, wtBarW, barH, false);
        smallFont.draw(batch,
            String.format("%.1f/%.0f", engine.getPlayer().totalWeight(),
                (double) engine.getPlayer().getMaxWeight()),
            wtBarX + wtBarW + 8f, lineY);
        drawHudDivider(x5, barCenterY - divH / 2f, divH);

        // ---- 区 5：声望值（单行） ----
        smallFont.setColor(draw.getUiLightText());
        draw.drawClampedLine(batch, smallFont,
            "声望 " + engine.getPlayer().getReputation(),
            x5 + 8f, lineY, zoneW - 16f);

        if (paused) drawPauseMenu(width, height);
    }

    /** 画各分区间隔线 */
    private void drawHudDivider(float x, float y, float height)
    {
        smallFont.setColor(draw.getUiDarkText().r, draw.getUiDarkText().g,
            draw.getUiDarkText().b, 0.3f);
        smallFont.draw(batch, "|", x - 4f, y + height - 8f);
    }

    public void drawFooter(float width, String actionMessage)
    {
        float ftCenter = FOOTER_HEIGHT / 2f;                 // 底栏垂直中心
        float logW = width * 0.60f;                          // 左区：日志

        // ---- 左区：日志嵌入面板 ----
        float insetX = 12f, insetY = 10f;
        float insetW = logW - insetX - 8f, insetH = FOOTER_HEIGHT - insetY * 2f;
        uiSkin.drawInset(batch, insetX, insetY, insetW, insetH);
        smallFont.setColor(draw.getUiDarkText());
        draw.drawClampedLine(batch, smallFont, "日志", insetX + 16f, insetY + insetH - 10f, 40f);
        smallFont.setColor(draw.getUiLightText());
        draw.drawClampedLine(batch, smallFont, actionMessage,
            insetX + 60f, insetY + insetH - 10f, insetW - 72f);

        // ---- 右区：按键 + 功能提示（双行） ----
        String[] keys  = { "WASD", "SPACE", "E",  "Q",  "I",  "M",  "ESC" };
        String[] hints = { "移动", "冲刺", "互动", "调查", "背包", "地图", "菜单" };
        int n = keys.length;
        float segW = (width - logW - 12f) / n;
        float keyY = ftCenter + 12f;   // 键名靠上
        float hintY = ftCenter - 10f;  // 功能提示靠下

        smallFont.setColor(draw.getUiLightText());
        for (int i = 0; i < n; i++) {
            float cx = logW + 12f + segW * i + segW / 2f;
            draw.drawCenteredWithSmallFont(batch, keys[i], cx, keyY);
            draw.drawCenteredWithSmallFont(batch, hints[i], cx, hintY);
        }
    }

    public void drawQuestTracker(float width, float height, int worldViewportX, int worldViewportWidth)
    {
        float leftSpace = worldViewportX;
        float rightSpace = width - (worldViewportX + worldViewportWidth);
        float sideSpace = Math.max(leftSpace, rightSpace);
        if (sideSpace < 132f || height < 430f) {
            return;
        }

        boolean useRight = rightSpace >= leftSpace;
        float panelW = draw.grid(Math.min(248f, sideSpace - 16f));
        float panelH = draw.grid(Math.min(176f, height - TOP_BAR_HEIGHT - FOOTER_HEIGHT - 48f));
        if (panelW < 112f || panelH < 136f) {
            return;
        }

        float panelX = useRight
            ? draw.grid(worldViewportX + worldViewportWidth + (rightSpace - panelW) / 2f)
            : draw.grid((leftSpace - panelW) / 2f);
        float panelY = draw.grid(height - TOP_BAR_HEIGHT - panelH - 24f);
        uiSkin.drawWindow(batch, panelX, panelY, panelW, panelH);
        draw.drawPanelHeader(batch, "当前目标", UiDrawUtils.ICON_ROOM,
            panelX + 12f, panelY + panelH - 42f, panelW - 24f);

        String[] lines = objectiveLines();
        float textX = panelX + 20f;
        float textY = panelY + panelH - 62f;
        smallFont.setColor(draw.getUiLightText());
        draw.drawMultilineClamped(batch, smallFont, lines[0], textX, textY,
            panelW - 40f, 16f, 2);

        smallFont.setColor(draw.getUiDarkText());
        draw.drawClampedLine(batch, smallFont, lines[1], textX, panelY + 66f, panelW - 40f);
        draw.drawClampedLine(batch, smallFont, lines[2], textX, panelY + 46f, panelW - 40f);
        draw.drawClampedLine(batch, smallFont, lines[3], textX, panelY + 26f, panelW - 40f);
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
        draw.drawShortcutRow(batch, "WASD", "移动角色 / 方向键", UiDrawUtils.ICON_MOVE, leftX, rowY - rowGap);
        draw.drawShortcutRow(batch, "出口", "走入出口切换房间", UiDrawUtils.ICON_ROOM, leftX, rowY - rowGap * 2f);
        draw.drawShortcutRow(batch, "Q", "调查当前房间", UiDrawUtils.ICON_LOOK, leftX, rowY - rowGap * 3f);
        draw.drawShortcutRow(batch, "B", "回退上一个房间", UiDrawUtils.ICON_BACK, leftX, rowY - rowGap * 4f);
        draw.drawShortcutRow(batch, "T", "返回标题画面", UiDrawUtils.ICON_TITLE, leftX, rowY - rowGap * 5f);

        draw.drawShortcutRow(batch, "E", "拾取地面物品 / 与 NPC 互动", UiDrawUtils.ICON_TAKE, rightX, rowY);
        draw.drawShortcutRow(batch, "I", "打开 / 关闭背包", UiDrawUtils.ICON_INVENTORY, rightX, rowY - rowGap);
        draw.drawShortcutRow(batch, "U", "使用背包中选中的物品", UiDrawUtils.ICON_USE, rightX, rowY - rowGap * 2f);
        draw.drawShortcutRow(batch, "F5", "保存当前进度", UiDrawUtils.ICON_SAVE, rightX, rowY - rowGap * 3f);
        draw.drawShortcutRow(batch, "F9", "读取存档", UiDrawUtils.ICON_LOAD, rightX, rowY - rowGap * 4f);
        draw.drawShortcutRow(batch, "M", "查看世界地图", UiDrawUtils.ICON_ROOM, rightX, rowY - rowGap * 5f);

        smallFont.setColor(draw.getUiDarkText());
        draw.drawCenteredInBoxWithSmallFont(batch, "ESC 继续", cx - 96f, py + 24f, 192f, 40f);
    }

    private String[] objectiveLines()
    {
        boolean vaultDone = engine.getQuestManager().isCompleted(QuestManager.QUEST_VAULT);
        boolean throneDone = engine.getQuestManager().isCompleted(QuestManager.QUEST_THRONE);
        boolean exploreDone = engine.getQuestManager().isCompleted(QuestManager.QUEST_EXPLORE);
        boolean hasGem = hasItem("gem-light");
        int explored = engine.getExploredRoomIds().size();

        String primary;
        if (!vaultDone && !hasGem) {
            primary = engine.isLockUnlocked("vault-door")
                ? "进入 vault，拾取 Light Gem。"
                : "寻找金库钥匙，打开 vault。";
        } else if (!throneDone) {
            primary = engine.isLockUnlocked("guard-gate")
                ? "穿过守卫之门，抵达 throne-hall。"
                : "找到打开守卫之门的方法。";
        } else if (!exploreDone) {
            primary = "继续探索 Realm 的未知房间。";
        } else {
            primary = "主线线索已齐，选择你的结局。";
        }

        String vault = (vaultDone || hasGem) ? "Light Gem: 已取得" : "Light Gem: 未取得";
        String gate = engine.isLockUnlocked("guard-gate") ? "守卫之门: 已开启" : "守卫之门: 未开启";
        String explore = "探索进度: " + Math.min(explored, 8) + "/8";
        return new String[] {primary, vault, gate, explore};
    }

    private boolean hasItem(String itemId)
    {
        for (Item item : engine.getPlayer().getInventory()) {
            if (item.getItemId().equals(itemId)) {
                return true;
            }
        }
        return false;
    }
}
