package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.RpgMain;
import cn.edu.whut.sept.zuul.client.render.PlayerRenderer;
import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
import cn.edu.whut.sept.zuul.client.ui.WorldMapRenderer;
import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.domain.RoomScene;
import cn.edu.whut.sept.zuul.engine.CombatAction;
import cn.edu.whut.sept.zuul.engine.CombatMode;
import cn.edu.whut.sept.zuul.engine.CombatOutcome;
import cn.edu.whut.sept.zuul.engine.CombatSnapshot;
import cn.edu.whut.sept.zuul.engine.CombatSystem;
import cn.edu.whut.sept.zuul.engine.Bullet;
import cn.edu.whut.sept.zuul.engine.EncounterMenu;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.ItemUseCheck;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatPhase;
import cn.edu.whut.sept.zuul.infra.GameLogger;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.SaveGameService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

/**
 * 主玩法画面：TMX 地图渲染、碰撞检测、房间切换、存档读档与像素风 HUD。
 */
public class GameScreen implements Screen
{
    private static final float TILE = 32f;
    private static final float PLAYER_W = 16f;
    private static final float PLAYER_H = 16f;
    private static final float SPEED = 128f;
    private static final float DASH_SPEED_MULTIPLIER = 3.0f;
    private static final float DASH_DURATION = 0.3f;
    private static final float DASH_COOLDOWN = 1.0f;
    private static final float ATTACK_COOLDOWN = 0.5f;
    private static final int WORLD_MARGIN_LEFT = 12;
    private static final int WORLD_MARGIN_RIGHT = 12;
    private static final int WORLD_MARGIN_BOTTOM = 96;
    private static final int WORLD_MARGIN_TOP = 64;
    private static final float TOP_BAR_HEIGHT = 56f;
    private static final float FOOTER_HEIGHT = 96f;
    private static final float SIDE_PANEL_MIN_WIDTH = 132f;
    private static final boolean SIDE_PANELS_ENABLED = false;
    private static final float UI_GRID = 8f;
    private static final float UI_EDGE = 8f;
    private static final float UI_PAD = 16f;
    private static final float UI_INSET = 12f;
    private static final float UI_HEADER_HEIGHT = 32f;
    private static final float UI_CHIP_HEIGHT = 40f;
    private static final float UI_CHIP_GAP = 8f;
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
    private static final int ICON_USE = 10;
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
    private final List<NpcPlaceholder> npcPlaceholders;
    private final List<ItemPlaceholder> itemPlaceholders;

    private float playerX;
    private float playerY;
    private PlayerRenderer playerRenderer;
    private String currentFacing = "south";
    private boolean isMovingLastFrame;
    private String actionMessage;
    private String currentMapPath;
    private float exitCooldown;
    private boolean isDashing;
    private float dashTimer;
    private float dashCooldownTimer;
    private boolean isAttacking;
    private float attackTimer;
    private float attackCooldownTimer;
    private int worldViewportX;
    private int worldViewportY;
    private int worldViewportWidth;
    private int worldViewportHeight;
    private boolean inventoryOpen;
    private boolean inventoryInspectMode;
    private int inventoryInspectIndex;
    private int inventoryScrollOffset;
    private boolean encounterMenuOpen;
    private EncounterMenu encounterMenu;
    private Dialogue activeDialogue;
    private CombatSnapshot activeCombatSnapshot;
    private final List<String> dialoguePages;
    private int dialoguePageIndex;
    private boolean paused;
    private boolean screenChanged;
    private boolean worldMapOpen;
    private final WorldMapRenderer worldMapRenderer;

    public GameScreen(RpgMain game, SpriteBatch batch, GameEngine engine)
    {
        this(game, batch, engine, Float.NaN, Float.NaN, "准备探索", "south");
    }

    public GameScreen(RpgMain game, SpriteBatch batch, GameEngine engine,
        float savedPlayerX, float savedPlayerY, String initialStatus)
    {
        this(game, batch, engine, savedPlayerX, savedPlayerY, initialStatus, "south");
    }

    public GameScreen(RpgMain game, SpriteBatch batch, GameEngine engine,
        float savedPlayerX, float savedPlayerY, String initialStatus, String savedFacing)
    {
        this.game = game;
        this.batch = batch;
        this.engine = engine;
        this.font = game.getFonts().getDefault();
        this.smallFont = game.getFonts().copyDefault(0.85f);
        this.shapes = new ShapeRenderer();
        shapes.setAutoShapeType(true);
        this.uiSkin = new GameUiSkin();
        this.layout = new GlyphLayout();
        this.worldCamera = new OrthographicCamera();
        this.uiCamera = new OrthographicCamera();
        this.actionMessage = initialStatus;
        this.npcPlaceholders = new java.util.ArrayList<>();
        this.itemPlaceholders = new java.util.ArrayList<>();
        this.dialoguePages = new ArrayList<>();
        this.dialoguePageIndex = 0;
        this.inventoryInspectIndex = 0;
        this.playerRenderer = new PlayerRenderer();
        this.worldMapRenderer = new WorldMapRenderer();
        this.currentFacing = (savedFacing != null && !savedFacing.isEmpty()) ? savedFacing : "south";

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
            // 冲刺计时器衰减
            if (isDashing) {
                dashTimer -= delta;
                if (dashTimer <= 0f) {
                    isDashing = false;
                    dashTimer = 0f;
                    dashCooldownTimer = DASH_COOLDOWN;
                }
            }
            if (dashCooldownTimer > 0f) {
                dashCooldownTimer -= delta;
            }
            // 攻击计时器衰减
            if (isAttacking) {
                attackTimer -= delta;
                if (playerRenderer.isAttackFinished() || attackTimer <= 0f) {
                    isAttacking = false;
                    attackTimer = 0f;
                    attackCooldownTimer = ATTACK_COOLDOWN;
                }
            }
            if (attackCooldownTimer > 0f) {
                attackCooldownTimer -= delta;
            }
            checkExitOverlap();
        }

        Gdx.gl.glClearColor(0.06f, 0.06f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (map == null || mapRenderer == null) {
            drawMapLoadError();
            return;
        }

        playerRenderer.update(delta, isMovingLastFrame, isDashing, isAttacking,
            PlayerRenderer.FacingDirection.fromString(currentFacing));

        applyWorldViewport();
        worldCamera.update();
        mapRenderer.setView(worldCamera);
        mapRenderer.render();
        drawNpcPlaceholders();
        drawItemPlaceholders();
        drawNpcDialogueBubble();
        drawPlayer();
        drawInteractionPrompt();

        applyFullViewport();
        batch.setProjectionMatrix(uiCamera.combined);

        if (engine.isInCombat() && engine.isUndertaleCombat()) {
            drawUtCombatOverlay();
        }

        batch.begin();
        if (!(engine.isInCombat() && engine.isUndertaleCombat())) {
            drawUiPanels();
            drawHud();
            drawFooter();
        }
        batch.end();

        if (worldMapOpen) {
            drawWorldMap(delta);
        }
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
            buildNpcPlaceholders(engine.getCurrentRoom().getRoomId());
            buildItemPlaceholders(engine.getCurrentRoom().getRoomId());
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
            isDashing = false;
            dashTimer = 0f;
            dashCooldownTimer = 0f;
            isAttacking = false;
            attackTimer = 0f;
            attackCooldownTimer = 0f;
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
            npcPlaceholders.clear();
            actionMessage = "地图加载失败: " + tmxPath;
        }
    }

    private void buildNpcPlaceholders(String roomId)
    {
        npcPlaceholders.clear();

        // 优先：若 TMX objects 层存在 type=npc 的对象，则直接读取其矩形范围与 npcId。
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
                // TMX 矩形是像素坐标（左下角），与当前 world 坐标一致。
                npcPlaceholders.add(NpcPlaceholder.forNpc(npcId, rect));
            }
        }

        // 兼容：地图暂未放 npc 对象时，用明显色块占位（不可穿过）。
        if (!npcPlaceholders.isEmpty()) {
            return;
        }
        if ("guard-room".equals(roomId)) {
            npcPlaceholders.add(NpcPlaceholder.guard(tileToWorldRect(15, 7, 1, 1)));
        } else if ("garden".equals(roomId)) {
            // 守卫守在庭院南侧门口，未进门也可按 E 对话解锁 guard-gate
            npcPlaceholders.add(NpcPlaceholder.guard(tileToWorldRect(12, 15, 1, 1)));
        } else if ("hidden-shrine".equals(roomId)) {
            npcPlaceholders.add(NpcPlaceholder.hermit(tileToWorldRect(15, 7, 1, 1)));
        } else if ("forge".equals(roomId)) {
            npcPlaceholders.add(NpcPlaceholder.merchant(tileToWorldRect(15, 7, 1, 1)));
        }
    }

    private Rectangle tileToWorldRect(int tileX, int tiledRowFromTop, int wTiles, int hTiles)
    {
        float x = tileX * TILE;
        float y = tileRowToGdxY(tiledRowFromTop) - (TILE - PLAYER_H) / 2f;
        return new Rectangle(x, y, wTiles * TILE, hTiles * TILE);
    }

    private void drawNpcPlaceholders()
    {
        if (npcPlaceholders.isEmpty()) {
            return;
        }
        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (NpcPlaceholder npc : npcPlaceholders) {
            shapes.setColor(npc.color);
            shapes.rect(npc.bounds.x, npc.bounds.y, npc.bounds.width, npc.bounds.height);
        }
        shapes.end();
    }

    // ---------- 道具占位块 ----------

    private void buildItemPlaceholders(String roomId)
    {
        itemPlaceholders.clear();
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
                rect = tileToWorldRect(def.tileX, def.tileY, 1, 1);
                color = def.color;
            } else {
                // 无定义的道具（如玩家丢弃的）放在默认位置
                rect = tileToWorldRect(15, 8, 1, 1);
                color = new Color(0.9f, 0.9f, 0.5f, 1f);
            }
            itemPlaceholders.add(new ItemPlaceholder(item.getItemId(), rect, color));
        }
    }

    private static ItemSpawnDef findSpawnDef(List<ItemSpawnDef> defs, String itemId)
    {
        if (defs == null) return null;
        for (ItemSpawnDef def : defs) {
            if (def.itemId.equals(itemId)) return def;
        }
        return null;
    }

    private void rebuildItemPlaceholders()
    {
        buildItemPlaceholders(engine.getCurrentRoom().getRoomId());
    }

    private void drawItemPlaceholders()
    {
        if (itemPlaceholders.isEmpty()) return;
        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (ItemPlaceholder ip : itemPlaceholders) {
            shapes.setColor(ip.color);
            shapes.rect(ip.bounds.x, ip.bounds.y, ip.bounds.width, ip.bounds.height);
        }
        shapes.set(ShapeRenderer.ShapeType.Line);
        for (ItemPlaceholder ip : itemPlaceholders) {
            shapes.setColor(1f, 1f, 1f, 0.7f);
            shapes.rect(ip.bounds.x, ip.bounds.y, ip.bounds.width, ip.bounds.height);
        }
        shapes.end();
    }

    /** 返回玩家附近可拾取的道具 ID，若距离太远返回 null。 */
    private String findNearbyItemId()
    {
        if (itemPlaceholders.isEmpty()) return null;
        Rectangle interactRect = new Rectangle(
            playerX - 10f, playerY - 10f,
            PLAYER_W + 20f, PLAYER_H + 20f);
        for (ItemPlaceholder ip : itemPlaceholders) {
            if (ip.bounds.overlaps(interactRect)) {
                return ip.itemId;
            }
        }
        return null;
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
            if (encounterMenuOpen) {
                engine.leaveEncounter();
                encounterMenuOpen = false;
                encounterMenu = null;
                actionMessage = "已关闭遭遇菜单";
                return;
            }
            if (inventoryOpen) {
                inventoryOpen = false;
                inventoryInspectMode = false;
                actionMessage = "背包已关闭";
                return;
            }
            paused = !paused;
            actionMessage = paused ? "已暂停" : "继续探索";
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            worldMapOpen = !worldMapOpen;
            return;
        }
        if (paused) {
            handlePauseInput();
            return;
        }

        if (encounterMenuOpen) {
            handleEncounterInput();
            return;
        }
        if (activeDialogue != null) {
            handleDialogueInput();
            return;
        }
        if (engine.isInDialogue()) {
            handleDialogueInput();
            return;
        }
        if (engine.isInCombat()) {
            if (engine.isUndertaleCombat()) {
                handleUtCombatInput(delta);
            } else {
                handleCombatInput();
            }
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
            if (!inventoryOpen) {
                inventoryInspectMode = false;
            }
            actionMessage = inventoryOpen
                ? "背包已打开：↑↓选择，U使用，X查看详情"
                : "背包已关闭";
        }

        if (inventoryOpen) {
            handleInventoryBrowseInput();
            return;
        }

        // 攻击：J键触发
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            if (!isAttacking && attackCooldownTimer <= 0f && !isDashing) {
                isAttacking = true;
                attackTimer = 1.0f;
                actionMessage = "攻击！";
            }
        }

        // 冲刺：F键触发
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            if (!isDashing && dashCooldownTimer <= 0f && !isAttacking) {
                isDashing = true;
                dashTimer = DASH_DURATION;
                actionMessage = "冲刺！";
            }
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
            String npcId = findNearbyNpcId();
            if (npcId != null) {
                startNpcEncounter(npcId);
            } else {
                String itemId = findNearbyItemId();
                if (itemId != null) {
                    if (engine.takeItem(itemId)) {
                        rebuildItemPlaceholders();
                        actionMessage = "拾取了 " + itemId;
                    } else {
                        actionMessage = "拾取失败（可能超重）";
                    }
                }
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            actionMessage = "按 I 打开背包，选择物品后按 U 使用";
        }
    }

    private String findNearbyNpcId()
    {
        if (npcPlaceholders.isEmpty()) {
            return null;
        }
        // interaction area intentionally larger than collision area
        // so pressing E works even if the player can't overlap the NPC.
        Rectangle interactRect = new Rectangle(
            playerX - 10f, playerY - 10f,
            PLAYER_W + 20f, PLAYER_H + 20f);
        for (NpcPlaceholder npc : npcPlaceholders) {
            if (npc.bounds.overlaps(interactRect)) {
                return npc.npcId;
            }
        }
        return null;
    }

    private void startNpcEncounter(String npcId)
    {
        EncounterMenu menu = engine.startNpcEncounter(npcId);
        if (menu == null) {
            actionMessage = engine.getLastMessage();
            return;
        }
        encounterMenu = menu;
        encounterMenuOpen = true;
        activeDialogue = null;
        activeCombatSnapshot = null;
        actionMessage = formatEncounterMenu(menu);
    }

    private String formatEncounterMenu(EncounterMenu menu)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("遇到 ").append(menu.npcId).append("。\n");
        if (menu.canTalk) {
            sb.append("按 1 交流。 ");
        }
        if (menu.canFight) {
            sb.append("按 2 杀害。 ");
        }
        if (menu.canUndertaleFight) {
            sb.append("按 4 UT战斗。 ");
        }
        if (menu.canLeave) {
            sb.append("按 3 离开。 ");
        }
        return sb.toString();
    }

    private void handleEncounterInput()
    {
        if (!encounterMenuOpen || encounterMenu == null) {
            encounterMenuOpen = false;
            encounterMenu = null;
            return;
        }
        String npcId = encounterMenu.npcId;

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            if (!encounterMenu.canTalk) {
                actionMessage = "你不能交流。";
                return;
            }
            activeDialogue = engine.talkNpc(npcId);
            encounterMenuOpen = false;
            encounterMenu = null;
            activeCombatSnapshot = null;
            actionMessage = formatDialogue(activeDialogue);
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            if (!encounterMenu.canFight) {
                actionMessage = "你不能战斗。";
                return;
            }
            activeCombatSnapshot = engine.startCombat(npcId);
            encounterMenuOpen = false;
            encounterMenu = null;
            activeDialogue = null;
            actionMessage = formatCombatSnapshot(activeCombatSnapshot);
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            if (encounterMenu.canLeave) {
                engine.leaveEncounter();
                actionMessage = "你离开了。";
            } else {
                actionMessage = "你现在不能离开。";
            }
            encounterMenuOpen = false;
            encounterMenu = null;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            if (!encounterMenu.canUndertaleFight) {
                actionMessage = "UT 战斗不可用。";
                return;
            }
            activeCombatSnapshot = engine.startCombat(npcId, CombatMode.UNDERTALE);
            encounterMenuOpen = false;
            encounterMenu = null;
            activeDialogue = null;
            actionMessage = "[UT] " + formatCombatSnapshot(activeCombatSnapshot);
            return;
        }
    }

    private void handleDialogueInput()
    {
        if (activeDialogue == null) {
            return;
        }

        if (dialoguePages.isEmpty()) {
            prepareDialoguePages(activeDialogue);
        }

        // Enter：翻页；到最后一页后若无选项/已结束则退出
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (dialoguePageIndex + 1 < dialoguePages.size()) {
                dialoguePageIndex++;
                actionMessage = formatDialogue(activeDialogue);
                return;
            }
            List<String> opts = activeDialogue.getOptionTexts();
            if (opts == null || opts.isEmpty() || !activeDialogue.isActive()) {
                activeDialogue = null;
                engine.endDialogue();
                actionMessage = "对话结束。";
                return;
            }
            actionMessage = formatDialogue(activeDialogue);
            return;
        }

        // 未到最后一页：只允许 Enter 翻页
        if (dialoguePageIndex + 1 < dialoguePages.size()) {
            return;
        }

        // 数字键选择选项
        List<String> opts = activeDialogue.getOptionTexts();
        if (opts == null || opts.isEmpty()) {
            return;
        }
        for (int i = 0; i < Math.min(9, opts.size()); i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                activeDialogue = engine.chooseDialogueOption(i);
                prepareDialoguePages(activeDialogue);
                actionMessage = formatDialogue(activeDialogue);
                return;
            }
        }
    }

    private void handleCombatInput()
    {
        if (!engine.isInCombat()) {
            activeCombatSnapshot = null;
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            activeCombatSnapshot = engine.combatAction(CombatAction.ATTACK, null);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            activeCombatSnapshot = engine.combatAction(CombatAction.DEFEND, null);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            activeCombatSnapshot = engine.combatAction(CombatAction.FLEE, null);
        } else {
            // NUM_4..NUM_9 => use bag item index 0..5
            List<Item> inv = engine.getPlayer().getInventory();
            for (int itemIdx = 0; itemIdx < Math.min(6, inv.size()); itemIdx++) {
                int key = Input.Keys.NUM_4 + itemIdx;
                if (Gdx.input.isKeyJustPressed(key)) {
                    activeCombatSnapshot =
                        engine.combatAction(CombatAction.USE_ITEM, inv.get(itemIdx).getItemId());
                    break;
                }
            }
        }

        actionMessage = formatCombatSnapshot(activeCombatSnapshot);
        if (!engine.isInCombat()) {
            activeCombatSnapshot = null;
        }
    }

    private UndertaleCombatEngine utEngine()
    {
        CombatSystem cs = engine.getCombatSystem();
        return (cs instanceof UndertaleCombatEngine) ? (UndertaleCombatEngine) cs : null;
    }

    private void handleUtCombatInput(float delta)
    {
        UndertaleCombatEngine ut = utEngine();
        if (ut == null) {
            activeCombatSnapshot = null;
            return;
        }

        // 每帧推进
        ut.updateFightBar(delta);
        ut.updateEnemyTurn(delta);

        UndertaleCombatPhase phase = ut.getPhase();

        if (phase == UndertaleCombatPhase.MENU) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                ut.selectFight();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                // ACT: 显示可用 ACT 选项（简化：只用第一个）
                if (!ut.getDef().actOptions.isEmpty()) {
                    String actId = ut.getDef().actOptions.keySet().iterator().next();
                    ut.selectAct(actId);
                }
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
                // ITEM: 用背包第一件可战斗物品
                for (Item it : engine.getPlayer().getInventory()) {
                    ut.selectItem(it.getItemId());
                    break;
                }
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
                ut.selectMercy();
            }
        } else if (phase == UndertaleCombatPhase.FIGHT_BAR) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                ut.pressFightBar();
            }
        } else if (phase == UndertaleCombatPhase.ENEMY_TURN) {
            float dx = 0f, dy = 0f;
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) dy = 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy = -1f;
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx = -1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx = 1f;
            ut.moveSoul(dx, dy);
        }

        activeCombatSnapshot = ut.snapshot();
        actionMessage = formatUtCombat(ut);

        if (ut.getOutcome() != CombatOutcome.ONGOING) {
            engine.applyCombatOutcome();
            activeCombatSnapshot = null;
        }
    }

    private String formatUtCombat(UndertaleCombatEngine ut)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[UT] ").append(ut.getDef().displayName)
            .append(" HP:").append(ut.snapshot().npcHp).append("/").append(ut.getDef().maxHp)
            .append(" | 你 HP:").append(ut.snapshot().playerHp).append("/").append(engine.getPlayer().getMaxHp())
            .append("\n");
        sb.append(ut.getPhaseMessage()).append("\n");

        UndertaleCombatPhase phase = ut.getPhase();
        if (phase == UndertaleCombatPhase.MENU) {
            sb.append("1=FIGHT 2=ACT 3=ITEM 4=MERCY");
        } else if (phase == UndertaleCombatPhase.FIGHT_BAR) {
            sb.append("ENTER/Space=攻击! [");
            int pos = (int)(ut.getFightBarPos() * 20);
            for (int i = 0; i < 20; i++)
                sb.append(i == 10 ? "|" : i == pos ? "▌" : "·");
            sb.append("]");
        } else if (phase == UndertaleCombatPhase.ENEMY_TURN) {
            sb.append("WASD=躲避! 子弹:").append(ut.getBullets().size())
                .append(" [").append(ut.getSoulX()).append(",").append(ut.getSoulY()).append("]");
        }
        return sb.toString();
    }

    // ========== UT 战斗画中画渲染 ==========

    private void drawUtCombatOverlay()
    {
        UndertaleCombatEngine ut = utEngine();
        if (ut == null) return;

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // 暗色遮罩
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(uiCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(0, 0, sw, sh);
        shapes.end();

        // 战斗框
        float boxW = sw * 0.64f;
        float boxH = sh * 0.56f;
        float boxX = (sw - boxW) / 2f;
        float boxY = (sh - boxH) / 2f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.02f, 0.05f, 0.92f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(1f, 1f, 1f, 0.9f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();

        UndertaleCombatPhase phase = ut.getPhase();

        // 弹幕 + 灵魂
        if (phase == UndertaleCombatPhase.ENEMY_TURN) {
            drawUtBullets(ut, boxX, boxY, boxW, boxH);
        }
        drawUtSoul(ut, boxX, boxY, boxW, boxH);

        // 节奏条
        if (phase == UndertaleCombatPhase.FIGHT_BAR) {
            drawUtFightBar(ut, boxX, boxY, boxW, boxH);
        }

        // HP 条
        batch.begin();
        float topY = boxY + boxH + 8f;
        font.setColor(Color.WHITE);
        font.draw(batch, ut.getDef().displayName, boxX, topY);
        drawHpBar(boxX + sw * 0.28f, topY - 14f, sw * 0.18f, 10f,
            (float) ut.snapshot().npcHp / ut.getDef().maxHp, true);

        font.draw(batch, engine.getPlayer().getName(), boxX + boxW - sw * 0.22f, topY);
        drawHpBar(boxX + boxW - sw * 0.22f + sw * 0.06f, topY - 14f, sw * 0.16f, 10f,
            (float) ut.snapshot().playerHp / engine.getPlayer().getMaxHp(), false);
        batch.end();

        // 菜单 / 提示
        batch.begin();
        float bottomY = boxY - 12f;
        smallFont.setColor(Color.WHITE);
        if (phase == UndertaleCombatPhase.MENU) {
            float btnW = 110f;
            float btnH = 28f;
            String[] labels = {"1 FIGHT", "2 ACT", "3 ITEM", "4 MERCY"};
            // 先画按钮背景（shapes）
            for (int i = 0; i < 4; i++) {
                float bx = boxX + i * (btnW + 6f);
                drawUtButtonBg(bx, boxY - btnH - 6f, btnW, btnH);
            }
            // 再画文字（batch 已 begin）
            for (int i = 0; i < 4; i++) {
                float bx = boxX + i * (btnW + 6f);
                smallFont.draw(batch, labels[i], bx + 8f, boxY - btnH - 6f + btnH - 8f);
            }
        } else {
            smallFont.draw(batch, ut.getPhaseMessage(), boxX, bottomY);
        }
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawUtSoul(UndertaleCombatEngine ut,
        float boxX, float boxY, float boxW, float boxH)
    {
        float sx = boxX + ut.getSoulX() * boxW;
        float sy = boxY + ut.getSoulY() * boxH;
        float sr = 7f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0.15f, 0.15f, 1f);
        shapes.circle(sx, sy, sr, 14);
        shapes.setColor(1f, 0.35f, 0.35f, 0.4f);
        shapes.circle(sx, sy, sr + 2f, 14);
        shapes.end();
    }

    private void drawUtBullets(UndertaleCombatEngine ut,
        float boxX, float boxY, float boxW, float boxH)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Bullet b : ut.getBullets()) {
            if (!b.alive) continue;
            float bx = boxX + b.x * boxW;
            float by = boxY + b.y * boxH;
            if (b.shape == Bullet.Shape.CIRCLE) {
                float br = b.radius * boxW * 0.8f;
                shapes.setColor(1f, 1f, 0.85f, 1f);
                shapes.circle(bx, by, Math.max(br, 3f), 8);
            } else {
                float bw = b.width * boxW;
                float bh = b.height * boxH;
                shapes.setColor(1f, 1f, 0.85f, 1f);
                shapes.rect(bx, by, Math.max(bw, 4f), Math.max(bh, 4f));
            }
        }
        shapes.end();
    }

    private void drawUtFightBar(UndertaleCombatEngine ut,
        float boxX, float boxY, float boxW, float boxH)
    {
        float barW = boxW * 0.5f;
        float barH = 10f;
        float barX = boxX + (boxW - barW) / 2f;
        float barY = boxY + boxH * 0.72f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.12f, 0.12f, 0.12f, 1f);
        shapes.rect(barX, barY, barW, barH);
        shapes.setColor(0.05f, 0.5f, 0.05f, 0.45f);
        shapes.rect(barX + barW * 0.4f, barY, barW * 0.2f, barH);

        float dotX = barX + ut.getFightBarPos() * barW - 3f;
        shapes.setColor(1f, 0.85f, 0.2f, 1f);
        shapes.rect(dotX, barY - 1f, 6f, barH + 2f);
        shapes.end();
    }

    private void drawUtButtonBg(float x, float y, float w, float h)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.12f, 0.12f, 0.18f, 0.9f);
        shapes.rect(x, y, w, h);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(1f, 0.55f, 0.1f, 0.65f);
        shapes.rect(x, y, w, h);
        shapes.end();
    }

    private void drawHpBar(float x, float y, float w, float h,
        float ratio, boolean enemy)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.12f, 0.12f, 0.12f, 1f);
        shapes.rect(x, y, w, h);
        if (ratio > 0f) {
            shapes.setColor(enemy ? 0.95f : 0.15f, enemy ? 0.2f : 0.75f, enemy ? 0.1f : 0.15f, 1f);
            shapes.rect(x, y, w * Math.min(ratio, 1f), h);
        }
        shapes.end();
    }

    private String formatDialogue(Dialogue d)
    {
        if (d == null) {
            return "对话结束。";
        }
        StringBuilder sb = new StringBuilder();
        if (dialoguePages.isEmpty()) {
            prepareDialoguePages(d);
        }
        sb.append(dialoguePages.get(Math.min(dialoguePageIndex, dialoguePages.size() - 1)));

        if (d.isActive() && d.getOptionTexts() != null && !d.getOptionTexts().isEmpty()) {
            if (dialoguePageIndex + 1 >= dialoguePages.size()) {
                sb.append("\n（Enter 继续 / 选 1-9）\n");
                for (int i = 0; i < d.getOptionTexts().size() && i < 9; i++) {
                    sb.append(i + 1).append(". ").append(d.getOptionTexts().get(i)).append(" ");
                }
            } else {
                sb.append("\n（Enter 继续）");
            }
        } else {
            if (dialoguePageIndex + 1 >= dialoguePages.size()) {
                sb.append("\n（Enter 退出）");
            } else {
                sb.append("\n（Enter 继续）");
            }
        }
        return sb.toString();
    }

    private void prepareDialoguePages(Dialogue d)
    {
        dialoguePages.clear();
        dialoguePageIndex = 0;
        if (d == null || d.getText() == null || d.getText().trim().isEmpty()) {
            dialoguePages.add("");
            return;
        }
        // 以空行分页：JSON 里写 \\n\\n
        String[] parts = d.getText().split("\\n\\n");
        for (String p : parts) {
            String page = p.trim();
            if (!page.isEmpty()) {
                dialoguePages.add(page);
            }
        }
        if (dialoguePages.isEmpty()) {
            dialoguePages.add(d.getText());
        }
    }

    private void drawNpcDialogueBubble()
    {
        if (activeDialogue == null) {
            return;
        }
        Rectangle npcBounds = findNpcBounds(activeDialogue.getNpcId());
        if (npcBounds == null) {
            return;
        }
        String text = formatDialogue(activeDialogue);
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        float padding = 6f;
        float bubbleW = 300f;
        float x = npcBounds.x + npcBounds.width / 2f - bubbleW / 2f;
        float y = npcBounds.y + npcBounds.height + 18f;

        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        layout.setText(smallFont, text, Color.WHITE, bubbleW - padding * 2f, Align.left, true);
        float bubbleH = layout.height + padding * 2f + 10f;
        batch.end();

        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(x, y, bubbleW, bubbleH);
        shapes.end();

        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        smallFont.setColor(Color.WHITE);
        smallFont.draw(batch, text, x + padding, y + bubbleH - padding);
        batch.end();
    }

    private Rectangle findNpcBounds(String npcId)
    {
        if (npcId == null) {
            return null;
        }
        for (NpcPlaceholder npc : npcPlaceholders) {
            if (npcId.equals(npc.npcId)) {
                return npc.bounds;
            }
        }
        return null;
    }

    private String formatCombatSnapshot(CombatSnapshot snap)
    {
        if (snap == null) {
            return "战斗尚未开始。";
        }
        String last = snap.logLines.isEmpty() ? "" : snap.logLines.get(snap.logLines.size() - 1);
        StringBuilder sb = new StringBuilder();
        sb.append("守卫 ").append(snap.npcHp).append("/").append(snap.npcMaxHp)
            .append(" | 你 ").append(snap.playerHp).append("/").append(snap.playerMaxHp)
            .append("\n");
        sb.append(last);
        if (snap.outcome != null && snap.outcome != cn.edu.whut.sept.zuul.engine.CombatOutcome.ONGOING) {
            sb.append("\n结果：").append(snap.outcome);
        } else {
            sb.append("\n战斗操作：1攻 2防 3逃 4-9用物品");
        }
        return sb.toString();
    }

    private void handleInventoryBrowseInput()
    {
        List<Item> items = engine.getPlayer().getInventory();
        if (items.isEmpty()) {
            inventoryInspectIndex = 0;
            inventoryScrollOffset = 0;
            inventoryInspectMode = false;
            return;
        }
        if (inventoryInspectIndex >= items.size()) {
            inventoryInspectIndex = items.size() - 1;
        }
        if (inventoryInspectIndex < 0) {
            inventoryInspectIndex = 0;
        }
        keepInventorySelectionVisible();

        if (!inventoryInspectMode) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                inventoryInspectIndex = (inventoryInspectIndex - 1 + items.size()) % items.size();
                keepInventorySelectionVisible();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                inventoryInspectIndex = (inventoryInspectIndex + 1) % items.size();
                keepInventorySelectionVisible();
            }
            for (int i = 0; i < Math.min(9, items.size()); i++) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                    inventoryInspectIndex = i;
                    keepInventorySelectionVisible();
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
                inventoryInspectMode = true;
                actionMessage = "查看物品详情（Enter 返回）";
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                tryEatSelectedItem();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.U)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                tryUseSelectedItem();
            }
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                inventoryInspectMode = false;
                actionMessage = "已返回背包列表";
            }
        }
    }

    private void tryUseSelectedItem()
    {
        List<Item> items = engine.getPlayer().getInventory();
        if (items.isEmpty()) {
            inventoryOpen = false;
            inventoryInspectMode = false;
            actionMessage = "背包是空的";
            return;
        }
        Item selected = items.get(inventoryInspectIndex);
        ItemUseCheck check = engine.checkItemUse(selected.getItemId());
        actionMessage = engine.tryUseItem(selected.getItemId());
        if (check.canUse) {
            inventoryInspectMode = false;
        }
        clampInventorySelection();
    }

    private void tryEatSelectedItem()
    {
        List<Item> items = engine.getPlayer().getInventory();
        if (items.isEmpty()) {
            inventoryOpen = false;
            inventoryInspectMode = false;
            actionMessage = "背包是空的";
            return;
        }
        Item selected = items.get(inventoryInspectIndex);
        engine.eatItem(selected.getItemId());
        if (selected.isMagicCookie()) {
            actionMessage = "吃下了 " + selected.getName() + "，负重上限 +20！";
        } else {
            actionMessage = selected.getName() + " 不可食用。";
        }
        clampInventorySelection();
    }

    private void clampInventorySelection()
    {
        int size = engine.getPlayer().getInventory().size();
        if (size == 0) {
            inventoryInspectIndex = 0;
            inventoryScrollOffset = 0;
            inventoryOpen = false;
            inventoryInspectMode = false;
        } else if (inventoryInspectIndex >= size) {
            inventoryInspectIndex = size - 1;
            keepInventorySelectionVisible();
        }
    }

    private void keepInventorySelectionVisible()
    {
        int size = engine.getPlayer().getInventory().size();
        if (size <= 0) {
            inventoryScrollOffset = 0;
            return;
        }
        int visibleRows = inventoryVisibleRows(inventoryPanelHeight());
        int maxOffset = Math.max(0, size - visibleRows);
        if (inventoryInspectIndex < inventoryScrollOffset) {
            inventoryScrollOffset = inventoryInspectIndex;
        } else if (inventoryInspectIndex >= inventoryScrollOffset + visibleRows) {
            inventoryScrollOffset = inventoryInspectIndex - visibleRows + 1;
        }
        inventoryScrollOffset = Math.max(0, Math.min(inventoryScrollOffset, maxOffset));
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
        if (isAttacking) {
            isMovingLastFrame = false;
            return;
        }

        if (isDashing) {
            // 冲刺模式：沿朝向高速移动，穿NPC，不改变朝向
            float dx = 0f;
            float dy = 0f;
            switch (currentFacing) {
                case "north": dy = SPEED * DASH_SPEED_MULTIPLIER * delta; break;
                case "south": dy = -SPEED * DASH_SPEED_MULTIPLIER * delta; break;
                case "west":  dx = -SPEED * DASH_SPEED_MULTIPLIER * delta; break;
                case "east":  dx = SPEED * DASH_SPEED_MULTIPLIER * delta; break;
            }
            isMovingLastFrame = true;
            if (dx != 0f && canMoveDuringDash(playerX + dx, playerY)) {
                playerX += dx;
            }
            if (dy != 0f && canMoveDuringDash(playerX, playerY + dy)) {
                playerY += dy;
            }
            clampPlayerToMap();
            return;
        }

        // 正常模式
        float dx = 0f;
        float dy = 0f;
        boolean wDown = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean sDown = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean aDown = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean dDown = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        boolean moving = wDown || sDown || aDown || dDown;
        if (moving) {
            if (wDown && !sDown) currentFacing = "north";
            else if (sDown && !wDown) currentFacing = "south";
            else if (aDown && !dDown) currentFacing = "west";
            else if (dDown && !aDown) currentFacing = "east";
        }
        isMovingLastFrame = moving;

        if (wDown) dy = SPEED * delta;
        if (sDown) dy = -SPEED * delta;
        if (aDown) dx = -SPEED * delta;
        if (dDown) dx = SPEED * delta;

        if ((wDown || sDown || aDown || dDown) && moveLogFrame % 30 == 0) {
            String dir = "";
            if (wDown) dir = "W/UP";
            if (sDown) dir = "S/DOWN";
            if (aDown) dir = "A/LEFT";
            if (dDown) dir = "D/RIGHT";
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
        if (collidesNpc(newX, newY)) {
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

    private boolean canMoveDuringDash(float newX, float newY)
    {
        if (newX < 0f || newY < 0f
            || newX + PLAYER_W > mapPixelWidth()
            || newY + PLAYER_H > mapPixelHeight()) {
            return false;
        }
        // 冲刺可穿过 NPC，不调用 collidesNpc
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

    private boolean collidesNpc(float newX, float newY)
    {
        if (npcPlaceholders.isEmpty()) {
            return false;
        }
        Rectangle playerRect = new Rectangle(newX, newY, PLAYER_W, PLAYER_H);
        for (NpcPlaceholder npc : npcPlaceholders) {
            if (npc.bounds.overlaps(playerRect)) {
                return true;
            }
        }
        return false;
    }

    private static final class NpcPlaceholder
    {
        final String npcId;
        final Rectangle bounds;
        final Color color;

        private NpcPlaceholder(String npcId, Rectangle bounds, Color color)
        {
            this.npcId = npcId;
            this.bounds = bounds;
            this.color = color;
        }

        static NpcPlaceholder guard(Rectangle bounds)
        {
            return new NpcPlaceholder("guard", bounds,
                new Color(0.85f, 0.2f, 0.2f, 1f));
        }

        static NpcPlaceholder hermit(Rectangle bounds)
        {
            return new NpcPlaceholder("hermit", bounds,
                new Color(0.2f, 0.8f, 0.35f, 1f));
        }

        static NpcPlaceholder merchant(Rectangle bounds)
        {
            return new NpcPlaceholder("merchant", bounds,
                new Color(0.95f, 0.65f, 0.15f, 1f));
        }

        static NpcPlaceholder forNpc(String npcId, Rectangle bounds)
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

    // ======================== 道具占位块系统 ========================

    /** 道具出生点定义（tile 坐标，左上角原点）。 */
    private static final class ItemSpawnDef
    {
        final String itemId;
        final int tileX;
        final int tileY;
        final Color color;

        ItemSpawnDef(String itemId, int tileX, int tileY, Color color)
        {
            this.itemId = itemId;
            this.tileX = tileX;
            this.tileY = tileY;
            this.color = color;
        }
    }

    /** 地图上的道具占位块。 */
    private static final class ItemPlaceholder
    {
        final String itemId;
        final Rectangle bounds;
        final Color color;

        ItemPlaceholder(String itemId, Rectangle bounds, Color color)
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
        // magic-cookie 随机出现在 3 个候选房间之一，三处都定义，实际只渲染存在的那个
        addItemSpawn("cellar", "magic-cookie", 24, 12, new Color(1f, 0.5f, 0.7f, 1f));
        addItemSpawn("library", "magic-cookie", 24, 12, new Color(1f, 0.5f, 0.7f, 1f));
        addItemSpawn("hidden-shrine", "magic-cookie", 22, 11, new Color(1f, 0.5f, 0.7f, 1f));
    }

    private static void addItemSpawn(String roomId, String itemId, int tileX, int tileY, Color color)
    {
        ITEM_SPAWNS.computeIfAbsent(roomId, k -> new ArrayList<>())
            .add(new ItemSpawnDef(itemId, tileX, tileY, color));
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
        if (playerRenderer == null) return;
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        playerRenderer.render(batch, playerX, playerY);
        batch.end();
    }

    private void drawInteractionPrompt()
    {
        if (paused || inventoryOpen || encounterMenuOpen || activeDialogue != null
            || engine.isInDialogue() || engine.isInCombat()) {
            return;
        }

        InteractionPrompt prompt = currentInteractionPrompt();
        if (prompt == null) {
            return;
        }

        float promptWidth = Math.max(88f, prompt.text.length() * 14f + 42f);
        float promptHeight = 28f;
        float x = Math.max(8f, Math.min(playerX + PLAYER_W / 2f - promptWidth / 2f,
            mapPixelWidth() - promptWidth - 8f));
        float y = Math.min(playerY + PLAYER_H + 14f, mapPixelHeight() - promptHeight - 8f);

        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        uiSkin.drawLightButton(batch, x, y, promptWidth, promptHeight);
        drawIcon(prompt.icon, x + 8f, y + 6f, 16f);
        smallFont.setColor(UI_DARK_TEXT);
        drawClampedLine(smallFont, prompt.text, x + 30f, y + 20f, promptWidth - 38f);
        batch.end();
    }

    private InteractionPrompt currentInteractionPrompt()
    {
        if (findNearbyNpcId() != null) {
            return new InteractionPrompt("E 交谈", ICON_LOOK);
        }
        String itemId = findNearbyItemId();
        if (itemId != null) {
            for (Item item : engine.getCurrentRoom().getItems()) {
                if (item.getItemId().equals(itemId)) {
                    return new InteractionPrompt("E 拾取 " + item.getName(), ICON_TAKE);
                }
            }
        }
        String exitName = nearbyExitTarget();
        if (exitName != null) {
            return new InteractionPrompt("进入 " + exitName, ICON_ROOM);
        }
        return null;
    }

    private String nearbyExitTarget()
    {
        if (objectsLayer == null) {
            return null;
        }
        Rectangle playerRect = new Rectangle(playerX - 16f, playerY - 16f,
            PLAYER_W + 32f, PLAYER_H + 32f);
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
            Rectangle exitRect = new Rectangle(objectX, objectY, objectWidth, objectHeight);
            if (playerRect.overlaps(exitRect)) {
                String targetRoomId = props.get("targetRoomId", String.class);
                return targetRoomId == null || targetRoomId.trim().isEmpty() ? "出口" : targetRoomId;
            }
        }
        return null;
    }

    private static final class InteractionPrompt
    {
        final String text;
        final int icon;

        InteractionPrompt(String text, int icon)
        {
            this.text = text;
            this.icon = icon;
        }
    }

    private void drawWorldMap(float delta)
    {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float panelW = Math.min(520f, width - 48f);
        float panelH = Math.min(420f, height - 88f);
        float panelX = (width - panelW) / 2f;
        float panelY = (height - panelH) / 2f;

        applyFullViewport();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(uiCamera.combined);

        worldMapRenderer.render(shapes, batch, smallFont,
            panelX, panelY, panelW, panelH,
            engine.getCurrentRoom().getRoomId(),
            engine.getExploredRoomIds(),
            engine::isLockUnlocked,
            delta);
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

    private void saveGame()
    {
        try {
            GameState state = engine.captureState();
            state.setPlayerX(playerX);
            state.setPlayerY(playerY);
            state.setFacing(currentFacing);
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
                state.getPlayerX(), state.getPlayerY(), "已读取存档", state.getFacing()));
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
        uiSkin.drawWindow(batch, UI_EDGE, height - TOP_BAR_HEIGHT - UI_EDGE,
            width - UI_EDGE * 2f, TOP_BAR_HEIGHT);
        uiSkin.drawWindow(batch, UI_EDGE, UI_EDGE, width - UI_EDGE * 2f, FOOTER_HEIGHT);
        uiSkin.drawInset(batch, UI_EDGE + 20f, UI_EDGE + 56f,
            width - (UI_EDGE + 20f) * 2f, 28f);
        drawSidePanels();

        if (paused) {
            float panelWidth = grid(Math.min(672f, width - 72f));
            float panelHeight = grid(Math.min(384f, height - 88f));
            float panelX = grid((width - panelWidth) / 2f);
            float panelY = grid((height - panelHeight) / 2f);
            uiSkin.drawWindow(batch, panelX, panelY, panelWidth, panelHeight);
            uiSkin.drawInset(batch, panelX + 32f, panelY + 72f,
                panelWidth - 64f, panelHeight - 144f);
            uiSkin.drawButton(batch, panelX + panelWidth / 2f - 96f,
                panelY + 24f, 192f, 40f);
        }
        if (inventoryOpen) {
            float panelWidth = grid(Math.min(520f, width - 96f));
            float panelHeight = grid(inventoryPanelHeight());
            float panelX = grid((width - panelWidth) / 2f);
            float panelY = grid(Math.max(WORLD_MARGIN_BOTTOM + 16f,
                height - WORLD_MARGIN_TOP - panelHeight - 16f));
            uiSkin.drawWindow(batch, panelX, panelY, panelWidth, panelHeight);
            uiSkin.drawInset(batch, panelX + 24f, panelY + 48f,
                panelWidth - 48f, panelHeight - 84f);
        }
    }

    private void drawHud()
    {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float contentX = UI_EDGE + 20f;
        float contentRight = width - UI_EDGE - 20f;
        float topY = height - 24f;
        float barY = height - 58f;
        float barWidth = grid(Math.max(72f, Math.min(176f, (width - 256f) / 2f)));
        float hpX = contentX;
        float weightX = grid(Math.max(width / 2f + 16f, hpX + barWidth + 120f));

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
        drawClampedLine(smallFont, "日志", UI_EDGE + 36f, 84f, 40f);
        drawMultilineClamped(smallFont, actionMessage, UI_EDGE + 80f, 84f,
            Gdx.graphics.getWidth() - 128f, 16f, 2);

        float gap = UI_CHIP_GAP;
        float totalWidth = 80f + 56f * 3f + 72f + gap * 4f;
        float x = grid(Math.max(20f, (Gdx.graphics.getWidth() - totalWidth) / 2f));
        float y = 16f;
        x = drawHintChip("WASD", x, y, 80f, UI_CHIP_HEIGHT, ICON_MOVE, 40f) + gap;
        x = drawHintChip("E", x, y, 56f, UI_CHIP_HEIGHT, ICON_TAKE, 28f) + gap;
        x = drawHintChip("Q", x, y, 56f, UI_CHIP_HEIGHT, ICON_LOOK, 28f) + gap;
        x = drawHintChip("I", x, y, 56f, UI_CHIP_HEIGHT, ICON_INVENTORY, 28f) + gap;
        drawHintChip("ESC", x, y, 72f, UI_CHIP_HEIGHT, ICON_MENU, 40f);
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
        float leftX = UI_EDGE;
        float leftWidth = grid(worldViewportX - UI_EDGE * 2f);
        float rightX = grid(worldViewportX + worldViewportWidth + UI_EDGE);
        float rightWidth = grid(Gdx.graphics.getWidth() - rightX - UI_EDGE);

        uiSkin.drawWindow(batch, leftX, panelY, leftWidth, panelHeight);
        uiSkin.drawInset(batch, leftX + UI_INSET, panelY + 24f,
            leftWidth - UI_INSET * 2f, panelHeight - 56f);
        uiSkin.drawWindow(batch, rightX, panelY, rightWidth, panelHeight);
        uiSkin.drawInset(batch, rightX + UI_INSET, panelY + 24f,
            rightWidth - UI_INSET * 2f, panelHeight - 56f);
    }

    private void drawSidePanelContent()
    {
        if (!hasSidePanels()) {
            return;
        }
        float panelY = WORLD_MARGIN_BOTTOM;
        float panelHeight = Gdx.graphics.getHeight() - WORLD_MARGIN_TOP - WORLD_MARGIN_BOTTOM;
        float leftX = UI_EDGE;
        float leftWidth = grid(worldViewportX - UI_EDGE * 2f);
        float rightX = grid(worldViewportX + worldViewportWidth + UI_EDGE);
        float rightWidth = grid(Gdx.graphics.getWidth() - rightX - UI_EDGE);
        float textInset = UI_PAD + 4f;

        float leftTextX = leftX + textInset;
        float leftTextWidth = leftWidth - textInset * 2f;
        drawPanelHeader("角色档案", ICON_TITLE, leftX + UI_PAD,
            panelY + panelHeight - 48f, leftWidth - UI_PAD * 2f);

        smallFont.setColor(UI_DARK_TEXT);
        drawIcon(ICON_ROOM, leftTextX, panelY + panelHeight - 80f, 18f);
        drawClampedLine(smallFont, "房间 " + engine.getCurrentRoom().getRoomId(),
            leftTextX + 26f, panelY + panelHeight - 64f, leftTextWidth - 26f);
        drawCompactStatusBar("生命", engine.getPlayer().getHp(), engine.getPlayer().getMaxHp(),
            leftTextX, panelY + panelHeight - 120f, leftTextWidth, true);
        drawCompactStatusBar("负重", engine.getPlayer().totalWeight(), engine.getPlayer().getMaxWeight(),
            leftTextX, panelY + panelHeight - 176f, leftTextWidth, false);
        drawClampedLine(smallFont, "声望 " + engine.getPlayer().getReputation(),
            leftTextX, panelY + panelHeight - 224f, leftTextWidth);

        float rightTextX = rightX + textInset;
        float rightTextWidth = rightWidth - textInset * 2f;
        drawPanelHeader("当前事件", ICON_LOOK, rightX + UI_PAD,
            panelY + panelHeight - 48f, rightWidth - UI_PAD * 2f);
        smallFont.setColor(UI_DARK_TEXT);
        drawMultilineClamped(smallFont, actionMessage, rightTextX, panelY + panelHeight - 72f,
            rightTextWidth, 18f, 5);

        float hintY = grid(Math.max(panelY + 48f, panelY + Math.min(160f, panelHeight - 184f)));
        drawPanelHeader("快捷键", ICON_MENU, rightX + UI_PAD, hintY + 96f,
            rightWidth - UI_PAD * 2f);
        drawSideShortcut("WASD", "移动", ICON_MOVE, rightTextX, hintY + 64f, rightTextWidth);
        drawSideShortcut("E", "拾取/交谈", ICON_TAKE, rightTextX, hintY + 38f, rightTextWidth);
        drawSideShortcut("I", "背包", ICON_INVENTORY, rightTextX, hintY + 12f, rightTextWidth);
        drawSideShortcut("U", "使用物品", ICON_USE, rightTextX, hintY - 14f, rightTextWidth);
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
        return SIDE_PANELS_ENABLED
            && worldViewportX - 20f >= SIDE_PANEL_MIN_WIDTH
            && Gdx.graphics.getWidth() - (worldViewportX + worldViewportWidth) - 20f >= SIDE_PANEL_MIN_WIDTH;
    }

    private void drawPauseMenu()
    {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float panelWidth = grid(Math.min(672f, width - 72f));
        float panelHeight = grid(Math.min(384f, height - 88f));
        float panelX = grid((width - panelWidth) / 2f);
        float panelY = grid((height - panelHeight) / 2f);
        float centerX = panelX + panelWidth / 2f;

        font.setColor(UI_LIGHT_TEXT);
        drawCentered("暂停菜单", centerX, panelY + panelHeight - 36);
        smallFont.setColor(UI_LIGHT_TEXT);
        drawCenteredWithSmallFont("按键说明", centerX, panelY + panelHeight - 64);

        float leftX = panelX + 48f;
        float rightX = panelX + panelWidth / 2f + 24f;
        float rowY = panelY + panelHeight - 104f;
        float rowGap = 32f;
        drawShortcutRow("ESC", "继续探索 / 打开菜单", ICON_MENU, leftX, rowY);
        drawShortcutRow("WASD", "移动角色", ICON_MOVE, leftX, rowY - rowGap);
        drawShortcutRow("出口", "走入出口切换房间", ICON_ROOM, leftX, rowY - rowGap * 2f);
        drawShortcutRow("Q", "调查当前房间", ICON_LOOK, leftX, rowY - rowGap * 3f);
        drawShortcutRow("B", "回退上一个房间", ICON_BACK, leftX, rowY - rowGap * 4f);
        drawShortcutRow("T", "返回标题画面", ICON_TITLE, leftX, rowY - rowGap * 5f);

        drawShortcutRow("E", "拾取地面物品 / 与 NPC 互动", ICON_TAKE, rightX, rowY);
        drawShortcutRow("I", "打开 / 关闭背包", ICON_INVENTORY, rightX, rowY - rowGap);
        drawShortcutRow("U", "使用背包中选中的物品", ICON_USE, rightX, rowY - rowGap * 2f);
        drawShortcutRow("F5", "保存当前进度", ICON_SAVE, rightX, rowY - rowGap * 3f);
        drawShortcutRow("F9", "读取存档", ICON_LOAD, rightX, rowY - rowGap * 4f);

        smallFont.setColor(UI_DARK_TEXT);
        drawCenteredInBoxWithSmallFont("ESC 继续", centerX - 96f, panelY + 24f, 192f, 40f);
    }

    private void drawPanelHeader(String title, int icon, float x, float y, float width)
    {
        if (width <= 42f) {
            return;
        }
        uiSkin.drawButton(batch, grid(x), grid(y), grid(width), UI_HEADER_HEIGHT);
        drawIcon(icon, x + 8f, y + 8f, 16f);
        smallFont.setColor(UI_LIGHT_TEXT);
        drawClampedLine(smallFont, title, x + 32f, y + 22f, width - 40f);
    }

    private void drawSideShortcut(String key, String label, int icon, float x, float y, float width)
    {
        y = grid(y);
        drawIcon(icon, x, y + 4f, 16f);
        uiSkin.drawButton(batch, x + 24f, y, 40f, 24f);
        smallFont.setColor(Color.WHITE);
        drawCenteredInBoxWithSmallFont(key, x + 24f, y, 40f, 24f);
        smallFont.setColor(UI_DARK_TEXT);
        drawClampedLine(smallFont, label, x + 72f, y + 18f, Math.max(24f, width - 72f));
    }

    private float inventoryPanelHeight()
    {
        float availableHeight = Math.max(120f, worldViewportHeight - 24f);
        if (inventoryInspectMode) {
            return Math.min(296f, availableHeight);
        }
        int rows = Math.max(3, Math.min(7, engine.getPlayer().getInventory().size()));
        return Math.min(Math.max(264f, 152f + rows * 30f), availableHeight);
    }

    private int inventoryVisibleRows(float panelHeight)
    {
        return Math.max(1, (int)((panelHeight - 156f) / 30f));
    }

    private void drawInventoryPanel()
    {
        float width = Gdx.graphics.getWidth();
        float panelWidth = grid(Math.min(520f, width - 96f));
        float panelHeight = grid(inventoryPanelHeight());
        float panelX = grid((width - panelWidth) / 2f);
        float panelY = grid(Math.max(WORLD_MARGIN_BOTTOM + 16f,
            Gdx.graphics.getHeight() - WORLD_MARGIN_TOP - panelHeight - 16f));

        font.setColor(UI_LIGHT_TEXT);
        List<Item> items = engine.getPlayer().getInventory();
        if (inventoryInspectIndex >= items.size()) {
            inventoryInspectIndex = Math.max(0, items.size() - 1);
        }

        font.draw(batch, inventoryInspectMode ? "物品详情" : "背包", panelX + 24,
            panelY + panelHeight - 26);
        font.setColor(UI_DARK_TEXT);

        float innerX = panelX + 32f;
        float topY = panelY + panelHeight - 56f;

        if (!inventoryInspectMode) {
            smallFont.setColor(UI_DARK_TEXT);
            uiSkin.drawLightButton(batch, innerX, topY - 20f, panelWidth - 64f, 28f);
            smallFont.setColor(UI_DARK_TEXT);
            drawClampedLine(smallFont, "↑↓选择  U/Enter使用  X详情  Esc关闭",
                innerX + 10f, topY - 1f, panelWidth - 84f);

            float rowY = topY - 60f;
            float rowH = 30f;
            float rowW = panelWidth - 64f;
            int visibleRows = inventoryVisibleRows(panelHeight);
            keepInventorySelectionVisible();
            int lastVisible = Math.min(items.size(), inventoryScrollOffset + visibleRows);
            for (int i = inventoryScrollOffset; i < lastVisible; i++) {
                float y = rowY - (i - inventoryScrollOffset) * rowH;
                Item it = items.get(i);
                ItemUseCheck check = engine.checkItemUse(it.getItemId());
                String status = itemUseStatus(check);
                if (i == inventoryInspectIndex) {
                    uiSkin.drawButton(batch, innerX, y, rowW, rowH);
                }
                layout.setText(smallFont, status);
                float statusWidth = layout.width;
                String line = (i + 1) + ". " + it.getName() + " (" + it.getWeight() + ")";
                smallFont.setColor(i == inventoryInspectIndex ? Color.WHITE : UI_DARK_TEXT);
                drawClampedLine(smallFont, line, innerX + 10f, y + rowH - 9f,
                    Math.max(40f, rowW - statusWidth - 24f));
                smallFont.setColor(i == inventoryInspectIndex ? Color.WHITE : UI_DARK_TEXT);
                smallFont.draw(batch, status, innerX + rowW - statusWidth - 10f, y + rowH - 9f);
            }

            if (!items.isEmpty()) {
                Item picked = items.get(inventoryInspectIndex);
                ItemUseCheck check = engine.checkItemUse(picked.getItemId());
                smallFont.setColor(UI_DARK_TEXT);
                drawClampedLine(smallFont, "效果提示: " + check.hint, innerX,
                    panelY + 30f, rowW - 72f);
            } else {
                smallFont.setColor(UI_DARK_TEXT);
                smallFont.draw(batch, "背包为空", innerX, panelY + 40f);
            }
            if (items.size() > visibleRows) {
                smallFont.setColor(UI_DARK_TEXT);
                drawRightAligned((inventoryScrollOffset + 1) + "-" + lastVisible + "/"
                        + items.size(),
                    panelX + panelWidth - 32f, panelY + 30f);
            }

            return;
        }

        if (items.isEmpty()) {
            smallFont.setColor(UI_DARK_TEXT);
            smallFont.draw(batch, "背包为空", innerX, panelY + panelHeight - 70f);
            return;
        }

        Item item = items.get(inventoryInspectIndex);
        ItemUseCheck check = engine.checkItemUse(item.getItemId());

        font.setColor(UI_LIGHT_TEXT);
        smallFont.setColor(UI_DARK_TEXT);
        smallFont.draw(batch, "物品ID: " + item.getItemId(), innerX, topY);
        smallFont.draw(batch, "重量: " + item.getWeight(), innerX, topY - 18f);
        String effect = item.getEffect() == null ? "无" : item.getEffect();
        smallFont.draw(batch, "效果: " + effect, innerX, topY - 36f);

        // 描述
        smallFont.setColor(UI_DARK_TEXT);
        drawMultilineClamped(smallFont, "描述: " + item.getDescription(), innerX,
            topY - 58f, panelWidth - 56f, 18f, 4);

        // 可用性提示
        smallFont.setColor(UI_DARK_TEXT);
        drawClampedLine(smallFont, "使用条件: " + check.hint, innerX, panelY + 44f,
            panelWidth - 64f);
        smallFont.setColor(UI_DARK_TEXT);
        smallFont.draw(batch, "Enter返回  Esc关闭", innerX, panelY + 24f);
    }

    private String itemUseStatus(ItemUseCheck check)
    {
        if (check.canUse) {
            return "可用";
        }
        if (check.requiresLocation) {
            return "需位置";
        }
        return "不可用";
    }

    private float grid(float value)
    {
        return Math.round(value / UI_GRID) * UI_GRID;
    }

    private float drawHintChip(String key, float x, float y, float width, float height, int icon,
        float keyWidth)
    {
        x = grid(x);
        y = grid(y);
        width = grid(width);
        height = grid(height);
        keyWidth = grid(keyWidth);
        uiSkin.drawLightButton(batch, x, y, width, height);
        uiSkin.drawButton(batch, x + 8f, y + 8f, keyWidth, height - 16f);
        drawIcon(icon, x + keyWidth + 14f, y + 8f, height - 16f);

        smallFont.setColor(Color.WHITE);
        drawCenteredInBoxWithSmallFont(key, x + 8f, y + 8f, keyWidth, height - 16f);
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
        } else if (icon == ICON_USE) {
            uiSkin.drawUseIcon(batch, x, y, size);
        } else {
            uiSkin.drawCircleIcon(batch, x, y, size);
        }
    }

    private void drawCentered(String text, float centerX, float y)
    {
        layout.setText(font, text);
        font.draw(batch, text, Math.round(centerX - layout.width / 2f), Math.round(y));
    }

    private void drawCenteredWithSmallFont(String text, float centerX, float y)
    {
        layout.setText(smallFont, text);
        smallFont.draw(batch, text, Math.round(centerX - layout.width / 2f), Math.round(y));
    }

    private void drawCenteredInBoxWithSmallFont(String text, float x, float y, float width,
        float height)
    {
        layout.setText(smallFont, text);
        smallFont.draw(batch, text, Math.round(x + (width - layout.width) / 2f),
            Math.round(y + (height + layout.height) / 2f + 1f));
    }

    private void drawShortcutRow(String key, String label, int icon, float x, float y)
    {
        drawIcon(icon, x, y + 3, 24);
        float keyWidth = key.length() >= 4 ? 54f : key.length() >= 3 ? 42f : 34f;
        uiSkin.drawButton(batch, x + 34, y, keyWidth, 28);
        smallFont.setColor(Color.WHITE);
        drawCenteredInBoxWithSmallFont(key, x + 34, y, keyWidth, 28);
        smallFont.setColor(UI_DARK_TEXT);
        drawClampedLine(smallFont, label, x + keyWidth + 78, y + 20, 210f);
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
        if (playerRenderer != null) playerRenderer.dispose();
    }
}
