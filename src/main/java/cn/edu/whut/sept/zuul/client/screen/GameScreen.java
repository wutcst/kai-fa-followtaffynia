package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.RpgMain;
import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.RoomScene;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.infra.GameState;
import cn.edu.whut.sept.zuul.infra.SaveGameService;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * 主玩法画面（第一步：占位渲染 + 方向键切换房间 + 本地移动）。
 */
public class GameScreen implements Screen
{
    private static final float TILE_SIZE = 32f;
    private static final String LOG_TAG = "GameScreen";
    private static final String CONTROL_HINT =
        "WASD 移动 | 方向键切换房间 | Q 调查 | E 拾取 | I 背包 | B 回退 | F5 存档 | F9 读档 | ESC 菜单";

    private final RpgMain game;
    private final SpriteBatch batch;
    private final GameEngine engine;
    private final BitmapFont font;
    private final ShapeRenderer shapes;

    private float playerX;
    private float playerY;
    private String actionMessage;
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
        this.shapes = new ShapeRenderer();
        this.actionMessage = initialStatus;

        if (Float.isNaN(savedPlayerX) || Float.isNaN(savedPlayerY)) {
            snapToSpawn();
        } else {
            playerX = savedPlayerX;
            playerY = savedPlayerY;
            clampPlayer();
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

        Gdx.gl.glClearColor(0.12f, 0.18f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawPlaceholderMap();
        drawPlayer();
        if (paused) {
            drawPauseOverlay();
        }
        if (inventoryOpen) {
            drawInventoryOverlay();
        }

        batch.begin();
        font.setColor(Color.WHITE);
        drawHud();
        drawFooter();
        batch.end();
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

        float speed = 128f * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            playerY += speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            playerY -= speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            playerX -= speed;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            playerX += speed;
        }
        clampPlayer();

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            changeRoom(Direction.NORTH);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            changeRoom(Direction.SOUTH);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            changeRoom(Direction.WEST);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            changeRoom(Direction.EAST);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            actionMessage = engine.look();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            if (engine.moveBack()) {
                snapToSpawn();
                actionMessage = "已回退至 " + engine.getCurrentRoom().getRoomId();
            } else {
                actionMessage = "无法回退";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            actionMessage = tryTakeFirstItem();
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

    private void changeRoom(Direction direction)
    {
        if (engine.movePlayer(direction)) {
            snapToSpawn();
            actionMessage = "进入 " + engine.getCurrentRoom().getRoomId();
        } else {
            actionMessage = "该方向无法前进";
        }
    }

    private void snapToSpawn()
    {
        RoomScene.SpawnPoint spawn = engine.resolveCurrentSpawn();
        playerX = spawn.tileX * TILE_SIZE;
        playerY = spawn.tileY * TILE_SIZE;
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

    private void clampPlayer()
    {
        playerX = Math.max(TILE_SIZE, Math.min(playerX, Gdx.graphics.getWidth() - TILE_SIZE * 2));
        playerY = Math.max(TILE_SIZE, Math.min(playerY, Gdx.graphics.getHeight() - TILE_SIZE * 2));
    }

    private void drawPlaceholderMap()
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.25f, 0.35f, 0.22f, 1f);
        shapes.rect(TILE_SIZE, TILE_SIZE, Gdx.graphics.getWidth() - TILE_SIZE * 2,
            Gdx.graphics.getHeight() - TILE_SIZE * 2);
        shapes.setColor(0.18f, 0.18f, 0.22f, 1f);
        for (int x = 0; x < Gdx.graphics.getWidth(); x += (int) TILE_SIZE) {
            for (int y = 0; y < Gdx.graphics.getHeight(); y += (int) TILE_SIZE) {
                if (x < TILE_SIZE || y < TILE_SIZE
                    || x > Gdx.graphics.getWidth() - TILE_SIZE * 2
                    || y > Gdx.graphics.getHeight() - TILE_SIZE * 2) {
                    shapes.rect(x, y, TILE_SIZE, TILE_SIZE);
                }
            }
        }
        shapes.end();
    }

    private void drawPauseOverlay()
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.08f, 0.12f, 1f);
        shapes.rect(260, 155, 440, 230);
        shapes.setColor(0.18f, 0.20f, 0.28f, 1f);
        shapes.rect(268, 163, 424, 214);
        shapes.end();
    }

    private void drawInventoryOverlay()
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.08f, 0.12f, 1f);
        shapes.rect(80, 76, Gdx.graphics.getWidth() - 160, 150);
        shapes.setColor(0.16f, 0.18f, 0.24f, 1f);
        shapes.rect(88, 84, Gdx.graphics.getWidth() - 176, 134);
        shapes.end();
    }

    private void drawPlayer()
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.3f, 0.55f, 0.95f, 1f);
        shapes.rect(playerX, playerY, TILE_SIZE, TILE_SIZE);
        shapes.end();
    }

    private void drawHud()
    {
        font.draw(batch, "玩家: " + engine.getPlayer().getName(), 16, Gdx.graphics.getHeight() - 20);
        font.draw(batch, "房间: " + engine.getCurrentRoom().getRoomId(), 16, Gdx.graphics.getHeight() - 45);
        font.draw(batch, "HP: " + engine.getPlayer().getHp() + " / " + engine.getPlayer().getMaxHp(),
            16, Gdx.graphics.getHeight() - 70);
        font.draw(batch, "声望: " + engine.getPlayer().getReputation(), 16, Gdx.graphics.getHeight() - 95);
        font.draw(batch, "负重: " + engine.getPlayer().totalWeight() + " / "
            + engine.getPlayer().getMaxWeight(), 16, Gdx.graphics.getHeight() - 120);

        if (paused) {
            font.draw(batch, "暂停菜单", 410, 340);
            font.draw(batch, "ESC 继续  |  F5 存档  |  F9 读档  |  T 返回标题", 300, 290);
        }
        if (inventoryOpen) {
            font.draw(batch, "背包", 104, 196);
            font.draw(batch, "地面物品: " + engine.getRoomItemsWithWeight(), 104, 166);
            font.draw(batch, "随身物品: " + engine.getPlayerItemsWithWeight(), 104, 136);
        }
    }

    private void drawFooter()
    {
        drawMultiline(actionMessage, 16, 86);
        font.draw(batch, CONTROL_HINT, 16, 24);
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

    private void drawMultiline(String text, float x, float y)
    {
        String[] lines = text.split("\\n");
        float lineY = y;
        for (String line : lines) {
            font.draw(batch, line, x, lineY);
            lineY -= 22;
        }
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
    }

    @Override
    public void dispose()
    {
        shapes.dispose();
    }
}
