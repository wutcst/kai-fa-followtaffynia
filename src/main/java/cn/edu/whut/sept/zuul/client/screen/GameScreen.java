package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.RpgMain;
import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.RoomScene;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * 主玩法画面：TMX 地图渲染 + 碰撞检测 + 房间切换。
 */
public class GameScreen implements Screen
{
    private static final float TILE = 32f;
    private static final float PLAYER_W = 16f;
    private static final float PLAYER_H = 16f;
    private static final float SPEED = 128f;

    private final RpgMain game;
    private final SpriteBatch batch;
    private final GameEngine engine;
    private final BitmapFont font;
    private final OrthographicCamera cam;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private TiledMapTileLayer wallLayer;
    private MapObjects objectsLayer;
    private final ShapeRenderer shapes;

    private float px, py;
    private String status;
    private String currentMapPath;
    private float exitCooldown;

    public GameScreen(RpgMain game, SpriteBatch batch, GameEngine engine)
    {
        this.game = game;
        this.batch = batch;
        this.engine = engine;
        this.font = game.getFonts().copyDefault(0.9f);
        this.shapes = new ShapeRenderer();
        this.cam = new OrthographicCamera();
        this.cam.setToOrtho(false, 960, 540);
        this.status = "WASD 移动 | 走进出口换房 | E 拾取 | U 使用物品 | Q 调查 | B 回退 | ESC 标题";

        loadCurrentRoom();
    }

    // ==================== 地图加载 ====================

    private void loadCurrentRoom()
    {
        String tmxPath = engine.getCurrentRoom().getScene().getTmxPath();
        if (tmxPath.equals(currentMapPath) && map != null) {
            snapToSpawn();
            return;
        }
        if (map != null) {
            map.dispose();
        }

        try {
            map = new TmxMapLoader().load(tmxPath);
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Failed to load map: " + tmxPath, e);
            // fallback: keep old map or create empty
            if (map == null) return;
        }

        mapRenderer = new OrthogonalTiledMapRenderer(map, batch);
        wallLayer = (TiledMapTileLayer) map.getLayers().get("wall");
        MapLayer objLayer = map.getLayers().get("objects");
        if (objLayer != null) {
            objectsLayer = objLayer.getObjects();
        }
        currentMapPath = tmxPath;
        snapToSpawn();
        exitCooldown = 0.3f;
        Gdx.app.log("GameScreen", "Loaded: " + tmxPath);
    }

    private void snapToSpawn()
    {
        RoomScene.SpawnPoint sp = engine.resolveCurrentSpawn();
        // tile coords → pixel center of tile
        px = sp.tileX * TILE + TILE / 2f - PLAYER_W / 2f;
        py = sp.tileY * TILE + TILE / 2f - PLAYER_H / 2f;
    }

    // ==================== 主循环 ====================

    @Override
    public void render(float delta)
    {
        if (map == null) {
            Gdx.gl.glClearColor(0.08f, 0.08f, 0.14f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            batch.begin();
            font.setColor(Color.RED);
            font.draw(batch, "地图加载失败！请检查 assets/ 是否在 classpath", 50, Gdx.graphics.getHeight() / 2f);
            batch.end();
            return;
        }
        if (exitCooldown > 0) exitCooldown -= delta;

        handleInput(delta);
        checkExitOverlap();

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        cam.update();
        mapRenderer.setView(cam);
        mapRenderer.render();

        drawPlayer();
        drawHud();
    }

    // ==================== 输入 ====================

    private void handleInput(float delta)
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            engine.captureState();
            dispose();
            game.setScreen(new TitleScreen(game, batch));
            return;
        }

        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))
            dy = SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))
            dy = -SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))
            dx = -SPEED * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT))
            dx = SPEED * delta;

        // 分离轴碰撞
        if (dx != 0 && canMove(px + dx, py)) {
            px += dx;
        }
        if (dy != 0 && canMove(px, py + dy)) {
            py += dy;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            status = engine.look();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            if (engine.moveBack()) {
                loadCurrentRoom();
                status = "回退至 " + engine.getCurrentRoom().getRoomId();
            } else {
                status = "无法回退";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            status = tryTakeFirstItem();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            status = tryUseFirstItem();
        }
    }

    // ==================== 碰撞检测 ====================

    private boolean canMove(float newX, float newY)
    {
        if (wallLayer == null) return true;
        // 检查玩家四角所在的 tile
        float left = newX;
        float right = newX + PLAYER_W;
        float bottom = newY;
        float top = newY + PLAYER_H;

        int[][] corners = {
            {(int)(left / TILE), (int)(bottom / TILE)},
            {(int)(right / TILE), (int)(bottom / TILE)},
            {(int)(left / TILE), (int)(top / TILE)},
            {(int)(right / TILE), (int)(top / TILE)},
        };

        for (int[] c : corners) {
            TiledMapTileLayer.Cell cell = wallLayer.getCell(c[0], c[1]);
            if (cell != null && cell.getTile() != null) {
                return false;
            }
        }
        return true;
    }

    // ==================== 出口检测 ====================

    private void checkExitOverlap()
    {
        if (exitCooldown > 0 || objectsLayer == null) return;

        Rectangle playerRect = new Rectangle(px, py, PLAYER_W, PLAYER_H);
        for (MapObject obj : objectsLayer) {
            if (!"exit".equals(obj.getProperties().get("type", String.class))) continue;

            MapProperties props = obj.getProperties();
            float ox = props.get("x", Float.class) != null ? props.get("x", Float.class) : 0f;
            float oy = props.get("y", Float.class) != null ? props.get("y", Float.class) : 0f;

            // Tiled 对象 y 坐标右下角原点 → 转换为 LibGDX 左下角原点
            float objYadjusted = Gdx.graphics.getHeight() - oy - TILE;

            Rectangle exitRect = new Rectangle(ox, objYadjusted, TILE, TILE);
            if (playerRect.overlaps(exitRect)) {
                String target = props.get("targetRoomId", String.class);
                String dirStr = props.get("direction", String.class);
                Direction dir = dirStr != null ? Direction.fromExitKey(dirStr) : Direction.DEFAULT;

                if (target != null && engine.movePlayer(dir)) {
                    loadCurrentRoom();
                    status = "进入 " + engine.getCurrentRoom().getRoomId();
                    exitCooldown = 0.5f;
                } else if (engine.movePlayer(dir)) {
                    loadCurrentRoom();
                    status = "进入 " + engine.getCurrentRoom().getRoomId();
                    exitCooldown = 0.5f;
                }
                return;
            }
        }
    }

    // ==================== 渲染 ====================

    private void drawPlayer()
    {
        shapes.setProjectionMatrix(cam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.25f, 0.55f, 0.95f, 1f);
        shapes.rect(px, py, PLAYER_W, PLAYER_H);
        shapes.end();
    }

    private void drawHud()
    {
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        float h = Gdx.graphics.getHeight();
        font.draw(batch, engine.getPlayer().getName() + " | " + engine.getCurrentRoom().getRoomId(), 10, h - 10);
        font.draw(batch, "HP: " + engine.getPlayer().getHp() + "/" + engine.getPlayer().getMaxHp()
            + " | 负重: " + engine.getPlayer().totalWeight() + "/" + engine.getPlayer().getMaxWeight(), 10, h - 28);
        font.draw(batch, status, 10, 24);
        batch.end();
    }

    // ==================== 交互 ====================

    private String tryTakeFirstItem()
    {
        if (engine.getCurrentRoom().getItems().isEmpty()) {
            return "这里没有可拾取的物品";
        }
        String itemId = engine.getCurrentRoom().getItems().get(0).getItemId();
        if (engine.takeItem(itemId)) {
            return "拾取了 " + itemId;
        }
        return "拾取失败（可能超重）";
    }

    private String tryUseFirstItem()
    {
        if (engine.getPlayer().getInventory().isEmpty()) {
            return "背包是空的";
        }
        String itemId = engine.getPlayer().getInventory().get(0).getItemId();
        return engine.useItem(itemId);
    }

    // ==================== 生命周期 ====================

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose()
    {
        if (map != null) {
            map.dispose();
            map = null;
        }
        if (shapes != null) {
            shapes.dispose();
        }
    }
}
