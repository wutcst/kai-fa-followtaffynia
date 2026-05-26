package cn.edu.whut.sept.zuul.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * 房间对应的 Tiled 场景元数据（成员 A 维护）。
 */
public class RoomScene
{
    private final String tmxPath;
    private final Map<Direction, SpawnPoint> spawns;

    public RoomScene(String tmxPath)
    {
        this.tmxPath = tmxPath;
        this.spawns = new HashMap<>();
    }

    public String getTmxPath()
    {
        return tmxPath;
    }

    public void addSpawn(Direction direction, float tileX, float tileY)
    {
        spawns.put(direction, new SpawnPoint(tileX, tileY));
    }

    /**
     * 按房间某一侧的 spawn 落点选取坐标。
     *
     * @param side 房间边界：north/south/east/west；与 {@link Direction} 一致
     */
    public SpawnPoint getSpawnAt(Direction side)
    {
        Direction spawnKey = side == null || side == Direction.DEFAULT
            ? Direction.DEFAULT
            : side;
        SpawnPoint point = spawns.get(spawnKey);
        if (point == null) {
            point = spawns.get(Direction.DEFAULT);
        }
        if (point == null) {
            point = new SpawnPoint(1f, 1f);
        }
        return point;
    }

    public static final class SpawnPoint
    {
        public final float tileX;
        public final float tileY;

        public SpawnPoint(float tileX, float tileY)
        {
            this.tileX = tileX;
            this.tileY = tileY;
        }
    }
}
