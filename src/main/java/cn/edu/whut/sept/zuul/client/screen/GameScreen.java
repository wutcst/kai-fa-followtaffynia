package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.RpgMain;
import cn.edu.whut.sept.zuul.client.audio.GameAudio.Cue;
import cn.edu.whut.sept.zuul.client.audio.GameAudio.Track;
import cn.edu.whut.sept.zuul.client.render.NpcRenderer;
import cn.edu.whut.sept.zuul.client.render.PlayerRenderer;
import cn.edu.whut.sept.zuul.client.render.UtCombatRenderer;
import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
import cn.edu.whut.sept.zuul.client.ui.UiDrawUtils;
import cn.edu.whut.sept.zuul.client.ui.WorldMapRenderer;
import cn.edu.whut.sept.zuul.engine.EndingType;
import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatEngine;
import cn.edu.whut.sept.zuul.infra.GameLogger;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.SaveGameService;

import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
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
    private static final float DASH_COOLDOWN = 0.2f;
    private static final float ATTACK_COOLDOWN = 0.2f;
    private static final float UI_GRID = 8f;
    private static final float TOP_BAR_HEIGHT = HudRenderer.TOP_BAR_HEIGHT; // 72
    private static final float FOOTER_HEIGHT = HudRenderer.FOOTER_HEIGHT;   // 72
    private static final Color UI_LIGHT_TEXT = new Color(1f, 0.96f, 0.82f, 1f);
    private static final Color UI_DARK_TEXT = new Color(0.26f, 0.18f, 0.1f, 1f);
    private static final Color ACCENT_GOLD = new Color(1f, 0.74f, 0.22f, 1f);
    private static final Color ACCENT_ARCANE = new Color(0.46f, 0.72f, 1f, 1f);
    private static final Color ACCENT_FIRE = new Color(1f, 0.42f, 0.12f, 1f);
    private static final Color ACCENT_GREEN = new Color(0.42f, 0.88f, 0.36f, 1f);
    private static final Color ACCENT_WARM = new Color(0.82f, 0.58f, 0.28f, 1f);
    private static final String LOG_TAG = "GameScreen";
    private static final Logger LOG = GameLogger.get();

    // infrastructure
    private final RpgMain game;
    private final SpriteBatch batch;
    private final GameEngine engine;
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final ShapeRenderer shapes;
    private final OrthographicCamera fullScreenCam;
    private final GlyphLayout layout;
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
    private boolean lastFrameInDialogueOrCombat;
    private final NpcRenderer npcRenderer;
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
    private int lastObservedHp;
    private float stepAccumulator;
    private float feedbackFlashTimer;
    private float visualTimer;
    private float roomBannerTimer;
    private float gameOverAlpha;
    private boolean gameOverAnnounced;
    private float shopPopupTimer;
    private int shopPopupJunkCount;
    private int shopPopupHerbCount;
    private String shopPopupTitle;
    private String shopPopupMessage;
    private String lastShopPopupSource;
    private EndingType activeEndingScreen;
    private String roomBannerRoomId;
    private final Color feedbackFlashColor;
    private final Map<String, Texture> npcPortraits;

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
        this.fullScreenCam = new OrthographicCamera();
        this.uiSkin = new GameUiSkin();
        this.layout = new GlyphLayout();
        this.draw = new UiDrawUtils(font, smallFont, uiSkin, this.layout,
            UI_LIGHT_TEXT, UI_DARK_TEXT, UI_GRID);
        this.camera = new CameraController();
        // room must be assigned first so lambdas below can capture the field reference
        this.room = new RoomController(engine, batch, TILE, PLAYER_W, PLAYER_H,
            null, null, r -> Gdx.app.postRunnable(r));
        this.npcManager = new NpcPlaceholderManager((tx, ty) -> room.tileToWorldRect(tx, ty, 1, 1));
        this.npcRenderer = new NpcRenderer("guard", "hermit", "merchant", "priest",
            "follower", "scholar", "apprentice", "golem");
        npcManager.setNpcRenderer(npcRenderer);
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
        this.lastObservedHp = engine.getPlayer().getHp();
        this.feedbackFlashColor = new Color(1f, 1f, 1f, 0f);
        this.npcPortraits = new HashMap<>();
        loadNpcPortraits();

        movement.setFacing((savedFacing != null && !savedFacing.isEmpty()) ? savedFacing : "south");

        camera.update(room.mapPixelWidth(), room.mapPixelHeight());
        room.loadCurrentRoom(false);
        markRoomBanner();
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

    @Override public void show() { updateMusic(); }
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
        if (utRenderer != null) utRenderer.dispose();
        if (npcRenderer != null) npcRenderer.dispose();
        for (Texture tex : npcPortraits.values()) {
            if (tex != null) tex.dispose();
        }
    }

    private void loadNpcPortraits()
    {
        String[] npcIds = {"guard", "hermit", "merchant", "priest", "follower", "scholar", "apprentice"};
        for (String npcId : npcIds) {
            Texture tex = loadPortraitTexture(npcId, "_head.png");
            if (tex == null) {
                tex = loadPortraitTexture(npcId, ".png");
            }
            if (tex != null) {
                npcPortraits.put(npcId, tex);
            }
        }
    }

    private Texture loadPortraitTexture(String npcId, String suffix)
    {
        try {
            Texture tex = new Texture(Gdx.files.internal("npc/" + npcId + suffix));
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            if (tex.getWidth() < 16 || tex.getHeight() < 16) {
                tex.dispose();
                return null;
            }
            return tex;
        } catch (Exception e) {
            LOG.warning("Failed to load portrait for " + npcId + suffix + ": " + e.getMessage());
            return null;
        }
    }

    // ==================== render ====================

    @Override
    public void render(float delta)
    {
        visualTimer += delta;
        if (engine.isPlayerDead()) {
            drawGameOverScreen(delta);
            return;
        }

        EndingType ending = engine.getCurrentEnding();
        if (ending != null && ending != EndingType.NONE) {
            drawEndingScreen(ending, delta);
            return;
        }

        handleInput(delta);
        if (screenChanged) return;
        updateMusic();

        if (!paused) {
            if (room.getExitCooldown() > 0f) room.setExitCooldown(room.getExitCooldown() - delta);
            if (roomBannerTimer > 0f) roomBannerTimer -= delta;
            movement.update(delta);
            movement.checkAttackFinished(playerRenderer.isAttackFinished());
            RoomController.ExitResult exit = room.checkExitOverlap(playerX, playerY);
            if (exit != null) {
                actionMessage = exit.message;
                if (exit.hasSpawn) {
                    game.getAudio().play(Cue.DOOR);
                    flash(1f, 0.82f, 0.38f, 0.18f);
                } else {
                    game.getAudio().play(Cue.ERROR);
                    flash(1f, 0.24f, 0.18f, 0.16f);
                }
                if (exit.hasSpawn) { playerX = exit.spawnX; playerY = exit.spawnY; }
                if (exit.hasSpawn) markRoomBanner();
                if (exit.hasSpawn) {
                    String autoCombat = engine.getPendingAutoCombat();
                    if (autoCombat != null) {
                        engine.clearPendingAutoCombat();
                        encounterUi.forceUtCombat(autoCombat);
                        actionMessage = "金库魔像突然苏醒了！";
                    }
                }
            }
        }
        updateDamageFeedback();
        updateFeedbackFlash(delta);
        updateShopPopup(delta);

        Gdx.gl.glClearColor(0.06f, 0.06f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (room.getMap() == null || room.getMapRenderer() == null) {
            drawMapLoadError();
            return;
        }

        playerRenderer.update(delta, movement.isMovingLastFrame(), movement.isDashing(),
            movement.isAttacking(),
            PlayerRenderer.FacingDirection.fromString(movement.getFacing()));
        npcRenderer.update(delta);

        // world rendering
        camera.applyWorldViewport();
        camera.getWorldCamera().update();
        room.getMapRenderer().setView(camera.getWorldCamera());
        room.getMapRenderer().render();
        drawRoomAtmosphere();
        batch.setProjectionMatrix(camera.getWorldCamera().combined);
        batch.begin();
        npcManager.drawNpcPlaceholders(batch);
        batch.end();
        shapes.setProjectionMatrix(camera.getWorldCamera().combined);
        itemManager.drawItemPlaceholders(shapes);
        interaction.drawPlayer(playerX, playerY);
        boolean interactionPromptVisible = interaction.drawInteractionPrompt(
            playerX, playerY, paused || worldMapOpen, inventory.isOpen(),
            encounterUi.isMenuOpen(), dialogueUi.isActive(),
            engine.isInDialogue(), engine.isInCombat());

        // UI rendering
        camera.applyFullViewport();

        // ---- 画状态栏背景（全物理屏宽，覆盖 letterbox 黑边） ----
        int physW = Gdx.graphics.getWidth();
        int physH = Gdx.graphics.getHeight();
        float s = camera.getScale();
        float topH = TOP_BAR_HEIGHT * s;
        float botH = FOOTER_HEIGHT * s;
        HdpiUtils.glViewport(0, 0, physW, physH);
        fullScreenCam.setToOrtho(false, physW, physH);
        fullScreenCam.update();
        shapes.setProjectionMatrix(fullScreenCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.22f, 0.12f, 0.05f, 1f);  // 木纹底色
        shapes.rect(0, physH - topH, physW, topH);   // 顶栏
        shapes.rect(0, 0, physW, botH);               // 底栏
        shapes.end();
        // ----

        batch.setProjectionMatrix(camera.getUiCamera().combined);

        if (engine.isInCombat() && engine.isUndertaleCombat()) {
            UndertaleCombatEngine ut = utRenderer.utEngine(engine);
            if (ut != null && ut.isShowingBattleLine()) {
                renderBattleLineOverlay(ut);
            } else {
                renderUtCombatOverlay();
            }
        } else if (dialogueUi.isActive()) {
            renderDialogueOverlay();
        }

        batch.begin();
        boolean utCombatActive = engine.isInCombat() && engine.isUndertaleCombat()
            && !utRenderer.utEngine(engine).isShowingBattleLine();
        if (!utCombatActive) {
            float w = CameraController.DESIGN_W;
            float h = CameraController.DESIGN_H;
            inventory.setWorldViewportHeight(camera.getWorldViewportHeight());
            hud.drawUiPanels(w, h, paused, inventory.isOpen(),
                inventory.inventoryPanelHeight());
            hud.drawHud(w, h, paused, actionMessage);
            if (!paused && !inventory.isOpen()) {
                hud.drawQuestTracker(w, h, camera.getWorldViewportX(), camera.getWorldViewportWidth());
            }
            drawRoomBanner(interactionPromptVisible);
            if (inventory.isOpen()) {
                float pw = draw.grid(Math.min(520f, w - 96f));
                float ph = draw.grid(inventory.inventoryPanelHeight());
                float px = draw.grid((w - pw) / 2f);
                float py = draw.grid(Math.max(112f, h - 64f - ph - 16f));
                inventory.render(px, py, pw, ph);
            }
            if (!dialogueUi.isActive()) {
                hud.drawFooter(w, actionMessage);
            }
        }
        batch.end();

        if (worldMapOpen) drawWorldMap(delta);
        drawShopPopup();
        drawFeedbackFlash();
    }

    private void renderUtCombatOverlay()
    {
        UndertaleCombatEngine ut = utRenderer.utEngine(engine);
        if (ut == null) return;
        int sw = (int) CameraController.DESIGN_W, sh = (int) CameraController.DESIGN_H;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.getUiCamera().combined);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(0, 0, sw, sh);
        shapes.end();

        float boxW = draw.grid(Math.max(360f, Math.min(760f, sw * 0.62f)));
        float boxH = draw.grid(Math.max(240f, Math.min(360f, sh * 0.46f)));
        float boxX = draw.grid((sw - boxW) / 2f);
        float minBoxY = FOOTER_HEIGHT + 64f;
        float maxBoxY = sh - TOP_BAR_HEIGHT - boxH - 28f;
        float centeredY = (sh - boxH) / 2f;
        float boxY = draw.grid(Math.max(minBoxY, Math.min(centeredY, maxBoxY)));
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.025f, 0.022f, 0.04f, 0.96f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(1f, 1f, 1f, 0.9f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();

        utRenderer.render(ut, engine, boxX, boxY, boxW, boxH);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ========== 战斗台词画中画（底部对话风格，Enter 继续）==========

    private void renderBattleLineOverlay(UndertaleCombatEngine ut)
    {
        String text = ut.getBattleLineText();
        if (text == null || text.isEmpty()) return;
        String colorTag = ut.getBattleLineColor();

        int sw = (int) CameraController.DESIGN_W;
        int sh = (int) CameraController.DESIGN_H;
        float boxH = sh * 0.28f;
        float boxY = FOOTER_HEIGHT;
        float boxX = 16f;
        float boxW = sw - 32f;

        Color lineColor = colorFromTag(colorTag);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.getUiCamera().combined);

        // 暗色底
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.02f, 0.06f, 0.95f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();

        // 边框
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(lineColor.r, lineColor.g, lineColor.b, 0.85f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();

        // NPC 名字
        batch.setProjectionMatrix(camera.getUiCamera().combined);
        batch.begin();
        font.setColor(lineColor);
        font.draw(batch, ut.getDef().displayName,
            boxX + 16f, boxY + boxH - 16f);

        // 战斗台词正文
        float textX = boxX + 16f;
        float textY = boxY + boxH - 44f;
        float textW = boxW - 32f;
        font.setColor(Color.WHITE);
        font.draw(batch, text, textX, textY, textW,
            com.badlogic.gdx.utils.Align.left, true);

        // "按 Enter 继续" 提示
        float hintW = Math.min(textW, 240f);
        float hintX = boxX + boxW - hintW - 16f;
        float hintY = boxY + 20f;
        smallFont.setColor(1f, 1f, 1f, 0.5f);
        smallFont.draw(batch, "按 Enter 继续",
            hintX, hintY, hintW,
            com.badlogic.gdx.utils.Align.right, false);
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private Color colorFromTag(String tag)
    {
        if (tag == null) return Color.WHITE;
        switch (tag.toLowerCase()) {
            case "red":    return Color.RED;
            case "green":  return Color.GREEN;
            case "blue":   return Color.CYAN;
            case "pink":   return Color.PINK;
            case "yellow": return Color.YELLOW;
            case "orange": return Color.ORANGE;
            case "white":  return Color.WHITE;
            default:       return new Color(0.7f, 0.7f, 0.9f, 1f);
        }
    }

    // ========== 传统 RPG 对话窗口（底部 1/4 屏幕） ==========

    private static final float DIALOG_PORTRAIT_W = 120f;
    private static final float DIALOG_PORTRAIT_H = 120f;
    private static final Color DIALOG_BORDER = new Color(1f, 0.72f, 0.24f, 0.92f);
    private static final Color DIALOG_DIM_BORDER = new Color(0.45f, 0.33f, 0.18f, 0.72f);
    private static final Color DIALOG_NPC_ACTIVE_FILL = new Color(0.25f, 0.28f, 0.44f, 1f);
    private static final Color DIALOG_NPC_DIM_FILL = new Color(0.10f, 0.10f, 0.16f, 0.78f);
    private static final Color DIALOG_PLAYER_ACTIVE_FILL = new Color(0.32f, 0.46f, 0.25f, 1f);
    private static final Color DIALOG_PLAYER_DIM_FILL = new Color(0.11f, 0.12f, 0.10f, 0.78f);

    private void renderDialogueOverlay()
    {
        Dialogue d = dialogueUi.getActiveDialogue();
        if (d == null) return;

        int sw = (int) CameraController.DESIGN_W;
        int sh = (int) CameraController.DESIGN_H;
        float boxH = sh * 0.28f;
        float boxY = FOOTER_HEIGHT;
        float boxX = 16f;
        float boxW = sw - 32f;

        String playerChoice = dialogueUi.getPlayerLastChoice();
        boolean showingChoice = playerChoice != null && !playerChoice.isEmpty();

        // 暗色遮罩
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.getUiCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.02f, 0.06f, 0.95f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();

        // 边框
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(1f, 0.7f, 0.3f, 0.8f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();

        // 立绘区
        float npcPx = boxX + 16f;
        float npcPy = boxY + (boxH - DIALOG_PORTRAIT_H) / 2f;
        float playerPx = boxX + boxW - DIALOG_PORTRAIT_W - 16f;
        float playerPy = npcPy;

        drawDialoguePortraitFrame(npcPx, npcPy, !showingChoice,
            showingChoice ? DIALOG_NPC_DIM_FILL : DIALOG_NPC_ACTIVE_FILL);
        drawDialoguePortraitFrame(playerPx, playerPy, showingChoice,
            showingChoice ? DIALOG_PLAYER_ACTIVE_FILL : DIALOG_PLAYER_DIM_FILL);

        // 文字和 NPC 头像
        batch.setProjectionMatrix(camera.getUiCamera().combined);
        batch.begin();

        // NPC 头像
        Texture npcTex = npcPortraits.get(d.getNpcId());
        if (npcTex != null) {
            Color c = batch.getColor();
            float alpha = showingChoice ? 0.6f : 1f;
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(npcTex, npcPx, npcPy, DIALOG_PORTRAIT_W, DIALOG_PORTRAIT_H);
            batch.setColor(c);
        }
        float textX = npcPx + DIALOG_PORTRAIT_W + 16f;
        float textW = playerPx - textX - 16f;

        String npcName = d.getNpcId();

        if (showingChoice) {
            // "你" 放在玩家头像上方
            float youNameY = playerPy + DIALOG_PORTRAIT_H + 28f;
            font.setColor(0.6f, 1f, 0.5f, 1f);
            draw.drawCentered(batch, "你", playerPx + DIALOG_PORTRAIT_W / 2f, youNameY);

            // NPC 回应（左对齐）
            String npcText = dialogueUi.formatDialogue(d);
            int ln = npcText.lastIndexOf('\n');
            String npcBody = ln > 0 ? npcText.substring(0, ln) : npcText;
            float npcTextY = 0;
            if (npcBody != null && !npcBody.trim().isEmpty()) {
                float respNameY = npcPy + DIALOG_PORTRAIT_H + 28f;
                font.setColor(1f, 0.82f, 0.4f, 1f);
                draw.drawCentered(batch, npcName, npcPx + DIALOG_PORTRAIT_W / 2f, respNameY);
                npcTextY = boxY + boxH - 40f;
                smallFont.setColor(0.9f, 0.9f, 0.92f, 1f);
                smallFont.draw(batch, npcBody, textX, npcTextY, textW,
                    com.badlogic.gdx.utils.Align.left, true);
            }

            // 玩家选择紧接在 NPC 回应下方，靠右对齐
            float playerTextY = npcTextY > 0 ? npcTextY - 26f : boxY + boxH - 40f;
            smallFont.setColor(0.6f, 1f, 0.5f, 1f);
            smallFont.draw(batch, "\"" + playerChoice + "\"",
                textX, playerTextY, textW,
                com.badlogic.gdx.utils.Align.right, true);
        } else {
            // NPC 说话
            // 名称放在 NPC 头像上方
            float nameY = npcPy + DIALOG_PORTRAIT_H + 28f;
            font.setColor(1f, 0.82f, 0.4f, 1f);
            draw.drawCentered(batch, npcName, npcPx + DIALOG_PORTRAIT_W / 2f, nameY);

            float textY = boxY + boxH - 40f;
            String text = dialogueUi.formatDialogue(d);
            int ln = text.lastIndexOf('\n');
            String body = ln > 0 ? text.substring(0, ln) : text;
            smallFont.setColor(0.9f, 0.9f, 0.92f, 1f);
            smallFont.draw(batch, body, textX, textY, textW,
                com.badlogic.gdx.utils.Align.left, true);

            // 如果有选项且已翻完所有文字，显示在底部
            if (dialogueUi.isAtChoicePoint()) {
                List<String> opts = d.getOptionTexts();
                StringBuilder optLine = new StringBuilder();
                for (int i = 0; i < opts.size(); i++) {
                    optLine.append(i + 1).append(". ").append(opts.get(i)).append("  ");
                }
                float optY = boxY + 54f;
                smallFont.setColor(1f, 0.85f, 0.3f, 1f);
                smallFont.draw(batch, optLine.toString(), textX, optY, textW,
                    com.badlogic.gdx.utils.Align.left, true);
            }
        }
        drawDialogueHint(boxX, boxY, boxW, dialogueUi.isAtChoicePoint()
            ? "数字键 1-9 选择  /  Enter 继续"
            : "Enter 继续 / 关闭对话");
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawDialoguePortraitFrame(float x, float y, boolean active, Color fill)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.03f, 0.025f, 0.035f, 0.98f);
        shapes.rect(x - 6f, y - 6f, DIALOG_PORTRAIT_W + 12f, DIALOG_PORTRAIT_H + 12f);
        shapes.setColor(fill);
        shapes.rect(x, y, DIALOG_PORTRAIT_W, DIALOG_PORTRAIT_H);
        shapes.setColor(1f, 0.9f, 0.55f, active ? 0.18f : 0.08f);
        shapes.rect(x + 8f, y + DIALOG_PORTRAIT_H - 20f, DIALOG_PORTRAIT_W - 16f, 8f);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        Color border = active ? DIALOG_BORDER : DIALOG_DIM_BORDER;
        shapes.setColor(border);
        shapes.rect(x - 6f, y - 6f, DIALOG_PORTRAIT_W + 12f, DIALOG_PORTRAIT_H + 12f);
        shapes.rect(x, y, DIALOG_PORTRAIT_W, DIALOG_PORTRAIT_H);
        shapes.end();
    }

    private void drawDialogueHint(float boxX, float boxY, float boxW, String hint)
    {
        float hintW = 280f;
        float hintH = 28f;
        float hintX = boxX + boxW - hintW - 18f;
        float hintY = boxY + 12f;
        uiSkin.drawButton(batch, hintX, hintY, hintW, hintH);
        smallFont.setColor(1f, 0.92f, 0.58f, 1f);
        draw.drawCenteredInBoxWithSmallFont(batch, hint, hintX, hintY, hintW, hintH);
    }

    private void drawMapLoadError()
    {
        camera.applyFullViewport();
        batch.setProjectionMatrix(camera.getUiCamera().combined);
        batch.begin();
        font.setColor(Color.RED);
        font.draw(batch, "地图加载失败，请检查 assets/maps 与 tilesets 路径",
            40, CameraController.DESIGN_H / 2f);
        hud.drawFooter(CameraController.DESIGN_W, actionMessage);
        batch.end();
    }

    // ========== 商店交易弹窗 ==========

    private void maybeShowShopPopup(String message)
    {
        if (message == null || message.equals(lastShopPopupSource)) {
            return;
        }
        if (!message.contains("商人")
            || (!message.contains("草药") && !message.contains("感兴趣"))) {
            return;
        }

        lastShopPopupSource = message;
        shopPopupJunkCount = extractFirstNumber(message);
        shopPopupHerbCount = shopPopupJunkCount;
        boolean traded = shopPopupJunkCount > 0;
        shopPopupTitle = traded ? "交易完成" : "没有可交换的杂物";
        shopPopupMessage = traded
            ? "交出 " + shopPopupJunkCount + " 件杂物，获得 "
                + shopPopupHerbCount + " 株草药"
            : "商人翻遍了背包，没有找到他感兴趣的东西。";
        shopPopupTimer = 4.2f;
        game.getAudio().play(traded ? Cue.USE : Cue.ERROR);
        flash(traded ? 0.45f : 1f, traded ? 0.9f : 0.22f,
            traded ? 0.55f : 0.15f, traded ? 0.13f : 0.15f);
    }

    private int extractFirstNumber(String message)
    {
        int value = 0;
        boolean reading = false;
        for (int i = 0; i < message.length(); i++) {
            char ch = message.charAt(i);
            if (ch >= '0' && ch <= '9') {
                reading = true;
                value = value * 10 + (ch - '0');
            } else if (reading) {
                return value;
            }
        }
        return value;
    }

    private void updateShopPopup(float delta)
    {
        if (shopPopupTimer > 0f) {
            shopPopupTimer = Math.max(0f, shopPopupTimer - delta);
        }
    }

    private void drawShopPopup()
    {
        if (shopPopupTimer <= 0f) {
            return;
        }

        camera.applyFullViewport();
        float alpha = shopPopupTimer < 0.45f ? shopPopupTimer / 0.45f : 1f;
        float w = CameraController.DESIGN_W;
        float h = CameraController.DESIGN_H;
        float panelW = 536f;
        float panelH = 178f;
        float panelX = (w - panelW) / 2f;
        float panelY = h * 0.56f - panelH / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.getUiCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.38f * alpha);
        shapes.rect(0f, 0f, w, h);
        shapes.setColor(0.03f, 0.025f, 0.03f, 0.96f * alpha);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.setColor(0.22f, 0.14f, 0.07f, 0.95f * alpha);
        shapes.rect(panelX + 6f, panelY + 6f, panelW - 12f, panelH - 12f);
        shapes.setColor(1f, 0.75f, 0.22f, 0.92f * alpha);
        drawFrameFilled(panelX, panelY, panelW, panelH, 3f);
        shapes.setColor(0.05f, 0.035f, 0.025f, 0.92f * alpha);
        shapes.rect(panelX + 18f, panelY + 64f, panelW - 36f, 74f);

        float iconY = panelY + 84f;
        drawJunkIcon(panelX + 106f, iconY, alpha);
        drawTradeArrow(panelX + panelW / 2f, iconY + 14f, alpha);
        drawHerbIcon(panelX + panelW - 138f, iconY, alpha);
        shapes.end();

        batch.setProjectionMatrix(camera.getUiCamera().combined);
        batch.begin();
        font.setColor(1f, 0.9f, 0.55f, alpha);
        layout.setText(font, shopPopupTitle);
        font.draw(batch, shopPopupTitle, panelX + (panelW - layout.width) / 2f,
            panelY + panelH - 28f);

        smallFont.setColor(1f, 0.96f, 0.82f, alpha);
        layout.setText(smallFont, shopPopupMessage);
        smallFont.draw(batch, shopPopupMessage, panelX + (panelW - layout.width) / 2f,
            panelY + 48f);

        if (shopPopupJunkCount > 0) {
            String left = "x" + shopPopupJunkCount;
            String right = "x" + shopPopupHerbCount;
            smallFont.setColor(1f, 0.88f, 0.48f, alpha);
            smallFont.draw(batch, left, panelX + 142f, iconY + 18f);
            smallFont.setColor(0.58f, 1f, 0.58f, alpha);
            smallFont.draw(batch, right, panelX + panelW - 102f, iconY + 18f);
        }
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawJunkIcon(float x, float y, float alpha)
    {
        shapes.setColor(0.43f, 0.28f, 0.12f, alpha);
        shapes.rect(x, y, 42f, 30f);
        shapes.setColor(0.24f, 0.14f, 0.06f, alpha);
        shapes.rect(x + 5f, y + 6f, 32f, 5f);
        shapes.rect(x + 10f, y + 18f, 22f, 4f);
        shapes.setColor(0.86f, 0.66f, 0.28f, alpha);
        shapes.rect(x + 9f, y + 28f, 24f, 7f);
        shapes.rect(x + 4f, y + 2f, 34f, 3f);
    }

    private void drawHerbIcon(float x, float y, float alpha)
    {
        shapes.setColor(0.10f, 0.34f, 0.12f, alpha);
        shapes.rect(x + 18f, y + 4f, 6f, 34f);
        shapes.setColor(0.34f, 0.86f, 0.28f, alpha);
        shapes.circle(x + 14f, y + 26f, 12f, 14);
        shapes.circle(x + 30f, y + 30f, 11f, 14);
        shapes.circle(x + 24f, y + 18f, 13f, 14);
        shapes.setColor(0.75f, 1f, 0.48f, 0.55f * alpha);
        shapes.rect(x + 20f, y + 12f, 3f, 22f);
    }

    private void drawTradeArrow(float x, float y, float alpha)
    {
        shapes.setColor(1f, 0.75f, 0.22f, alpha);
        shapes.rect(x - 34f, y - 3f, 58f, 6f);
        shapes.triangle(x + 30f, y + 14f, x + 30f, y - 14f, x + 50f, y);
    }

    private void drawFrameFilled(float x, float y, float w, float h, float t)
    {
        shapes.rect(x, y, w, t);
        shapes.rect(x, y + h - t, w, t);
        shapes.rect(x, y, t, h);
        shapes.rect(x + w - t, y, t, h);
    }

    // ==================== input ====================

    private void handleInput(float delta)
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (worldMapOpen) {
                worldMapOpen = false;
                actionMessage = "地图已关闭";
                game.getAudio().play(Cue.MENU_CLOSE);
                return;
            }
            if (encounterUi.isMenuOpen()) {
                engine.leaveEncounter();
                actionMessage = "已关闭遭遇菜单";
                game.getAudio().play(Cue.MENU_CLOSE);
                return;
            }
            if (inventory.isOpen()) {
                inventory.close();
                actionMessage = "背包已关闭";
                game.getAudio().play(Cue.MENU_CLOSE);
                return;
            }
            paused = !paused;
            actionMessage = paused ? "已暂停" : "继续探索";
            game.getAudio().play(paused ? Cue.MENU_OPEN : Cue.MENU_CLOSE);
            return;
        }

        if (worldMapOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                worldMapOpen = false;
                actionMessage = "地图已关闭";
                game.getAudio().play(Cue.MENU_CLOSE);
            }
            return;
        }

        if (paused) { handlePauseInput(); return; }
        if (encounterUi.isMenuOpen()) {
            String msg = encounterUi.handleMenuInput();
            if (msg != null) {
                actionMessage = msg;
                game.getAudio().play(isFailureMessage(msg) ? Cue.ERROR : Cue.CLICK);
            }
            return;
        }
        boolean nowInDialogueOrCombat = dialogueUi.isActive() || engine.isInDialogue()
            || engine.isInCombat();
        if (nowInDialogueOrCombat) {
            lastFrameInDialogueOrCombat = true;
            if (dialogueUi.isActive() || engine.isInDialogue()) {
                String before = actionMessage;
                StringBuilder sb = new StringBuilder(actionMessage);
                dialogueUi.handleInput(sb);
                actionMessage = sb.toString();
                if (!actionMessage.equals(before)) {
                    game.getAudio().play(Cue.CLICK);
                    maybeShowShopPopup(engine.getLastMessage());
                }
                // 对话结束 → 自动触发战斗
                String autoCombat = dialogueUi.getPendingCombatNpcId();
                if (autoCombat != null) {
                    dialogueUi.clearPendingCombat();
                    encounterUi.forceUtCombat(autoCombat);
                    actionMessage = "战斗开始！";
                }
                return;
            }
            if (engine.isInCombat()) {
                if (engine.isUndertaleCombat()) {
                    int beforeHp = engine.getPlayer().getHp();
                    String msg = encounterUi.handleUtCombatInput(delta, utRenderer.utEngine(engine));
                    if (msg != null) actionMessage = msg;
                    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)
                        || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                        || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                        game.getAudio().playThrottled(Cue.ATTACK, 140L);
                    } else if (engine.getPlayer().getHp() < beforeHp) {
                        game.getAudio().playThrottled(Cue.HIT, 120L);
                    }
                } else {
                    int beforeHp = engine.getPlayer().getHp();
                    String msg = encounterUi.handleCombatInput();
                    if (msg != null) {
                        actionMessage = msg;
                        game.getAudio().play(msg.contains("造成") ? Cue.ATTACK : Cue.CLICK);
                    }
                    if (engine.getPlayer().getHp() < beforeHp) {
                        game.getAudio().play(Cue.HIT);
                    }
                }
                return;
            }
        } else if (lastFrameInDialogueOrCombat) {
            lastFrameInDialogueOrCombat = false;
            room.rebuildNpcs();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) saveGame();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) { loadGame(); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            inventory.toggle();
            actionMessage = inventory.isOpen()
                ? "背包已打开：↑↓选择，U使用，X查看详情" : "背包已关闭";
            game.getAudio().play(inventory.isOpen() ? Cue.MENU_OPEN : Cue.MENU_CLOSE);
        }
        if (inventory.isOpen()) {
            String msg = inventoryInput.handleInput();
            if (msg != null) {
                actionMessage = msg;
                if (isFailureMessage(msg)) {
                    game.getAudio().play(Cue.ERROR);
                    flash(1f, 0.22f, 0.15f, 0.14f);
                } else {
                    game.getAudio().play(Cue.USE);
                    flash(0.45f, 0.9f, 0.55f, 0.12f);
                }
            }
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            worldMapOpen = true;
            actionMessage = "已打开世界地图";
            game.getAudio().play(Cue.MENU_OPEN);
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.J) && movement.canStartAttack()) {
            movement.startAttack(1.0f);
            actionMessage = "攻击！";
            game.getAudio().play(Cue.ATTACK);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && movement.canStartDash()) {
            movement.startDash();
            actionMessage = "冲刺！";
            game.getAudio().play(Cue.DASH);
        }

        movePlayer(delta);

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) actionMessage = engine.look();
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            if (engine.moveBack()) {
                float[] sp = room.loadCurrentRoom(true);
                if (sp != null) {
                    playerX = sp[0];
                    playerY = sp[1];
                    markRoomBanner();
                }
                actionMessage = "已回退至 " + engine.getCurrentRoom().getRoomId();
                game.getAudio().play(Cue.DOOR);
            } else {
                actionMessage = "无法回退";
                game.getAudio().play(Cue.ERROR);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            // 优先检查王座互动和传送阵互动
            float pcx = playerX + PLAYER_W / 2f;
            float pcy = playerY + PLAYER_H / 2f;
            if (engine.tryTriggerEnding(pcx, pcy)) {
                actionMessage = engine.getLastMessage();
                game.getAudio().play(Cue.MENU_OPEN);
                return;
            }
            if (engine.tryTriggerTeleport(pcx, pcy)) {
                actionMessage = engine.getLastMessage();
                game.getAudio().play(Cue.DOOR);
                room.loadCurrentRoom(true);
                return;
            }

            String npcId = npcManager.findNearbyNpcId(playerX, playerY, PLAYER_W, PLAYER_H);
            if (npcId != null) {
                encounterUi.openMenu(npcId);
                actionMessage = "遇到 " + npcId + "。按 1 交流 / 2 战斗 / 3 离开";
                game.getAudio().play(Cue.MENU_OPEN);
            } else {
                String itemId = itemManager.findNearbyItemId(playerX, playerY, PLAYER_W, PLAYER_H);
                if (itemId != null) {
                    String itemName = currentRoomItemName(itemId);
                    if (engine.takeItem(itemId)) {
                        itemManager.rebuildItemPlaceholders();
                        actionMessage = "拾取了 " + itemName;
                        game.getAudio().play(Cue.PICKUP);
                        flash(1f, 0.85f, 0.35f, 0.14f);
                    } else {
                        actionMessage = "拾取失败（可能超重）";
                        game.getAudio().play(Cue.ERROR);
                        flash(1f, 0.22f, 0.15f, 0.14f);
                    }
                }
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            actionMessage = "按 I 打开背包，选择物品后按 U 使用";
            game.getAudio().play(Cue.ERROR);
        }
    }

    private String currentRoomItemName(String itemId)
    {
        if (itemId == null || engine.getCurrentRoom() == null) {
            return itemId;
        }
        for (Item item : engine.getCurrentRoom().getItems()) {
            if (itemId.equals(item.getItemId())) {
                return item.getName();
            }
        }
        return itemId;
    }

    private void handlePauseInput()
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) saveGame();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) loadGame();
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            game.getAudio().play(Cue.CLICK);
            switchToTitle();
        }
    }

    private void movePlayer(float delta)
    {
        boolean w = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean s = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean a = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean d = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        if ((w || s || a || d) && moveLogFrame % 30 == 0) {
            int mr = room.getMap() == null ? 15 : (int)(room.mapPixelHeight() / TILE);
            LOG.info("moveKey: pixel=(" + (int)playerX + "," + (int)playerY
                + ") tile=(" + (int)(playerX/TILE) + "," + movement.gdxYToTiledRow(playerY, mr) + ")");
        }
        moveLogFrame++;

        PlayerMovementController.MoveResult r = movement.applyMovement(
            delta, playerX, playerY, w, s, a, d,
            room.getWallLayer(), room.mapPixelWidth(), room.mapPixelHeight());
        if (r.isMoving && (Math.abs(r.newX - playerX) > 0.01f || Math.abs(r.newY - playerY) > 0.01f)) {
            stepAccumulator += Math.abs(r.newX - playerX) + Math.abs(r.newY - playerY);
            if (stepAccumulator >= 48f) {
                stepAccumulator = 0f;
                game.getAudio().playThrottled(Cue.STEP, 90L);
            }
        }
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
            game.getAudio().play(Cue.SAVE);
            flash(0.45f, 0.9f, 0.55f, 0.13f);
        } catch (Exception e) {
            actionMessage = "存档失败: " + e.getClass().getSimpleName();
            game.getAudio().play(Cue.ERROR);
            flash(1f, 0.22f, 0.15f, 0.16f);
        }
    }

    private void loadGame()
    {
        try {
            GameState state = SaveGameService.load();
            GameEngine loaded = new GameEngine(state.getPlayerName());
            loaded.restoreState(state);
            screenChanged = true;
            game.getAudio().play(Cue.LOAD);
            game.setScreen(new GameScreen(game, batch, loaded,
                state.getPlayerX(), state.getPlayerY(), "已读取存档", state.getFacing()));
            deferDispose();
        } catch (Exception e) {
            actionMessage = "读档失败: " + e.getClass().getSimpleName();
            game.getAudio().play(Cue.ERROR);
            flash(1f, 0.22f, 0.15f, 0.16f);
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

    private void markRoomBanner()
    {
        if (engine.getCurrentRoom() == null) {
            return;
        }
        roomBannerRoomId = engine.getCurrentRoom().getRoomId();
        roomBannerTimer = 2.15f;
    }

    private void drawRoomAtmosphere()
    {
        if (room.getMap() == null || engine.getCurrentRoom() == null) {
            return;
        }

        String roomId = engine.getCurrentRoom().getRoomId();
        Color accent = roomAccent(roomId);
        if (accent == null) {
            return;
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.getWorldCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < 22; i++) {
            float drift = (visualTimer * (7f + i % 5) + i * 13f) % 32f;
            float x = ((i * 97f + roomId.length() * 29f)
                % Math.max(1f, room.mapPixelWidth() - 24f)) + 8f;
            float y = ((i * 53f + roomId.length() * 17f + drift)
                % Math.max(1f, room.mapPixelHeight() - 48f)) + 24f;
            float pulse = 0.42f + 0.28f * (float) Math.sin(visualTimer * 2.4f + i);
            float size = (i % 3 == 0) ? 4f : 2f;
            pixelRect(x, y, size, size, accent.r, accent.g, accent.b, pulse);
        }

        if ("teleport-alcove".equals(roomId)) {
            float cx = room.mapPixelWidth() * 0.5f;
            float cy = room.mapPixelHeight() * 0.52f;
            for (int i = 0; i < 12; i++) {
                float angle = visualTimer * 1.4f + i * 0.52f;
                float radius = 28f + (i % 4) * 8f;
                pixelRect(cx + (float)Math.cos(angle) * radius,
                    cy + (float)Math.sin(angle) * radius, 5f, 5f,
                    0.42f, 0.90f, 1f, 0.52f);
            }
        }

        if ("forge".equals(roomId)) {
            for (int i = 0; i < 10; i++) {
                float x = room.mapPixelWidth() * 0.62f + i * 9f;
                float y = room.mapPixelHeight() * 0.36f
                    + ((visualTimer * 44f + i * 7f) % 58f);
                pixelRect(x, y, 3f, 5f, 1f, 0.42f, 0.10f, 0.72f);
            }
        }

        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private Color roomAccent(String roomId)
    {
        if ("vault".equals(roomId) || "throne-hall".equals(roomId)) {
            return ACCENT_GOLD;
        }
        if ("teleport-alcove".equals(roomId) || "hidden-shrine".equals(roomId)) {
            return ACCENT_ARCANE;
        }
        if ("forge".equals(roomId) || "pub".equals(roomId)) {
            return ACCENT_FIRE;
        }
        if ("garden".equals(roomId) || "outside".equals(roomId)) {
            return ACCENT_GREEN;
        }
        if ("library".equals(roomId) || "theatre".equals(roomId)) {
            return ACCENT_WARM;
        }
        return null;
    }

    private void drawRoomBanner(boolean interactionPromptVisible)
    {
        if (roomBannerTimer <= 0f || roomBannerRoomId == null || paused || inventory.isOpen()
            || interactionPromptVisible || encounterUi.isMenuOpen() || dialogueUi.isActive()
            || engine.isInCombat()) {
            return;
        }

        float width = CameraController.DESIGN_W;
        float height = CameraController.DESIGN_H;
        float panelWidth = draw.grid(Math.min(440f, width - 96f));
        float panelHeight = 54f;
        float panelX = draw.grid((width - panelWidth) / 2f);
        float panelY = draw.grid(height - TOP_BAR_HEIGHT - panelHeight - 18f);
        float alpha = Math.min(1f, Math.min(roomBannerTimer * 1.8f,
            (2.15f - roomBannerTimer) * 5f));
        if (alpha <= 0.03f) {
            return;
        }

        batch.setColor(1f, 1f, 1f, alpha);
        uiSkin.drawButton(batch, panelX, panelY, panelWidth, panelHeight);
        uiSkin.drawOrnamentLine(batch, panelX + 24f, panelY + 12f,
            panelWidth - 48f, 8f);
        batch.setColor(Color.WHITE);

        font.setColor(1f, 0.96f, 0.82f, alpha);
        draw.drawCentered(batch, roomLabel(roomBannerRoomId),
            panelX + panelWidth / 2f, panelY + 40f);
        smallFont.setColor(0.95f, 0.82f, 0.55f, alpha);
        draw.drawCenteredWithSmallFont(batch, roomSubtitle(roomBannerRoomId),
            panelX + panelWidth / 2f, panelY + 21f);
        font.setColor(UI_LIGHT_TEXT);
        smallFont.setColor(UI_DARK_TEXT);
    }

    private String roomLabel(String roomId)
    {
        switch (roomId) {
            case "outside": return "黄昏广场";
            case "theatre": return "旧讲堂";
            case "pub": return "暖灯酒馆";
            case "lab": return "冷光机房";
            case "office": return "档案办公室";
            case "library": return "尘封图书馆";
            case "cellar": return "潮湿地窖";
            case "vault": return "失落金库";
            case "hidden-shrine": return "隐秘神龛";
            case "garden": return "月下庭院";
            case "guard-room": return "王座哨站";
            case "armory": return "旧军械库";
            case "forge": return "铁匠铺";
            case "teleport-alcove": return "传送壁龛";
            case "throne-hall": return "王座大厅";
            default: return roomId;
        }
    }

    private String roomSubtitle(String roomId)
    {
        switch (roomId) {
            case "vault": return "宝石的微光照亮石砖";
            case "hidden-shrine": return "空气里漂着低声回响";
            case "teleport-alcove": return "空间边缘正在轻微颤动";
            case "forge": return "火星从铁砧旁溅落";
            case "guard-room": return "有人在前方守住道路";
            case "throne-hall": return "Realm 的命运在这里收束";
            case "garden": return "草叶在夜风里发亮";
            case "library": return "书页味混着尘埃";
            default: return "继续探索 Chronicle of the Lost Realms";
        }
    }

    private void pixelRect(float x, float y, float width, float height,
        float r, float g, float b, float a)
    {
        shapes.setColor(r, g, b, a);
        shapes.rect(Math.round(x), Math.round(y),
            Math.max(1f, Math.round(width)), Math.max(1f, Math.round(height)));
    }

    private void drawWorldMap(float delta)
    {
        float w = CameraController.DESIGN_W, h = CameraController.DESIGN_H;
        float pw = Math.min(520f, w - 48f), ph = Math.min(420f, h - 88f);
        float px = (w - pw) / 2f, py = (h - ph) / 2f;
        camera.applyFullViewport();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.getUiCamera().combined);
        worldMapRenderer.render(shapes, batch, smallFont, px, py, pw, ph,
            engine.getCurrentRoom().getRoomId(), engine.getExploredRoomIds(),
            engine::isLockUnlocked, delta);
    }

    private void updateDamageFeedback()
    {
        int hp = engine.getPlayer().getHp();
        if (hp < lastObservedHp) {
            game.getAudio().play(Cue.HIT);
            flash(1f, 0.08f, 0.04f, 0.22f);
        }
        lastObservedHp = hp;
    }

    private void updateMusic()
    {
        game.getAudio().playMusic(resolveMusicTrack());
    }

    private Track resolveMusicTrack()
    {
        if (engine.isInCombat()) {
            return Track.COMBAT;
        }

        String roomId = engine.getCurrentRoom() == null
            ? ""
            : engine.getCurrentRoom().getRoomId().toLowerCase();
        if (roomId.contains("cellar")
            || roomId.contains("vault")
            || roomId.contains("guard")
            || roomId.contains("armory")
            || roomId.contains("forge")
            || roomId.contains("teleport")
            || roomId.contains("throne")) {
            return Track.DUNGEON;
        }
        return Track.EXPLORE;
    }

    private void flash(float r, float g, float b, float alpha)
    {
        feedbackFlashColor.set(r, g, b, alpha);
        feedbackFlashTimer = 0.18f;
    }

    private void updateFeedbackFlash(float delta)
    {
        if (feedbackFlashTimer <= 0f) {
            return;
        }
        feedbackFlashTimer = Math.max(0f, feedbackFlashTimer - delta);
    }

    private void drawFeedbackFlash()
    {
        if (feedbackFlashTimer <= 0f) {
            return;
        }
        float ratio = feedbackFlashTimer / 0.18f;
        camera.applyFullViewport();
        shapes.setProjectionMatrix(camera.getUiCamera().combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(feedbackFlashColor.r, feedbackFlashColor.g,
            feedbackFlashColor.b, feedbackFlashColor.a * ratio);
        shapes.rect(0f, 0f, CameraController.DESIGN_W, CameraController.DESIGN_H);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ========== 死亡 / 结局画面 ==========

    private float endingAlpha = 0f;
    private static final float ENDING_FADE_SPEED = 0.6f;

    private void drawGameOverScreen(float delta)
    {
        if (!gameOverAnnounced) {
            gameOverAnnounced = true;
            game.getAudio().stopMusic();
            game.getAudio().play(Cue.HIT);
        }
        gameOverAlpha = Math.min(1f, gameOverAlpha + delta * 1.25f);

        camera.applyFullViewport();
        float w = CameraController.DESIGN_W;
        float h = CameraController.DESIGN_H;
        Gdx.gl.glClearColor(0.015f, 0.01f, 0.015f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.getUiCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.015f, 0.01f, 0.015f, 1f);
        shapes.rect(0f, 0f, w, h);
        shapes.setColor(0.24f, 0.02f, 0.025f, 0.42f * gameOverAlpha);
        shapes.rect(0f, 0f, w, h);
        for (int i = 0; i < 36; i++) {
            float x = (i * 91f + visualTimer * 12f) % w;
            float y = (i * 53f + visualTimer * 18f) % h;
            float size = 2f + (i % 4) * 2f;
            shapes.setColor(0.85f, 0.04f, 0.03f, (0.18f + (i % 3) * 0.06f) * gameOverAlpha);
            shapes.rect(x, y, size, size);
        }

        float cx = w / 2f;
        float heartY = h * 0.68f;
        drawBrokenHeart(cx, heartY, 38f, gameOverAlpha);
        drawFlatline(cx, h * 0.50f, gameOverAlpha);

        float panelW = 560f;
        float panelH = 206f;
        float panelX = (w - panelW) / 2f;
        float panelY = h * 0.20f;
        shapes.setColor(0f, 0f, 0f, 0.70f * gameOverAlpha);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.setColor(0.74f, 0.06f, 0.05f, 0.84f * gameOverAlpha);
        drawFrameFilled(panelX, panelY, panelW, panelH, 3f);
        shapes.end();

        batch.setProjectionMatrix(camera.getUiCamera().combined);
        batch.begin();
        font.setColor(1f, 0.16f, 0.12f, gameOverAlpha);
        layout.setText(font, "你倒下了");
        font.draw(batch, "你倒下了", cx - layout.width / 2f, panelY + panelH - 42f);

        smallFont.setColor(0.9f, 0.82f, 0.78f, gameOverAlpha);
        String body = "失落 Realm 吞没了最后一页。";
        layout.setText(smallFont, body);
        smallFont.draw(batch, body, cx - layout.width / 2f, panelY + panelH - 82f);

        String hint = SaveGameService.hasSave()
            ? "F9 / L 读档    T 返回标题"
            : "暂无存档    T 返回标题";
        smallFont.setColor(1f, 0.74f, 0.32f, gameOverAlpha);
        layout.setText(smallFont, hint);
        smallFont.draw(batch, hint, cx - layout.width / 2f, panelY + 66f);
        if (actionMessage != null && actionMessage.startsWith("读档失败")) {
            smallFont.setColor(1f, 0.42f, 0.36f, gameOverAlpha);
            layout.setText(smallFont, actionMessage);
            smallFont.draw(batch, actionMessage, cx - layout.width / 2f, panelY + 34f);
        }
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)
            || Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            loadGame();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            game.getAudio().play(Cue.CLICK);
            switchToTitle();
        }
    }

    private void drawEndingScreen(EndingType ending, float delta)
    {
        if (activeEndingScreen != ending) {
            activeEndingScreen = ending;
            endingAlpha = 0f;
            game.getAudio().stopMusic();
            game.getAudio().play(Cue.MENU_OPEN);
        }
        endingAlpha = Math.min(1f, endingAlpha + ENDING_FADE_SPEED * delta);

        Gdx.gl.glClearColor(0.01f, 0.01f, 0.015f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.applyFullViewport();
        float w = CameraController.DESIGN_W;
        float h = CameraController.DESIGN_H;
        float cx = w / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.getUiCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawEndingBackdrop(ending, w, h, endingAlpha);
        drawEndingParticles(ending, w, h, endingAlpha);
        drawThroneSilhouette(ending, w, h, endingAlpha);
        shapes.end();

        batch.setProjectionMatrix(camera.getUiCamera().combined);
        batch.begin();

        String title = ending.getTitle();
        GlyphLayout gl = new GlyphLayout(font, title);
        Color titleColor = endingTitleColor(ending);
        font.setColor(titleColor.r, titleColor.g, titleColor.b, endingAlpha);
        font.draw(batch, title, cx - gl.width / 2f, h * 0.94f);

        String desc = ending.getDescription();
        smallFont.setColor(0.92f, 0.88f, 0.78f, endingAlpha);
        smallFont.draw(batch, desc, cx - 420f, h * 0.58f, 840f,
            com.badlogic.gdx.utils.Align.center, true);

        String hint = "按 T 返回标题";
        gl.setText(smallFont, hint);
        smallFont.setColor(1f, 0.9f, 0.58f, 0.72f * endingAlpha);
        smallFont.draw(batch, hint, cx - gl.width / 2f, h * 0.12f);

        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            switchToTitle();
        }
    }

    private void drawEndingBackdrop(EndingType ending, float w, float h, float alpha)
    {
        if (ending == EndingType.LIGHT) {
            shapes.setColor(0.08f, 0.07f, 0.05f, 1f);
            shapes.rect(0f, 0f, w, h);
            shapes.setColor(0.95f, 0.68f, 0.18f, 0.24f * alpha);
            shapes.rect(0f, h * 0.50f, w, h * 0.50f);
            for (int i = 0; i < 9; i++) {
                float beamX = i * w / 8f - 18f;
                shapes.setColor(1f, 0.86f, 0.34f, (0.07f + i % 2 * 0.05f) * alpha);
                shapes.rect(beamX, 0f, 36f, h);
            }
        } else if (ending == EndingType.SHADOW) {
            shapes.setColor(0.015f, 0.01f, 0.018f, 1f);
            shapes.rect(0f, 0f, w, h);
            shapes.setColor(0.42f, 0.02f, 0.04f, 0.36f * alpha);
            shapes.rect(0f, 0f, w, h);
            for (int i = 0; i < 10; i++) {
                float x = i * 108f - 40f;
                shapes.setColor(0f, 0f, 0f, 0.46f * alpha);
                shapes.triangle(x, 0f, x + 92f, 0f, x + 42f, h * (0.35f + (i % 3) * 0.08f));
            }
        } else if (ending == EndingType.FAKE) {
            // 假结局：灰白褪色，王座如幻影
            shapes.setColor(0.12f, 0.12f, 0.14f, 1f);
            shapes.rect(0f, 0f, w, h);
            shapes.setColor(0.68f, 0.66f, 0.62f, 0.08f * alpha);
            shapes.rect(0f, 0f, w, h);
        } else {
            shapes.rect(0f, 0f, w, h);
            shapes.setColor(0.34f, 0.34f, 0.32f, 0.20f * alpha);
            shapes.rect(0f, h * 0.48f, w, h * 0.52f);
            for (int i = 0; i < 12; i++) {
                float x = i * 86f;
                shapes.setColor(0.11f, 0.12f, 0.13f, 0.72f * alpha);
                shapes.rect(x, h * 0.10f, 42f, h * 0.42f);
            }
        }
    }

    private void drawEndingParticles(EndingType ending, float w, float h, float alpha)
    {
        for (int i = 0; i < 72; i++) {
            float x = (i * 79f + visualTimer * (ending == EndingType.SHADOW ? 9f : 18f)) % w;
            float yBase = (i * 43f) % h;
            float y = ending == EndingType.SHADOW
                ? h - ((yBase + visualTimer * 34f) % h)
                : (yBase + visualTimer * (ending == EndingType.NEUTRAL ? 22f : 14f)) % h;
            float size = 2f + (i % 4);
            if (ending == EndingType.LIGHT) {
                shapes.setColor(1f, 0.82f, 0.24f, (0.30f + (i % 3) * 0.09f) * alpha);
                shapes.rect(x, y, size, size);
            } else if (ending == EndingType.SHADOW) {
                shapes.setColor(0.90f, 0.05f, 0.06f, (0.18f + (i % 3) * 0.07f) * alpha);
                shapes.rect(x, y, size + 1f, size + 5f);
            } else if (ending == EndingType.FAKE) {
                // 假结局：灰烬粒子缓慢向上飘散
                float ay = h - ((yBase + visualTimer * 6f) % h);
                shapes.setColor(0.45f, 0.44f, 0.46f, (0.10f + (i % 5) * 0.04f) * alpha);
                shapes.rect(x, ay, 3f, 3f);
            } else {
                shapes.setColor(0.78f, 0.74f, 0.62f, (0.22f + (i % 4) * 0.05f) * alpha);
                shapes.rect(x, y, 16f + (i % 3) * 4f, 10f);
                shapes.setColor(0.32f, 0.31f, 0.28f, 0.24f * alpha);
                shapes.rect(x + 3f, y + 5f, 10f, 1f);
            }
        }
    }

    private void drawThroneSilhouette(EndingType ending, float w, float h, float alpha)
    {
        float cx = w / 2f;
        float baseY = h * 0.13f;
        float throneW = 176f;
        Color glow = endingTitleColor(ending);
        shapes.setColor(glow.r, glow.g, glow.b, 0.14f * alpha);
        shapes.circle(cx, baseY + 132f, 176f, 30);
        shapes.setColor(0.02f, 0.018f, 0.02f, 0.88f * alpha);
        shapes.rect(cx - throneW / 2f, baseY, throneW, 138f);
        shapes.rect(cx - throneW * 0.34f, baseY + 126f, throneW * 0.68f, 92f);
        shapes.rect(cx - throneW * 0.52f, baseY + 18f, 34f, 92f);
        shapes.rect(cx + throneW * 0.33f, baseY + 18f, 34f, 92f);
        shapes.triangle(cx - 62f, baseY + 218f, cx, baseY + 258f, cx + 62f, baseY + 218f);
        shapes.setColor(glow.r, glow.g, glow.b, 0.48f * alpha);
        drawFrameFilled(cx - throneW / 2f, baseY, throneW, 138f, 3f);
    }

    private void drawBrokenHeart(float x, float y, float r, float alpha)
    {
        shapes.setColor(0f, 0f, 0f, 0.70f * alpha);
        shapes.circle(x - r * 0.42f, y + r * 0.20f, r * 0.56f, 16);
        shapes.circle(x + r * 0.42f, y + r * 0.20f, r * 0.56f, 16);
        shapes.triangle(x - r, y + r * 0.1f, x + r, y + r * 0.1f, x, y - r * 1.15f);
        shapes.setColor(0.94f, 0.04f, 0.04f, alpha);
        shapes.circle(x - r * 0.42f, y + r * 0.20f, r * 0.48f, 16);
        shapes.circle(x + r * 0.42f, y + r * 0.20f, r * 0.48f, 16);
        shapes.triangle(x - r * 0.88f, y + r * 0.08f,
            x + r * 0.88f, y + r * 0.08f, x, y - r);
        shapes.setColor(0.02f, 0.01f, 0.015f, alpha);
        shapes.rect(x - 3f, y - r * 0.66f, 6f, r * 1.55f);
        shapes.rect(x + 8f, y - r * 0.30f, 6f, r * 0.58f);
    }

    private void drawFlatline(float x, float y, float alpha)
    {
        shapes.setColor(1f, 0.10f, 0.08f, 0.74f * alpha);
        shapes.rect(x - 210f, y, 96f, 4f);
        shapes.rect(x - 112f, y, 32f, 4f);
        shapes.rect(x - 82f, y, 4f, 36f);
        shapes.rect(x - 82f, y + 32f, 36f, 4f);
        shapes.rect(x - 46f, y - 24f, 4f, 60f);
        shapes.rect(x - 46f, y - 24f, 36f, 4f);
        shapes.rect(x - 10f, y - 24f, 4f, 28f);
        shapes.rect(x - 6f, y, 216f, 4f);
    }

    private Color endingTitleColor(EndingType ending)
    {
        if (ending == EndingType.LIGHT) {
            return new Color(1f, 0.84f, 0.24f, 1f);
        }
        if (ending == EndingType.SHADOW) {
            return new Color(1f, 0.18f, 0.12f, 1f);
        }
        if (ending == EndingType.NEUTRAL) {
            return new Color(0.88f, 0.78f, 0.52f, 1f);
        }
        return new Color(0.52f, 0.52f, 0.54f, 1f);  // FAKE: 灰白色
    }

    private boolean isFailureMessage(String message)
    {
        if (message == null) {
            return false;
        }
        return message.contains("不能")
            || message.contains("不可")
            || message.contains("没有")
            || message.contains("失败")
            || message.contains("需位置")
            || message.contains("已经打开");
    }
}
