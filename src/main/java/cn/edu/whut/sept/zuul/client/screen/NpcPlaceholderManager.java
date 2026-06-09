package cn.edu.whut.sept.zuul.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;
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

    public void buildNpcPlaceholders(String roomId, MapObjects objectsLayer)
    {
        placeholders.clear();

        if (objectsLayer != null) {
            for (MapObject obj : objectsLayer) {
                String type = obj.getProperties().get("type", String.class);
                if (!"npc".equals(type)) {
                    continue;
                }
                if (!(obj instanceof RectangleMapObject)) {
                    continue;
                }
                String npcId = obj.getProperties().get("npcId", String.class);
                if (npcId == null || npcId.trim().isEmpty()) {
                    continue;
                }
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                placeholders.add(NpcPlaceholder.forNpc(npcId, rect));
            }
        }

        if (!placeholders.isEmpty()) {
            return;
        }
        if ("guard-room".equals(roomId)) {
            placeholders.add(NpcPlaceholder.guard(tileToWorld.apply(15, 7)));
        } else if ("garden".equals(roomId)) {
            placeholders.add(NpcPlaceholder.guard(tileToWorld.apply(12, 15)));
        } else if ("hidden-shrine".equals(roomId)) {
            placeholders.add(NpcPlaceholder.hermit(tileToWorld.apply(15, 7)));
        } else if ("forge".equals(roomId)) {
            placeholders.add(NpcPlaceholder.merchant(tileToWorld.apply(15, 7)));
        }
    }

    public void drawNpcPlaceholders(ShapeRenderer shapes)
    {
        if (placeholders.isEmpty()) {
            return;
        }
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (NpcPlaceholder npc : placeholders) {
            shapes.setColor(npc.color);
            shapes.rect(npc.bounds.x, npc.bounds.y, npc.bounds.width, npc.bounds.height);
        }
        shapes.end();
    }

    public boolean collidesNpc(float newX, float newY, float playerW, float playerH)
    {
        if (placeholders.isEmpty()) {
            return false;
        }
        Rectangle playerRect = new Rectangle(newX, newY, playerW, playerH);
        for (NpcPlaceholder npc : placeholders) {
            if (npc.bounds.overlaps(playerRect)) {
                return true;
            }
        }
        return false;
    }

    public String findNearbyNpcId(float playerX, float playerY, float playerW, float playerH)
    {
        if (placeholders.isEmpty()) {
            return null;
        }
        Rectangle interactRect = new Rectangle(
            playerX - 10f, playerY - 10f,
            playerW + 20f, playerH + 20f);
        for (NpcPlaceholder npc : placeholders) {
            if (npc.bounds.overlaps(interactRect)) {
                return npc.npcId;
            }
        }
        return null;
    }

    public Rectangle findNpcBounds(String npcId)
    {
        if (npcId == null) {
            return null;
        }
        for (NpcPlaceholder npc : placeholders) {
            if (npcId.equals(npc.npcId)) {
                return npc.bounds;
            }
        }
        return null;
    }

    public static final class NpcPlaceholder
    {
        public final String npcId;
        public final Rectangle bounds;
        public final Color color;

        private NpcPlaceholder(String npcId, Rectangle bounds, Color color)
        {
            this.npcId = npcId;
            this.bounds = bounds;
            this.color = color;
        }

        public static NpcPlaceholder guard(Rectangle bounds)
        {
            return new NpcPlaceholder("guard", bounds,
                new Color(0.85f, 0.2f, 0.2f, 1f));
        }

        public static NpcPlaceholder hermit(Rectangle bounds)
        {
            return new NpcPlaceholder("hermit", bounds,
                new Color(0.2f, 0.8f, 0.35f, 1f));
        }

        public static NpcPlaceholder merchant(Rectangle bounds)
        {
            return new NpcPlaceholder("merchant", bounds,
                new Color(0.95f, 0.65f, 0.15f, 1f));
        }

        public static NpcPlaceholder forNpc(String npcId, Rectangle bounds)
        {
            if ("guard".equals(npcId)) {
                return guard(bounds);
            }
            if ("hermit".equals(npcId)) {
                return hermit(bounds);
            }
            if ("merchant".equals(npcId)) {
                return merchant(bounds);
            }
            return new NpcPlaceholder(npcId, bounds,
                new Color(0.55f, 0.3f, 0.9f, 1f));
        }
    }
}
