package cn.edu.whut.sept.zuul.engine;

/**
 * Enemy-turn warning telegraph shown before or during bullet waves.
 * Coordinates are normalized to the combat arena.
 */
public class WarningZone
{
    public enum Type { LINE, BOX }

    public final Type type;
    public final float x1;
    public final float y1;
    public final float x2;
    public final float y2;
    public final float duration;
    private float remaining;

    private WarningZone(Type type, float x1, float y1, float x2, float y2, float duration)
    {
        this.type = type;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.duration = Math.max(0.01f, duration);
        this.remaining = this.duration;
    }

    public static WarningZone line(float x1, float y1, float x2, float y2, float duration)
    {
        return new WarningZone(Type.LINE, x1, y1, x2, y2, duration);
    }

    public static WarningZone box(float x, float y, float w, float h, float duration)
    {
        return new WarningZone(Type.BOX, x, y, x + w, y + h, duration);
    }

    public void update(float delta)
    {
        remaining -= delta;
    }

    public boolean isAlive()
    {
        return remaining > 0f;
    }

    public float alpha()
    {
        float t = remaining / duration;
        if (t < 0f) return 0f;
        if (t > 1f) return 1f;
        return 0.25f + t * 0.75f;
    }
}
