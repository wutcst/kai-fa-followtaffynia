package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.RpgMain;
import cn.edu.whut.sept.zuul.client.audio.GameAudio.Cue;
import cn.edu.whut.sept.zuul.client.audio.GameAudio.Track;
import cn.edu.whut.sept.zuul.client.render.PlayerRenderer;
import cn.edu.whut.sept.zuul.client.render.UtCombatRenderer;
import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
import cn.edu.whut.sept.zuul.client.ui.UiDrawUtils;
import cn.edu.whut.sept.zuul.client.ui.WorldMapRenderer;
import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatEngine;
import cn.edu.whut.sept.zuul.infra.GameLogger;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.SaveGameService;

import java.util.logging.Logger;
import java.util.List;
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
    private static final float DASH_COOLDOWN = 0.2f;
    private static final float ATTACK_COOLDOWN = 0.2f;
    private static final float UI_GRID = 8f;
    private static final float TOP_BAR_HEIGHT = 56f;
    private static final float FOOTER_HEIGHT = 96f;
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
    private boolean lastFrameInDialogueOrCombat;
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
    private final Color feedbackFlashColor;

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
        this.lastObservedHp = engine.getPlayer().getHp();
        this.feedbackFlashColor = new Color(1f, 1f, 1f, 0f);

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
    }

    // ==================== render ====================

    @Override
    public void render(float delta)
    {
        handleInput(delta);
        if (screenChanged) return;
        updateMusic();

        if (!paused) {
            if (room.getExitCooldown() > 0f) room.setExitCooldown(room.getExitCooldown() - delta);
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
            }
        }
        updateDamageFeedback();
        updateFeedbackFlash(delta);

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
        interaction.drawPlayer(playerX, playerY);
        interaction.drawInteractionPrompt(playerX, playerY, paused, inventory.isOpen(),
            encounterUi.isMenuOpen(), dialogueUi.isActive(),
            engine.isInDialogue(), engine.isInCombat());

        // UI rendering
        camera.applyFullViewport();
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
            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();
            inventory.setWorldViewportHeight(camera.getWorldViewportHeight());
            hud.drawUiPanels(w, h, paused, inventory.isOpen(),
                inventory.inventoryPanelHeight());
            hud.drawHud(w, h, paused, actionMessage);
            if (!paused && !inventory.isOpen()) {
                hud.drawQuestTracker(w, h, camera.getWorldViewportX(), camera.getWorldViewportWidth());
            }
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
        drawFeedbackFlash();
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

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();
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

    private static final float DIALOG_PORTRAIT_W = 96f;
    private static final float DIALOG_PORTRAIT_H = 96f;

    private void renderDialogueOverlay()
    {
        Dialogue d = dialogueUi.getActiveDialogue();
        if (d == null) return;

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();
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

        // NPC 立绘：玩家选择时灰，NPC 说话时亮
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(showingChoice ? 0.12f : 0.3f,
                        showingChoice ? 0.12f : 0.3f,
                        showingChoice ? 0.12f : 0.5f,
                        showingChoice ? 0.6f : 1f);
        shapes.rect(npcPx, npcPy, DIALOG_PORTRAIT_W, DIALOG_PORTRAIT_H);
        shapes.end();

        // 主角立绘：玩家选择时亮，NPC 说话时灰
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(showingChoice ? 0.4f : 0.12f,
                        showingChoice ? 0.6f : 0.12f,
                        showingChoice ? 0.3f : 0.12f,
                        showingChoice ? 1f : 0.6f);
        shapes.rect(playerPx, playerPy, DIALOG_PORTRAIT_W, DIALOG_PORTRAIT_H);
        shapes.end();

        // 文字
        batch.setProjectionMatrix(camera.getUiCamera().combined);
        batch.begin();
        float textX = npcPx + DIALOG_PORTRAIT_W + 16f;
        float textW = playerPx - textX - 16f;

        String npcName = d.getNpcId();

        if (showingChoice) {
            // 玩家刚说的话 — 绿色高亮
            float playerTextY = boxY + boxH - 20f;
            smallFont.setColor(0.6f, 1f, 0.5f, 1f);
            smallFont.draw(batch, "你", textX, playerTextY + 14f);
            smallFont.draw(batch, "\"" + playerChoice + "\"", textX, playerTextY, textW,
                com.badlogic.gdx.utils.Align.left, true);

            // NPC 回应（如果有的话）
            String npcText = dialogueUi.formatDialogue(d);
            int ln = npcText.lastIndexOf('\n');
            String npcBody = ln > 0 ? npcText.substring(0, ln) : npcText;
            if (npcBody != null && !npcBody.trim().isEmpty()) {
                float npcTextY = boxY + boxH - 52f;
                font.setColor(0.6f, 0.6f, 0.6f, 1f);
                font.draw(batch, npcName, textX, npcTextY + 14f);
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, npcBody, textX, npcTextY, textW,
                    com.badlogic.gdx.utils.Align.left, true);
            }
        } else {
            // NPC 说话
            float textY = boxY + boxH - 24f;
            font.setColor(Color.WHITE);
            font.draw(batch, npcName, textX, textY + 16f);

            String text = dialogueUi.formatDialogue(d);
            int ln = text.lastIndexOf('\n');
            String body = ln > 0 ? text.substring(0, ln) : text;
            smallFont.draw(batch, body, textX, textY, textW,
                com.badlogic.gdx.utils.Align.left, true);

            // 如果有选项且已翻完所有文字，显示在底部
            if (dialogueUi.isAtChoicePoint()) {
                List<String> opts = d.getOptionTexts();
                StringBuilder optLine = new StringBuilder();
                for (int i = 0; i < opts.size(); i++) {
                    optLine.append(i + 1).append(". ").append(opts.get(i)).append("  ");
                }
                float optY = boxY + 26f;
                smallFont.setColor(1f, 0.85f, 0.3f, 1f);
                smallFont.draw(batch, optLine.toString(), textX, optY, textW,
                    com.badlogic.gdx.utils.Align.left, true);
            }
        }
        batch.end();

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
                if (sp != null) { playerX = sp[0]; playerY = sp[1]; }
                actionMessage = "已回退至 " + engine.getCurrentRoom().getRoomId();
                game.getAudio().play(Cue.DOOR);
            } else {
                actionMessage = "无法回退";
                game.getAudio().play(Cue.ERROR);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            String npcId = npcManager.findNearbyNpcId(playerX, playerY, PLAYER_W, PLAYER_H);
            if (npcId != null) {
                encounterUi.openMenu(npcId);
                actionMessage = "遇到 " + npcId + "。按 1 交流 / 2 杀害 / 3 离开 / 4 UT战斗";
                game.getAudio().play(Cue.MENU_OPEN);
            } else {
                String itemId = itemManager.findNearbyItemId(playerX, playerY, PLAYER_W, PLAYER_H);
                if (itemId != null) {
                    if (engine.takeItem(itemId)) {
                        itemManager.rebuildItemPlaceholders();
                        actionMessage = "拾取了 " + itemId;
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
            int mr = room.getMap() == null ? 17 : (int)(room.mapPixelHeight() / TILE);
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
        shapes.rect(0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
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
