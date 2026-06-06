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
        panelBrown = createWoodWindowTexture();
        panelBeige = createParchmentTexture(false);
        panelInsetBeige = createParchmentTexture(true);
        buttonLongBrown = createButtonTexture(false);
        buttonLongBeige = createButtonTexture(true);
        buttonSquareBrown = createSquareButtonTexture(false);
        buttonSquareBeige = createSquareButtonTexture(true);
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
        iconUse = createUseIcon();
        gemGreen = createGemIcon();
        ornamentLine = createOrnamentLine();
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

    private static Texture createWoodWindowTexture()
    {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0.2f, 0.1f, 0.04f, 1f);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        fill(pixmap, 4, 4, 56, 56, 0.36f, 0.2f, 0.09f, 1f);
        fill(pixmap, 8, 8, 48, 48, 0.45f, 0.27f, 0.13f, 1f);
        fill(pixmap, 11, 11, 42, 42, 0.4f, 0.23f, 0.11f, 1f);
        drawGoldFrame(pixmap, 1, 1, 62, 62);
        drawCornerCaps(pixmap, 0, 0, 64, 64);
        fill(pixmap, 12, 8, 40, 2, 0.9f, 0.72f, 0.35f, 1f);
        fill(pixmap, 12, 54, 40, 2, 0.32f, 0.16f, 0.06f, 1f);
        return finishIcon(pixmap);
    }

    private static Texture createParchmentTexture(boolean inset)
    {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        float base = inset ? 0.74f : 0.86f;
        fill(pixmap, 2, 2, 60, 60, 0.42f, 0.26f, 0.13f, 1f);
        fill(pixmap, 5, 5, 54, 54, base, base * 0.84f, base * 0.58f, 1f);
        fill(pixmap, 8, 8, 48, 48, base + 0.04f, base * 0.88f, base * 0.62f, 1f);
        drawGoldFrame(pixmap, 1, 1, 62, 62);
        if (!inset) {
            drawCornerCaps(pixmap, 0, 0, 64, 64);
        }
        fill(pixmap, 10, 10, 6, 2, 0.66f, 0.46f, 0.25f, 0.55f);
        fill(pixmap, 48, 50, 7, 2, 0.66f, 0.46f, 0.25f, 0.45f);
        return finishIcon(pixmap);
    }

    private static Texture createButtonTexture(boolean light)
    {
        Pixmap pixmap = new Pixmap(72, 40, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        fill(pixmap, 3, 3, 66, 34, 0.48f, 0.29f, 0.14f, 1f);
        if (light) {
            fill(pixmap, 6, 6, 60, 28, 0.88f, 0.73f, 0.49f, 1f);
        } else {
            fill(pixmap, 6, 6, 60, 28, 0.58f, 0.36f, 0.18f, 1f);
        }
        drawGoldFrame(pixmap, 1, 1, 70, 38);
        fill(pixmap, 12, 6, 48, 1, 0.98f, 0.78f, 0.34f, 1f);
        return finishIcon(pixmap);
    }

    private static Texture createSquareButtonTexture(boolean light)
    {
        Pixmap pixmap = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        if (light) {
            fill(pixmap, 4, 4, 32, 32, 0.84f, 0.68f, 0.42f, 1f);
            fill(pixmap, 7, 7, 26, 26, 0.9f, 0.78f, 0.56f, 1f);
        } else {
            fill(pixmap, 4, 4, 32, 32, 0.25f, 0.13f, 0.06f, 1f);
            fill(pixmap, 7, 7, 26, 26, 0.42f, 0.24f, 0.12f, 1f);
        }
        drawGoldFrame(pixmap, 1, 1, 38, 38);
        return finishIcon(pixmap);
    }

    private static Texture createUseIcon()
    {
        Pixmap pixmap = createIconCanvas();
        setGold(pixmap);
        pixmap.fillRectangle(6, 13, 10, 3);
        pixmap.fillRectangle(9, 10, 6, 8);
        pixmap.fillRectangle(16, 8, 3, 3);
        setLight(pixmap);
        pixmap.fillRectangle(11, 6, 2, 4);
        pixmap.fillRectangle(9, 8, 6, 2);
        pixmap.fillRectangle(18, 5, 2, 2);
        pixmap.fillRectangle(4, 6, 2, 2);
        setDark(pixmap);
        pixmap.fillRectangle(8, 14, 6, 1);
        pixmap.fillRectangle(10, 17, 4, 1);
        pixmap.fillRectangle(17, 10, 1, 1);
        return finishIcon(pixmap);
    }

    private static Texture createGemIcon()
    {
        Pixmap pixmap = createIconCanvas();
        setGold(pixmap);
        pixmap.fillRectangle(8, 3, 8, 3);
        pixmap.fillRectangle(5, 6, 14, 3);
        pixmap.fillRectangle(3, 9, 18, 6);
        pixmap.fillRectangle(6, 15, 12, 3);
        pixmap.fillRectangle(9, 18, 6, 3);
        pixmap.setColor(0.08f, 0.55f, 0.27f, 1f);
        pixmap.fillRectangle(8, 8, 8, 8);
        pixmap.setColor(0.25f, 0.95f, 0.48f, 1f);
        pixmap.fillRectangle(10, 6, 4, 3);
        pixmap.fillRectangle(7, 10, 3, 4);
        return finishIcon(pixmap);
    }

    private static Texture createOrnamentLine()
    {
        Pixmap pixmap = new Pixmap(96, 16, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        setGold(pixmap);
        pixmap.fillRectangle(8, 7, 80, 2);
        pixmap.fillRectangle(2, 6, 8, 4);
        pixmap.fillRectangle(86, 6, 8, 4);
        pixmap.fillRectangle(43, 4, 10, 8);
        pixmap.setColor(0.08f, 0.55f, 0.27f, 1f);
        pixmap.fillRectangle(46, 6, 4, 4);
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

    private static void fill(Pixmap pixmap, int x, int y, int width, int height,
        float r, float g, float b, float a)
    {
        pixmap.setColor(r, g, b, a);
        pixmap.fillRectangle(x, y, width, height);
    }

    private static void drawGoldFrame(Pixmap pixmap, int x, int y, int width, int height)
    {
        fill(pixmap, x, y, width, 2, 0.95f, 0.68f, 0.22f, 1f);
        fill(pixmap, x, y + height - 2, width, 2, 0.45f, 0.25f, 0.08f, 1f);
        fill(pixmap, x, y, 2, height, 0.95f, 0.68f, 0.22f, 1f);
        fill(pixmap, x + width - 2, y, 2, height, 0.45f, 0.25f, 0.08f, 1f);
        fill(pixmap, x + 3, y + 3, width - 6, 1, 1f, 0.84f, 0.36f, 1f);
        fill(pixmap, x + 3, y + height - 4, width - 6, 1, 0.2f, 0.1f, 0.04f, 1f);
    }

    private static void drawCornerCaps(Pixmap pixmap, int x, int y, int width, int height)
    {
        setGold(pixmap);
        pixmap.fillRectangle(x, y, 10, 4);
        pixmap.fillRectangle(x, y, 4, 10);
        pixmap.fillRectangle(x + width - 10, y, 10, 4);
        pixmap.fillRectangle(x + width - 4, y, 4, 10);
        pixmap.fillRectangle(x, y + height - 4, 10, 4);
        pixmap.fillRectangle(x, y + height - 10, 4, 10);
        pixmap.fillRectangle(x + width - 10, y + height - 4, 10, 4);
        pixmap.fillRectangle(x + width - 4, y + height - 10, 4, 10);
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
