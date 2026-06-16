package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.render.NpcRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * NPC 占位管理 —— 从 GameScreen 提取。
 */
public class NpcPlaceholderManager
{
    private final BiFunction<Integer, Integer, Rectangle> tileToWorld;
    private final List<NpcPlaceholder> placeholders = new ArrayList<>();
    private NpcRenderer npcRenderer;

    public NpcPlaceholderManager(BiFunction<Integer, Integer, Rectangle> tileToWorld)
    {
        this.tileToWorld = tileToWorld;
    }

    public void setNpcRenderer(NpcRenderer renderer)
    {
        this.npcRenderer = renderer;
    }

    public List<NpcPlaceholder> getPlaceholders()
    {
        return placeholders;
    }

    public void buildNpcPlaceholders(String roomId, MapObjects objectsLayer,
        Set<String> defeatedNpcs, boolean guardGateUnlocked)
    {
        placeholders.clear();
        boolean guardDealt = defeatedNpcs.contains("guard");

        if (objectsLayer != null) {
            for (MapObject obj : objectsLayer) {
                String type = obj.getProperties().get("type", String.class);
                if (!"npc".equals(type)) continue;
                if (!(obj instanceof RectangleMapObject)) continue;
                String npcId = obj.getProperties().get("npcId", String.class);
                if (npcId == null || npcId.trim().isEmpty()) continue;
                if (shouldSkip(npcId, roomId, defeatedNpcs, guardGateUnlocked)) continue;
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                placeholders.add(NpcPlaceholder.forNpc(npcId, rect));
            }
        }
    }

    private boolean shouldSkip(String npcId, String roomId,
        Set<String> defeatedNpcs, boolean guardGateUnlocked)
    {
        if (defeatedNpcs != null && defeatedNpcs.contains(npcId)) return true;

        if ("guard".equals(npcId)) {
            if ("garden".equals(roomId) && guardGateUnlocked) return true;
            if ("guard-room".equals(roomId) && !guardGateUnlocked) return true;
        }
        return false;
    }

    /**
     * 用 SpriteBatch 绘制所有 NPC 精灵。
     */
    public void drawNpcPlaceholders(SpriteBatch batch)
    {
        if (placeholders.isEmpty() || npcRenderer == null) return;

        for (NpcPlaceholder npc : placeholders) {
            if (!npcRenderer.hasNpc(npc.npcId)) continue;

            Rectangle b = npc.bounds;
            int rw = npcRenderer.getRenderW();
            int rh = npcRenderer.getRenderH();

            // 水平居中，脚底对齐 bounds 底部（b.y）
            float drawX = b.x + b.width / 2f - rw / 2f;
            float drawY = b.y - 50f;

            npcRenderer.render(batch, npc.npcId, drawX, drawY);
        }
    }

    public boolean collidesNpc(float newX, float newY, float playerW, float playerH)
    {
        if (placeholders.isEmpty()) return false;
        Rectangle playerRect = new Rectangle(newX, newY, playerW, playerH);
        for (NpcPlaceholder npc : placeholders) {
            if (npc.bounds.overlaps(playerRect)) return true;
        }
        return false;
    }

    public String findNearbyNpcId(float playerX, float playerY, float playerW, float playerH)
    {
        if (placeholders.isEmpty()) return null;
        Rectangle interactRect = new Rectangle(
            playerX - 10f, playerY - 10f, playerW + 20f, playerH + 20f);
        for (NpcPlaceholder npc : placeholders) {
            if (npc.bounds.overlaps(interactRect)) return npc.npcId;
        }
        return null;
    }

    public Rectangle findNpcBounds(String npcId)
    {
        if (npcId == null) return null;
        for (NpcPlaceholder npc : placeholders) {
            if (npcId.equals(npc.npcId)) return npc.bounds;
        }
        return null;
    }

    public static final class NpcPlaceholder
    {
        public final String npcId;
        public final Rectangle bounds;

        private NpcPlaceholder(String npcId, Rectangle bounds)
        {
            this.npcId = npcId;
            this.bounds = bounds;
        }

        public static NpcPlaceholder forNpc(String npcId, Rectangle bounds)
        {
            return new NpcPlaceholder(npcId, bounds);
        }
    }
}
