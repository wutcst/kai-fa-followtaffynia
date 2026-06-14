package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 物品占位管理 —— 从 GameScreen 提取。
 */
public class ItemPlaceholderManager
{
    private final GameEngine engine;
    private final BiFunction<Integer, Integer, Rectangle> tileToWorld;
    private final List<ItemPlaceholder> placeholders = new ArrayList<>();

    public ItemPlaceholderManager(GameEngine engine,
                                   BiFunction<Integer, Integer, Rectangle> tileToWorld)
    {
        this.engine = engine;
        this.tileToWorld = tileToWorld;
    }

    public List<ItemPlaceholder> getPlaceholders()
    {
        return placeholders;
    }

    public void buildItemPlaceholders(String roomId)
    {
        placeholders.clear();
        List<ItemSpawnDef> defs = ITEM_SPAWNS.get(roomId);
        List<Item> roomItems = engine.getCurrentRoom().getItems();
        if (roomItems.isEmpty()) {
            return;
        }

        for (Item item : roomItems) {
            ItemSpawnDef def = findSpawnDef(defs, item.getItemId());
            Rectangle rect;
            Color color;
            if (def != null) {
                rect = tileToWorld.apply(def.tileX, def.tileY);
                color = def.color;
            } else {
                rect = tileToWorld.apply(15, 8);
                color = new Color(0.9f, 0.9f, 0.5f, 1f);
            }
            placeholders.add(new ItemPlaceholder(item.getItemId(), rect, color));
        }
    }

    public void rebuildItemPlaceholders()
    {
        buildItemPlaceholders(engine.getCurrentRoom().getRoomId());
    }

    public void drawItemPlaceholders(ShapeRenderer shapes)
    {
        if (placeholders.isEmpty()) return;
        float iconSize = 22f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (ItemPlaceholder ip : placeholders) {
            float x = ip.bounds.x + (ip.bounds.width - iconSize) / 2f;
            float y = ip.bounds.y + (ip.bounds.height - iconSize) / 2f;
            pixelRect(shapes, x + 3f, y - 3f, iconSize - 4f, 5f, 0f, 0f, 0f, 0.26f);
            drawItemIcon(shapes, ip, x, y, iconSize);
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (ItemPlaceholder ip : placeholders) {
            float x = ip.bounds.x + (ip.bounds.width - iconSize) / 2f;
            float y = ip.bounds.y + (ip.bounds.height - iconSize) / 2f;
            shapes.setColor(0.18f, 0.12f, 0.08f, 0.9f);
            shapes.rect(Math.round(x), Math.round(y), iconSize, iconSize);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawItemIcon(ShapeRenderer shapes, ItemPlaceholder item, float x, float y,
        float size)
    {
        String id = item.itemId;
        if (id.contains("key")) {
            drawKeyIcon(shapes, x, y);
        } else if (id.contains("gem") || id.contains("crystal") || id.contains("warp")) {
            drawGemWorldIcon(shapes, x, y, item.color);
        } else if (id.contains("herb")) {
            drawHerbIcon(shapes, x, y);
        } else if (id.contains("torch")) {
            drawTorchIcon(shapes, x, y);
        } else if (id.contains("sword")) {
            drawSwordWorldIcon(shapes, x, y);
        } else if (id.contains("shield")) {
            drawShieldWorldIcon(shapes, x, y);
        } else if (id.contains("cookie")) {
            drawCookieIcon(shapes, x, y);
        } else if (id.contains("coin")) {
            drawCoinIcon(shapes, x, y);
        } else if (id.contains("tome") || id.contains("note")) {
            drawBookIcon(shapes, x, y, id.contains("note"));
        } else if (id.contains("mug")) {
            drawMugIcon(shapes, x, y);
        } else if (id.contains("barrel")) {
            drawBarrelIcon(shapes, x, y);
        } else {
            pixelRect(shapes, x + 3f, y + 3f, size - 6f, size - 6f,
                item.color.r, item.color.g, item.color.b, 1f);
            pixelRect(shapes, x + 6f, y + size - 8f, 6f, 3f, 1f, 1f, 1f, 0.55f);
        }
    }

    private void drawKeyIcon(ShapeRenderer shapes, float x, float y)
    {
        pixelRect(shapes, x + 4f, y + 11f, 12f, 4f, 0.88f, 0.68f, 0.24f, 1f);
        pixelRect(shapes, x + 14f, y + 7f, 5f, 4f, 0.88f, 0.68f, 0.24f, 1f);
        pixelRect(shapes, x + 14f, y + 15f, 5f, 4f, 0.88f, 0.68f, 0.24f, 1f);
        pixelRect(shapes, x + 2f, y + 8f, 8f, 10f, 0.96f, 0.82f, 0.34f, 1f);
        pixelRect(shapes, x + 5f, y + 11f, 3f, 4f, 0.14f, 0.10f, 0.06f, 1f);
    }

    private void drawGemWorldIcon(ShapeRenderer shapes, float x, float y, Color color)
    {
        pixelRect(shapes, x + 8f, y + 3f, 6f, 3f, color.r, color.g, color.b, 1f);
        pixelRect(shapes, x + 5f, y + 6f, 12f, 4f, color.r, color.g, color.b, 1f);
        pixelRect(shapes, x + 3f, y + 10f, 16f, 5f, color.r, color.g, color.b, 1f);
        pixelRect(shapes, x + 7f, y + 15f, 8f, 4f,
            color.r * 0.72f, color.g * 0.72f, color.b * 0.72f, 1f);
        pixelRect(shapes, x + 9f, y + 7f, 4f, 3f, 0.92f, 1f, 0.88f, 0.82f);
    }

    private void drawHerbIcon(ShapeRenderer shapes, float x, float y)
    {
        pixelRect(shapes, x + 10f, y + 4f, 3f, 14f, 0.15f, 0.48f, 0.16f, 1f);
        pixelRect(shapes, x + 5f, y + 10f, 8f, 5f, 0.24f, 0.74f, 0.28f, 1f);
        pixelRect(shapes, x + 12f, y + 13f, 8f, 5f, 0.34f, 0.86f, 0.32f, 1f);
        pixelRect(shapes, x + 8f, y + 17f, 7f, 4f, 0.52f, 0.96f, 0.42f, 1f);
    }

    private void drawTorchIcon(ShapeRenderer shapes, float x, float y)
    {
        pixelRect(shapes, x + 9f, y + 3f, 4f, 15f, 0.48f, 0.28f, 0.12f, 1f);
        pixelRect(shapes, x + 7f, y + 15f, 8f, 4f, 0.28f, 0.16f, 0.08f, 1f);
        pixelRect(shapes, x + 6f, y + 18f, 10f, 4f, 0.94f, 0.28f, 0.08f, 1f);
        pixelRect(shapes, x + 8f, y + 20f, 6f, 5f, 1f, 0.74f, 0.18f, 1f);
    }

    private void drawSwordWorldIcon(ShapeRenderer shapes, float x, float y)
    {
        pixelRect(shapes, x + 5f, y + 4f, 4f, 4f, 0.42f, 0.24f, 0.12f, 1f);
        pixelRect(shapes, x + 8f, y + 7f, 6f, 3f, 0.74f, 0.55f, 0.24f, 1f);
        pixelRect(shapes, x + 12f, y + 9f, 4f, 12f, 0.70f, 0.72f, 0.74f, 1f);
        pixelRect(shapes, x + 15f, y + 18f, 3f, 4f, 0.90f, 0.92f, 0.86f, 1f);
    }

    private void drawShieldWorldIcon(ShapeRenderer shapes, float x, float y)
    {
        pixelRect(shapes, x + 6f, y + 6f, 12f, 12f, 0.46f, 0.24f, 0.12f, 1f);
        pixelRect(shapes, x + 8f, y + 4f, 8f, 4f, 0.62f, 0.36f, 0.18f, 1f);
        pixelRect(shapes, x + 9f, y + 8f, 6f, 9f, 0.78f, 0.52f, 0.26f, 1f);
        pixelRect(shapes, x + 11f, y + 9f, 2f, 8f, 0.24f, 0.14f, 0.08f, 0.8f);
    }

    private void drawCookieIcon(ShapeRenderer shapes, float x, float y)
    {
        pixelRect(shapes, x + 5f, y + 6f, 14f, 12f, 0.78f, 0.48f, 0.27f, 1f);
        pixelRect(shapes, x + 7f, y + 18f, 10f, 3f, 0.90f, 0.62f, 0.36f, 1f);
        pixelRect(shapes, x + 8f, y + 9f, 3f, 3f, 0.34f, 0.14f, 0.08f, 1f);
        pixelRect(shapes, x + 14f, y + 13f, 3f, 3f, 0.34f, 0.14f, 0.08f, 1f);
        pixelRect(shapes, x + 11f, y + 16f, 2f, 2f, 1f, 0.76f, 0.94f, 1f);
    }

    private void drawCoinIcon(ShapeRenderer shapes, float x, float y)
    {
        pixelRect(shapes, x + 4f, y + 5f, 10f, 4f, 0.76f, 0.48f, 0.12f, 1f);
        pixelRect(shapes, x + 7f, y + 9f, 10f, 4f, 0.94f, 0.70f, 0.18f, 1f);
        pixelRect(shapes, x + 10f, y + 13f, 10f, 4f, 1f, 0.84f, 0.24f, 1f);
        pixelRect(shapes, x + 12f, y + 15f, 4f, 1f, 1f, 0.96f, 0.58f, 1f);
    }

    private void drawBookIcon(ShapeRenderer shapes, float x, float y, boolean note)
    {
        if (note) {
            pixelRect(shapes, x + 5f, y + 4f, 13f, 16f, 0.82f, 0.74f, 0.52f, 1f);
            pixelRect(shapes, x + 8f, y + 8f, 7f, 2f, 0.34f, 0.22f, 0.12f, 1f);
            pixelRect(shapes, x + 8f, y + 13f, 6f, 2f, 0.34f, 0.22f, 0.12f, 1f);
        } else {
            pixelRect(shapes, x + 4f, y + 4f, 15f, 17f, 0.28f, 0.12f, 0.08f, 1f);
            pixelRect(shapes, x + 7f, y + 6f, 10f, 13f, 0.52f, 0.24f, 0.12f, 1f);
            pixelRect(shapes, x + 9f, y + 10f, 6f, 2f, 0.82f, 0.66f, 0.32f, 1f);
        }
    }

    private void drawMugIcon(ShapeRenderer shapes, float x, float y)
    {
        pixelRect(shapes, x + 5f, y + 6f, 11f, 12f, 0.56f, 0.36f, 0.18f, 1f);
        pixelRect(shapes, x + 16f, y + 9f, 4f, 6f, 0.66f, 0.42f, 0.20f, 1f);
        pixelRect(shapes, x + 7f, y + 16f, 7f, 3f, 0.96f, 0.86f, 0.64f, 1f);
    }

    private void drawBarrelIcon(ShapeRenderer shapes, float x, float y)
    {
        pixelRect(shapes, x + 5f, y + 4f, 13f, 16f, 0.42f, 0.23f, 0.11f, 1f);
        pixelRect(shapes, x + 4f, y + 7f, 15f, 3f, 0.28f, 0.16f, 0.08f, 1f);
        pixelRect(shapes, x + 4f, y + 15f, 15f, 3f, 0.28f, 0.16f, 0.08f, 1f);
        pixelRect(shapes, x + 8f, y + 5f, 2f, 14f, 0.56f, 0.34f, 0.18f, 1f);
        pixelRect(shapes, x + 14f, y + 5f, 2f, 14f, 0.56f, 0.34f, 0.18f, 1f);
    }

    private void pixelRect(ShapeRenderer shapes, float x, float y, float width, float height,
        float r, float g, float b, float a)
    {
        shapes.setColor(r, g, b, a);
        shapes.rect(Math.round(x), Math.round(y),
            Math.max(1f, Math.round(width)), Math.max(1f, Math.round(height)));
    }

    public String findNearbyItemId(float playerX, float playerY, float playerW, float playerH)
    {
        if (placeholders.isEmpty()) return null;
        Rectangle interactRect = new Rectangle(
            playerX - 10f, playerY - 10f,
            playerW + 20f, playerH + 20f);
        for (ItemPlaceholder ip : placeholders) {
            if (ip.bounds.overlaps(interactRect)) {
                return ip.itemId;
            }
        }
        return null;
    }

    public static ItemSpawnDef findSpawnDef(List<ItemSpawnDef> defs, String itemId)
    {
        if (defs == null) return null;
        for (ItemSpawnDef def : defs) {
            if (def.itemId.equals(itemId)) return def;
        }
        return null;
    }

    // ======================== 道具占位块系统 ========================

    /** 道具出生点定义（tile 坐标，左上角原点）。 */
    public static final class ItemSpawnDef
    {
        public final String itemId;
        public final int tileX;
        public final int tileY;
        public final Color color;

        public ItemSpawnDef(String itemId, int tileX, int tileY, Color color)
        {
            this.itemId = itemId;
            this.tileX = tileX;
            this.tileY = tileY;
            this.color = color;
        }
    }

    /** 地图上的道具占位块。 */
    public static final class ItemPlaceholder
    {
        public final String itemId;
        public final Rectangle bounds;
        public final Color color;

        public ItemPlaceholder(String itemId, Rectangle bounds, Color color)
        {
            this.itemId = itemId;
            this.bounds = bounds;
            this.color = color;
        }
    }

    /** 每个房间中道具的 tile 坐标与占位色（左上角原点）。 */
    private static final Map<String, List<ItemSpawnDef>> ITEM_SPAWNS = new HashMap<>();
    static
    {
        addItemSpawn("outside", "welcome-note", 14, 8, new Color(0.9f, 0.85f, 0.65f, 1f));
        addItemSpawn("theatre", "torch", 22, 8, new Color(1f, 0.5f, 0.1f, 1f));
        addItemSpawn("pub", "ale-mug", 5, 10, new Color(0.9f, 0.65f, 0.2f, 1f));
        addItemSpawn("lab", "key-vault", 18, 7, new Color(0.7f, 0.7f, 0.75f, 1f));
        addItemSpawn("office", "key-guard", 12, 7, new Color(0.55f, 0.55f, 0.6f, 1f));
        addItemSpawn("library", "ancient-tome", 8, 7, new Color(0.4f, 0.22f, 0.1f, 1f));
        addItemSpawn("cellar", "old-barrel", 8, 10, new Color(0.5f, 0.3f, 0.15f, 1f));
        addItemSpawn("vault", "gem-light", 22, 6, new Color(0.3f, 0.9f, 0.95f, 1f));
        addItemSpawn("vault", "gold-coins", 22, 10, new Color(1f, 0.85f, 0.15f, 1f));
        addItemSpawn("hidden-shrine", "crystal-shard", 14, 7, new Color(0.3f, 0.6f, 1f, 1f));
        addItemSpawn("garden", "healing-herb", 10, 10, new Color(0.2f, 0.85f, 0.3f, 1f));
        addItemSpawn("armory", "sword-rusty", 22, 8, new Color(0.6f, 0.6f, 0.65f, 1f));
        addItemSpawn("armory", "shield-wooden", 22, 12, new Color(0.6f, 0.4f, 0.2f, 1f));
        addItemSpawn("teleport-alcove", "warp-dust", 22, 8, new Color(0.65f, 0.3f, 0.9f, 1f));
        addItemSpawn("cellar", "magic-cookie", 24, 12, new Color(1f, 0.5f, 0.7f, 1f));
        addItemSpawn("library", "magic-cookie", 24, 12, new Color(1f, 0.5f, 0.7f, 1f));
        addItemSpawn("hidden-shrine", "magic-cookie", 22, 11, new Color(1f, 0.5f, 0.7f, 1f));
    }

    private static void addItemSpawn(String roomId, String itemId, int tileX, int tileY, Color color)
    {
        ITEM_SPAWNS.computeIfAbsent(roomId, k -> new ArrayList<>())
            .add(new ItemSpawnDef(itemId, tileX, tileY, color));
    }
}
