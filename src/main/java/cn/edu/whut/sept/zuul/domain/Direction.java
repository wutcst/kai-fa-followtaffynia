package cn.edu.whut.sept.zuul.domain;

/**
 * 全项目统一方向枚举，与 Tiled exit/spawn 的 direction 属性一致。
 */
public enum Direction
{
    NORTH,
    SOUTH,
    EAST,
    WEST,
    DEFAULT;

    public String toExitKey()
    {
        if (this == DEFAULT) {
            return "default";
        }
        return name().toLowerCase();
    }

    public Direction opposite()
    {
        switch (this) {
            case NORTH: return SOUTH;
            case SOUTH: return NORTH;
            case EAST: return WEST;
            case WEST: return EAST;
            default: return DEFAULT;
        }
    }

    public static Direction fromExitKey(String key)
    {
        if (key == null || key.isEmpty() || "default".equalsIgnoreCase(key)) {
            return DEFAULT;
        }
        try {
            return valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }
}
