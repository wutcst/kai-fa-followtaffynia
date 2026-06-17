package cn.edu.whut.sept.zuul.client;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;

/**
 * LibGDX 桌面启动器（设计文档规定的 GUI 入口）。
 * 
 */
public class DesktopLauncher
{
    public static void main(String[] args)
    {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Chronicle of the Lost Realms");
        config.setWindowedMode(960, 540);
        config.setHdpiMode(HdpiMode.Logical);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new RpgMain(), config);
    }
}
