package cn.edu.whut.sept.zuul.engine;

/**
 * 弹幕实体。在战斗框内移动，灵魂碰到则受伤。
 * 坐标使用归一化坐标系（0..1，战斗框左上角为原点）。
 */
public class Bullet
{
    public enum Shape { CIRCLE, RECT }

    public final Shape shape;
    public float x, y;          // 归一化坐标
    public float radius;        // 圆形半径（归一化）
    public float width, height; // 矩形宽高（归一化）
    public float vx, vy;        // 速度（归一化/秒）
    public boolean alive = true;
    public int damage = 5;

    // ---- 工厂方法 ----

    public static Bullet circle(float x, float y, float radius, float vx, float vy, int damage)
    {
        Bullet b = new Bullet(Shape.CIRCLE);
        b.x = x;
        b.y = y;
        b.radius = radius;
        b.vx = vx;
        b.vy = vy;
        b.damage = damage;
        return b;
    }

    public static Bullet rect(float x, float y, float w, float h, float vx, float vy, int damage)
    {
        Bullet b = new Bullet(Shape.RECT);
        b.x = x;
        b.y = y;
        b.width = w;
        b.height = h;
        b.vx = vx;
        b.vy = vy;
        b.damage = damage;
        return b;
    }

    private Bullet(Shape shape)
    {
        this.shape = shape;
    }

    /** 每帧更新位置。超出战斗框即标记死亡。 */
    public void update(float delta)
    {
        x += vx * delta;
        y += vy * delta;
        if (x < -0.2f || x > 1.2f || y < -0.2f || y > 1.2f) {
            alive = false;
        }
    }

    /** 圆形碰撞检测（灵魂半径约 0.04）。 */
    public boolean collidesWith(float soulX, float soulY, float soulRadius)
    {
        if (shape == Shape.CIRCLE) {
            float dx = x - soulX;
            float dy = y - soulY;
            return dx * dx + dy * dy < (radius + soulRadius) * (radius + soulRadius);
        }
        // RECT: AABB 检测
        float left = x;
        float right = x + width;
        float top = y;
        float bottom = y + height;
        return soulX + soulRadius > left && soulX - soulRadius < right
            && soulY + soulRadius > top && soulY - soulRadius < bottom;
    }
}
