package cn.edu.whut.sept.zuul.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 弹幕图案生成器。每个图案有固定的持续时间和子弹发射逻辑。
 * 每帧调用 {@link #update(float, List)} 向列表追加新子弹。
 */
public abstract class BulletPattern
{
    protected final Random rng;
    protected float timer;
    protected final float duration;
    protected float lastDelta;

    protected BulletPattern(float duration, Random rng)
    {
        this.duration = duration;
        this.rng = rng;
        this.timer = 0f;
    }

    /** 还剩多少时间。 */
    public boolean isFinished()
    {
        return timer >= duration;
    }

    public float getProgress()
    {
        return duration > 0 ? timer / duration : 1f;
    }

    /** 每帧调用，向列表追加新子弹。 */
    public void update(float delta, List<Bullet> bullets)
    {
        lastDelta = delta;
        timer += delta;
        emit(bullets);
    }

    /** 子类实现：生成子弹。 */
    protected abstract void emit(List<Bullet> bullets);

    // ========== 工厂 ==========

    /** 从一侧发射一排圆形子弹。 */
    public static BulletPattern wave(float duration, int totalBullets, int damage, Random rng)
    {
        return new BulletPattern(duration, rng)
        {
            private int spawned;
            private float spawnTimer;
            private final float interval = duration / totalBullets;

            @Override
            protected void emit(List<Bullet> bullets)
            {
                spawnTimer += lastDelta;
                while (spawnTimer >= interval && spawned < totalBullets) {
                    spawnTimer -= interval;
                    float y = (float) spawned / totalBullets;
                    // 从右侧射入，向左移动
                    bullets.add(Bullet.circle(1.05f, y, 0.03f, -0.5f, 0f, damage));
                    spawned++;
                }
            }
        };
    }

    /** 从中心向外扩散。 */
    public static BulletPattern burst(float duration, int totalBullets, int damage, Random rng)
    {
        return new BulletPattern(duration, rng)
        {
            private int spawned;
            private float spawnTimer;
            private final float interval = duration / totalBullets;

            @Override
            protected void emit(List<Bullet> bullets)
            {
                spawnTimer += lastDelta;
                while (spawnTimer >= interval && spawned < totalBullets) {
                    spawnTimer -= interval;
                    double angle = (double) spawned / totalBullets * Math.PI * 2;
                    float vx = (float) Math.cos(angle) * 0.4f;
                    float vy = (float) Math.sin(angle) * 0.4f;
                    bullets.add(Bullet.circle(0.5f, 0.5f, 0.03f, vx, vy, damage));
                    spawned++;
                }
            }
        };
    }

    /** 随机散射：从四个方向随机射入。 */
    public static BulletPattern randomScatter(float duration, int totalBullets, int damage, Random rng)
    {
        return new BulletPattern(duration, rng)
        {
            private int spawned;
            private float spawnTimer;
            private final float interval = duration / totalBullets;

            @Override
            protected void emit(List<Bullet> bullets)
            {
                spawnTimer += lastDelta;
                while (spawnTimer >= interval && spawned < totalBullets) {
                    spawnTimer -= interval;
                    // 四个边随机选一个
                    int side = rng.nextInt(4);
                    float x, y, vx, vy;
                    switch (side) {
                        case 0: x = rng.nextFloat(); y = -0.1f; vx = 0f; vy = 0.3f + rng.nextFloat() * 0.2f; break;
                        case 1: x = rng.nextFloat(); y = 1.1f; vx = 0f; vy = -0.3f - rng.nextFloat() * 0.2f; break;
                        case 2: x = -0.1f; y = rng.nextFloat(); vx = 0.3f + rng.nextFloat() * 0.2f; vy = 0f; break;
                        default: x = 1.1f; y = rng.nextFloat(); vx = -0.3f - rng.nextFloat() * 0.2f; vy = 0f; break;
                    }
                    bullets.add(Bullet.circle(x, y, 0.025f, vx, vy, damage));
                    spawned++;
                }
            }
        };
    }

    /** 旋转弹幕：从四个角落发射旋转子弹。 */
    public static BulletPattern spiral(float duration, int totalBullets, int damage, Random rng)
    {
        return new BulletPattern(duration, rng)
        {
            private int spawned;
            private float spawnTimer;
            private final float interval = duration / totalBullets;

            @Override
            protected void emit(List<Bullet> bullets)
            {
                spawnTimer += lastDelta;
                while (spawnTimer >= interval && spawned < totalBullets) {
                    spawnTimer -= interval;
                    double angle = (double) spawned / totalBullets * Math.PI * 4;
                    float x = (float) (0.5f + Math.cos(angle) * 0.4f);
                    float y = (float) (0.5f + Math.sin(angle) * 0.4f);
                    // 子弹从生成点向中心反方向移动
                    float vx = (float) -Math.cos(angle) * 0.3f;
                    float vy = (float) -Math.sin(angle) * 0.3f;
                    bullets.add(Bullet.circle(x, y, 0.03f, vx, vy, damage));
                    spawned++;
                }
            }
        };
    }
}
