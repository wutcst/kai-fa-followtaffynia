package cn.edu.whut.sept.zuul.client.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * 程序化纹理工厂 —— 像素图标、窗口纹理、按钮纹理。
 */
public final class TextureFactory
{
    private TextureFactory() {}

    public static Texture createMoveIcon() {
        Pixmap p = createIconCanvas();
        setGold(p); p.fillRectangle(11, 5, 2, 14); p.fillRectangle(5, 11, 14, 2);
        p.fillRectangle(10, 3, 4, 3); p.fillRectangle(10, 18, 4, 3);
        p.fillRectangle(3, 10, 3, 4); p.fillRectangle(18, 10, 3, 4);
        setDark(p); p.fillRectangle(11, 8, 2, 8); p.fillRectangle(8, 11, 8, 2);
        return finishIcon(p);
    }

    public static Texture createRoomIcon() {
        Pixmap p = createIconCanvas();
        setGold(p); p.fillRectangle(6, 10, 12, 9); p.fillTriangle(4, 10, 12, 4, 20, 10);
        setDark(p); p.fillRectangle(10, 14, 4, 5); p.fillRectangle(7, 11, 10, 2);
        return finishIcon(p);
    }

    public static Texture createLookIcon() {
        Pixmap p = createIconCanvas();
        setGold(p); p.drawCircle(10, 10, 5); p.drawCircle(10, 10, 4);
        p.fillRectangle(14, 15, 6, 3); p.fillRectangle(17, 18, 3, 2);
        setDark(p); p.fillRectangle(9, 9, 3, 3);
        return finishIcon(p);
    }

    public static Texture createInventoryIcon() {
        Pixmap p = createIconCanvas();
        setGold(p); p.fillRectangle(6, 10, 12, 10); p.fillRectangle(8, 7, 8, 4);
        setDark(p); p.fillRectangle(10, 7, 4, 2); p.fillRectangle(8, 12, 8, 2);
        p.fillRectangle(11, 15, 2, 3);
        return finishIcon(p);
    }

    public static Texture createBackIcon() {
        Pixmap p = createIconCanvas();
        setGold(p); p.fillRectangle(7, 8, 11, 3); p.fillRectangle(7, 11, 3, 5);
        p.fillRectangle(10, 16, 6, 3); p.fillTriangle(4, 9, 9, 5, 9, 13);
        setDark(p); p.fillRectangle(12, 17, 4, 1);
        return finishIcon(p);
    }

    public static Texture createSaveIcon() {
        Pixmap p = createIconCanvas();
        setGold(p); p.fillRectangle(5, 5, 14, 15);
        setDark(p); p.fillRectangle(8, 6, 7, 5); p.fillRectangle(8, 15, 8, 4);
        setLight(p); p.fillRectangle(10, 16, 4, 2);
        return finishIcon(p);
    }

    public static Texture createLoadIcon() {
        Pixmap p = createIconCanvas();
        setGold(p); p.fillRectangle(6, 5, 12, 15);
        setDark(p); p.fillRectangle(8, 7, 1, 11); p.fillRectangle(10, 9, 6, 2);
        p.fillRectangle(10, 13, 5, 2); p.fillRectangle(10, 17, 4, 1);
        return finishIcon(p);
    }

    public static Texture createMenuIcon() {
        Pixmap p = createIconCanvas();
        setGold(p); p.fillRectangle(5, 6, 14, 3); p.fillRectangle(5, 11, 14, 3);
        p.fillRectangle(5, 16, 14, 3);
        setDark(p); p.fillRectangle(7, 7, 10, 1); p.fillRectangle(7, 12, 10, 1);
        p.fillRectangle(7, 17, 10, 1);
        return finishIcon(p);
    }

    public static Texture createTitleIcon() {
        Pixmap p = createIconCanvas();
        setGold(p); p.fillRectangle(8, 9, 8, 10); p.fillTriangle(5, 10, 12, 5, 19, 10);
        setDark(p); p.fillRectangle(11, 14, 3, 5); p.fillRectangle(4, 18, 16, 2);
        return finishIcon(p);
    }

    public static Texture createUseIcon() {
        Pixmap p = createIconCanvas();
        setGold(p); p.fillRectangle(6, 13, 10, 3); p.fillRectangle(9, 10, 6, 8);
        p.fillRectangle(16, 8, 3, 3);
        setLight(p); p.fillRectangle(11, 6, 2, 4); p.fillRectangle(9, 8, 6, 2);
        p.fillRectangle(18, 5, 2, 2); p.fillRectangle(4, 6, 2, 2);
        setDark(p); p.fillRectangle(8, 14, 6, 1); p.fillRectangle(10, 17, 4, 1);
        p.fillRectangle(17, 10, 1, 1);
        return finishIcon(p);
    }

    public static Texture createGemIcon() {
        Pixmap p = createIconCanvas();
        setGold(p); p.fillRectangle(8, 3, 8, 3); p.fillRectangle(5, 6, 14, 3);
        p.fillRectangle(3, 9, 18, 6); p.fillRectangle(6, 15, 12, 3);
        p.fillRectangle(9, 18, 6, 3);
        p.setColor(0.08f, 0.55f, 0.27f, 1f); p.fillRectangle(8, 8, 8, 8);
        p.setColor(0.25f, 0.95f, 0.48f, 1f);
        p.fillRectangle(10, 6, 4, 3); p.fillRectangle(7, 10, 3, 4);
        return finishIcon(p);
    }

    public static Texture createOrnamentLine() {
        Pixmap p = new Pixmap(96, 16, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None); p.setColor(0, 0, 0, 0); p.fill();
        p.setBlending(Pixmap.Blending.SourceOver);
        setGold(p); p.fillRectangle(8, 7, 80, 2); p.fillRectangle(2, 6, 8, 4);
        p.fillRectangle(86, 6, 8, 4); p.fillRectangle(43, 4, 10, 8);
        p.setColor(0.08f, 0.55f, 0.27f, 1f); p.fillRectangle(46, 6, 4, 4);
        return finishIcon(p);
    }

    public static Texture createWoodWindow() {
        Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None); p.setColor(0.2f, 0.1f, 0.04f, 1f); p.fill();
        p.setBlending(Pixmap.Blending.SourceOver);
        fill(p, 4, 4, 56, 56, 0.36f, 0.2f, 0.09f, 1f);
        fill(p, 8, 8, 48, 48, 0.45f, 0.27f, 0.13f, 1f);
        fill(p, 11, 11, 42, 42, 0.4f, 0.23f, 0.11f, 1f);
        drawGoldFrame(p, 1, 1, 62, 62); drawCornerCaps(p, 0, 0, 64, 64);
        fill(p, 12, 8, 40, 2, 0.9f, 0.72f, 0.35f, 1f);
        fill(p, 12, 54, 40, 2, 0.32f, 0.16f, 0.06f, 1f);
        return finishIcon(p);
    }

    public static Texture createParchment(boolean inset) {
        Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None); p.setColor(0, 0, 0, 0); p.fill();
        p.setBlending(Pixmap.Blending.SourceOver);
        float base = inset ? 0.74f : 0.86f;
        fill(p, 2, 2, 60, 60, 0.42f, 0.26f, 0.13f, 1f);
        fill(p, 5, 5, 54, 54, base, base * 0.84f, base * 0.58f, 1f);
        fill(p, 8, 8, 48, 48, base + 0.04f, base * 0.88f, base * 0.62f, 1f);
        drawGoldFrame(p, 1, 1, 62, 62);
        if (!inset) drawCornerCaps(p, 0, 0, 64, 64);
        fill(p, 10, 10, 6, 2, 0.66f, 0.46f, 0.25f, 0.55f);
        fill(p, 48, 50, 7, 2, 0.66f, 0.46f, 0.25f, 0.45f);
        return finishIcon(p);
    }

    public static Texture createButton(boolean light) {
        Pixmap p = new Pixmap(72, 40, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None); p.setColor(0, 0, 0, 0); p.fill();
        p.setBlending(Pixmap.Blending.SourceOver);
        fill(p, 3, 3, 66, 34, 0.48f, 0.29f, 0.14f, 1f);
        fill(p, 6, 6, 60, 28, light ? 0.88f : 0.58f, light ? 0.73f : 0.36f,
            light ? 0.49f : 0.18f, 1f);
        drawGoldFrame(p, 1, 1, 70, 38);
        fill(p, 12, 6, 48, 1, 0.98f, 0.78f, 0.34f, 1f);
        return finishIcon(p);
    }

    public static Texture createSquareButton(boolean light) {
        Pixmap p = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None); p.setColor(0, 0, 0, 0); p.fill();
        p.setBlending(Pixmap.Blending.SourceOver);
        if (light) {
            fill(p, 4, 4, 32, 32, 0.84f, 0.68f, 0.42f, 1f);
            fill(p, 7, 7, 26, 26, 0.9f, 0.78f, 0.56f, 1f);
        } else {
            fill(p, 4, 4, 32, 32, 0.25f, 0.13f, 0.06f, 1f);
            fill(p, 7, 7, 26, 26, 0.42f, 0.24f, 0.12f, 1f);
        }
        drawGoldFrame(p, 1, 1, 38, 38);
        return finishIcon(p);
    }

    // --- helpers ---

    private static Pixmap createIconCanvas() {
        Pixmap p = new Pixmap(24, 24, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None); p.setColor(0, 0, 0, 0); p.fill();
        p.setBlending(Pixmap.Blending.SourceOver); return p;
    }

    private static Texture finishIcon(Pixmap p) {
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose(); return t;
    }

    private static void setGold(Pixmap p) { p.setColor(0.93f, 0.72f, 0.33f, 1f); }
    private static void setLight(Pixmap p) { p.setColor(1f, 0.9f, 0.58f, 1f); }
    private static void setDark(Pixmap p) { p.setColor(0.24f, 0.14f, 0.05f, 1f); }

    private static void fill(Pixmap p, int x, int y, int w, int h,
        float r, float g, float b, float a) {
        p.setColor(r, g, b, a); p.fillRectangle(x, y, w, h);
    }

    private static void drawGoldFrame(Pixmap p, int x, int y, int w, int h) {
        fill(p, x, y, w, 2, 0.95f, 0.68f, 0.22f, 1f);
        fill(p, x, y + h - 2, w, 2, 0.45f, 0.25f, 0.08f, 1f);
        fill(p, x, y, 2, h, 0.95f, 0.68f, 0.22f, 1f);
        fill(p, x + w - 2, y, 2, h, 0.45f, 0.25f, 0.08f, 1f);
        fill(p, x + 3, y + 3, w - 6, 1, 1f, 0.84f, 0.36f, 1f);
        fill(p, x + 3, y + h - 4, w - 6, 1, 0.2f, 0.1f, 0.04f, 1f);
    }

    private static void drawCornerCaps(Pixmap p, int x, int y, int w, int h) {
        setGold(p);
        p.fillRectangle(x, y, 10, 4); p.fillRectangle(x, y, 4, 10);
        p.fillRectangle(x + w - 10, y, 10, 4); p.fillRectangle(x + w - 4, y, 4, 10);
        p.fillRectangle(x, y + h - 4, 10, 4); p.fillRectangle(x, y + h - 10, 4, 10);
        p.fillRectangle(x + w - 10, y + h - 4, 10, 4);
        p.fillRectangle(x + w - 4, y + h - 10, 4, 10);
    }
}
