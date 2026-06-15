package cn.edu.whut.sept.zuul.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;

/**
 * 相机/视口控制器 —— 负责世界相机和 UI 相机的视口计算。
 * 使用设计分辨率 1280×720 作为虚拟画布，等比缩放到实际屏幕。
 */
public class CameraController
{
    /** 设计分辨率宽度（16:9，2560×1440 的精确一半） */
    public static final float DESIGN_W = 1280f;
    /** 设计分辨率高度 */
    public static final float DESIGN_H = 720f;

    /** 顶栏高度（含边距）：72px + 8px = 80px */
    public static final int TOP_MARGIN = 80;
    /** 底栏高度：72px */
    public static final int BOTTOM_MARGIN = 72;
    /** 世界视口水平边距 */
    public static final int WORLD_MARGIN_H = 12;

    private static final int WORLD_MARGIN_LEFT = WORLD_MARGIN_H;
    private static final int WORLD_MARGIN_RIGHT = WORLD_MARGIN_H;
    private static final int WORLD_MARGIN_BOTTOM = BOTTOM_MARGIN;
    private static final int WORLD_MARGIN_TOP = TOP_MARGIN;

    private final OrthographicCamera worldCamera;
    private final OrthographicCamera uiCamera;

    private int worldViewportX, worldViewportY;
    private int worldViewportWidth, worldViewportHeight;

    /** 设计分辨率 → 实际屏幕的缩放比 */
    private float scale;
    /** 设计区域映射到屏幕后的偏移（黑边） */
    private int offsetX, offsetY;
    /** 设计区域映射到屏幕后的宽高 */
    private int renderW, renderH;

    public CameraController()
    {
        this.worldCamera = new OrthographicCamera();
        this.uiCamera = new OrthographicCamera();
    }

    public OrthographicCamera getWorldCamera() { return worldCamera; }
    public OrthographicCamera getUiCamera() { return uiCamera; }
    public int getWorldViewportX() { return worldViewportX; }
    public int getWorldViewportY() { return worldViewportY; }
    public int getWorldViewportWidth() { return worldViewportWidth; }
    public int getWorldViewportHeight() { return worldViewportHeight; }

    /** 设计分辨率 → 屏幕的实际缩放比（最大 2.0，避免 UI 过大） */
    public float getScale() { return scale; }

    /** viewport 左右黑边宽度（设计单位），HUD 面板需往此方向延伸才能占满物理屏幕 */
    public float getViewportGap() {
        return scale > 0 ? offsetX / scale : 0;
    }

    public void update(float mapPixelWidth, float mapPixelHeight)
    {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        // 计算设计分辨率到实际屏幕的等比缩放
        float scaleX = (float) screenW / DESIGN_W;
        float scaleY = (float) screenH / DESIGN_H;
        scale = Math.min(scaleX, scaleY);
        // 限制最大缩放，防止在超高分辨率下 UI 过大（可选）
        if (scale > 2.5f) scale = 2.5f;

        renderW = Math.round(DESIGN_W * scale);
        renderH = Math.round(DESIGN_H * scale);
        offsetX = (screenW - renderW) / 2;
        offsetY = (screenH - renderH) / 2;

        // 在设计空间内计算世界视口
        updateWorldViewport((int) DESIGN_W, (int) DESIGN_H, mapPixelWidth, mapPixelHeight);

        // UI 相机总是使用设计分辨率
        uiCamera.setToOrtho(false, DESIGN_W, DESIGN_H);
        uiCamera.update();

        // 世界相机使用地图像素大小
        worldCamera.setToOrtho(false, mapPixelWidth, mapPixelHeight);
        worldCamera.position.set(mapPixelWidth / 2f, mapPixelHeight / 2f, 0f);
        worldCamera.update();
    }

    private void updateWorldViewport(int dw, int dh, float mapW, float mapH)
    {
        int availW = Math.max(1, dw - WORLD_MARGIN_LEFT - WORLD_MARGIN_RIGHT);
        int availH = Math.max(1, dh - WORLD_MARGIN_BOTTOM - WORLD_MARGIN_TOP);
        float mapAspect = mapW / mapH;
        float availAspect = (float) availW / availH;
        if (availAspect > mapAspect) {
            worldViewportHeight = availH;
            worldViewportWidth = Math.round(worldViewportHeight * mapAspect);
            worldViewportX = WORLD_MARGIN_LEFT + (availW - worldViewportWidth) / 2;
            worldViewportY = WORLD_MARGIN_BOTTOM;
        } else {
            worldViewportWidth = availW;
            worldViewportHeight = Math.round(worldViewportWidth / mapAspect);
            worldViewportX = WORLD_MARGIN_LEFT;
            worldViewportY = WORLD_MARGIN_BOTTOM + (availH - worldViewportHeight) / 2;
        }
    }

    /** 将设计空间中的世界视口矩形映射到屏幕空间 */
    public void applyWorldViewport()
    {
        int sx = offsetX + Math.round(worldViewportX * scale);
        int sy = offsetY + Math.round(worldViewportY * scale);
        int sw = Math.round(worldViewportWidth * scale);
        int sh = Math.round(worldViewportHeight * scale);
        HdpiUtils.glViewport(sx, sy, sw, sh);
    }

    /** 将整个设计区域映射到屏幕 */
    public void applyFullViewport()
    {
        HdpiUtils.glViewport(offsetX, offsetY, renderW, renderH);
    }

    public void resize(int width, int height)
    {
        update(1, 1);
    }
}
