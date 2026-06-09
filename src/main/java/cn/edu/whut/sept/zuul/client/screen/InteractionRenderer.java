package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.render.PlayerRenderer;
import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
import cn.edu.whut.sept.zuul.client.ui.UiDrawUtils;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * 交互渲染器 —— 玩家绘制 + 交互提示。
 */
public class InteractionRenderer
{
    private static final Color UI_DARK_TEXT = new Color(0.26f, 0.18f, 0.1f, 1f);

    private final GameEngine engine;
    private final SpriteBatch batch;
    private final BitmapFont smallFont;
    private final UiDrawUtils draw;
    private final GameUiSkin uiSkin;
    private final OrthographicCamera worldCamera;
    private final NpcPlaceholderManager npcManager;
    private final ItemPlaceholderManager itemManager;
    private final RoomController room;

    private PlayerRenderer playerRenderer;
    private float playerW, playerH;

    public InteractionRenderer(GameEngine engine, SpriteBatch batch, BitmapFont smallFont,
                                UiDrawUtils draw, GameUiSkin uiSkin, OrthographicCamera worldCamera,
                                NpcPlaceholderManager npcManager,
                                ItemPlaceholderManager itemManager,
                                RoomController room)
    {
        this.engine = engine;
        this.batch = batch;
        this.smallFont = smallFont;
        this.draw = draw;
        this.uiSkin = uiSkin;
        this.worldCamera = worldCamera;
        this.npcManager = npcManager;
        this.itemManager = itemManager;
        this.room = room;
    }

    public void setPlayerRenderer(PlayerRenderer pr) { this.playerRenderer = pr; }
    public void setPlayerSize(float w, float h) { this.playerW = w; this.playerH = h; }

    public void drawPlayer(float playerX, float playerY)
    {
        if (playerRenderer == null) return;
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        playerRenderer.render(batch, playerX, playerY);
        batch.end();
    }

    /**
     * 渲染交互提示（E 交谈 / E 拾取 / 进入房间）。
     * @return true 表示有可见提示
     */
    public boolean drawInteractionPrompt(float playerX, float playerY, boolean paused,
                                          boolean inventoryOpen, boolean encounterMenuOpen,
                                          boolean dialogueActive, boolean inDialogue,
                                          boolean inCombat)
    {
        if (paused || inventoryOpen || encounterMenuOpen || dialogueActive
            || inDialogue || inCombat) return false;

        InteractionPrompt prompt = currentInteractionPrompt(playerX, playerY);
        if (prompt == null) return false;

        float promptWidth = Math.max(88f, prompt.text.length() * 14f + 42f);
        float promptHeight = 28f;
        float x = Math.max(8f, Math.min(playerX + playerW / 2f - promptWidth / 2f,
            room.mapPixelWidth() - promptWidth - 8f));
        float y = Math.min(playerY + playerH + 14f, room.mapPixelHeight() - promptHeight - 8f);

        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        uiSkin.drawLightButton(batch, x, y, promptWidth, promptHeight);
        draw.drawIcon(batch, prompt.icon, x + 8f, y + 6f, 16f);
        smallFont.setColor(UI_DARK_TEXT);
        draw.drawClampedLine(batch, smallFont, prompt.text, x + 30f, y + 20f, promptWidth - 38f);
        batch.end();
        return true;
    }

    private InteractionPrompt currentInteractionPrompt(float playerX, float playerY)
    {
        if (npcManager.findNearbyNpcId(playerX, playerY, playerW, playerH) != null)
            return new InteractionPrompt("E 交谈", UiDrawUtils.ICON_LOOK);

        String itemId = itemManager.findNearbyItemId(playerX, playerY, playerW, playerH);
        if (itemId != null) {
            for (Item item : engine.getCurrentRoom().getItems()) {
                if (item.getItemId().equals(itemId))
                    return new InteractionPrompt("E 拾取 " + item.getName(), UiDrawUtils.ICON_TAKE);
            }
        }

        String exitName = room.nearbyExitTarget(playerX, playerY);
        if (exitName != null)
            return new InteractionPrompt("进入 " + exitName, UiDrawUtils.ICON_ROOM);

        return null;
    }

    static class InteractionPrompt
    {
        final String text;
        final int icon;
        InteractionPrompt(String text, int icon) { this.text = text; this.icon = icon; }
    }
}
