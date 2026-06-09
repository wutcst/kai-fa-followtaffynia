package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.RpgMain;
import cn.edu.whut.sept.zuul.client.render.PlayerRenderer;
import cn.edu.whut.sept.zuul.client.render.UtCombatRenderer;
import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
import cn.edu.whut.sept.zuul.client.ui.UiDrawUtils;
import cn.edu.whut.sept.zuul.client.ui.WorldMapRenderer;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatEngine;
import cn.edu.whut.sept.zuul.infra.GameLogger;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.SaveGameService;

import java.util.logging.Logger;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * 主游戏画面 —— 纯编排层，所有具体工作委托给子组件。
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
    private static final float UI_GRID = 8f;
    private static final Color UI_LIGHT_TEXT = new Color(1f, 0.96f, 0.82f, 1f);
    private static final Color UI_DARK_TEXT = new Color(0.26f, 0.18f, 0.1f, 1f);
    private static final String LOG_TAG = "GameScreen";
    private static final Logger LOG = GameLogger.get();

    // infrastructure
    private final RpgMain game;
    private final SpriteBatch batch;
    private final GameEngine engine;
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final ShapeRenderer shapes;
    private final GameUiSkin uiSkin;

    // sub-components
    private final UiDrawUtils draw;
    private final CameraController camera;
    private final NpcPlaceholderManager npcManager;
    private final ItemPlaceholderManager itemManager;
    private final RoomController room;
    private final PlayerMovementController movement;
    private final UtCombatRenderer utRenderer;
    private final WorldMapRenderer worldMapRenderer;
    private final PlayerRenderer playerRenderer;
    private final DialogueUi dialogueUi;
    private final EncounterUi encounterUi;
    private final InteractionRenderer interaction;
    private final InventoryPanel inventory;
    private final InventoryInputHandler inventoryInput;
    private final HudRenderer hud;

    // player state
    private float playerX, playerY;
    private String actionMessage;
    private boolean paused;
    private boolean screenChanged;
    private boolean worldMapOpen;
    private int moveLogFrame;

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
        GlyphLayout layout = new GlyphLayout();
        this.draw = new UiDrawUtils(font, smallFont, uiSkin, layout, UI_LIGHT_TEXT, UI_DARK_TEXT, UI_GRID);
        this.camera = new CameraController();
        // room must be assigned first so lambdas below can capture the field reference
        this.room = new RoomController(engine, batch, TILE, PLAYER_W, PLAYER_H,
            null, null, r -> Gdx.app.postRunnable(r));
        this.npcManager = new NpcPlaceholderManager((tx, ty) -> room.tileToWorldRect(tx, ty, 1, 1));
        this.itemManager = new ItemPlaceholderManager(engine, (tx, ty) -> room.tileToWorldRect(tx, ty, 1, 1));
        room.setManagers(npcManager, itemManager);
        this.movement = new PlayerMovementController(npcManager, TILE, PLAYER_W, PLAYER_H,
            SPEED, DASH_SPEED_MULTIPLIER, DASH_DURATION, DASH_COOLDOWN, ATTACK_COOLDOWN);
        this.utRenderer = new UtCombatRenderer(batch, shapes, font, smallFont);
        this.worldMapRenderer = new WorldMapRenderer();
        this.playerRenderer = new PlayerRenderer();
        this.dialogueUi = new DialogueUi(engine, batch, smallFont, shapes, draw, layout,
            npcManager, camera.getWorldCamera());
        this.encounterUi = new EncounterUi(engine, batch, shapes, font, smallFont, dialogueUi);
        this.interaction = new InteractionRenderer(engine, batch, smallFont, draw, uiSkin,
            camera.getWorldCamera(), npcManager, itemManager, room);
        interaction.setPlayerRenderer(playerRenderer);
        interaction.setPlayerSize(PLAYER_W, PLAYER_H);
        this.inventory = new InventoryPanel(engine, batch, font, smallFont, uiSkin, layout, draw);
        this.inventoryInput = new InventoryInputHandler(inventory, engine);
        this.hud = new HudRenderer(engine, batch, font, smallFont, uiSkin, draw);
        this.actionMessage = initialStatus;

        movement.setFacing((savedFacing != null && !savedFacing.isEmpty()) ? savedFacing : "south");

        camera.update(room.mapPixelWidth(), room.mapPixelHeight());
        room.loadCurrentRoom(false);
        if (Float.isNaN(savedPlayerX) || Float.isNaN(savedPlayerY)) {
            float[] sp = room.resolveSpawn();
            playerX = sp[0];
            playerY = sp[1];
        } else {
            playerX = savedPlayerX;
            playerY = savedPlayerY;
        }
        playerX = movement.clampX(playerX, room.mapPixelWidth());
        playerY = movement.clampY(playerY, room.mapPixelHeight());
    }

    // ==================== Screen lifecycle ====================

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void resize(int width, int height)
    {
        camera.update(room.mapPixelWidth(), room.mapPixelHeight());
    }

    @Override
    public void dispose()
    {
        room.disposeMap();
        shapes.dispose();
        uiSkin.dispose();
        smallFont.dispose();
        if (playerRenderer != null) playerRenderer.dispose();
    }

    // ==================== render ====================

    @Override
    public void render(float delta)
    {
        handleInput(delta);
        if (screenChanged) return;

        if (!paused) {
            if (room.getExitCooldown() > 0f) room.setExitCooldown(room.getExitCooldown() - delta);
            movement.update(delta);
            movement.checkAttackFinished(playerRenderer.isAttackFinished());
            RoomController.ExitResult exit = room.checkExitOverlap(playerX, playerY);
            if (exit != null) {
                actionMessage = exit.message;
                if (exit.hasSpawn) { playerX = exit.spawnX; playerY = exit.spawnY; }
            }
        }

        Gdx.gl.glClearColor(0.06f, 0.06f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (room.getMap() == null || room.getMapRenderer() == null) {
            drawMapLoadError();
            return;
        }

        playerRenderer.update(delta, movement.isMovingLastFrame(), movement.isDashing(),
            movement.isAttacking(),
            PlayerRenderer.FacingDirection.fromString(movement.getFacing()));

        // world rendering
        camera.applyWorldViewport();
        camera.getWorldCamera().update();
        room.getMapRenderer().setView(camera.getWorldCamera());
        room.getMapRenderer().render();
        shapes.setProjectionMatrix(camera.getWorldCamera().combined);
        npcManager.drawNpcPlaceholders(shapes);
        shapes.setProjectionMatrix(camera.getWorldCamera().combined);
        itemManager.drawItemPlaceholders(shapes);
        dialogueUi.drawNpcDialogueBubble();
        interaction.drawPlayer(playerX, playerY);
        interaction.drawInteractionPrompt(playerX, playerY, paused, inventory.isOpen(),
            encounterUi.isMenuOpen(), dialogueUi.isActive(),
            engine.isInDialogue(), engine.isInCombat());

        // UI rendering
        camera.applyFullViewport();
        batch.setProjectionMatrix(camera.getUiCamera().combined);

        if (engine.isInCombat() && engine.isUndertaleCombat()) {
            renderUtCombatOverlay();
        }

        batch.begin();
        if (!(engine.isInCombat() && engine.isUndertaleCombat())) {
            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();
            inventory.setWorldViewportHeight(camera.getWorldViewportHeight());
            hud.drawUiPanels(w, h, paused, inventory.isOpen(),
                inventory.inventoryPanelHeight());
            hud.drawHud(w, h, paused, actionMessage);
            if (inventory.isOpen()) {
                float pw = draw.grid(Math.min(520f, w - 96f));
                float ph = draw.grid(inventory.inventoryPanelHeight());
                float px = draw.grid((w - pw) / 2f);
                float py = draw.grid(Math.max(112f, h - 64f - ph - 16f));
                inventory.render(px, py, pw, ph);
            }
            hud.drawFooter(w, actionMessage);
        }
        batch.end();

        if (worldMapOpen) drawWorldMap(delta);
    }

    private void renderUtCombatOverlay()
    {
        UndertaleCombatEngine ut = utRenderer.utEngine(engine);
        if (ut == null) return;
        int sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.getUiCamera().combined);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(0, 0, sw, sh);
        shapes.end();

        float boxW = sw * 0.64f, boxH = sh * 0.56f;
        float boxX = (sw - boxW) / 2f, boxY = (sh - boxH) / 2f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.02f, 0.05f, 0.92f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(1f, 1f, 1f, 0.9f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();

        utRenderer.render(ut, engine, boxX, boxY, boxW, boxH);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawMapLoadError()
    {
        camera.applyFullViewport();
        batch.setProjectionMatrix(camera.getUiCamera().combined);
        batch.begin();
        font.setColor(Color.RED);
        font.draw(batch, "地图加载失败，请检查 assets/maps 与 tilesets 路径",
            40, Gdx.graphics.getHeight() / 2f);
        hud.drawFooter(Gdx.graphics.getWidth(), actionMessage);
        batch.end();
    }

    // ==================== input ====================

    private void handleInput(float delta)
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (encounterUi.isMenuOpen()) {
                engine.leaveEncounter();
                actionMessage = "已关闭遭遇菜单";
                return;
            }
            if (inventory.isOpen()) {
                inventory.close();
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
        if (paused) { handlePauseInput(); return; }
        if (encounterUi.isMenuOpen()) {
            String msg = encounterUi.handleMenuInput();
            if (msg != null) actionMessage = msg;
            return;
        }
        if (dialogueUi.isActive() || engine.isInDialogue()) {
            StringBuilder sb = new StringBuilder(actionMessage);
            dialogueUi.handleInput(sb);
            actionMessage = sb.toString();
            return;
        }
        if (engine.isInCombat()) {
            if (engine.isUndertaleCombat()) {
                String msg = encounterUi.handleUtCombatInput(delta, utRenderer.utEngine(engine));
                if (msg != null) actionMessage = msg;
            } else {
                String msg = encounterUi.handleCombatInput();
                if (msg != null) actionMessage = msg;
            }
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) saveGame();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) { loadGame(); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            inventory.toggle();
            actionMessage = inventory.isOpen()
                ? "背包已打开：↑↓选择，U使用，X查看详情" : "背包已关闭";
        }
        if (inventory.isOpen()) { inventoryInput.handleInput(); return; }

        if (Gdx.input.isKeyJustPressed(Input.Keys.J) && movement.canStartAttack()) {
            movement.startAttack(1.0f);
            actionMessage = "攻击！";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F) && movement.canStartDash()) {
            movement.startDash();
            actionMessage = "冲刺！";
        }

        movePlayer(delta);

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) actionMessage = engine.look();
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            if (engine.moveBack()) {
                float[] sp = room.loadCurrentRoom(true);
                if (sp != null) { playerX = sp[0]; playerY = sp[1]; }
                actionMessage = "已回退至 " + engine.getCurrentRoom().getRoomId();
            } else actionMessage = "无法回退";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            String npcId = npcManager.findNearbyNpcId(playerX, playerY, PLAYER_W, PLAYER_H);
            if (npcId != null) {
                encounterUi.openMenu(npcId);
                actionMessage = "遇到 " + npcId + "。按 1 交流 / 2 杀害 / 3 离开 / 4 UT战斗";
            } else {
                String itemId = itemManager.findNearbyItemId(playerX, playerY, PLAYER_W, PLAYER_H);
                if (itemId != null) {
                    if (engine.takeItem(itemId)) {
                        itemManager.rebuildItemPlaceholders();
                        actionMessage = "拾取了 " + itemId;
                    } else actionMessage = "拾取失败（可能超重）";
                }
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.U))
            actionMessage = "按 I 打开背包，选择物品后按 U 使用";
    }

    private void handlePauseInput()
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) saveGame();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) loadGame();
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) switchToTitle();
    }

    private void movePlayer(float delta)
    {
        boolean w = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean s = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean a = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean d = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        if ((w || s || a || d) && moveLogFrame % 30 == 0) {
            int mr = room.getMap() == null ? 17 : (int)(room.mapPixelHeight() / TILE);
            LOG.info("moveKey: pixel=(" + (int)playerX + "," + (int)playerY
                + ") tile=(" + (int)(playerX/TILE) + "," + movement.gdxYToTiledRow(playerY, mr) + ")");
        }
        moveLogFrame++;

        PlayerMovementController.MoveResult r = movement.applyMovement(
            delta, playerX, playerY, w, s, a, d,
            room.getWallLayer(), room.mapPixelWidth(), room.mapPixelHeight());
        playerX = r.newX;
        playerY = r.newY;
    }

    // ==================== save / load / world map ====================

    private void saveGame()
    {
        try {
            GameState state = engine.captureState();
            state.setPlayerX(playerX);
            state.setPlayerY(playerY);
            state.setFacing(movement.getFacing());
            SaveGameService.save(state);
            actionMessage = "已保存到 " + SaveGameService.defaultSavePath();
        } catch (Exception e) {
            actionMessage = "存档失败: " + e.getClass().getSimpleName();
        }
    }

    private void loadGame()
    {
        try {
            GameState state = SaveGameService.load();
            GameEngine loaded = new GameEngine(state.getPlayerName());
            loaded.restoreState(state);
            screenChanged = true;
            game.setScreen(new GameScreen(game, batch, loaded,
                state.getPlayerX(), state.getPlayerY(), "已读取存档", state.getFacing()));
            deferDispose();
        } catch (Exception e) {
            actionMessage = "读档失败: " + e.getClass().getSimpleName();
        }
    }

    private void switchToTitle()
    {
        screenChanged = true;
        game.setScreen(new TitleScreen(game, batch));
        deferDispose();
    }

    private void deferDispose()
    {
        final GameScreen self = this;
        Gdx.app.postRunnable(self::dispose);
    }

    private void drawWorldMap(float delta)
    {
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float pw = Math.min(520f, w - 48f), ph = Math.min(420f, h - 88f);
        float px = (w - pw) / 2f, py = (h - ph) / 2f;
        camera.applyFullViewport();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.getUiCamera().combined);
        worldMapRenderer.render(shapes, batch, smallFont, px, py, pw, ph,
            engine.getCurrentRoom().getRoomId(), engine.getExploredRoomIds(),
            engine::isLockUnlocked, delta);
    }
}
