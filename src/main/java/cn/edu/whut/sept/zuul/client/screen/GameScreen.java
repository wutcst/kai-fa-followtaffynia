package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.RpgMain;
import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.domain.RoomScene;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.infra.GameLogger;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.SaveGameService;

import java.util.logging.Logger;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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
 * 主玩法画面：TMX 地图渲染、碰撞检测、房间切换、存档读档与像素风 HUD。
 */
public class GameScreen implements Screen
{
    private static final float TILE = 32f;
    private static final float PLAYER_W = 16f;
    private static final float PLAYER_H = 16f;
    private static final float SPEED = 128f;
    private static final int WORLD_MARGIN_LEFT = 12;
    private static final int WORLD_MARGIN_RIGHT = 12;
    private static final int WORLD_MARGIN_BOTTOM = 112;
    private static final int WORLD_MARGIN_TOP = 82;
    private static final float TOP_BAR_HEIGHT = 66f;
    private static final float FOOTER_HEIGHT = 96f;
    private static final float SIDE_PANEL_MIN_WIDTH = 132f;
    private static final int ICON_MOVE = 0;
    private static final int ICON_ROOM = 1;
    private static final int ICON_LOOK = 2;
    private static final int ICON_TAKE = 3;
    private static final int ICON_INVENTORY = 4;
    private static final int ICON_BACK = 5;
    private static final int ICON_SAVE = 6;
    private static final int ICON_LOAD = 7;
    private static final int ICON_MENU = 8;
    private static final int ICON_TITLE = 9;
    private static final String LOG_TAG = "GameScreen";
    private static final Color UI_LIGHT_TEXT = new Color(1f, 0.96f, 0.82f, 1f);
    private static final Color UI_DARK_TEXT = new Color(0.26f, 0.18f, 0.1f, 1f);
    private static final Logger LOG = GameLogger.get();

    private final RpgMain game;
    private final SpriteBatch batch;
    private final GameEngine engine;
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final ShapeRenderer shapes;
    private final GameUiSkin uiSkin;
    private final GlyphLayout layout;
    private final OrthographicCamera worldCamera;
    private final OrthographicCamera uiCamera;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private TiledMapTileLayer wallLayer;
    private MapObjects objectsLayer;

    private float playerX;
    private float playerY;
    private String actionMessage;
    private String currentMapPath;
    private float exitCooldown;
    private int worldViewportX;
    private int worldViewportY;
    private int worldViewportWidth;
    private int worldViewportHeight;
    private boolean inventoryOpen;
    private boolean paused;
    private boolean screenChanged;

    public GameScreen(RpgMain game, SpriteBatch batch, GameEngine engine)
    {
        this(game, batch, engine, Float.NaN, Float.NaN, "准备探索");
    }

    public GameScreen(RpgMain game, SpriteBatch batch, GameEngine engine,
        float savedPlayerX, float savedPlayerY, String initialStatus)
    {
        this.game = game;
        this.batch = batch;
        this.engine = engine;
        this.font = game.getFonts().getDefault();
        this.smallFont = game.getFonts().copyDefault(0.85f);
        this.shapes = new ShapeRenderer();
        this.uiSkin = new GameUiSkin();
        this.layout = new GlyphLayout();
        this.worldCamera = new OrthographicCamera();
        this.uiCamera = new OrthographicCamera();
        this.actionMessage = initialStatus;

        updateCameras(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        loadCurrentRoom(false);
        if (Float.isNaN(savedPlayerX) || Float.isNaN(savedPlayerY)) {
            snapToSpawn();
        } else {
            playerX = savedPlayerX;
            playerY = savedPlayerY;
            clampPlayerToMap();
        }
    }

    @Override
    public void show()
    {
    }

    @Override
    public void render(float delta)
    {
        handleInput(delta);
        if (screenChanged) {
            return;
        }
        if (!paused) {
            if (exitCooldown > 0f) {
                exitCooldown -= delta;
            }
            checkExitOverlap();
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (map == null || mapRenderer == null) {
            drawMapLoadError();
            return;
        }

        applyWorldViewport();
        worldCamera.update();
        mapRenderer.setView(worldCamera);
        mapRenderer.render();
        drawPlayer();

        applyFullViewport();
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        drawUiPanels();
        drawHud();
        drawFooter();
        batch.end();
    }

    private void loadCurrentRoom(boolean snapAfterLoad)
    {
        String tmxPath = engine.getCurrentRoom().getScene().getTmxPath();
        if (tmxPath.equals(currentMapPath) && map != null) {
            if (snapAfterLoad) {
                snapToSpawn();
            }
            return;
        }
        disposeMap();

        try {
            String roomId = engine.getCurrentRoom().getRoomId();
            LOG.info("loadMap: room=" + roomId
                + " | tmx=" + tmxPath
                + " | entryDir=" + engine.getEntryDirection().toExitKey());
            map = new TmxMapLoader().load(tmxPath);
            mapRenderer = new OrthogonalTiledMapRenderer(map, 1f, batch);
            wallLayer = (TiledMapTileLayer) map.getLayers().get("wall");
            MapLayer objectLayer = map.getLayers().get("objects");
            objectsLayer = objectLayer == null ? null : objectLayer.getObjects();
            if (objectsLayer != null) {
                int exitCount = 0;
                for (MapObject obj : objectsLayer) {
                    if ("exit".equals(obj.getProperties().get("type", String.class))) {
                        exitCount++;
                    }
                }
                LOG.info("loadMap: " + tmxPath + " loaded | exits=" + exitCount
                    + " | objects=" + objectsLayer.getCount());
            } else {
                LOG.info("loadMap: " + tmxPath + " loaded | NO objects layer");
            }
            currentMapPath = tmxPath;
            exitCooldown = 0.3f;
            if (snapAfterLoad) {
                snapToSpawn();
            }
            updateCameras(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        } catch (Exception e) {
            LOG.warning("loadMap: FAILED " + tmxPath + " | " + e.getMessage());
            map = null;
            mapRenderer = null;
            wallLayer = null;
            objectsLayer = null;
            actionMessage = "地图加载失败: " + tmxPath;
        }
    }

    private void snapToSpawn()
    {
        RoomScene.SpawnPoint spawn = engine.resolveCurrentSpawn();
        playerX = spawn.tileX * TILE + TILE / 2f - PLAYER_W / 2f;
        playerY = tileRowToGdxY(spawn.tileY);
        clampPlayerToMap();
        LOG.info("spawn: tile=(" + spawn.tileX + "," + spawn.tileY
            + ") | pixel=(" + (int)playerX + "," + (int)playerY + ")"
            + " | entryDir=" + engine.getEntryDirection().toExitKey());
    }

    /** Tiled 格子行号（0=地图顶部）→ LibGDX 玩家左下角 y */
    private float tileRowToGdxY(float tileRowFromTop)
    {
        int mapRows = (int) (mapPixelHeight() / TILE);
        float rowFromBottom = mapRows - 1 - tileRowFromTop;
        return rowFromBottom * TILE + (TILE - PLAYER_H) / 2f;
    }

    private void handleInput(float delta)
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
            inventoryOpen = false;
            actionMessage = paused ? "已暂停" : "继续探索";
            return;
        }
        if (paused) {
            handlePauseInput();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            saveGame();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            loadGame();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            inventoryOpen = !inventoryOpen;
            actionMessage = inventoryOpen ? "背包已打开" : "背包已关闭";
        }

        movePlayer(delta);

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            actionMessage = engine.look();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            if (engine.moveBack()) {
                loadCurrentRoom(true);
                actionMessage = "已回退至 " + engine.getCurrentRoom().getRoomId();
            } else {
                actionMessage = "无法回退";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            actionMessage = tryTakeFirstItem();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            actionMessage = tryUseFirstItem();
        }
    }

    private void handlePauseInput()
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            saveGame();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            loadGame();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            switchToTitle();
        }
    }

    private int moveLogFrame;

    private void movePlayer(float delta)
    {
        float dx = 0f;
        float dy = 0f;
        boolean wDown = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean sDown = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean aDown = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean dDown = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        if (wDown) dy = SPEED * delta;
        if (sDown) dy = -SPEED * delta;
        if (aDown) dx = -SPEED * delta;
        if (dDown) dx = SPEED * delta;

        if ((wDown || sDown || aDown || dDown) && moveLogFrame % 30 == 0) {
            String dir = "";
            if (wDown) dir = "W/UP(往屏幕上方)";
            if (sDown) dir = "S/DOWN(往屏幕下方)";
            if (aDown) dir = "A/LEFT(往屏幕左)";
            if (dDown) dir = "D/RIGHT(往屏幕右)";
            LOG.info("moveKey: " + dir
                + " | pixel=(" + (int)playerX + "," + (int)playerY + ")"
                + " | tile=(" + (int)(playerX/TILE) + "," + gdxYToTiledRow(playerY) + ")");
        }
        moveLogFrame++;

        if (dx != 0f && canMove(playerX + dx, playerY)) {
            playerX += dx;
        }
        if (dy != 0f && canMove(playerX, playerY + dy)) {
            playerY += dy;
        }
        clampPlayerToMap();
    }

    private int gdxYToTiledRow(float gdxY)
    {
        int mapRows = map == null ? 17 : (int)(mapPixelHeight() / TILE);
        return mapRows - 1 - (int)((gdxY + PLAYER_H / 2f) / TILE);
    }

    private boolean canMove(float newX, float newY)
    {
        if (newX < 0f || newY < 0f
            || newX + PLAYER_W > mapPixelWidth()
            || newY + PLAYER_H > mapPixelHeight()) {
            return false;
        }
        if (wallLayer == null) {
            return true;
        }

        float left = newX;
        float right = newX + PLAYER_W;
        float bottom = newY;
        float top = newY + PLAYER_H;
        int[][] corners = {
            {(int) (left / TILE), (int) (bottom / TILE)},
            {(int) (right / TILE), (int) (bottom / TILE)},
            {(int) (left / TILE), (int) (top / TILE)},
            {(int) (right / TILE), (int) (top / TILE)}
        };

        for (int[] corner : corners) {
            TiledMapTileLayer.Cell cell = wallLayer.getCell(corner[0], corner[1]);
            if (cell != null && cell.getTile() != null) {
                return false;
            }
        }
        return true;
    }

    private void checkExitOverlap()
    {
        if (exitCooldown > 0f || objectsLayer == null) {
            return;
        }

        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_W, PLAYER_H);
        for (MapObject object : objectsLayer) {
            if (!"exit".equals(object.getProperties().get("type", String.class))) {
                continue;
            }

            MapProperties props = object.getProperties();
            float objectX = props.get("x", Float.class) == null ? 0f : props.get("x", Float.class);
            float objectY = props.get("y", Float.class) == null ? 0f : props.get("y", Float.class);
            float objectWidth = props.get("width", Float.class) == null
                ? TILE : props.get("width", Float.class);
            float objectHeight = props.get("height", Float.class) == null
                ? TILE : props.get("height", Float.class);
            // LibGDX TmxMapLoader 已自动将 TMX 的 Y(top-down) 转为 GDX Y(bottom-up)，
            // objectY 直接就是 GDX 坐标，不需要再调用 tiledTopYToGdxY 转换。
            float gdxObjectY = objectY;
            Rectangle exitRect = new Rectangle(objectX, gdxObjectY, objectWidth, objectHeight);

            if (playerRect.overlaps(exitRect)) {
                String targetRoomId = props.get("targetRoomId", String.class);
                Direction direction = resolveDirectionToTarget(targetRoomId);
                if (direction == Direction.DEFAULT) {
                    String directionValue = props.get("direction", String.class);
                    direction = Direction.fromExitKey(directionValue);
                    LOG.info("exit: overlap target=" + targetRoomId
                        + " | fallback to TMX direction=" + directionValue
                        + " | resolved=" + direction.toExitKey());
                }
                LOG.info("exit: trigger target=" + targetRoomId
                    + " | direction=" + direction.toExitKey()
                    + " | from=" + engine.getCurrentRoom().getRoomId());
                if (direction != Direction.DEFAULT && engine.movePlayer(direction)) {
                    LOG.info("exit: MOVE OK -> " + engine.getCurrentRoom().getRoomId());
                    loadCurrentRoom(true);
                    actionMessage = "进入 " + engine.getCurrentRoom().getRoomId();
                    exitCooldown = 0.5f;
                } else if (direction != Direction.DEFAULT) {
                    LOG.warning("exit: MOVE FAILED target=" + targetRoomId
                        + " | " + engine.getLastMessage());
                    actionMessage = engine.getLastMessage();
                    exitCooldown = 0.5f;
                } else {
                    LOG.warning("exit: SKIP target=" + targetRoomId
                        + " | direction=DEFAULT (unresolved)");
                }
                return;
            }
        }
    }

    private Direction resolveDirectionToTarget(String targetRoomId)
    {
        if (targetRoomId == null) {
            return Direction.DEFAULT;
        }
        Direction[] directions = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        };
        for (Direction direction : directions) {
            Room room = engine.getCurrentRoom().getExit(direction.toExitKey());
            if (room != null && targetRoomId.equals(room.getRoomId())) {
                LOG.info("exit: resolve target=" + targetRoomId
                    + " -> direction=" + direction.toExitKey()
                    + " (engine match)");
                return direction;
            }
        }
        LOG.info("exit: resolve target=" + targetRoomId
            + " -> DEFAULT (no engine exit matches, will try TMX direction property)");
        return Direction.DEFAULT;
    }

    private void drawPlayer()
    {
        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.3f, 0.55f, 0.95f, 1f);
        shapes.rect(playerX, playerY, PLAYER_W, PLAYER_H);
        shapes.end();
    }

    private void drawMapLoadError()
    {
        applyFullViewport();
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        font.setColor(Color.RED);
        font.draw(batch, "地图加载失败，请检查 assets/maps 与 tilesets 路径",
            40, Gdx.graphics.getHeight() / 2f);
        drawFooter();
        batch.end();
    }

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

    private void saveGame()
    {
        try {
            GameState state = engine.captureState();
            state.setPlayerX(playerX);
            state.setPlayerY(playerY);
            SaveGameService.save(state);
            Gdx.app.log(LOG_TAG, "Saved game to " + SaveGameService.defaultSavePath());
            actionMessage = "已保存到 " + SaveGameService.defaultSavePath();
        } catch (Exception e) {
            Gdx.app.error(LOG_TAG, "Save failed", e);
            actionMessage = "存档失败: " + e.getClass().getSimpleName();
        }
    }

    private void loadGame()
    {
        try {
            GameState state = SaveGameService.load();
            GameEngine loadedEngine = new GameEngine(state.getPlayerName());
            loadedEngine.restoreState(state);
            screenChanged = true;
            Gdx.app.log(LOG_TAG, "Loaded game from " + SaveGameService.defaultSavePath());
            game.setScreen(new GameScreen(game, batch, loadedEngine,
                state.getPlayerX(), state.getPlayerY(), "已读取存档"));
            disposeLater();
        } catch (Exception e) {
            Gdx.app.error(LOG_TAG, "Load failed", e);
            actionMessage = "读档失败: " + e.getClass().getSimpleName();
        }
    }

    private void switchToTitle()
    {
        screenChanged = true;
        Gdx.app.log(LOG_TAG, "Return to title screen");
        game.setScreen(new TitleScreen(game, batch));
        disposeLater();
    }

    private void drawUiPanels()
    {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        uiSkin.drawWindow(batch, 10, height - TOP_BAR_HEIGHT - 8, width - 20, TOP_BAR_HEIGHT);
        uiSkin.drawWindow(batch, 10, 8, width - 20, FOOTER_HEIGHT);
        uiSkin.drawInset(batch, 28, 64, width - 56, 28);
        drawSidePanels();

        if (paused) {
            float panelWidth = Math.min(660f, width - 72f);
            float panelHeight = Math.min(380f, height - 86f);
            float panelX = (width - panelWidth) / 2f;
            float panelY = (height - panelHeight) / 2f;
            uiSkin.drawWindow(batch, panelX, panelY, panelWidth, panelHeight);
            uiSkin.drawInset(batch, panelX + 30, panelY + 72, panelWidth - 60, panelHeight - 138);
            uiSkin.drawButton(batch, panelX + panelWidth / 2f - 92, panelY + 24, 184, 42);
        }
        if (inventoryOpen) {
            float panelWidth = Math.min(360f, width - 56f);
            float panelHeight = Math.min(190f, Math.max(120f, worldViewportHeight - 24f));
            float panelX = Math.max(24f, width - panelWidth - 28f);
            float panelY = Math.max(WORLD_MARGIN_BOTTOM + 12f,
                height - WORLD_MARGIN_TOP - panelHeight - 12f);
            uiSkin.drawWindow(batch, panelX, panelY, panelWidth, panelHeight);
            uiSkin.drawInset(batch, panelX + 20, panelY + 42, panelWidth - 40, panelHeight - 74);
        }
    }

    private void drawHud()
    {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float contentX = 28f;
        float contentRight = width - 28f;
        float topY = height - 25f;
        float barY = height - 58f;
        float barWidth = Math.max(120f, Math.min(220f, (width - 260f) / 2f));
        float hpX = contentX;
        float weightX = hpX + barWidth + 146f;

        smallFont.setColor(UI_LIGHT_TEXT);
        drawClampedLine(smallFont, "玩家 " + engine.getPlayer().getName(), contentX, topY, 180f);
        drawClampedLine(smallFont, "房间 " + engine.getCurrentRoom().getRoomId(),
            contentX + 210f, topY, Math.max(160f, contentRight - contentX - 370f));
        drawRightAligned("声望 " + engine.getPlayer().getReputation(), contentRight, topY);

        if (hasSidePanels()) {
            drawSidePanelContent();
        } else {
            drawStatusBar("生命", engine.getPlayer().getHp(), engine.getPlayer().getMaxHp(),
                hpX, barY, barWidth, true);
            drawStatusBar("负重", engine.getPlayer().totalWeight(), engine.getPlayer().getMaxWeight(),
                weightX, barY, barWidth, false);
        }

        if (paused) {
            drawPauseMenu();
        }
        if (inventoryOpen) {
            drawInventoryPanel();
        }
    }

    private void drawFooter()
    {
        smallFont.setColor(UI_DARK_TEXT);
        drawClampedLine(smallFont, actionMessage, 44, 84, Gdx.graphics.getWidth() - 88);

        float gap = 9f;
        float totalWidth = 78 + 60 + 60 + 60 + 60 + 60 + 66 + 66 + 72 + gap * 8f;
        float x = Math.max(20f, (Gdx.graphics.getWidth() - totalWidth) / 2f);
        float y = 15f;
        x = drawHintChip("WASD", x, y, 78, 42, ICON_MOVE, 42) + gap;
        x = drawHintChip("Q", x, y, 60, 42, ICON_LOOK, 28) + gap;
        x = drawHintChip("E", x, y, 60, 42, ICON_TAKE, 28) + gap;
        x = drawHintChip("U", x, y, 60, 42, ICON_TAKE, 28) + gap;
        x = drawHintChip("I", x, y, 60, 42, ICON_INVENTORY, 28) + gap;
        x = drawHintChip("B", x, y, 60, 42, ICON_BACK, 28) + gap;
        x = drawHintChip("F5", x, y, 66, 42, ICON_SAVE, 34) + gap;
        x = drawHintChip("F9", x, y, 66, 42, ICON_LOAD, 34) + gap;
        drawHintChip("ESC", x, y, 72, 42, ICON_MENU, 38);
    }

    private void drawStatusBar(String label, int value, int maxValue, float x, float y, float width,
        boolean hpBar)
    {
        smallFont.setColor(UI_LIGHT_TEXT);
        smallFont.draw(batch, label, x, y + 16);
        float barX = x + 42;
        float ratio = maxValue <= 0 ? 0f : (float) value / maxValue;
        if (hpBar) {
            uiSkin.drawRedBar(batch, barX, y, width, 18, ratio);
        } else {
            uiSkin.drawYellowBar(batch, barX, y, width, 18, ratio);
        }
        smallFont.draw(batch, value + "/" + maxValue, barX + width + 10, y + 16);
    }

    private void drawSidePanels()
    {
        if (!hasSidePanels()) {
            return;
        }
        float panelY = WORLD_MARGIN_BOTTOM;
        float panelHeight = Gdx.graphics.getHeight() - WORLD_MARGIN_TOP - WORLD_MARGIN_BOTTOM;
        float leftX = 10f;
        float leftWidth = worldViewportX - 18f;
        float rightX = worldViewportX + worldViewportWidth + 8f;
        float rightWidth = Gdx.graphics.getWidth() - rightX - 10f;

        uiSkin.drawWindow(batch, leftX, panelY, leftWidth, panelHeight);
        uiSkin.drawInset(batch, leftX + 12f, panelY + 18f, leftWidth - 24f, panelHeight - 48f);
        uiSkin.drawWindow(batch, rightX, panelY, rightWidth, panelHeight);
        uiSkin.drawInset(batch, rightX + 12f, panelY + 18f, rightWidth - 24f, panelHeight - 48f);
    }

    private void drawSidePanelContent()
    {
        if (!hasSidePanels()) {
            return;
        }
        float panelY = WORLD_MARGIN_BOTTOM;
        float panelHeight = Gdx.graphics.getHeight() - WORLD_MARGIN_TOP - WORLD_MARGIN_BOTTOM;
        float leftX = 10f;
        float leftWidth = worldViewportX - 18f;
        float rightX = worldViewportX + worldViewportWidth + 8f;
        float rightWidth = Gdx.graphics.getWidth() - rightX - 10f;
        float textInset = 22f;

        smallFont.setColor(UI_LIGHT_TEXT);
        smallFont.draw(batch, "状态", leftX + textInset, panelY + panelHeight - 18f);
        smallFont.draw(batch, "行动", rightX + textInset, panelY + panelHeight - 18f);

        smallFont.setColor(UI_DARK_TEXT);
        float leftTextX = leftX + textInset;
        float leftTextWidth = leftWidth - textInset * 2f;
        drawClampedLine(smallFont, "房间 " + engine.getCurrentRoom().getRoomId(),
            leftTextX, panelY + panelHeight - 52f, leftTextWidth);
        drawCompactStatusBar("生命", engine.getPlayer().getHp(), engine.getPlayer().getMaxHp(),
            leftTextX, panelY + panelHeight - 100f, leftTextWidth, true);
        drawCompactStatusBar("负重", engine.getPlayer().totalWeight(), engine.getPlayer().getMaxWeight(),
            leftTextX, panelY + panelHeight - 148f, leftTextWidth, false);
        drawClampedLine(smallFont, "声望 " + engine.getPlayer().getReputation(),
            leftTextX, panelY + panelHeight - 190f, leftTextWidth);

        float rightTextX = rightX + textInset;
        float rightTextWidth = rightWidth - textInset * 2f;
        drawMultilineClamped(smallFont, actionMessage, rightTextX, panelY + panelHeight - 52f,
            rightTextWidth, 17f, 4);
        float hintY = panelY + 116f;
        drawClampedLine(smallFont, "WASD 移动", rightTextX, hintY + 68f, rightTextWidth);
        drawClampedLine(smallFont, "出口 切换房间", rightTextX, hintY + 46f, rightTextWidth);
        drawClampedLine(smallFont, "Q 调查  E 拾取", rightTextX, hintY + 24f, rightTextWidth);
        drawClampedLine(smallFont, "U 使用  I 背包", rightTextX, hintY + 2f, rightTextWidth);
        drawClampedLine(smallFont, "F5 存档  F9 读档", rightTextX, hintY - 20f, rightTextWidth);
    }

    private void drawCompactStatusBar(String label, int value, int maxValue, float x, float y,
        float width, boolean hpBar)
    {
        smallFont.setColor(UI_DARK_TEXT);
        drawClampedLine(smallFont, label + " " + value + "/" + maxValue, x, y + 26f, width);
        float ratio = maxValue <= 0 ? 0f : (float) value / maxValue;
        if (hpBar) {
            uiSkin.drawRedBar(batch, x, y, width, 14f, ratio);
        } else {
            uiSkin.drawYellowBar(batch, x, y, width, 14f, ratio);
        }
    }

    private boolean hasSidePanels()
    {
        return worldViewportX - 20f >= SIDE_PANEL_MIN_WIDTH
            && Gdx.graphics.getWidth() - (worldViewportX + worldViewportWidth) - 20f >= SIDE_PANEL_MIN_WIDTH;
    }

    private void drawPauseMenu()
    {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float panelWidth = Math.min(660f, width - 72f);
        float panelHeight = Math.min(380f, height - 86f);
        float panelX = (width - panelWidth) / 2f;
        float panelY = (height - panelHeight) / 2f;
        float centerX = panelX + panelWidth / 2f;

        font.setColor(UI_LIGHT_TEXT);
        drawCentered("暂停菜单", centerX, panelY + panelHeight - 36);
        smallFont.setColor(UI_LIGHT_TEXT);
        drawCenteredWithSmallFont("按键说明", centerX, panelY + panelHeight - 64);

        float leftX = panelX + 50;
        float rightX = panelX + panelWidth / 2f + 22;
        float rowY = panelY + panelHeight - 104;
        float rowGap = 31f;
        drawShortcutRow("ESC", "继续探索 / 打开菜单", ICON_MENU, leftX, rowY);
        drawShortcutRow("WASD", "移动角色", ICON_MOVE, leftX, rowY - rowGap);
        drawShortcutRow("出口", "走入出口切换房间", ICON_ROOM, leftX, rowY - rowGap * 2f);
        drawShortcutRow("Q", "调查当前房间", ICON_LOOK, leftX, rowY - rowGap * 3f);
        drawShortcutRow("B", "回退上一个房间", ICON_BACK, leftX, rowY - rowGap * 4f);
        drawShortcutRow("T", "返回标题画面", ICON_TITLE, leftX, rowY - rowGap * 5f);

        drawShortcutRow("E", "拾取地面物品", ICON_TAKE, rightX, rowY);
        drawShortcutRow("U", "使用背包第一件物品", ICON_TAKE, rightX, rowY - rowGap);
        drawShortcutRow("I", "打开 / 关闭背包", ICON_INVENTORY, rightX, rowY - rowGap * 2f);
        drawShortcutRow("F5", "保存当前进度", ICON_SAVE, rightX, rowY - rowGap * 3f);
        drawShortcutRow("F9", "读取存档", ICON_LOAD, rightX, rowY - rowGap * 4f);

        smallFont.setColor(UI_DARK_TEXT);
        drawCenteredInBoxWithSmallFont("ESC 继续", centerX - 92, panelY + 24, 184, 42);
    }

    private void drawInventoryPanel()
    {
        float width = Gdx.graphics.getWidth();
        float panelWidth = Math.min(360f, width - 56f);
        float panelHeight = Math.min(190f, Math.max(120f, worldViewportHeight - 24f));
        float panelX = Math.max(24f, width - panelWidth - 28f);
        float panelY = Math.max(WORLD_MARGIN_BOTTOM + 12f,
            Gdx.graphics.getHeight() - WORLD_MARGIN_TOP - panelHeight - 12f);

        font.setColor(UI_LIGHT_TEXT);
        font.draw(batch, "背包", panelX + 24, panelY + panelHeight - 26);
        font.setColor(UI_DARK_TEXT);
        drawMultiline("地面物品: " + engine.getRoomItemsWithWeight(),
            panelX + 32, panelY + panelHeight - 70, 22);
        drawMultiline("随身物品: " + engine.getPlayerItemsWithWeight(),
            panelX + 32, panelY + panelHeight - 102, 22);
    }

    private float drawHintChip(String key, float x, float y, float width, float height, int icon,
        float keyWidth)
    {
        uiSkin.drawLightButton(batch, x, y, width, height);
        uiSkin.drawButton(batch, x + 6, y + 7, keyWidth, height - 14);
        drawIcon(icon, x + keyWidth + 13, y + 8, height - 16);

        smallFont.setColor(Color.WHITE);
        drawCenteredInBoxWithSmallFont(key, x + 6, y + 7, keyWidth, height - 14);
        return x + width;
    }

    private void drawIcon(int icon, float x, float y, float size)
    {
        if (icon == ICON_MOVE) {
            uiSkin.drawMoveIcon(batch, x, y, size);
        } else if (icon == ICON_ROOM) {
            uiSkin.drawRoomIcon(batch, x, y, size);
        } else if (icon == ICON_LOOK) {
            uiSkin.drawLookIcon(batch, x, y, size);
        } else if (icon == ICON_TAKE) {
            uiSkin.drawHandIcon(batch, x, y, size);
        } else if (icon == ICON_INVENTORY) {
            uiSkin.drawInventoryIcon(batch, x, y, size);
        } else if (icon == ICON_BACK) {
            uiSkin.drawBackIcon(batch, x, y, size);
        } else if (icon == ICON_SAVE) {
            uiSkin.drawSaveIcon(batch, x, y, size);
        } else if (icon == ICON_LOAD) {
            uiSkin.drawLoadIcon(batch, x, y, size);
        } else if (icon == ICON_MENU) {
            uiSkin.drawMenuIcon(batch, x, y, size);
        } else if (icon == ICON_TITLE) {
            uiSkin.drawTitleIcon(batch, x, y, size);
        } else {
            uiSkin.drawCircleIcon(batch, x, y, size);
        }
    }

    private void drawCentered(String text, float centerX, float y)
    {
        layout.setText(font, text);
        font.draw(batch, text, centerX - layout.width / 2f, y);
    }

    private void drawCenteredWithSmallFont(String text, float centerX, float y)
    {
        layout.setText(smallFont, text);
        smallFont.draw(batch, text, centerX - layout.width / 2f, y);
    }

    private void drawCenteredInBoxWithSmallFont(String text, float x, float y, float width,
        float height)
    {
        layout.setText(smallFont, text);
        smallFont.draw(batch, text, x + (width - layout.width) / 2f,
            y + (height + layout.height) / 2f + 1f);
    }

    private void drawShortcutRow(String key, String label, int icon, float x, float y)
    {
        drawIcon(icon, x, y + 3, 24);
        float keyWidth = key.length() >= 4 ? 54f : key.length() >= 3 ? 42f : 34f;
        uiSkin.drawButton(batch, x + 34, y, keyWidth, 28);
        smallFont.setColor(Color.WHITE);
        drawCenteredInBoxWithSmallFont(key, x + 34, y, keyWidth, 28);
        smallFont.setColor(UI_DARK_TEXT);
        smallFont.draw(batch, label, x + keyWidth + 78, y + 20);
    }

    private void drawClampedLine(BitmapFont activeFont, String text, float x, float y,
        float maxWidth)
    {
        String line = text == null ? "" : text.replace('\n', ' ');
        layout.setText(activeFont, line);
        if (layout.width <= maxWidth) {
            activeFont.draw(batch, line, x, y);
            return;
        }

        String suffix = "...";
        while (line.length() > 1) {
            line = line.substring(0, line.length() - 1);
            layout.setText(activeFont, line + suffix);
            if (layout.width <= maxWidth) {
                activeFont.draw(batch, line + suffix, x, y);
                return;
            }
        }
        activeFont.draw(batch, suffix, x, y);
    }

    private void drawMultilineClamped(BitmapFont activeFont, String text, float x, float y,
        float maxWidth, float lineHeight, int maxLines)
    {
        String source = text == null ? "" : text.replace('\n', ' ');
        int start = 0;
        int lineCount = 0;
        while (start < source.length() && lineCount < maxLines) {
            int end = source.length();
            String line = source.substring(start, end);
            layout.setText(activeFont, line);
            while (line.length() > 1 && layout.width > maxWidth) {
                end--;
                line = source.substring(start, end);
                layout.setText(activeFont, line + (lineCount == maxLines - 1 ? "..." : ""));
            }
            if (lineCount == maxLines - 1 && end < source.length()) {
                activeFont.draw(batch, line + "...", x, y - lineHeight * lineCount);
                return;
            }
            activeFont.draw(batch, line, x, y - lineHeight * lineCount);
            start = end;
            while (start < source.length() && source.charAt(start) == ' ') {
                start++;
            }
            lineCount++;
        }
    }

    private void drawRightAligned(String text, float rightX, float y)
    {
        layout.setText(smallFont, text);
        smallFont.draw(batch, text, rightX - layout.width, y);
    }

    private void drawMultiline(String text, float x, float y, float lineHeight)
    {
        String[] lines = text.split("\\n");
        float lineY = y;
        for (String line : lines) {
            font.draw(batch, line, x, lineY);
            lineY -= lineHeight;
        }
    }

    private void clampPlayerToMap()
    {
        float maxX = Math.max(0f, mapPixelWidth() - PLAYER_W);
        float maxY = Math.max(0f, mapPixelHeight() - PLAYER_H);
        playerX = Math.max(0f, Math.min(playerX, maxX));
        playerY = Math.max(0f, Math.min(playerY, maxY));
    }

    private float mapPixelWidth()
    {
        if (map == null) {
            return 960f;
        }
        MapProperties props = map.getProperties();
        Integer width = props.get("width", Integer.class);
        Integer tileWidth = props.get("tilewidth", Integer.class);
        if (width == null || tileWidth == null) {
            return 960f;
        }
        return width * tileWidth;
    }

    /** Tiled 对象 y（自上向下）→ LibGDX 世界 y（自下向上），与出口检测一致。 */
    private float tiledTopYToGdxY(float tiledTopY, float objectHeight)
    {
        return mapPixelHeight() - tiledTopY - objectHeight;
    }

    private float mapPixelHeight()
    {
        if (map == null) {
            return 544f;
        }
        MapProperties props = map.getProperties();
        Integer height = props.get("height", Integer.class);
        Integer tileHeight = props.get("tileheight", Integer.class);
        if (height == null || tileHeight == null) {
            return 544f;
        }
        return height * tileHeight;
    }

    private void updateCameras(int width, int height)
    {
        float mapWidth = mapPixelWidth();
        float mapHeight = mapPixelHeight();
        updateWorldViewport(width, height, mapWidth, mapHeight);
        worldCamera.setToOrtho(false, mapWidth, mapHeight);
        worldCamera.position.set(mapWidth / 2f, mapHeight / 2f, 0f);
        worldCamera.update();
        uiCamera.setToOrtho(false, width, height);
        uiCamera.update();
    }

    private void updateWorldViewport(int width, int height, float mapWidth, float mapHeight)
    {
        int availableX = WORLD_MARGIN_LEFT;
        int availableY = WORLD_MARGIN_BOTTOM;
        int availableWidth = Math.max(1, width - WORLD_MARGIN_LEFT - WORLD_MARGIN_RIGHT);
        int availableHeight = Math.max(1, height - WORLD_MARGIN_BOTTOM - WORLD_MARGIN_TOP);
        float mapAspect = mapWidth / mapHeight;
        float availableAspect = (float) availableWidth / availableHeight;
        if (availableAspect > mapAspect) {
            worldViewportHeight = availableHeight;
            worldViewportWidth = Math.round(worldViewportHeight * mapAspect);
            worldViewportX = availableX + (availableWidth - worldViewportWidth) / 2;
            worldViewportY = availableY;
        } else {
            worldViewportWidth = availableWidth;
            worldViewportHeight = Math.round(worldViewportWidth / mapAspect);
            worldViewportX = availableX;
            worldViewportY = availableY + (availableHeight - worldViewportHeight) / 2;
        }
    }

    private void applyWorldViewport()
    {
        Gdx.gl.glViewport(worldViewportX, worldViewportY, worldViewportWidth, worldViewportHeight);
    }

    private void applyFullViewport()
    {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void disposeLater()
    {
        final GameScreen oldScreen = this;
        Gdx.app.postRunnable(new Runnable()
        {
            @Override
            public void run()
            {
                oldScreen.dispose();
            }
        });
    }

    private void disposeMap()
    {
        if (mapRenderer != null) {
            mapRenderer.dispose();
            mapRenderer = null;
        }
        if (map != null) {
            map.dispose();
            map = null;
        }
        wallLayer = null;
        objectsLayer = null;
    }

    @Override
    public void resize(int width, int height)
    {
        updateCameras(width, height);
    }

    @Override
    public void pause()
    {
    }

    @Override
    public void resume()
    {
    }

    @Override
    public void hide()
    {
    }

    @Override
    public void dispose()
    {
        disposeMap();
        shapes.dispose();
        uiSkin.dispose();
        smallFont.dispose();
    }
}
