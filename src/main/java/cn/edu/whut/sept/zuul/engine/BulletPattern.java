package cn.edu.whut.sept.zuul.engine;

import java.util.List;
import java.util.Random;

/**
 * 弹幕图案生成器。每个图案有固定的持续时间和子弹发射逻辑。
 * 每帧调用 {@link #update(float, List, List)} 向列表追加新子弹和预警。
 */
public abstract class BulletPattern
{
    protected final Random rng;
    protected float timer;
    protected final float duration;
    protected float lastDelta;
    /** 当前灵魂归一化位置（供瞄准类图案使用）。 */
    protected float soulX = 0.5f;
    protected float soulY = 0.5f;

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

    /** 每帧调用，向列表追加新子弹和预警。 */
    public void update(float delta, List<Bullet> bullets, List<WarningZone> warnings)
    {
        lastDelta = delta;
        timer += delta;
        emit(bullets, warnings);
    }

    /** 带灵魂位置的更新（瞄准类图案据此锁定玩家）。 */
    public void update(float delta, List<Bullet> bullets, List<WarningZone> warnings,
                       float soulX, float soulY)
    {
        this.soulX = soulX;
        this.soulY = soulY;
        update(delta, bullets, warnings);
    }

    /** 子类实现：生成子弹。 */
    protected abstract void emit(List<Bullet> bullets, List<WarningZone> warnings);

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
            protected void emit(List<Bullet> bullets, List<WarningZone> warnings)
            {
                spawnTimer += lastDelta;
                while (spawnTimer >= interval && spawned < totalBullets) {
                    spawnTimer -= interval;
                    float y = (float) spawned / totalBullets;
                    warnings.add(WarningZone.line(0.02f, y, 0.98f, y, 0.42f));
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
            protected void emit(List<Bullet> bullets, List<WarningZone> warnings)
            {
                spawnTimer += lastDelta;
                while (spawnTimer >= interval && spawned < totalBullets) {
                    spawnTimer -= interval;
                    double angle = (double) spawned / totalBullets * Math.PI * 2;
                    float vx = (float) Math.cos(angle) * 0.4f;
                    float vy = (float) Math.sin(angle) * 0.4f;
                    if (spawned % 2 == 0) {
                        warnings.add(WarningZone.box(0.43f, 0.36f, 0.14f, 0.28f, 0.36f));
                    }
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
            protected void emit(List<Bullet> bullets, List<WarningZone> warnings)
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
                    if (side == 0 || side == 1) {
                        warnings.add(WarningZone.line(x, 0.02f, x, 0.98f, 0.38f));
                    } else {
                        warnings.add(WarningZone.line(0.02f, y, 0.98f, y, 0.38f));
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
            protected void emit(List<Bullet> bullets, List<WarningZone> warnings)
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
                    if (spawned % 3 == 0) {
                        warnings.add(WarningZone.box(0.12f, 0.12f, 0.76f, 0.76f, 0.32f));
                    }
                    bullets.add(Bullet.circle(x, y, 0.03f, vx, vy, damage));
                    spawned++;
                }
            }
        };
    }

    /** 瞄准弹：从顶部随机点锁定玩家当前位置射出（先短预警，再发射）。 */
    public static BulletPattern aimed(float duration, int totalBullets, int damage, Random rng)
    {
        return new BulletPattern(duration, rng)
        {
            private int spawned;
            private float spawnTimer;
            private final float interval = duration / totalBullets;

            @Override
            protected void emit(List<Bullet> bullets, List<WarningZone> warnings)
            {
                spawnTimer += lastDelta;
                while (spawnTimer >= interval && spawned < totalBullets) {
                    spawnTimer -= interval;
                    float sx = rng.nextFloat();
                    float sy = -0.08f;
                    float dx = soulX - sx;
                    float dy = soulY - sy;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    if (len < 1e-3f) len = 1f;
                    float speed = 0.5f;
                    warnings.add(WarningZone.line(sx, 0.02f, sx, 0.22f, 0.3f));
                    bullets.add(Bullet.circle(sx, sy, 0.030f,
                        dx / len * speed, dy / len * speed, damage)
                        .withKind(Bullet.Kind.AIMED));
                    spawned++;
                }
            }
        };
    }

    /**
     * 守卫招式「飞剑」：长剑从四面八方高速飞来，先亮红线预警全程飞行轨迹（约 0.5s），
     * 再射出长条剑沿轨迹飞过场地、最终插入对面边框停留片刻。命中持续受伤（有无敌帧）。
     * <p>飞行/嵌入由引擎对 {@link Bullet.Kind#SWORD} 的边界吸附逻辑处理。</p>
     */
    public static BulletPattern flyingSwords(float duration, int damage, Random rng)
    {
        return new BulletPattern(duration, rng)
        {
            private static final float WARN = 0.5f;
            private static final float CYCLE = 1.5f;
            private float waveStart = 0.15f;
            private boolean warned;
            private boolean struck;
            private int dir;     // 0:向左 1:向右 2:向下 3:向上
            private float pos;   // 垂直/水平轨迹位置

            @Override
            protected void emit(List<Bullet> bullets, List<WarningZone> warnings)
            {
                float local = timer - waveStart;
                if (local < 0f) return;
                if (!warned) {
                    dir = rng.nextInt(4);
                    pos = 0.2f + rng.nextFloat() * 0.6f;
                    if (dir <= 1) warnings.add(WarningZone.line(0f, pos, 1f, pos, WARN));
                    else warnings.add(WarningZone.line(pos, 0f, pos, 1f, WARN));
                    warned = true;
                } else if (!struck && local >= WARN) {
                    addSword(bullets);
                    struck = true;
                } else if (struck && local >= CYCLE) {
                    warned = false;
                    struck = false;
                    waveStart = timer;
                }
            }

            private void addSword(List<Bullet> bullets)
            {
                float len = 0.45f, t = 0.06f, sp = 2.4f;
                Bullet s;
                switch (dir) {
                    case 0:  s = Bullet.rect(1.06f, pos - t / 2f, len, t, -sp, 0f, damage); break; // 飞向左，插左框
                    case 1:  s = Bullet.rect(-0.51f, pos - t / 2f, len, t, sp, 0f, damage); break; // 飞向右，插右框
                    case 2:  s = Bullet.rect(pos - t / 2f, 1.06f, t, len, 0f, -sp, damage); break; // 飞向下，插底框
                    default: s = Bullet.rect(pos - t / 2f, -0.51f, t, len, 0f, sp, damage); break;  // 飞向上，插顶框
                }
                bullets.add(s.withKind(Bullet.Kind.SWORD).withLife(1.7f));
            }
        };
    }

    /** 神父招式「圣十字」：交替从顶/左推出整排子弹墙，各留安全缺口，形成十字封锁。 */
    public static BulletPattern crossSweep(float duration, int damage, Random rng)
    {
        return new BulletPattern(duration, rng)
        {
            private float spawnTimer;
            private float interval = 1.0f;
            private boolean vertical;
            private static final int N = 7;

            @Override
            protected void emit(List<Bullet> bullets, List<WarningZone> warnings)
            {
                spawnTimer += lastDelta;
                if (spawnTimer < interval) return;
                spawnTimer -= interval;
                interval = 0.85f + rng.nextFloat() * 0.4f;
                int gap = rng.nextInt(N);
                for (int i = 0; i < N; i++) {
                    if (i == gap) continue;
                    float p = (i + 0.5f) / N;
                    if (vertical) {            // 从顶部落下
                        warnings.add(WarningZone.line(p, 0f, p, 1f, 0.3f));
                        bullets.add(Bullet.circle(p, 1.08f, 0.028f, 0f, -0.5f, damage));
                    } else {                   // 从左侧推进
                        warnings.add(WarningZone.line(0f, p, 1f, p, 0.3f));
                        bullets.add(Bullet.circle(-0.08f, p, 0.028f, 0.5f, 0f, damage));
                    }
                }
                vertical = !vertical;
            }
        };
    }

    /** 追随者招式「血环」：从中心一圈圈向外扩散的子弹环。 */
    public static BulletPattern ringWaves(float duration, int damage, Random rng)
    {
        return new BulletPattern(duration, rng)
        {
            private float spawnTimer;
            private final float interval = 0.85f;
            private int ring;

            @Override
            protected void emit(List<Bullet> bullets, List<WarningZone> warnings)
            {
                spawnTimer += lastDelta;
                if (spawnTimer < interval) return;
                spawnTimer -= interval;
                int n = 12;
                float phase = (ring % 2) * (float) Math.PI / n;   // 交错
                warnings.add(WarningZone.box(0.42f, 0.42f, 0.16f, 0.16f, 0.28f));
                for (int i = 0; i < n; i++) {
                    double a = phase + i * Math.PI * 2 / n;
                    float vx = (float) Math.cos(a) * 0.42f;
                    float vy = (float) Math.sin(a) * 0.42f;
                    bullets.add(Bullet.circle(0.5f, 0.5f, 0.026f, vx, vy, damage));
                }
                ring++;
            }
        };
    }

    /**
     * 魔像招式「土墙」：玩家进入重力模式后，从右侧不断冲出不同高度的土墙，
     * 须用 A/D + 跳跃躲过。墙贴底生成、向左移动，命中持续受伤（有无敌帧）。
     */
    public static BulletPattern gravityWalls(float duration, int damage, Random rng)
    {
        return new BulletPattern(duration, rng)
        {
            private float spawnTimer;
            private float interval = 1.0f;
            private boolean first = true;

            @Override
            protected void emit(List<Bullet> bullets, List<WarningZone> warnings)
            {
                if (first) { spawnTimer = 0.6f; first = false; }
                spawnTimer += lastDelta;
                if (spawnTimer >= interval) {
                    spawnTimer -= interval;
                    interval = 0.85f + rng.nextFloat() * 0.5f;
                    float h = 0.14f + rng.nextInt(3) * 0.11f;   // 0.14 / 0.25 / 0.36
                    float w = 0.07f;
                    warnings.add(WarningZone.box(1f - 0.02f, 0f, 0.02f, h, 0.3f));
                    bullets.add(Bullet.rect(1.12f, 0f, w, h, -0.55f, 0f, damage)
                        .withKind(Bullet.Kind.WALL));
                }
            }
        };
    }

    /** 柱列弹：成排从顶部落下的子弹墙，每排随机留一个安全列缺口。 */
    public static BulletPattern pillars(float duration, int waves, int damage, Random rng)
    {
        return new BulletPattern(duration, rng)
        {
            private int done;
            private float waveTimer;
            private final float interval = duration / Math.max(1, waves);
            private static final int COLS = 6;

            @Override
            protected void emit(List<Bullet> bullets, List<WarningZone> warnings)
            {
                waveTimer += lastDelta;
                while (waveTimer >= interval && done < waves) {
                    waveTimer -= interval;
                    int gap = rng.nextInt(COLS);
                    for (int c = 0; c < COLS; c++) {
                        if (c == gap) continue;
                        float x = (c + 0.5f) / COLS;
                        warnings.add(WarningZone.line(x, 0.02f, x, 0.98f, 0.3f));
                        bullets.add(Bullet.circle(x, -0.08f, 0.03f, 0f, 0.5f, damage));
                    }
                    done++;
                }
            }
        };
    }
}
