package cn.edu.whut.sept.zuul.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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

    public NpcPlaceholderManager(BiFunction<Integer, Integer, Rectangle> tileToWorld)
    {
        this.tileToWorld = tileToWorld;
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

        if (!placeholders.isEmpty()) return;

        // 所有 NPC 已改用 tmx 地图贴图，不再生成色块占位
    }

    private boolean shouldSkip(String npcId, String roomId,
        Set<String> defeatedNpcs, boolean guardGateUnlocked)
    {
        // 被杀 → 永不再出现
        if (defeatedNpcs != null && defeatedNpcs.contains(npcId)) return true;

        if ("guard".equals(npcId)) {
            if ("garden".equals(roomId) && guardGateUnlocked) {
                // 守卫之门已开 → 庭院守卫消失（他进去了/不需要再守门）
                return true;
            }
            if ("guard-room".equals(roomId) && !guardGateUnlocked) {
                // 门还没开 → 守卫室里的守卫还不该出现（他在外面守门）
                return true;
            }
        }
        return false;
    }

    public void drawNpcPlaceholders(ShapeRenderer shapes)
    {
        if (placeholders.isEmpty()) return;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (NpcPlaceholder npc : placeholders) {
            if (npc.hasMapSprite) continue; // 有地图贴图的 NPC 不画色块
            shapes.setColor(npc.color);
            shapes.rect(npc.bounds.x, npc.bounds.y, npc.bounds.width, npc.bounds.height);
        }
        shapes.end();
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
        public final Color color;
        public final boolean hasMapSprite;

        private NpcPlaceholder(String npcId, Rectangle bounds, Color color, boolean hasMapSprite)
        {
            this.npcId = npcId; this.bounds = bounds; this.color = color;
            this.hasMapSprite = hasMapSprite;
        }
        public static NpcPlaceholder guard(Rectangle bounds) {
            return new NpcPlaceholder("guard", bounds, new Color(0.85f, 0.2f, 0.2f, 1f), false);
        }
        public static NpcPlaceholder hermit(Rectangle bounds) {
            return new NpcPlaceholder("hermit", bounds, new Color(0.2f, 0.8f, 0.35f, 1f), false);
        }
        public static NpcPlaceholder merchant(Rectangle bounds) {
            return new NpcPlaceholder("merchant", bounds, new Color(0.95f, 0.65f, 0.15f, 1f), false);
        }
        public static NpcPlaceholder forNpc(String npcId, Rectangle bounds) {
            if ("guard".equals(npcId)) return new NpcPlaceholder("guard", bounds, new Color(0.85f, 0.2f, 0.2f, 1f), true);
            if ("hermit".equals(npcId)) return new NpcPlaceholder("hermit", bounds, new Color(0.2f, 0.8f, 0.35f, 1f), true);
            if ("merchant".equals(npcId)) return new NpcPlaceholder("merchant", bounds, new Color(0.95f, 0.65f, 0.15f, 1f), true);
            return new NpcPlaceholder(npcId, bounds, new Color(0.55f, 0.3f, 0.9f, 1f), true);
        }
    }
}
