package cn.edu.whut.sept.zuul.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 世界地图渲染器 —— 以节点图展示房间拓扑与探索状态。
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

    private final WorldMapTopology topology = new WorldMapTopology();
    private final GlyphLayout layout = new GlyphLayout();
    private float animTimer;

    public void render(ShapeRenderer shapes, SpriteBatch batch, BitmapFont font,
        float panelX, float panelY, float panelW, float panelH,
        String currentRoomId, Set<String> exploredIds,
        Predicate<String> isUnlocked, float delta)
    {
        animTimer += delta;

        float pad = 28f, titleH = 28f;
        float contentX = panelX + pad, contentY = panelY + pad;
        float contentW = panelW - pad * 2f, contentH = panelH - pad * 2f - titleH;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(BG);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.end();

        batch.begin();
        font.setColor(TITLE_COLOR);
        layout.setText(font, "世界地图  [M] 关闭");
        font.draw(batch, "世界地图  [M] 关闭", panelX + (panelW - layout.width) / 2f, panelY + panelH - pad + 4f);
        batch.end();

        int minCol = 0, maxCol = 4, minRow = 0, maxRow = 4;
        float totalW = (maxCol - minCol) * CELL_GAP + CELL_SIZE;
        float totalH = (maxRow - minRow) * CELL_GAP + CELL_SIZE;
        float scale = Math.min(contentW / totalW, contentH / totalH);
        float cs = CELL_SIZE * scale, cg = CELL_GAP * scale;
        float gridX = contentX + (contentW - totalW * scale) / 2f + cs / 2f;
        float gridY = contentY + (contentH - totalH * scale) / 2f + cs / 2f;

        // 连线
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (String[] conn : topology.connections) {
            int[] a = topology.roomPos.get(conn[0]), b = topology.roomPos.get(conn[1]);
            if (a == null || b == null) continue;
            boolean aExp = exploredIds.contains(conn[0]), bExp = exploredIds.contains(conn[1]);
            float x1 = gridX + a[0] * cg, y1 = gridY + (maxRow - a[1]) * cg;
            float x2 = gridX + b[0] * cg, y2 = gridY + (maxRow - b[1]) * cg;
            shapes.setColor(aExp && bExp ? LINE_EXPLORED : LINE_UNEXPLORED);
            shapes.rectLine(x1, y1, x2, y2, LINE_WIDTH * scale);
        }

        // 节点
        for (Map.Entry<String, int[]> e : topology.roomPos.entrySet()) {
            String rid = e.getKey();
            int[] pos = e.getValue();
            float cx = gridX + pos[0] * cg, cy = gridY + (maxRow - pos[1]) * cg;
            float half = cs / 2f;

            Color color;
            if (rid.equals(currentRoomId)) color = CURRENT;
            else if (!exploredIds.contains(rid)) color = UNEXPLORED;
            else if ("teleport-alcove".equals(rid)) color = TELEPORT;
            else if ("throne-hall".equals(rid)) color = ENDING;
            else if (topology.roomLockIds.containsKey(rid) && !isUnlocked.test(topology.roomLockIds.get(rid)))
                color = LOCKED;
            else color = EXPLORED;

            shapes.setColor(color);
            shapes.rect(cx - half, cy - half, cs, cs);

            if (rid.equals(currentRoomId)) {
                float pulse = 0.45f + 0.55f * (float) Math.sin(animTimer * 3.5f);
                shapes.setColor(1f, 0.95f, 0.5f, 0.25f + pulse * 0.45f);
                float r = half + 4f * scale + pulse * 3f * scale;
                shapes.rect(cx - r, cy - r, r * 2f, r * 2f);
            }
        }
        shapes.end();

        // 标签
        batch.begin();
        for (Map.Entry<String, int[]> e : topology.roomPos.entrySet()) {
            String rid = e.getKey();
            if (!exploredIds.contains(rid)) continue;
            int[] pos = e.getValue();
            float cx = gridX + pos[0] * cg, cy = gridY + (maxRow - pos[1]) * cg;
            String label = WorldMapTopology.roomLabel(rid);
            font.setColor(LABEL_COLOR);
            layout.setText(font, label);
            font.draw(batch, label, cx - layout.width / 2f, cy - cs / 2f - 5f * scale);
        }
        batch.end();
    }
}
