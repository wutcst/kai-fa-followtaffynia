package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.RpgMain;
import cn.edu.whut.sept.zuul.client.audio.GameAudio.Cue;
import cn.edu.whut.sept.zuul.client.audio.GameAudio.Track;
import cn.edu.whut.sept.zuul.client.ui.GameUiSkin;
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
    private final GameUiSkin uiSkin;
    private final GlyphLayout layout;
    private final CameraController camera;
    private final InputAdapter inputAdapter;
    private String playerName;
    private String statusMessage;
    private boolean screenChanged;

    public TitleScreen(RpgMain game, SpriteBatch batch)
    {
        this.game = game;
        this.batch = batch;
        this.font = game.getFonts().copyDefault(1.2f);
        this.smallFont = game.getFonts().copyDefault(0.86f);
        this.uiSkin = new GameUiSkin();
        this.layout = new GlyphLayout();
        this.camera = new CameraController();
        this.playerName = DEFAULT_NAME;
        this.statusMessage = SaveGameService.hasSave() ? "按 L 读取存档" : "暂无存档";
        this.inputAdapter = new InputAdapter()
        {
            @Override
            public boolean keyTyped(char character)
            {
                if (character >= 32 && playerName.length() < MAX_NAME_LENGTH) {
                    playerName += character;
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyDown(int keycode)
            {
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
                    game.getAudio().play(Cue.LOAD);
                    loadGame();
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

        camera.applyFullViewport();
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float width = CameraController.DESIGN_W;
        float height = CameraController.DESIGN_H;
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

    private void loadGame()
    {
        try {
            GameState state = SaveGameService.load();
            GameEngine engine = new GameEngine(state.getPlayerName());
            engine.restoreState(state);
            Gdx.app.log(LOG_TAG, "Loaded game from title: " + SaveGameService.defaultSavePath());
            switchToGame(new GameScreen(game, batch, engine,
                state.getPlayerX(), state.getPlayerY(), "已读取存档", state.getFacing()));
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
        uiSkin.dispose();
    }

    private void updateCamera()
    {
        camera.update(1f, 1f);
    }
}
