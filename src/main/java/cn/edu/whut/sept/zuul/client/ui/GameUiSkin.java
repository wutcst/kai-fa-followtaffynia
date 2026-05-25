package cn.edu.whut.sept.zuul.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

/**
 * Kenney RPG UI 皮肤封装，避免 Screen 直接散落素材路径和拉伸参数。
 */
public class GameUiSkin implements Disposable
{
    private static final int BAR_LEFT = 0;
    private static final int BAR_MID = 1;
    private static final int BAR_RIGHT = 2;

    private final Texture panelBrown;
    private final Texture panelBeige;
    private final Texture panelInsetBeige;
    private final Texture buttonLongBrown;
    private final Texture buttonLongBeige;
    private final Texture buttonSquareBrown;
    private final Texture buttonSquareBeige;
    private final Texture iconCheck;
    private final Texture iconCircle;
    private final Texture iconCross;
    private final Texture cursorHand;
    private final Texture cursorSword;
    private final Texture iconMove;
    private final Texture iconRoom;
    private final Texture iconLook;
    private final Texture iconInventory;
    private final Texture iconBack;
    private final Texture iconSave;
    private final Texture iconLoad;
    private final Texture iconMenu;
    private final Texture iconTitle;
    private final Texture[] barBack;
    private final Texture[] barRed;
    private final Texture[] barYellow;
    private final Texture[] barGreen;
    private final Texture[] barBlue;

    private final NinePatch windowPatch;
    private final NinePatch panelPatch;
    private final NinePatch insetPatch;
    private final NinePatch buttonBrownPatch;
    private final NinePatch buttonBeigePatch;

    public GameUiSkin()
    {
        panelBrown = loadTexture("panel_brown.png");
        panelBeige = loadTexture("panel_beige.png");
        panelInsetBeige = loadTexture("panelInset_beige.png");
        buttonLongBrown = loadTexture("buttonLong_brown.png");
        buttonLongBeige = loadTexture("buttonLong_beige.png");
        buttonSquareBrown = loadTexture("buttonSquare_brown.png");
        buttonSquareBeige = loadTexture("buttonSquare_beige.png");
        iconCheck = loadTexture("iconCheck_beige.png");
        iconCircle = loadTexture("iconCircle_blue.png");
        iconCross = loadTexture("iconCross_brown.png");
        cursorHand = loadTexture("cursorHand_beige.png");
        cursorSword = loadTexture("cursorSword_gold.png");
        iconMove = createMoveIcon();
        iconRoom = createRoomIcon();
        iconLook = createLookIcon();
        iconInventory = createInventoryIcon();
        iconBack = createBackIcon();
        iconSave = createSaveIcon();
        iconLoad = createLoadIcon();
        iconMenu = createMenuIcon();
        iconTitle = createTitleIcon();
        barBack = loadBar("barBack");
        barRed = loadBar("barRed");
        barYellow = loadBar("barYellow");
        barGreen = loadBar("barGreen");
        barBlue = loadBar("barBlue");

        windowPatch = new NinePatch(panelBrown, 24, 24, 24, 24);
        panelPatch = new NinePatch(panelBeige, 24, 24, 24, 24);
        insetPatch = new NinePatch(panelInsetBeige, 20, 20, 20, 20);
        buttonBrownPatch = new NinePatch(buttonLongBrown, 32, 32, 18, 18);
        buttonBeigePatch = new NinePatch(buttonLongBeige, 32, 32, 18, 18);
    }

    public void drawWindow(SpriteBatch batch, float x, float y, float width, float height)
    {
        windowPatch.draw(batch, x, y, width, height);
    }

    public void drawPanel(SpriteBatch batch, float x, float y, float width, float height)
    {
        panelPatch.draw(batch, x, y, width, height);
    }

    public void drawInset(SpriteBatch batch, float x, float y, float width, float height)
    {
        insetPatch.draw(batch, x, y, width, height);
    }

    public void drawButton(SpriteBatch batch, float x, float y, float width, float height)
    {
        buttonBrownPatch.draw(batch, x, y, width, height);
    }

    public void drawLightButton(SpriteBatch batch, float x, float y, float width, float height)
    {
        buttonBeigePatch.draw(batch, x, y, width, height);
    }

    public void drawKeyButton(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(buttonSquareBrown, x, y, size, size);
    }

    public void drawLightKeyButton(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(buttonSquareBeige, x, y, size, size);
    }

    public void drawCheckIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(iconCheck, x, y, size, size);
    }

    public void drawCircleIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(iconCircle, x, y, size, size);
    }

    public void drawCrossIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(iconCross, x, y, size, size);
    }

    public void drawHandIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(cursorHand, x, y, size, size);
    }

    public void drawSwordIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(cursorSword, x, y, size, size);
    }

    public void drawMoveIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(iconMove, x, y, size, size);
    }

    public void drawRoomIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(iconRoom, x, y, size, size);
    }

    public void drawLookIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(iconLook, x, y, size, size);
    }

    public void drawInventoryIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(iconInventory, x, y, size, size);
    }

    public void drawBackIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(iconBack, x, y, size, size);
    }

    public void drawSaveIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(iconSave, x, y, size, size);
    }

    public void drawLoadIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(iconLoad, x, y, size, size);
    }

    public void drawMenuIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(iconMenu, x, y, size, size);
    }

    public void drawTitleIcon(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(iconTitle, x, y, size, size);
    }

    public void drawRedBar(SpriteBatch batch, float x, float y, float width, float height, float ratio)
    {
        drawBar(batch, x, y, width, height, barRed, ratio);
    }

    public void drawYellowBar(SpriteBatch batch, float x, float y, float width, float height, float ratio)
    {
        drawBar(batch, x, y, width, height, barYellow, ratio);
    }

    public void drawGreenBar(SpriteBatch batch, float x, float y, float width, float height, float ratio)
    {
        drawBar(batch, x, y, width, height, barGreen, ratio);
    }

    public void drawBlueBar(SpriteBatch batch, float x, float y, float width, float height, float ratio)
    {
        drawBar(batch, x, y, width, height, barBlue, ratio);
    }

    private void drawBar(SpriteBatch batch, float x, float y, float width, float height,
        Texture[] fillTextures, float ratio)
    {
        drawBarPieces(batch, barBack, x, y, width, height);
        float clamped = Math.max(0f, Math.min(1f, ratio));
        if (clamped <= 0f) {
            return;
        }
        drawBarPieces(batch, fillTextures, x, y, Math.max(height, width * clamped), height);
    }

    private static void drawBarPieces(SpriteBatch batch, Texture[] textures, float x, float y,
        float width, float height)
    {
        if (width <= 0f || height <= 0f) {
            return;
        }
        float capWidth = Math.min(height, width / 2f);
        if (width <= capWidth * 2f + 1f) {
            batch.draw(textures[BAR_MID], x, y, width, height);
            return;
        }
        batch.draw(textures[BAR_LEFT], x, y, capWidth, height);
        batch.draw(textures[BAR_MID], x + capWidth, y, width - capWidth * 2f, height);
        batch.draw(textures[BAR_RIGHT], x + width - capWidth, y, capWidth, height);
    }

    private static Texture loadTexture(String fileName)
    {
        FileHandle file = Gdx.files.internal("assets/ui/kenney-rpg/" + fileName);
        Texture texture = new Texture(file);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private static Texture[] loadBar(String prefix)
    {
        return new Texture[] {
            loadTexture(prefix + "_horizontalLeft.png"),
            loadTexture(prefix + "_horizontalMid.png"),
            loadTexture(prefix + "_horizontalRight.png")
        };
    }

    private static Texture createMoveIcon()
    {
        Pixmap pixmap = createIconCanvas();
        setGold(pixmap);
        pixmap.fillRectangle(11, 5, 2, 14);
        pixmap.fillRectangle(5, 11, 14, 2);
        pixmap.fillRectangle(10, 3, 4, 3);
        pixmap.fillRectangle(10, 18, 4, 3);
        pixmap.fillRectangle(3, 10, 3, 4);
        pixmap.fillRectangle(18, 10, 3, 4);
        setDark(pixmap);
        pixmap.fillRectangle(11, 8, 2, 8);
        pixmap.fillRectangle(8, 11, 8, 2);
        return finishIcon(pixmap);
    }

    private static Texture createRoomIcon()
    {
        Pixmap pixmap = createIconCanvas();
        setGold(pixmap);
        pixmap.fillRectangle(6, 10, 12, 9);
        pixmap.fillTriangle(4, 10, 12, 4, 20, 10);
        setDark(pixmap);
        pixmap.fillRectangle(10, 14, 4, 5);
        pixmap.fillRectangle(7, 11, 10, 2);
        return finishIcon(pixmap);
    }

    private static Texture createLookIcon()
    {
        Pixmap pixmap = createIconCanvas();
        setGold(pixmap);
        pixmap.drawCircle(10, 10, 5);
        pixmap.drawCircle(10, 10, 4);
        pixmap.fillRectangle(14, 15, 6, 3);
        pixmap.fillRectangle(17, 18, 3, 2);
        setDark(pixmap);
        pixmap.fillRectangle(9, 9, 3, 3);
        return finishIcon(pixmap);
    }

    private static Texture createInventoryIcon()
    {
        Pixmap pixmap = createIconCanvas();
        setGold(pixmap);
        pixmap.fillRectangle(6, 10, 12, 10);
        pixmap.fillRectangle(8, 7, 8, 4);
        setDark(pixmap);
        pixmap.fillRectangle(10, 7, 4, 2);
        pixmap.fillRectangle(8, 12, 8, 2);
        pixmap.fillRectangle(11, 15, 2, 3);
        return finishIcon(pixmap);
    }

    private static Texture createBackIcon()
    {
        Pixmap pixmap = createIconCanvas();
        setGold(pixmap);
        pixmap.fillRectangle(7, 8, 11, 3);
        pixmap.fillRectangle(7, 11, 3, 5);
        pixmap.fillRectangle(10, 16, 6, 3);
        pixmap.fillTriangle(4, 9, 9, 5, 9, 13);
        setDark(pixmap);
        pixmap.fillRectangle(12, 17, 4, 1);
        return finishIcon(pixmap);
    }

    private static Texture createSaveIcon()
    {
        Pixmap pixmap = createIconCanvas();
        setGold(pixmap);
        pixmap.fillRectangle(5, 5, 14, 15);
        setDark(pixmap);
        pixmap.fillRectangle(8, 6, 7, 5);
        pixmap.fillRectangle(8, 15, 8, 4);
        setLight(pixmap);
        pixmap.fillRectangle(10, 16, 4, 2);
        return finishIcon(pixmap);
    }

    private static Texture createLoadIcon()
    {
        Pixmap pixmap = createIconCanvas();
        setGold(pixmap);
        pixmap.fillRectangle(6, 5, 12, 15);
        setDark(pixmap);
        pixmap.fillRectangle(8, 7, 1, 11);
        pixmap.fillRectangle(10, 9, 6, 2);
        pixmap.fillRectangle(10, 13, 5, 2);
        pixmap.fillRectangle(10, 17, 4, 1);
        return finishIcon(pixmap);
    }

    private static Texture createMenuIcon()
    {
        Pixmap pixmap = createIconCanvas();
        setGold(pixmap);
        pixmap.fillRectangle(5, 6, 14, 3);
        pixmap.fillRectangle(5, 11, 14, 3);
        pixmap.fillRectangle(5, 16, 14, 3);
        setDark(pixmap);
        pixmap.fillRectangle(7, 7, 10, 1);
        pixmap.fillRectangle(7, 12, 10, 1);
        pixmap.fillRectangle(7, 17, 10, 1);
        return finishIcon(pixmap);
    }

    private static Texture createTitleIcon()
    {
        Pixmap pixmap = createIconCanvas();
        setGold(pixmap);
        pixmap.fillRectangle(8, 9, 8, 10);
        pixmap.fillTriangle(5, 10, 12, 5, 19, 10);
        setDark(pixmap);
        pixmap.fillRectangle(11, 14, 3, 5);
        pixmap.fillRectangle(4, 18, 16, 2);
        return finishIcon(pixmap);
    }

    private static Pixmap createIconCanvas()
    {
        Pixmap pixmap = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        return pixmap;
    }

    private static Texture finishIcon(Pixmap pixmap)
    {
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    private static void setGold(Pixmap pixmap)
    {
        pixmap.setColor(0.93f, 0.72f, 0.33f, 1f);
    }

    private static void setLight(Pixmap pixmap)
    {
        pixmap.setColor(1f, 0.9f, 0.58f, 1f);
    }

    private static void setDark(Pixmap pixmap)
    {
        pixmap.setColor(0.24f, 0.14f, 0.05f, 1f);
    }

    @Override
    public void dispose()
    {
        panelBrown.dispose();
        panelBeige.dispose();
        panelInsetBeige.dispose();
        buttonLongBrown.dispose();
        buttonLongBeige.dispose();
        buttonSquareBrown.dispose();
        buttonSquareBeige.dispose();
        iconCheck.dispose();
        iconCircle.dispose();
        iconCross.dispose();
        cursorHand.dispose();
        cursorSword.dispose();
        iconMove.dispose();
        iconRoom.dispose();
        iconLook.dispose();
        iconInventory.dispose();
        iconBack.dispose();
        iconSave.dispose();
        iconLoad.dispose();
        iconMenu.dispose();
        iconTitle.dispose();
        disposeBar(barBack);
        disposeBar(barRed);
        disposeBar(barYellow);
        disposeBar(barGreen);
        disposeBar(barBlue);
    }

    private static void disposeBar(Texture[] bar)
    {
        for (Texture texture : bar) {
            texture.dispose();
        }
    }
}
