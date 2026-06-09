package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Room;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.infra.GameLogger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * 房间/地图控制器 —— 负责地图加载、出口检测、spawn点定位。
 */
public class RoomController
{
    private static final Logger LOG = GameLogger.get();

    private final GameEngine engine;
    private final float tile;
    private final float playerW;
    private final float playerH;
    private final Consumer<Runnable> deferDispose;
    private NpcPlaceholderManager npcManager;
    private ItemPlaceholderManager itemManager;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private TiledMapTileLayer wallLayer;
    private MapObjects objectsLayer;
    private String currentMapPath;
    private float exitCooldown;

    public void setManagers(NpcPlaceholderManager npm, ItemPlaceholderManager ipm)
    {
        this.npcManager = npm;
        this.itemManager = ipm;
    }

    public RoomController(GameEngine engine, float tile, float playerW, float playerH,
                           NpcPlaceholderManager npcManager,
                           ItemPlaceholderManager itemManager,
                           Consumer<Runnable> deferDispose)
    {
        this.engine = engine;
        this.tile = tile;
        this.playerW = playerW;
        this.playerH = playerH;
        this.npcManager = npcManager;
        this.itemManager = itemManager;
        this.deferDispose = deferDispose;
    }

    public TiledMap getMap() { return map; }
    public TiledMapTileLayer getWallLayer() { return wallLayer; }
    public MapObjects getObjectsLayer() { return objectsLayer; }
    public OrthogonalTiledMapRenderer getMapRenderer() { return mapRenderer; }
    public float getExitCooldown() { return exitCooldown; }
    public void setExitCooldown(float v) { this.exitCooldown = v; }

    public float mapPixelWidth()
    {
        if (map == null) return 960f;
        MapProperties props = map.getProperties();
        Integer w = props.get("width", Integer.class);
        Integer tw = props.get("tilewidth", Integer.class);
        return (w == null || tw == null) ? 960f : w * tw;
    }

    public float mapPixelHeight()
    {
        if (map == null) return 544f;
        MapProperties props = map.getProperties();
        Integer h = props.get("height", Integer.class);
        Integer th = props.get("tileheight", Integer.class);
        return (h == null || th == null) ? 544f : h * th;
    }

    /** Tiled 格子行号（0=地图顶部）→ LibGDX 玩家左下角 y */
    public float tileRowToGdxY(float tileRowFromTop)
    {
        int mapRows = (int) (mapPixelHeight() / tile);
        float rowFromBottom = mapRows - 1 - tileRowFromTop;
        return rowFromBottom * tile + (tile - playerH) / 2f;
    }

    public Rectangle tileToWorldRect(int tileX, int tiledRowFromTop, int wTiles, int hTiles)
    {
        float x = tileX * tile;
        float y = tileRowToGdxY(tiledRowFromTop) - (tile - playerH) / 2f;
        return new Rectangle(x, y, wTiles * tile, hTiles * tile);
    }

    /** Tiled 对象 y（自上向下）→ LibGDX 世界 y（自下向上） */
    public float tiledTopYToGdxY(float tiledTopY, float objectHeight)
    {
        return mapPixelHeight() - tiledTopY - objectHeight;
    }

    public void loadCurrentRoom(boolean snapAfterLoad)
    {
        String tmxPath = engine.getCurrentRoom().getScene().getTmxPath();
        if (tmxPath.equals(currentMapPath) && map != null) {
            if (snapAfterLoad) return;
            return;
        }
        disposeMap();
        try {
            String roomId = engine.getCurrentRoom().getRoomId();
            LOG.info("loadMap: room=" + roomId + " | tmx=" + tmxPath
                + " | entryDir=" + engine.getEntryDirection().toExitKey());
            map = new TmxMapLoader().load(tmxPath);
            mapRenderer = new OrthogonalTiledMapRenderer(map, 1f, null);
            wallLayer = (TiledMapTileLayer) map.getLayers().get("wall");
            MapLayer objLayer = map.getLayers().get("objects");
            objectsLayer = objLayer == null ? null : objLayer.getObjects();
            npcManager.buildNpcPlaceholders(roomId, objectsLayer);
            itemManager.buildItemPlaceholders(roomId);
            currentMapPath = tmxPath;
            exitCooldown = 0.3f;
        } catch (Exception e) {
            LOG.warning("loadMap: FAILED " + tmxPath + " | " + e.getMessage());
            map = null;
            mapRenderer = null;
            wallLayer = null;
            objectsLayer = null;
            npcManager.getPlaceholders().clear();
        }
    }

    public void snapToSpawn()
    {
        cn.edu.whut.sept.zuul.domain.RoomScene.SpawnPoint spawn = engine.resolveCurrentSpawn();
        float x = spawn.tileX * tile + tile / 2f - playerW / 2f;
        float y = tileRowToGdxY(spawn.tileY);
        // return position without clamping (caller handles)
        LOG.info("spawn: tile=(" + spawn.tileX + "," + spawn.tileY + ") entryDir="
            + engine.getEntryDirection().toExitKey());
        // Results must be read by caller since we don't manage playerX/Y
    }

    public float getSpawnX()
    {
        cn.edu.whut.sept.zuul.domain.RoomScene.SpawnPoint spawn = engine.resolveCurrentSpawn();
        return spawn.tileX * tile + tile / 2f - playerW / 2f;
    }

    public float getSpawnY()
    {
        cn.edu.whut.sept.zuul.domain.RoomScene.SpawnPoint spawn = engine.resolveCurrentSpawn();
        return tileRowToGdxY(spawn.tileY);
    }

    /**
     * 检查玩家与出口的重叠并触发房间切换。
     * @return 新的 actionMessage 若切换房间，否则 null
     */
    public String checkExitOverlap(float playerX, float playerY)
    {
        if (exitCooldown > 0f || objectsLayer == null) return null;
        Rectangle playerRect = new Rectangle(playerX, playerY, playerW, playerH);
        for (MapObject obj : objectsLayer) {
            if (!"exit".equals(obj.getProperties().get("type", String.class)))
                continue;
            MapProperties props = obj.getProperties();
            float ox = props.get("x", Float.class) == null ? 0f : props.get("x", Float.class);
            float oy = props.get("y", Float.class) == null ? 0f : props.get("y", Float.class);
            float ow = props.get("width", Float.class) == null ? tile : props.get("width", Float.class);
            float oh = props.get("height", Float.class) == null ? tile : props.get("height", Float.class);
            Rectangle exitRect = new Rectangle(ox, oy, ow, oh);
            if (!playerRect.overlaps(exitRect)) continue;
            String target = props.get("targetRoomId", String.class);
            Direction dir = resolveDirectionToTarget(target);
            if (dir == Direction.DEFAULT) {
                String dv = props.get("direction", String.class);
                dir = Direction.fromExitKey(dv);
            }
            LOG.info("exit: trigger target=" + target + " dir=" + dir.toExitKey());
            if (dir != Direction.DEFAULT && engine.movePlayer(dir)) {
                loadCurrentRoom(true);
                exitCooldown = 0.5f;
                return "进入 " + engine.getCurrentRoom().getRoomId();
            }
            if (dir != Direction.DEFAULT) {
                exitCooldown = 0.5f;
                return engine.getLastMessage();
            }
            break;
        }
        return null;
    }

    private Direction resolveDirectionToTarget(String targetRoomId)
    {
        if (targetRoomId == null) return Direction.DEFAULT;
        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (Direction d : dirs) {
            Room r = engine.getCurrentRoom().getExit(d.toExitKey());
            if (r != null && targetRoomId.equals(r.getRoomId())) return d;
        }
        return Direction.DEFAULT;
    }

    public String nearbyExitTarget(float playerX, float playerY)
    {
        if (objectsLayer == null) return null;
        Rectangle pr = new Rectangle(playerX - 16f, playerY - 16f, playerW + 32f, playerH + 32f);
        for (MapObject obj : objectsLayer) {
            if (!"exit".equals(obj.getProperties().get("type", String.class))) continue;
            MapProperties props = obj.getProperties();
            float ox = props.get("x", Float.class) == null ? 0f : props.get("x", Float.class);
            float oy = props.get("y", Float.class) == null ? 0f : props.get("y", Float.class);
            float ow = props.get("width", Float.class) == null ? tile : props.get("width", Float.class);
            float oh = props.get("height", Float.class) == null ? tile : props.get("height", Float.class);
            Rectangle er = new Rectangle(ox, oy, ow, oh);
            if (pr.overlaps(er)) {
                String tid = props.get("targetRoomId", String.class);
                return tid == null || tid.trim().isEmpty() ? "出口" : tid;
            }
        }
        return null;
    }

    public void disposeMap()
    {
        if (mapRenderer != null) { mapRenderer.dispose(); mapRenderer = null; }
        if (map != null) { map.dispose(); map = null; }
        wallLayer = null;
        objectsLayer = null;
    }

    public void deferDispose()
    {
        final RoomController self = this;
        deferDispose.accept(self::disposeMap);
    }
}
