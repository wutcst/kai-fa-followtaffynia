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
   * 根据进入方向选取复活点：从北侧进入时使用 direction=south 的 spawn。
   */
    public SpawnPoint resolveSpawn(Direction entryDirection)
    {
        Direction spawnKey = entryDirection == Direction.DEFAULT
            ? Direction.DEFAULT
            : entryDirection.opposite();
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
