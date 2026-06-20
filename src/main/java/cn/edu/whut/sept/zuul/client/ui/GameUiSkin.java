package cn.edu.whut.sept.zuul.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
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
    private final Texture iconUse;
    private final Texture gemGreen;
    private final Texture ornamentLine;
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
        panelBrown = TextureFactory.createWoodWindow();
        panelBeige = TextureFactory.createParchment(false);
        panelInsetBeige = TextureFactory.createParchment(true);
        buttonLongBrown = TextureFactory.createButton(false);
        buttonLongBeige = TextureFactory.createButton(true);
        buttonSquareBrown = TextureFactory.createSquareButton(false);
        buttonSquareBeige = TextureFactory.createSquareButton(true);
        iconCheck = loadTexture("iconCheck_beige.png");
        iconCircle = loadTexture("iconCircle_blue.png");
        iconCross = loadTexture("iconCross_brown.png");
        cursorHand = loadTexture("cursorHand_beige.png");
        cursorSword = loadTexture("cursorSword_gold.png");
        iconMove = TextureFactory.createMoveIcon();
        iconRoom = TextureFactory.createRoomIcon();
        iconLook = TextureFactory.createLookIcon();
        iconInventory = TextureFactory.createInventoryIcon();
        iconBack = TextureFactory.createBackIcon();
        iconSave = TextureFactory.createSaveIcon();
        iconLoad = TextureFactory.createLoadIcon();
        iconMenu = TextureFactory.createMenuIcon();
        iconTitle = TextureFactory.createTitleIcon();
        iconUse = TextureFactory.createUseIcon();
        gemGreen = TextureFactory.createGemIcon();
        ornamentLine = TextureFactory.createOrnamentLine();
        barBack = loadBar("barBack");
        barRed = loadBar("barRed");
        barYellow = loadBar("barYellow");
        barGreen = loadBar("barGreen");
        barBlue = loadBar("barBlue");

        windowPatch = new NinePatch(panelBrown, 18, 18, 18, 18);
        panelPatch = new NinePatch(panelBeige, 18, 18, 18, 18);
        insetPatch = new NinePatch(panelInsetBeige, 18, 18, 18, 18);
        buttonBrownPatch = new NinePatch(buttonLongBrown, 18, 18, 12, 12);
        buttonBeigePatch = new NinePatch(buttonLongBeige, 18, 18, 12, 12);
    }

    public void drawWindow(SpriteBatch batch, float x, float y, float width, float height)
    {
        windowPatch.draw(batch, px(x), px(y), size(width), size(height));
    }

    public void drawPanel(SpriteBatch batch, float x, float y, float width, float height)
    {
        panelPatch.draw(batch, px(x), px(y), size(width), size(height));
    }

    public void drawInset(SpriteBatch batch, float x, float y, float width, float height)
    {
        insetPatch.draw(batch, px(x), px(y), size(width), size(height));
    }

    public void drawButton(SpriteBatch batch, float x, float y, float width, float height)
    {
        buttonBrownPatch.draw(batch, px(x), px(y), size(width), size(height));
    }

    public void drawLightButton(SpriteBatch batch, float x, float y, float width, float height)
    {
        buttonBeigePatch.draw(batch, px(x), px(y), size(width), size(height));
    }

    public void drawKeyButton(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(buttonSquareBrown, px(x), px(y), size(size), size(size));
    }

    public void drawLightKeyButton(SpriteBatch batch, float x, float y, float size)
    {
        batch.draw(buttonSquareBeige, px(x), px(y), size(size), size(size));
    }

    public void drawCheckIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconCheck, x, y, size);
    }

    public void drawCircleIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconCircle, x, y, size);
    }

    public void drawCrossIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconCross, x, y, size);
    }

    public void drawHandIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, cursorHand, x, y, size);
    }

    public void drawSwordIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, cursorSword, x, y, size);
    }

    public void drawMoveIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconMove, x, y, size);
    }

    public void drawRoomIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconRoom, x, y, size);
    }

    public void drawLookIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconLook, x, y, size);
    }

    public void drawInventoryIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconInventory, x, y, size);
    }

    public void drawBackIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconBack, x, y, size);
    }

    public void drawSaveIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconSave, x, y, size);
    }

    public void drawLoadIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconLoad, x, y, size);
    }

    public void drawMenuIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconMenu, x, y, size);
    }

    public void drawTitleIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconTitle, x, y, size);
    }

    public void drawUseIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, iconUse, x, y, size);
    }

    public void drawGemIcon(SpriteBatch batch, float x, float y, float size)
    {
        drawSquare(batch, gemGreen, x, y, size);
    }

    public void drawOrnamentLine(SpriteBatch batch, float x, float y, float width, float height)
    {
        batch.draw(ornamentLine, px(x), px(y), size(width), size(height));
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
        x = px(x);
        y = px(y);
        width = size(width);
        height = size(height);
        drawBarPieces(batch, barBack, x, y, width, height);
        float clamped = Math.max(0f, Math.min(1f, ratio));
        if (clamped <= 0f) {
            return;
        }
        drawBarPieces(batch, fillTextures, x, y, size(Math.max(height, width * clamped)), height);
    }

    private static void drawBarPieces(SpriteBatch batch, Texture[] textures, float x, float y,
        float width, float height)
    {
        if (width <= 0f || height <= 0f) {
            return;
        }
        x = px(x);
        y = px(y);
        width = size(width);
        height = size(height);
        float capWidth = Math.min(height, width / 2f);
        if (width <= capWidth * 2f + 1f) {
            batch.draw(textures[BAR_MID], x, y, width, height);
            return;
        }
        batch.draw(textures[BAR_LEFT], x, y, capWidth, height);
        batch.draw(textures[BAR_MID], x + capWidth, y, width - capWidth * 2f, height);
        batch.draw(textures[BAR_RIGHT], x + width - capWidth, y, capWidth, height);
    }

    private static void drawSquare(SpriteBatch batch, Texture texture, float x, float y, float size)
    {
        float pixelSize = size(size);
        batch.draw(texture, px(x), px(y), pixelSize, pixelSize);
    }

    private static float px(float value)
    {
        return Math.round(value);
    }

    private static float size(float value)
    {
        return Math.max(1f, Math.round(value));
    }

    private static Texture loadTexture(String fileName)
    {
        FileHandle file = Gdx.files.internal("assets/ui/kenney-rpg/" + fileName);
        Texture texture = new Texture(file);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private static Texture loadGeneratedTexture(String fileName)
    {
        FileHandle file = Gdx.files.internal("assets/ui/generated/" + fileName);
        if (!file.exists()) {
            return null;
        }
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
        iconUse.dispose();
        gemGreen.dispose();
        ornamentLine.dispose();
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
