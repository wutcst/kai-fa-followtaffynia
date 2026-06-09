package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import com.badlogic.gdx.graphics.Color;
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
        float iconSize = 18f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (ItemPlaceholder ip : placeholders) {
            float x = ip.bounds.x + (ip.bounds.width - iconSize) / 2f;
            float y = ip.bounds.y + (ip.bounds.height - iconSize) / 2f;
            // 阴影
            shapes.setColor(0f, 0f, 0f, 0.32f);
            shapes.rect(x + 2f, y - 2f, iconSize, iconSize);
            // 主体色块
            shapes.setColor(ip.color);
            shapes.rect(x, y, iconSize, iconSize);
            // 高光线
            shapes.setColor(1f, 1f, 1f, 0.55f);
            shapes.rect(x + 4f, y + iconSize - 6f, 5f, 3f);
        }
        shapes.end();
        // 边框
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (ItemPlaceholder ip : placeholders) {
            float x = ip.bounds.x + (ip.bounds.width - iconSize) / 2f;
            float y = ip.bounds.y + (ip.bounds.height - iconSize) / 2f;
            shapes.setColor(0.18f, 0.12f, 0.08f, 0.9f);
            shapes.rect(x, y, iconSize, iconSize);
        }
        shapes.end();
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
