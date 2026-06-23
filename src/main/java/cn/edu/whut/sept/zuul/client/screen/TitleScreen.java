package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.RpgMain;
import cn.edu.whut.sept.zuul.client.audio.GameAudio.Cue;
import cn.edu.whut.sept.zuul.client.audio.GameAudio.Track;
import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
import cn.edu.whut.sept.zuul.client.ui.UiDrawUtils;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.SaveGameService;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * 标题画面：输入玩家姓名并开始新游戏。
 */
public class TitleScreen implements Screen
{
    private static final String LOG_TAG = "TitleScreen";
    private static final String DEFAULT_NAME = "编年史者";
    private static final int MAX_NAME_LENGTH = 12;
    private static final float UI_GRID = 8f;
    private static final float UI_PAD = 24f;
    private static final Color LIGHT_TEXT = new Color(1f, 0.96f, 0.84f, 1f);
    private static final Color DARK_TEXT = new Color(0.28f, 0.19f, 0.1f, 1f);

    private final RpgMain game;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final ShapeRenderer shapes;
    private final GameUiSkin uiSkin;
    private final GlyphLayout layout;
    private final CameraController camera;
    private final UiDrawUtils draw;
    private final SaveLoadMenu saveLoadMenu;
    private final InputAdapter inputAdapter;
    private String playerName;
    private String statusMessage;
    private float visualTimer;
    private boolean screenChanged;

    public TitleScreen(RpgMain game, SpriteBatch batch)
    {
        this.game = game;
        this.batch = batch;
        this.font = game.getFonts().copyDefault(1.2f);
        this.smallFont = game.getFonts().copyDefault(0.86f);
        this.shapes = new ShapeRenderer();
        this.uiSkin = new GameUiSkin();
        this.layout = new GlyphLayout();
        this.camera = new CameraController();
        this.draw = new UiDrawUtils(font, smallFont, uiSkin, this.layout,
            LIGHT_TEXT, DARK_TEXT, UI_GRID);
        this.saveLoadMenu = new SaveLoadMenu(font, smallFont, uiSkin, draw, camera,
            batch, shapes, game.getAudio());
        this.playerName = DEFAULT_NAME;
        this.statusMessage = SaveGameService.hasAnySave() ? "按 L 读取存档" : "暂无存档";
        this.inputAdapter = new InputAdapter()
        {
            @Override
            public boolean keyTyped(char character)
            {
                if (saveLoadMenu.isOpen()) {
                    return false;
                }
                if (character >= 32 && playerName.length() < MAX_NAME_LENGTH) {
                    playerName += character;
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyDown(int keycode)
            {
                if (saveLoadMenu.isOpen()) {
                    return false;
                }
                if (keycode == Input.Keys.BACKSPACE && playerName.length() > 0) {
                    playerName = playerName.substring(0, playerName.length() - 1);
                    game.getAudio().playThrottled(Cue.CLICK, 40L);
                    return true;
                }
                if (keycode == Input.Keys.ENTER) {
                    game.getAudio().play(Cue.CLICK);
                    startGame();
                    return true;
                }
                if (keycode == Input.Keys.L) {
                    if (SaveGameService.hasAnySave()) {
                        saveLoadMenu.open(SaveLoadMenu.Mode.LOAD);
                    } else {
                        statusMessage = "暂无存档";
                        game.getAudio().play(Cue.ERROR);
                    }
                    return true;
                }
                return false;
            }
        };
        updateCamera();
    }

    @Override
    public void show()
    {
        game.getAudio().playMusic(Track.TITLE);
        Gdx.input.setInputProcessor(inputAdapter);
    }

    @Override
    public void render(float delta)
    {
        if (screenChanged) {
            return;
        }

        if (saveLoadMenu.isOpen()) {
            handleSaveLoadMenu();
            if (screenChanged) {
                return;
            }
        }

        camera.applyFullViewport();
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float width = CameraController.DESIGN_W;
        float height = CameraController.DESIGN_H;
        visualTimer += delta;
        drawTitleBackdrop(width, height);

        float panelWidth = grid(Math.min(544f, width - 96f));
        float panelHeight = grid(Math.min(368f, height - 88f));
        float panelX = grid((width - panelWidth) / 2f);
        float panelY = grid((height - panelHeight) / 2f);
        float centerX = panelX + panelWidth / 2f;

        batch.setProjectionMatrix(camera.getUiCamera().combined);
        batch.begin();
        uiSkin.drawWindow(batch, panelX, panelY, panelWidth, panelHeight);
        uiSkin.drawInset(batch, panelX + UI_PAD * 2f, panelY + 152f,
            panelWidth - UI_PAD * 4f, 64f);
        uiSkin.drawLightButton(batch, panelX + 104f, panelY + 70f, panelWidth - 208f, 44f);

        font.setColor(LIGHT_TEXT);
        drawCentered("Chronicle of the Lost Realms", centerX, panelY + panelHeight - 64f);
        drawCentered("失落 Realm 编年史", centerX, panelY + panelHeight - 96f);

        font.setColor(DARK_TEXT);
        drawCentered("姓名: " + playerName, centerX, panelY + 192f);
        drawCenteredInBox("Enter 开始新游戏", panelX + 104f, panelY + 70f,
            panelWidth - 208f, 44f);

        smallFont.setColor(LIGHT_TEXT);
        drawCenteredSmall("直接输入文字修改姓名", centerX, panelY + 136f);
        drawCenteredSmall(statusMessage, centerX, panelY + 42f);
        batch.end();

        if (saveLoadMenu.isOpen()) {
            saveLoadMenu.render();
        }
    }

    private void drawTitleBackdrop(float width, float height)
    {
        shapes.setProjectionMatrix(camera.getUiCamera().combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        fill(0, 0, width, height, 0.05f, 0.06f, 0.11f, 1f);
        fill(0, 0, width, height * 0.45f, 0.08f, 0.11f, 0.12f, 1f);
        fill(0, 0, width, 88f, 0.11f, 0.12f, 0.1f, 1f);

        float moonX = grid(width - 152f);
        float moonY = grid(height - 118f);
        fill(moonX, moonY, 48f, 48f, 0.91f, 0.78f, 0.43f, 1f);
        fill(moonX + 8f, moonY + 8f, 32f, 32f, 1f, 0.9f, 0.55f, 1f);
        fill(moonX + 32f, moonY + 6f, 12f, 12f, 0.05f, 0.06f, 0.11f, 1f);

        for (int i = 0; i < 34; i++) {
            float x = grid((i * 83f + 29f) % Math.max(1f, width));
            float y = grid(height * 0.54f + (i * 37f) % Math.max(1f, height * 0.38f));
            float sparkle = ((int)(visualTimer * 2f) + i) % 4 == 0 ? 4f : 2f;
            fill(x, y, sparkle, sparkle, 0.86f, 0.78f, 0.53f, 0.85f);
        }

        float horizon = grid(height * 0.38f);
        fill(0, horizon, width, 18f, 0.13f, 0.13f, 0.18f, 1f);
        for (int i = 0; i < 14; i++) {
            float towerW = 42f + (i % 3) * 14f;
            float towerH = 64f + (i % 4) * 18f;
            float x = grid(i * 84f - 24f);
            fill(x, horizon, towerW, towerH, 0.10f, 0.11f, 0.16f, 1f);
            fill(x + 8f, horizon + towerH, towerW - 16f, 14f, 0.08f, 0.09f, 0.13f, 1f);
            fill(x + towerW / 2f - 4f, horizon + 18f, 8f, 12f, 0.9f, 0.54f, 0.18f, 0.7f);
        }

        float roadCenter = width / 2f;
        fill(roadCenter - 150f, 0, 300f, 104f, 0.24f, 0.19f, 0.13f, 1f);
        for (int row = 0; row < 7; row++) {
            float y = row * 16f;
            float offset = row % 2 == 0 ? 0f : 20f;
            for (float x = roadCenter - 148f - offset; x < roadCenter + 150f; x += 40f) {
                fill(x, y, 36f, 2f, 0.13f, 0.1f, 0.08f, 0.7f);
                fill(x, y + 14f, 36f, 2f, 0.33f, 0.27f, 0.18f, 0.65f);
            }
        }

        for (int side = -1; side <= 1; side += 2) {
            float x = roadCenter + side * 204f;
            float flame = 10f + (((int)(visualTimer * 8f) + side) & 1) * 4f;
            fill(x, 76f, 8f, 76f, 0.18f, 0.11f, 0.06f, 1f);
            fill(x - 12f, 144f, 32f, 12f, 0.28f, 0.15f, 0.07f, 1f);
            fill(x - flame / 2f + 4f, 156f, flame, 24f, 0.95f, 0.52f, 0.12f, 0.9f);
            fill(x - 4f, 162f, 12f, 16f, 1f, 0.86f, 0.34f, 1f);
        }

        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void fill(float x, float y, float width, float height,
        float r, float g, float b, float a)
    {
        shapes.setColor(r, g, b, a);
        shapes.rect(grid(x), grid(y),
            Math.max(1f, Math.round(width)), Math.max(1f, Math.round(height)));
    }

    private void startGame()
    {
        if (playerName.trim().isEmpty()) {
            playerName = DEFAULT_NAME;
        }
        GameEngine engine = new GameEngine(playerName);
        Gdx.app.log(LOG_TAG, "Start new game as " + engine.getPlayer().getName());
        switchToGame(new GameScreen(game, batch, engine));
    }

    /** 处理槽位面板返回的操作（确认读取 / 取消）。 */
    private void handleSaveLoadMenu()
    {
        SaveLoadMenu.Result result = saveLoadMenu.handleInput();
        if (result.type == SaveLoadMenu.ResultType.CONFIRM) {
            loadGame(result.slot);
        } else if (result.type == SaveLoadMenu.ResultType.CANCEL) {
            statusMessage = "按 L 读取存档";
        }
    }

    private void loadGame(int slot)
    {
        try {
            GameState state = SaveGameService.load(slot);
            GameEngine engine = new GameEngine(state.getPlayerName());
            engine.restoreState(state);
            Gdx.app.log(LOG_TAG, "Loaded game from title slot " + slot);
            game.getAudio().play(Cue.LOAD);
            switchToGame(new GameScreen(game, batch, engine,
                state.getPlayerX(), state.getPlayerY(), "已读取存档 " + slot, state.getFacing()));
        } catch (Exception e) {
            Gdx.app.error(LOG_TAG, "Load from title failed", e);
            statusMessage = "读档失败: " + e.getClass().getSimpleName();
            game.getAudio().play(Cue.ERROR);
        }
    }

    private void switchToGame(GameScreen screen)
    {
        screenChanged = true;
        game.setScreen(screen);
        disposeLater();
    }

    private void disposeLater()
    {
        final TitleScreen oldScreen = this;
        Gdx.app.postRunnable(new Runnable()
        {
            @Override
            public void run()
            {
                oldScreen.dispose();
            }
        });
    }

    private void drawCentered(String text, float centerX, float y)
    {
        layout.setText(font, text);
        font.draw(batch, text, Math.round(centerX - layout.width / 2f), Math.round(y));
    }

    private void drawCenteredInBox(String text, float x, float y, float width, float height)
    {
        layout.setText(font, text);
        font.draw(batch, text, Math.round(x + (width - layout.width) / 2f),
            Math.round(y + (height + layout.height) / 2f + 1f));
    }

    private void drawCenteredSmall(String text, float centerX, float y)
    {
        layout.setText(smallFont, text);
        smallFont.draw(batch, text, Math.round(centerX - layout.width / 2f), Math.round(y));
    }

    private float grid(float value)
    {
        return Math.round(value / UI_GRID) * UI_GRID;
    }

    @Override
    public void resize(int width, int height)
    {
        updateCamera();
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
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose()
    {
        font.dispose();
        smallFont.dispose();
        shapes.dispose();
        uiSkin.dispose();
    }

    private void updateCamera()
    {
        camera.update(1f, 1f);
    }
}
