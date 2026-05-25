package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.client.RpgMain;
import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.RoomScene;
import cn.edu.whut.sept.zuul.engine.GameEngine;
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

    private final RpgMain game;
    private final SpriteBatch batch;
    private final GameEngine engine;
    private final BitmapFont font;
    private final ShapeRenderer shapes;

    private float playerX;
    private float playerY;
    private String statusMessage;

    public GameScreen(RpgMain game, SpriteBatch batch, GameEngine engine)
    {
        this.game = game;
        this.batch = batch;
        this.engine = engine;
        this.font = game.getFonts().getDefault();
        this.shapes = new ShapeRenderer();
        this.statusMessage = "WASD 移动 | 方向键切换房间 | Q 调查 | E 拾取 | B 回退 | ESC 标题";

        snapToSpawn();
    }

    @Override
    public void show()
    {
    }

    @Override
    public void render(float delta)
    {
        handleInput(delta);

        Gdx.gl.glClearColor(0.12f, 0.18f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawPlaceholderMap();
        drawPlayer();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "玩家: " + engine.getPlayer().getName(), 16, Gdx.graphics.getHeight() - 20);
        font.draw(batch, "房间: " + engine.getCurrentRoom().getRoomId(), 16, Gdx.graphics.getHeight() - 45);
        font.draw(batch, statusMessage, 16, 40);
        batch.end();
    }

    private void handleInput(float delta)
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            shapes.dispose();
            game.setScreen(new TitleScreen(game, batch));
            return;
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
            statusMessage = engine.look();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            if (engine.moveBack()) {
                snapToSpawn();
                statusMessage = "已回退至 " + engine.getCurrentRoom().getRoomId();
            } else {
                statusMessage = "无法回退";
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            statusMessage = tryTakeFirstItem();
        }
    }

    private void changeRoom(Direction direction)
    {
        if (engine.movePlayer(direction)) {
            snapToSpawn();
            statusMessage = "进入 " + engine.getCurrentRoom().getRoomId();
        } else {
            statusMessage = "该方向无法前进";
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

    private void drawPlayer()
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.3f, 0.55f, 0.95f, 1f);
        shapes.rect(playerX, playerY, TILE_SIZE, TILE_SIZE);
        shapes.end();
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
