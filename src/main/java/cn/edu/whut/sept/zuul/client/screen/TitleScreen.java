package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.RpgMain;
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

    private final RpgMain game;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final InputAdapter inputAdapter;
    private String playerName;
    private String statusMessage;

    public TitleScreen(RpgMain game, SpriteBatch batch)
    {
        this.game = game;
        this.batch = batch;
        this.font = game.getFonts().copyDefault(1.2f);
        this.layout = new GlyphLayout();
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
                    return true;
                }
                if (keycode == Input.Keys.ENTER) {
                    startGame();
                    return true;
                }
                if (keycode == Input.Keys.L) {
                    loadGame();
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    public void show()
    {
        Gdx.input.setInputProcessor(inputAdapter);
    }

    @Override
    public void render(float delta)
    {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        font.setColor(Color.WHITE);
        drawCentered("Chronicle of the Lost Realms", Gdx.graphics.getHeight() - 80);
        drawCentered("失落 Realm 编年史", Gdx.graphics.getHeight() - 110);
        drawCentered("姓名: " + playerName, Gdx.graphics.getHeight() / 2f);
        drawCentered("直接输入文字修改姓名，Enter 开始新游戏", 130);
        drawCentered(statusMessage, 95);
        batch.end();
    }

    private void startGame()
    {
        if (playerName.trim().isEmpty()) {
            playerName = DEFAULT_NAME;
        }
        GameEngine engine = new GameEngine(playerName);
        Gdx.app.log(LOG_TAG, "Start new game as " + engine.getPlayer().getName());
        game.setScreen(new GameScreen(game, batch, engine));
    }

    private void loadGame()
    {
        try {
            GameState state = SaveGameService.load();
            GameEngine engine = new GameEngine(state.getPlayerName());
            engine.restoreState(state);
            Gdx.app.log(LOG_TAG, "Loaded game from title: " + SaveGameService.defaultSavePath());
            game.setScreen(new GameScreen(game, batch, engine,
                state.getPlayerX(), state.getPlayerY(), "已读取存档"));
        } catch (Exception e) {
            Gdx.app.error(LOG_TAG, "Load from title failed", e);
            statusMessage = "读档失败: " + e.getClass().getSimpleName();
        }
    }

    private void drawCentered(String text, float y)
    {
        layout.setText(font, text);
        font.draw(batch, text, (Gdx.graphics.getWidth() - layout.width) / 2f, y);
    }

    @Override
    public void resize(int width, int height)
    {
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
    }
}
