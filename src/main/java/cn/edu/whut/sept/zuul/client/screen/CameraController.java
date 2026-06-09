package cn.edu.whut.sept.zuul.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;

/**
 * 相机/视口控制器 —— 负责世界相机和 UI 相机的视口计算。
 */
public class CameraController
{
    private static final int WORLD_MARGIN_LEFT = 12;
    private static final int WORLD_MARGIN_RIGHT = 12;
    private static final int WORLD_MARGIN_BOTTOM = 96;
    private static final int WORLD_MARGIN_TOP = 64;

    private final OrthographicCamera worldCamera;
    private final OrthographicCamera uiCamera;

    private int worldViewportX, worldViewportY;
    private int worldViewportWidth, worldViewportHeight;

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

    public void update(float mapPixelWidth, float mapPixelHeight)
    {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        updateWorldViewport(w, h, mapPixelWidth, mapPixelHeight);
        worldCamera.setToOrtho(false, mapPixelWidth, mapPixelHeight);
        worldCamera.position.set(mapPixelWidth / 2f, mapPixelHeight / 2f, 0f);
        worldCamera.update();
        uiCamera.setToOrtho(false, w, h);
        uiCamera.update();
    }

    private void updateWorldViewport(int width, int height, float mapW, float mapH)
    {
        int availW = Math.max(1, width - WORLD_MARGIN_LEFT - WORLD_MARGIN_RIGHT);
        int availH = Math.max(1, height - WORLD_MARGIN_BOTTOM - WORLD_MARGIN_TOP);
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

    public void applyWorldViewport()
    {
        Gdx.gl.glViewport(worldViewportX, worldViewportY, worldViewportWidth, worldViewportHeight);
    }

    public void applyFullViewport()
    {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height)
    {
        update(1, 1); // actual values set by caller
    }
}
