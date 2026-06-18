package cn.edu.whut.sept.zuul.engine;

/**
 * 弹幕实体。在战斗框内移动，灵魂碰到则受伤。
 * 坐标使用归一化坐标系（0..1，战斗框左上角为原点）。
 */
public class Bullet
{
    public enum Shape { CIRCLE, RECT }

    /** 子弹种类，决定渲染外观与命中行为（剑/墙为持续型危险，命中不消失）。 */
    public enum Kind { NORMAL, AIMED, SWORD, WALL }

    private static int nextVisualVariant;

    public final Shape shape;
    public final int visualVariant;
    public float x, y;          // 归一化坐标
    public float radius;        // 圆形半径（归一化）
    public float width, height; // 矩形宽高（归一化）
    public float vx, vy;        // 速度（归一化/秒）
    public boolean alive = true;
    public int damage = 5;
    public Kind kind = Kind.NORMAL;
    /** 存活时长（秒）；&lt;0 表示无限（直到出界）。由引擎按真实时间倒计时。 */
    public float life = -1f;

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
        this.visualVariant = nextVisualVariant++;
    }

    /** 链式设置种类。 */
    public Bullet withKind(Kind kind)
    {
        this.kind = kind;
        return this;
    }

    /** 链式设置存活时长（秒）。 */
    public Bullet withLife(float seconds)
    {
        this.life = seconds;
        return this;
    }

    /** 每帧更新位置。超出战斗框即标记死亡。 */
    public void update(float delta)
    {
        x += vx * delta;
        y += vy * delta;
        // 飞剑由 life/边框吸附管理生命周期，且需从屏幕外长距离飞入，
        // 故不走越界即死逻辑（否则从 x=-0.5 等屏外生成时会瞬间消失）。
        if (kind == Kind.SWORD) return;
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
