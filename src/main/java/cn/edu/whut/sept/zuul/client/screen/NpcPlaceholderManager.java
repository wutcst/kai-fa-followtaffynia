package cn.edu.whut.sept.zuul.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
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
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (NpcPlaceholder npc : placeholders) {
            if (npc.hasMapSprite) continue;
            drawNpcSprite(shapes, npc);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawNpcSprite(ShapeRenderer shapes, NpcPlaceholder npc)
    {
        Rectangle b = npc.bounds;
        float x = Math.round(b.x + b.width / 2f - 14f);
        float y = Math.round(b.y + 1f);
        pixelRect(shapes, x + 3f, y - 3f, 22f, 5f, 0f, 0f, 0f, 0.34f);

        if ("guard".equals(npc.npcId)) {
            drawGuardSprite(shapes, x, y);
        } else if ("hermit".equals(npc.npcId)) {
            drawHermitSprite(shapes, x, y);
        } else if ("merchant".equals(npc.npcId)) {
            drawMerchantSprite(shapes, x, y);
        } else {
            drawGenericNpcSprite(shapes, x, y, npc.color);
        }
    }

    private void drawGuardSprite(ShapeRenderer shapes, float x, float y)
    {
        pixelRect(shapes, x + 9f, y + 0f, 4f, 5f, 0.12f, 0.12f, 0.15f, 1f);
        pixelRect(shapes, x + 16f, y + 0f, 4f, 5f, 0.12f, 0.12f, 0.15f, 1f);
        pixelRect(shapes, x + 7f, y + 5f, 16f, 15f, 0.62f, 0.66f, 0.72f, 1f);
        pixelRect(shapes, x + 10f, y + 8f, 10f, 9f, 0.82f, 0.85f, 0.86f, 1f);
        pixelRect(shapes, x + 8f, y + 17f, 14f, 9f, 0.55f, 0.58f, 0.65f, 1f);
        pixelRect(shapes, x + 11f, y + 21f, 8f, 4f, 0.90f, 0.78f, 0.38f, 1f);
        pixelRect(shapes, x + 22f, y + 7f, 5f, 12f, 0.55f, 0.12f, 0.10f, 1f);
        pixelRect(shapes, x + 24f, y + 10f, 7f, 7f, 0.72f, 0.18f, 0.14f, 1f);
        pixelRect(shapes, x + 3f, y + 7f, 2f, 24f, 0.72f, 0.56f, 0.28f, 1f);
        pixelRect(shapes, x + 1f, y + 28f, 6f, 3f, 0.88f, 0.74f, 0.32f, 1f);
    }

    private void drawHermitSprite(ShapeRenderer shapes, float x, float y)
    {
        pixelRect(shapes, x + 9f, y + 0f, 5f, 5f, 0.10f, 0.10f, 0.12f, 1f);
        pixelRect(shapes, x + 16f, y + 0f, 5f, 5f, 0.10f, 0.10f, 0.12f, 1f);
        pixelRect(shapes, x + 6f, y + 4f, 18f, 19f, 0.18f, 0.36f, 0.28f, 1f);
        pixelRect(shapes, x + 9f, y + 9f, 12f, 11f, 0.23f, 0.50f, 0.35f, 1f);
        pixelRect(shapes, x + 10f, y + 20f, 10f, 7f, 0.11f, 0.22f, 0.18f, 1f);
        pixelRect(shapes, x + 12f, y + 17f, 6f, 5f, 0.86f, 0.70f, 0.48f, 1f);
        pixelRect(shapes, x + 26f, y + 3f, 2f, 26f, 0.62f, 0.42f, 0.22f, 1f);
        pixelRect(shapes, x + 23f, y + 27f, 8f, 4f, 0.35f, 0.92f, 0.66f, 1f);
        pixelRect(shapes, x + 24f, y + 28f, 6f, 2f, 0.86f, 1f, 0.72f, 0.9f);
    }

    private void drawMerchantSprite(ShapeRenderer shapes, float x, float y)
    {
        pixelRect(shapes, x + 8f, y + 0f, 5f, 5f, 0.15f, 0.10f, 0.06f, 1f);
        pixelRect(shapes, x + 17f, y + 0f, 5f, 5f, 0.15f, 0.10f, 0.06f, 1f);
        pixelRect(shapes, x + 7f, y + 5f, 16f, 16f, 0.58f, 0.33f, 0.16f, 1f);
        pixelRect(shapes, x + 9f, y + 8f, 12f, 10f, 0.76f, 0.51f, 0.27f, 1f);
        pixelRect(shapes, x + 10f, y + 19f, 10f, 7f, 0.82f, 0.62f, 0.38f, 1f);
        pixelRect(shapes, x + 12f, y + 16f, 6f, 5f, 0.86f, 0.64f, 0.42f, 1f);
        pixelRect(shapes, x + 22f, y + 8f, 6f, 10f, 0.24f, 0.15f, 0.08f, 1f);
        pixelRect(shapes, x + 2f, y + 12f, 7f, 6f, 0.45f, 0.26f, 0.12f, 1f);
        pixelRect(shapes, x + 10f, y + 11f, 10f, 3f, 0.94f, 0.72f, 0.28f, 1f);
    }

    private void drawGenericNpcSprite(ShapeRenderer shapes, float x, float y, Color color)
    {
        shapes.setColor(color);
        shapes.rect(x + 7f, y + 4f, 18f, 18f);
        pixelRect(shapes, x + 10f, y + 20f, 12f, 8f, 0.78f, 0.62f, 0.42f, 1f);
    }

    private void pixelRect(ShapeRenderer shapes, float x, float y, float width, float height,
        float r, float g, float b, float a)
    {
        shapes.setColor(r, g, b, a);
        shapes.rect(Math.round(x), Math.round(y),
            Math.max(1f, Math.round(width)), Math.max(1f, Math.round(height)));
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
