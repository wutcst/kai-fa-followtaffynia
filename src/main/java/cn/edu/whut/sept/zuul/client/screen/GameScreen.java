package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.RpgMain;
import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.domain.RoomScene;
import cn.edu.whut.sept.zuul.engine.CombatAction;
import cn.edu.whut.sept.zuul.engine.CombatSnapshot;
import cn.edu.whut.sept.zuul.engine.EncounterMenu;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.ItemUseCheck;
import cn.edu.whut.sept.zuul.infra.GameLogger;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.SaveGameService;

import java.util.ArrayList;
import java.util.List;
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
    private static final float PLAY_LEFT = 24f;
    private static final float PLAY_RIGHT_MARGIN = 24f;
    private static final float PLAY_BOTTOM = 116f;
    private static final float PLAY_TOP_MARGIN = 80f;
    private static final float TOP_BAR_HEIGHT = 64f;
    private static final float FOOTER_HEIGHT = 96f;
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
    private final List<NpcPlaceholder> npcPlaceholders;

    private float playerX;
    private float playerY;
    private String actionMessage;
    private String currentMapPath;
    private float exitCooldown;
    private boolean inventoryOpen;
    private boolean inventoryUseMode;
    private int inventorySelectedIndex;
    private boolean inventoryInspectMode;
    private int inventoryInspectIndex;
    private boolean encounterMenuOpen;
    private EncounterMenu encounterMenu;
    private Dialogue activeDialogue;
    private CombatSnapshot activeCombatSnapshot;
    private final List<String> dialoguePages;
    private int dialoguePageIndex;
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
        this.npcPlaceholders = new java.util.ArrayList<>();
        this.dialoguePages = new ArrayList<>();
        this.dialoguePageIndex = 0;
        this.inventoryInspectIndex = 0;

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

        worldCamera.update();
        mapRenderer.setView(worldCamera);
        mapRenderer.render();
        drawNpcPlaceholders();
        drawNpcDialogueBubble();
        drawPlayer();

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
            buildNpcPlaceholders(engine.getCurrentRoom().getRoomId());
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
            if (inventoryUseMode) {
                closeInventoryUseMode();
                return;
            }
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
        if (paused) {
            handlePauseInput();
            return;
        }
        if (inventoryUseMode) {
            handleInventoryUseInput();
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
            handleCombatInput();
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
            if (inventoryUseMode) {
                closeInventoryUseMode();
            }
            inventoryOpen = !inventoryOpen;
            if (!inventoryOpen) {
                inventoryUseMode = false;
                inventoryInspectMode = false;
            }
            actionMessage = inventoryOpen ? "背包已打开（仅查看）" : "背包已关闭";
        }

        if (inventoryOpen && !inventoryUseMode) {
            handleInventoryBrowseInput();
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
                actionMessage = tryTakeFirstItem();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            openInventoryUseMode();
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

    private void openInventoryUseMode()
    {
        if (engine.getPlayer().getInventory().isEmpty()) {
            actionMessage = "背包是空的";
            return;
        }
        inventoryOpen = true;
        inventoryUseMode = true;
        inventorySelectedIndex = 0;
        actionMessage = "选择要使用的物品（↑↓ / 数字键 选择，Enter 使用，Esc 关闭）";
    }

    private void closeInventoryUseMode()
    {
        inventoryUseMode = false;
        inventoryOpen = false;
        actionMessage = "已关闭物品使用";
    }

    private void handleInventoryUseInput()
    {
        List<Item> items = engine.getPlayer().getInventory();
        if (items.isEmpty()) {
            closeInventoryUseMode();
            actionMessage = "背包是空的";
            return;
        }
        if (inventorySelectedIndex >= items.size()) {
            inventorySelectedIndex = items.size() - 1;
        }
        if (inventorySelectedIndex < 0) {
            inventorySelectedIndex = 0;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            inventorySelectedIndex = (inventorySelectedIndex - 1 + items.size()) % items.size();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            inventorySelectedIndex = (inventorySelectedIndex + 1) % items.size();
        }
        for (int i = 0; i < Math.min(9, items.size()); i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                inventorySelectedIndex = i;
                tryUseSelectedItem();
                return;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            tryUseSelectedItem();
        }
    }

    private void handleInventoryBrowseInput()
    {
        List<Item> items = engine.getPlayer().getInventory();
        if (items.isEmpty()) {
            inventoryInspectIndex = 0;
            inventoryInspectMode = false;
            return;
        }
        if (inventoryInspectIndex >= items.size()) {
            inventoryInspectIndex = items.size() - 1;
        }
        if (inventoryInspectIndex < 0) {
            inventoryInspectIndex = 0;
        }

        if (!inventoryInspectMode) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                inventoryInspectIndex = (inventoryInspectIndex - 1 + items.size()) % items.size();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                inventoryInspectIndex = (inventoryInspectIndex + 1) % items.size();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
                inventoryInspectMode = true;
                actionMessage = "查看物品详情（Enter 返回）";
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
            closeInventoryUseMode();
            return;
        }
        Item selected = items.get(inventorySelectedIndex);
        ItemUseCheck check = engine.checkItemUse(selected.getItemId());
        actionMessage = engine.tryUseItem(selected.getItemId());
        if (check.canUse) {
            closeInventoryUseMode();
        }
        clampInventorySelection();
    }

    private void clampInventorySelection()
    {
        int size = engine.getPlayer().getInventory().size();
        if (size == 0) {
            inventorySelectedIndex = 0;
        } else if (inventorySelectedIndex >= size) {
            inventorySelectedIndex = size - 1;
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
        uiSkin.drawWindow(batch, 12, height - TOP_BAR_HEIGHT - 8, width - 24, TOP_BAR_HEIGHT);
        uiSkin.drawWindow(batch, 12, 8, width - 24, FOOTER_HEIGHT);
        uiSkin.drawInset(batch, 30, 64, width - 60, 28);

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
            float panelWidth = Math.min(400f, width - 56f);
            float panelHeight = inventoryPanelHeight();
            float panelX = Math.max(24f, width - panelWidth - 28f);
            float panelY = Math.max(playBottom() + 12f, playTop() - panelHeight - 12f);
            uiSkin.drawWindow(batch, panelX, panelY, panelWidth, panelHeight);
            float insetHeight = panelHeight - (inventoryUseMode ? 52f : 74f);
            uiSkin.drawInset(batch, panelX + 20, panelY + 42, panelWidth - 40, insetHeight);
        }
    }

    private void drawHud()
    {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        smallFont.setColor(UI_LIGHT_TEXT);
        smallFont.draw(batch, "玩家 " + engine.getPlayer().getName(), 30, height - 28);
        smallFont.draw(batch, "房间 " + engine.getCurrentRoom().getRoomId(), 250, height - 28);
        smallFont.draw(batch, "声望 " + engine.getPlayer().getReputation(), width - 132, height - 28);

        drawStatusBar("HP", engine.getPlayer().getHp(), engine.getPlayer().getMaxHp(),
            30, height - 58, 210, true);
        drawStatusBar("负重", engine.getPlayer().totalWeight(), engine.getPlayer().getMaxWeight(),
            360, height - 58, 210, false);

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
        float totalWidth = 78 + 82 + 60 + 60 + 60 + 60 + 60 + 66 + 66 + 72 + gap * 9f;
        float x = Math.max(20f, (Gdx.graphics.getWidth() - totalWidth) / 2f);
        float y = 15f;
        x = drawHintChip("WASD", x, y, 78, 42, ICON_MOVE, 42) + gap;
        x = drawHintChip("↑↓←→", x, y, 82, 42, ICON_MOVE, 46) + gap;
        x = drawHintChip("出口", x, y, 60, 42, ICON_ROOM, 28) + gap;
        x = drawHintChip("Q", x, y, 60, 42, ICON_LOOK, 28) + gap;
        x = drawHintChip("E", x, y, 60, 42, ICON_TAKE, 28) + gap;
        x = drawHintChip("U", x, y, 60, 42, ICON_INVENTORY, 28) + gap;
        x = drawHintChip("I", x, y, 60, 42, ICON_INVENTORY, 28) + gap;
        x = drawHintChip("F5", x, y, 66, 42, ICON_SAVE, 34) + gap;
        x = drawHintChip("F9", x, y, 66, 42, ICON_LOAD, 34) + gap;
        drawHintChip("ESC", x, y, 72, 42, ICON_MENU, 38);
    }

    private void drawStatusBar(String label, int value, int maxValue, float x, float y, float width,
        boolean hpBar)
    {
        smallFont.setColor(UI_LIGHT_TEXT);
        smallFont.draw(batch, label, x, y + 16);
        float barX = x + 54;
        float ratio = maxValue <= 0 ? 0f : (float) value / maxValue;
        if (hpBar) {
            uiSkin.drawRedBar(batch, barX, y, width, 18, ratio);
        } else {
            uiSkin.drawYellowBar(batch, barX, y, width, 18, ratio);
        }
        smallFont.draw(batch, value + " / " + maxValue, barX + width + 12, y + 16);
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
        float rowY = panelY + panelHeight - 116;
        float rowGap = 36f;
        drawShortcutRow("ESC", "继续探索 / 打开菜单", ICON_MENU, leftX, rowY);
        drawShortcutRow("WASD", "移动角色", ICON_MOVE, leftX, rowY - rowGap);
        drawShortcutRow("↑↓←→", "移动角色", ICON_MOVE, leftX, rowY - rowGap * 2f);
        drawShortcutRow("出口", "走入出口切换房间", ICON_ROOM, leftX, rowY - rowGap * 3f);
        drawShortcutRow("Q", "调查当前房间", ICON_LOOK, leftX, rowY - rowGap * 4f);

        drawShortcutRow("E", "拾取地面物品", ICON_TAKE, rightX, rowY);
        drawShortcutRow("U", "打开背包并选择物品使用", ICON_INVENTORY, rightX, rowY - rowGap);
        drawShortcutRow("I", "打开 / 关闭背包", ICON_INVENTORY, rightX, rowY - rowGap * 2f);
        drawShortcutRow("F5", "保存当前进度", ICON_SAVE, rightX, rowY - rowGap * 3f);
        drawShortcutRow("F9", "读取存档", ICON_LOAD, rightX, rowY - rowGap * 4f);

        smallFont.setColor(UI_DARK_TEXT);
        drawCenteredInBoxWithSmallFont("ESC 继续", centerX - 92, panelY + 24, 184, 42);
    }

    private float inventoryPanelHeight()
    {
        if (inventoryUseMode) {
            int rows = Math.max(1, engine.getPlayer().getInventory().size());
            return Math.min(88f + rows * 30f, playHeight() - 24f);
        }
        if (inventoryInspectMode) {
            return Math.min(260f, playHeight() - 24f);
        }
        return Math.min(190f, playHeight() - 24f);
    }

    private void drawInventoryPanel()
    {
        float width = Gdx.graphics.getWidth();
        float panelWidth = Math.min(400f, width - 56f);
        float panelHeight = inventoryPanelHeight();
        float panelX = Math.max(24f, width - panelWidth - 28f);
        float panelY = Math.max(playBottom() + 12f, playTop() - panelHeight - 12f);

        font.setColor(UI_LIGHT_TEXT);
        if (inventoryUseMode) {
            font.draw(batch, "使用物品", panelX + 24, panelY + panelHeight - 26);
            drawInventoryUseList(panelX, panelY, panelWidth, panelHeight);
            return;
        }

        List<Item> items = engine.getPlayer().getInventory();
        if (inventoryInspectIndex >= items.size()) {
            inventoryInspectIndex = Math.max(0, items.size() - 1);
        }

        font.draw(batch, inventoryInspectMode ? "物品详情" : "背包", panelX + 24,
            panelY + panelHeight - 26);
        font.setColor(UI_DARK_TEXT);

        float innerX = panelX + 28f;
        float topY = panelY + panelHeight - 52f;

        if (!inventoryInspectMode) {
            smallFont.setColor(UI_DARK_TEXT);
            smallFont.draw(batch, "↑↓选择  X查看详情  Esc关闭", innerX, topY);

            float rowY = topY - 28f;
            float rowH = 26f;
            float rowW = panelWidth - 56f;
            for (int i = 0; i < items.size() && i < 12; i++) {
                float y = rowY - i * rowH;
                if (i == inventoryInspectIndex) {
                    uiSkin.drawButton(batch, innerX, y, rowW, rowH);
                }
                Item it = items.get(i);
                String line = (i + 1) + ". " + it.getName() + " (" + it.getWeight() + ")";
                smallFont.setColor(i == inventoryInspectIndex ? Color.WHITE : UI_DARK_TEXT);
                smallFont.draw(batch, line, innerX + 8f, y + rowH - 7f);
            }

            if (!items.isEmpty()) {
                Item picked = items.get(inventoryInspectIndex);
                ItemUseCheck check = engine.checkItemUse(picked.getItemId());
                smallFont.setColor(UI_DARK_TEXT);
                drawClampedLine(smallFont, "效果提示: " + check.hint, innerX,
                    panelY + 22f, rowW);
            } else {
                smallFont.setColor(UI_DARK_TEXT);
                smallFont.draw(batch, "背包为空", innerX, panelY + 36f);
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
        font.setColor(UI_DARK_TEXT);
        drawMultiline("描述: " + item.getDescription(), innerX, topY - 58f, 20f);

        // 可用性提示
        smallFont.setColor(UI_DARK_TEXT);
        drawClampedLine(smallFont, "使用条件: " + check.hint, innerX, panelY + 44f,
            panelWidth - 56f);
        smallFont.setColor(UI_DARK_TEXT);
        smallFont.draw(batch, "Enter返回  Esc关闭", innerX, panelY + 20f);
    }

    private void drawInventoryUseList(float panelX, float panelY, float panelWidth,
        float panelHeight)
    {
        List<Item> items = engine.getPlayer().getInventory();
        float rowY = panelY + panelHeight - 58f;
        float rowHeight = 28f;
        smallFont.setColor(UI_DARK_TEXT);
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            ItemUseCheck check = engine.checkItemUse(item.getItemId());
            boolean selected = i == inventorySelectedIndex;
            float rowX = panelX + 28f;
            float rowW = panelWidth - 56f;
            if (selected) {
                uiSkin.drawButton(batch, rowX, rowY - 20f, rowW, rowHeight);
            }
            String prefix = (i < 9) ? ((i + 1) + ". ") : "   ";
            String status;
            if (check.canUse) {
                status = " [可用]";
            } else if (check.requiresLocation) {
                status = " [需在门锁前]";
            } else {
                status = " [不可用]";
            }
            String line = prefix + item.getName() + " (" + item.getWeight() + ")" + status;
            smallFont.setColor(selected ? Color.WHITE : UI_DARK_TEXT);
            smallFont.draw(batch, line, rowX + 8f, rowY);
            rowY -= rowHeight;
        }
        if (!items.isEmpty()) {
            ItemUseCheck hint = engine.checkItemUse(items.get(inventorySelectedIndex).getItemId());
            smallFont.setColor(UI_DARK_TEXT);
            drawClampedLine(smallFont, hint.hint, panelX + 28f, panelY + 18f, panelWidth - 56f);
        }
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

    private float playBottom()
    {
        return PLAY_BOTTOM;
    }

    private float playTop()
    {
        return playBottom() + playHeight();
    }

    private float playWidth()
    {
        return Math.max(TILE * 3f, Gdx.graphics.getWidth() - PLAY_LEFT - PLAY_RIGHT_MARGIN);
    }

    private float playHeight()
    {
        return Math.max(TILE * 3f, Gdx.graphics.getHeight() - PLAY_BOTTOM - PLAY_TOP_MARGIN);
    }

    private void updateCameras(int width, int height)
    {
        worldCamera.setToOrtho(false, 960f, 540f);
        worldCamera.position.set(480f, 270f, 0f);
        worldCamera.update();
        uiCamera.setToOrtho(false, width, height);
        uiCamera.update();
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
