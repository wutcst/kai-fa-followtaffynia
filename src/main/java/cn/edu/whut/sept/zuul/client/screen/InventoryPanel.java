package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
import cn.edu.whut.sept.zuul.client.ui.UiDrawUtils;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.ItemUseCheck;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.List;

/**
 * 背包面板 —— 从 GameScreen 提取。
 */
public class InventoryPanel
{
    private final GameEngine engine;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final GameUiSkin uiSkin;
    private final GlyphLayout layout;
    private final UiDrawUtils draw;

    private boolean inventoryOpen;
    private boolean inventoryInspectMode;
    private int inventoryInspectIndex;
    private int inventoryScrollOffset;
    private float worldViewportHeight;

    private static final float WORLD_MARGIN_BOTTOM = 96f;
    private static final float WORLD_MARGIN_TOP = 64f;

    public InventoryPanel(GameEngine engine, SpriteBatch batch, BitmapFont font,
                           BitmapFont smallFont, GameUiSkin uiSkin, GlyphLayout layout,
                           UiDrawUtils draw)
    {
        this.engine = engine;
        this.batch = batch;
        this.font = font;
        this.smallFont = smallFont;
        this.uiSkin = uiSkin;
        this.layout = layout;
        this.draw = draw;
    }

    public boolean isOpen() { return inventoryOpen; }
    public boolean isInspectMode() { return inventoryInspectMode; }
    public int getInspectIndex() { return inventoryInspectIndex; }

    public void toggle() {
        inventoryOpen = !inventoryOpen;
        if (!inventoryOpen) {
            inventoryInspectMode = false;
        }
        inventoryInspectIndex = 0;
        inventoryScrollOffset = 0;
    }

    public void close() {
        inventoryOpen = false;
        inventoryInspectMode = false;
    }

    // --- methods for InventoryInputHandler ---
    public void resetSelection() { inventoryInspectIndex = 0; inventoryScrollOffset = 0; inventoryInspectMode = false; }
    public void clampIndex() {
        int size = engine.getPlayer().getInventory().size();
        if (size == 0) return;
        if (inventoryInspectIndex >= size) inventoryInspectIndex = size - 1;
        if (inventoryInspectIndex < 0) inventoryInspectIndex = 0;
        keepVisible();
    }
    public void moveSelection(int delta) {
        int size = engine.getPlayer().getInventory().size();
        if (size == 0) return;
        inventoryInspectIndex = (inventoryInspectIndex + delta + size) % size;
        keepVisible();
    }
    public void selectIndex(int i) { inventoryInspectIndex = i; keepVisible(); }
    public void enterInspect() { inventoryInspectMode = true; }
    public void exitInspect() { inventoryInspectMode = false; }
    public void clampSelection() {
        int size = engine.getPlayer().getInventory().size();
        if (size == 0) { close(); inventoryInspectIndex = 0; inventoryScrollOffset = 0; }
        else if (inventoryInspectIndex >= size) { inventoryInspectIndex = size - 1; keepVisible(); }
    }

    public void render(float panelX, float panelY, float panelWidth, float panelHeight)
    {
        List<Item> items = engine.getPlayer().getInventory();
        if (inventoryInspectIndex >= items.size()) {
            inventoryInspectIndex = Math.max(0, items.size() - 1);
        }

        font.setColor(draw.getUiLightText());
        font.draw(batch, inventoryInspectMode ? "物品详情" : "背包", panelX + 24,
            panelY + panelHeight - 26);
        font.setColor(draw.getUiDarkText());

        float innerX = panelX + 32f;
        float topY = panelY + panelHeight - 56f;

        if (!inventoryInspectMode) {
            smallFont.setColor(draw.getUiDarkText());
            uiSkin.drawLightButton(batch, innerX, topY - 20f, panelWidth - 64f, 28f);
            smallFont.setColor(draw.getUiDarkText());
            draw.drawClampedLine(batch, smallFont, "↑↓选择  U/Enter使用  X详情  Esc关闭",
                innerX + 10f, topY - 1f, panelWidth - 84f);

            float rowY = topY - 60f;
            float rowH = 30f;
            float rowW = panelWidth - 64f;
            int visibleRows = inventoryVisibleRows(panelHeight);
            keepVisible();
            int lastVisible = Math.min(items.size(), inventoryScrollOffset + visibleRows);
            for (int i = inventoryScrollOffset; i < lastVisible; i++) {
                float y = rowY - (i - inventoryScrollOffset) * rowH;
                Item it = items.get(i);
                ItemUseCheck check = engine.checkItemUse(it.getItemId());
                String status = itemUseStatus(check);
                if (i == inventoryInspectIndex) {
                    uiSkin.drawButton(batch, innerX, y, rowW, rowH);
                }
                layout.setText(smallFont, status);
                float statusWidth = layout.width;
                String line = (i + 1) + ". " + it.getName() + " (" + fmtWeight(it.getWeight()) + ")";
                smallFont.setColor(i == inventoryInspectIndex ? Color.WHITE : draw.getUiDarkText());
                draw.drawClampedLine(batch, smallFont, line, innerX + 10f, y + rowH - 9f,
                    Math.max(40f, rowW - statusWidth - 24f));
                smallFont.setColor(i == inventoryInspectIndex ? Color.WHITE : draw.getUiDarkText());
                smallFont.draw(batch, status, innerX + rowW - statusWidth - 10f, y + rowH - 9f);
            }

            if (!items.isEmpty()) {
                Item picked = items.get(inventoryInspectIndex);
                ItemUseCheck check = engine.checkItemUse(picked.getItemId());
                smallFont.setColor(draw.getUiLightText());
                draw.drawClampedLine(batch, smallFont, "效果提示: " + check.hint, innerX,
                    panelY + 30f, rowW - 72f);
            } else {
                smallFont.setColor(draw.getUiLightText());
                smallFont.draw(batch, "背包为空", innerX, panelY + 40f);
            }
            if (items.size() > visibleRows) {
                smallFont.setColor(draw.getUiDarkText());
                draw.drawRightAligned(batch, (inventoryScrollOffset + 1) + "-" + lastVisible + "/"
                        + items.size(),
                    panelX + panelWidth - 32f, panelY + 30f);
            }
            return;
        }

        if (items.isEmpty()) {
            smallFont.setColor(draw.getUiLightText());
            smallFont.draw(batch, "背包为空", innerX, panelY + panelHeight - 70f);
            return;
        }

        Item item = items.get(inventoryInspectIndex);
        ItemUseCheck check = engine.checkItemUse(item.getItemId());

        font.setColor(draw.getUiLightText());
        smallFont.setColor(draw.getUiDarkText());
        smallFont.draw(batch, "物品ID: " + item.getItemId(), innerX, topY);
        smallFont.draw(batch, "重量: " + fmtWeight(item.getWeight()), innerX, topY - 18f);
        String effect = item.getEffect() == null ? "无" : item.getEffect();
        smallFont.draw(batch, "效果: " + effect, innerX, topY - 36f);

        smallFont.setColor(draw.getUiDarkText());
        draw.drawMultilineClamped(batch, smallFont, "描述: " + item.getDescription(), innerX,
            topY - 58f, panelWidth - 56f, 18f, 4);

        smallFont.setColor(draw.getUiLightText());
        draw.drawClampedLine(batch, smallFont, "使用条件: " + check.hint, innerX, panelY + 44f,
            panelWidth - 64f);
        smallFont.setColor(draw.getUiLightText());
        smallFont.draw(batch, "Enter返回  Esc关闭", innerX, panelY + 24f);
    }

    private void keepVisible()
    {
        int size = engine.getPlayer().getInventory().size();
        if (size <= 0) { inventoryScrollOffset = 0; return; }
        int visibleRows = inventoryVisibleRows(inventoryPanelHeight());
        int maxOffset = Math.max(0, size - visibleRows);
        if (inventoryInspectIndex < inventoryScrollOffset)
            inventoryScrollOffset = inventoryInspectIndex;
        else if (inventoryInspectIndex >= inventoryScrollOffset + visibleRows)
            inventoryScrollOffset = inventoryInspectIndex - visibleRows + 1;
        inventoryScrollOffset = Math.max(0, Math.min(inventoryScrollOffset, maxOffset));
    }

    public void setWorldViewportHeight(float h) { this.worldViewportHeight = h; }

    public float inventoryPanelHeight()
    {
        float availableHeight = Math.max(120f, worldViewportHeight - 24f);
        if (inventoryInspectMode) {
            return Math.min(296f, availableHeight);
        }
        int rows = Math.max(3, Math.min(7, engine.getPlayer().getInventory().size()));
        return Math.min(Math.max(264f, 152f + rows * 30f), availableHeight);
    }

    private int inventoryVisibleRows(float panelHeight)
    {
        return Math.max(1, (int)((panelHeight - 156f) / 30f));
    }

    private String itemUseStatus(ItemUseCheck check)
    {
        if (check.canUse) {
            return check.hint.startsWith("被动") ? "被动" : "可用";
        }
        if (check.requiresLocation) {
            return "需位置";
        }
        return "不可用";
    }

    private static String fmtWeight(double w) {
        return w == (long) w ? String.valueOf((long) w) : String.format("%.1f", w);
    }
}
