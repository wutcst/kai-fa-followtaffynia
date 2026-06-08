package cn.edu.whut.sept.zuul.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 世界地图渲染器——以节点图的形式展示 15 个房间的拓扑结构与探索状态。
 */
public class WorldMapRenderer
{
    private static final float CELL_SIZE = 14f;
    private static final float CELL_GAP = 44f;
    private static final float LINE_WIDTH = 2.5f;

    private static final Color BG = new Color(0.06f, 0.05f, 0.04f, 0.94f);
    private static final Color EXPLORED = new Color(0.88f, 0.72f, 0.35f, 1f);
    private static final Color UNEXPLORED = new Color(0.22f, 0.18f, 0.13f, 0.7f);
    private static final Color CURRENT = new Color(1f, 0.92f, 0.35f, 1f);
    private static final Color LOCKED = new Color(0.92f, 0.28f, 0.18f, 1f);
    private static final Color TELEPORT = new Color(0.62f, 0.35f, 0.92f, 1f);
    private static final Color ENDING = new Color(1f, 0.82f, 0.2f, 1f);
    private static final Color LINE_EXPLORED = new Color(0.52f, 0.38f, 0.18f, 0.75f);
    private static final Color LINE_UNEXPLORED = new Color(0.26f, 0.2f, 0.12f, 0.32f);
    private static final Color LABEL_COLOR = new Color(0.92f, 0.82f, 0.6f, 1f);
    private static final Color TITLE_COLOR = new Color(0.95f, 0.85f, 0.5f, 1f);

    private static final Map<String, int[]> ROOM_POS = new LinkedHashMap<>();
    private static final List<String[]> CONNECTIONS = new ArrayList<>();
    /** 特殊房间的 lockId（null 表示非上锁）。 */
    private static final Map<String, String> ROOM_LOCK_IDS = new LinkedHashMap<>();

    static
    {
        // (col, row) 布局
        ROOM_POS.put("hidden-shrine", new int[]{2, 0});
        ROOM_POS.put("theatre", new int[]{1, 1});
        ROOM_POS.put("library", new int[]{2, 1});
        ROOM_POS.put("teleport-alcove", new int[]{3, 1});
        ROOM_POS.put("office", new int[]{0, 2});
        ROOM_POS.put("outside", new int[]{1, 2});
        ROOM_POS.put("pub", new int[]{2, 2});
        ROOM_POS.put("garden", new int[]{3, 2});
        ROOM_POS.put("armory", new int[]{4, 2});
        ROOM_POS.put("lab", new int[]{1, 3});
        ROOM_POS.put("cellar", new int[]{2, 3});
        ROOM_POS.put("guard-room", new int[]{3, 3});
        ROOM_POS.put("vault", new int[]{1, 4});
        ROOM_POS.put("throne-hall", new int[]{3, 4});
        ROOM_POS.put("forge", new int[]{4, 4});

        CONNECTIONS.add(new String[]{"outside", "theatre"});
        CONNECTIONS.add(new String[]{"outside", "pub"});
        CONNECTIONS.add(new String[]{"outside", "lab"});
        CONNECTIONS.add(new String[]{"outside", "office"});
        CONNECTIONS.add(new String[]{"theatre", "library"});
        CONNECTIONS.add(new String[]{"pub", "cellar"});
        CONNECTIONS.add(new String[]{"pub", "garden"});
        CONNECTIONS.add(new String[]{"lab", "vault"});
        CONNECTIONS.add(new String[]{"library", "hidden-shrine"});
        CONNECTIONS.add(new String[]{"library", "teleport-alcove"});
        CONNECTIONS.add(new String[]{"garden", "guard-room"});
        CONNECTIONS.add(new String[]{"garden", "armory"});
        CONNECTIONS.add(new String[]{"guard-room", "armory"});
        CONNECTIONS.add(new String[]{"guard-room", "throne-hall"});
        CONNECTIONS.add(new String[]{"armory", "forge"});

        ROOM_LOCK_IDS.put("vault", "vault-door");
        ROOM_LOCK_IDS.put("guard-room", "guard-gate");
    }

    private final GlyphLayout layout = new GlyphLayout();
    private float animTimer;

    /**
     * 渲染世界地图面板。
     *
     * @param isUnlocked  检查 lockId 是否已解锁的回调
     */
    public void render(ShapeRenderer shapes, SpriteBatch batch, BitmapFont font,
        float panelX, float panelY, float panelW, float panelH,
        String currentRoomId, Set<String> exploredIds,
        java.util.function.Predicate<String> isUnlocked,
        float delta)
    {
        animTimer += delta;

        float pad = 28f;
        float titleH = 28f;
        float contentX = panelX + pad;
        float contentY = panelY + pad;
        float contentW = panelW - pad * 2f;
        float contentH = panelH - pad * 2f - titleH;

        // -- 面板背景 --
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(BG);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.end();

        // -- 标题 --
        batch.begin();
        font.setColor(TITLE_COLOR);
        layout.setText(font, "世界地图  [M] 关闭");
        font.draw(batch, "世界地图  [M] 关闭",
            panelX + (panelW - layout.width) / 2f,
            panelY + panelH - pad + 4f);
        batch.end();

        // -- 计算网格缩放 --
        int minCol = 0, maxCol = 4;
        int minRow = 0, maxRow = 4;

        float totalW = (maxCol - minCol) * CELL_GAP + CELL_SIZE;
        float totalH = (maxRow - minRow) * CELL_GAP + CELL_SIZE;

        float scale = Math.min(contentW / totalW, contentH / totalH);
        float cellSize = CELL_SIZE * scale;
        float cellGap = CELL_GAP * scale;

        float gridX = contentX + (contentW - totalW * scale) / 2f + cellSize / 2f;
        float gridY = contentY + (contentH - totalH * scale) / 2f + cellSize / 2f;

        // -- 连线 --
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (String[] conn : CONNECTIONS) {
            int[] posA = ROOM_POS.get(conn[0]);
            int[] posB = ROOM_POS.get(conn[1]);
            if (posA == null || posB == null) continue;

            boolean aExp = exploredIds.contains(conn[0]);
            boolean bExp = exploredIds.contains(conn[1]);

            float x1 = gridX + posA[0] * cellGap;
            float y1 = gridY + (maxRow - posA[1]) * cellGap;
            float x2 = gridX + posB[0] * cellGap;
            float y2 = gridY + (maxRow - posB[1]) * cellGap;

            shapes.setColor(aExp && bExp ? LINE_EXPLORED : LINE_UNEXPLORED);
            shapes.rectLine(x1, y1, x2, y2, LINE_WIDTH * scale);
        }

        // -- 房间节点 --
        for (Map.Entry<String, int[]> entry : ROOM_POS.entrySet()) {
            String roomId = entry.getKey();
            int[] pos = entry.getValue();
            boolean explored = exploredIds.contains(roomId);

            float cx = gridX + pos[0] * cellGap;
            float cy = gridY + (maxRow - pos[1]) * cellGap;
            float half = cellSize / 2f;

            Color color;
            if (roomId.equals(currentRoomId)) {
                color = CURRENT;
            } else if (!explored) {
                color = UNEXPLORED;
            } else if ("teleport-alcove".equals(roomId)) {
                color = TELEPORT;
            } else if ("throne-hall".equals(roomId)) {
                color = ENDING;
            } else if (ROOM_LOCK_IDS.containsKey(roomId)
                && !isUnlocked.test(ROOM_LOCK_IDS.get(roomId))) {
                color = LOCKED;
            } else {
                color = EXPLORED;
            }

            shapes.setColor(color);
            shapes.rect(cx - half, cy - half, cellSize, cellSize);

            // 当前房间脉冲光圈
            if (roomId.equals(currentRoomId)) {
                float pulse = 0.45f + 0.55f * (float) Math.sin(animTimer * 3.5f);
                shapes.setColor(1f, 0.95f, 0.5f, 0.25f + pulse * 0.45f);
                float r = half + 4f * scale + pulse * 3f * scale;
                shapes.rect(cx - r, cy - r, r * 2f, r * 2f);
            }
        }
        shapes.end();

        // -- 房间标签 --
        batch.begin();
        for (Map.Entry<String, int[]> entry : ROOM_POS.entrySet()) {
            String roomId = entry.getKey();
            int[] pos = entry.getValue();
            if (!exploredIds.contains(roomId)) continue;

            float cx = gridX + pos[0] * cellGap;
            float cy = gridY + (maxRow - pos[1]) * cellGap;
            float half = cellSize / 2f;

            String label = getRoomLabel(roomId);
            font.setColor(LABEL_COLOR);
            layout.setText(font, label);
            float labelW = layout.width;
            font.draw(batch, label, cx - labelW / 2f, cy - half - 5f * scale);
        }
        batch.end();
    }

    private static String getRoomLabel(String roomId)
    {
        switch (roomId) {
            case "outside": return "广场";
            case "theatre": return "讲堂";
            case "pub": return "酒馆";
            case "lab": return "机房";
            case "office": return "办公室";
            case "library": return "图书馆";
            case "cellar": return "地窖";
            case "vault": return "金库";
            case "hidden-shrine": return "神龛";
            case "garden": return "庭院";
            case "guard-room": return "哨站";
            case "armory": return "军械库";
            case "forge": return "铁匠铺";
            case "teleport-alcove": return "传送室";
            case "throne-hall": return "王座厅";
            default: return roomId;
        }
    }
}
