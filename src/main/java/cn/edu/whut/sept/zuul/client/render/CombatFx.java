package cn.edu.whut.sept.zuul.client.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * UT 战斗"打击感"特效系统（juice）。
 *
 * <p>纯客户端表现层：不改变任何战斗逻辑，只在每帧根据玩家/敌人 HP 的变化
 * 驱动一组视觉反馈，让战斗手感接近商业像素游戏。包含：</p>
 * <ul>
 *   <li><b>屏幕震动</b>：trauma 衰减模型，受击/命中时抖动整个战斗框</li>
 *   <li><b>命中顿帧</b>（hitstop）：完美攻击瞬间冻结数十毫秒，强化打击反馈</li>
 *   <li><b>受击闪红</b>：玩家受伤时战斗框闪红一帧</li>
 *   <li><b>红心碎裂粒子</b> / <b>金色斩击粒子</b></li>
 *   <li><b>浮动伤害数字</b></li>
 *   <li><b>红心拖尾</b>：躲避时灵魂残影</li>
 *   <li><b>血条缓动掉血</b> + 受击抖动</li>
 *   <li><b>完美命中金环</b>扩散</li>
 * </ul>
 *
 * <p>坐标约定：粒子/拖尾/伤害数字使用战斗框归一化坐标（0..1），渲染时由
 * 调用方传入已应用震动偏移的 box 矩形，从而所有特效随框一起抖动。</p>
 */
public class CombatFx
{
    // ---- 调参常量 ----
    private static final float SHAKE_MAX_PX = 9f;
    private static final float TRAUMA_DECAY = 1.7f;
    private static final float FLASH_DECAY = 3.4f;
    private static final float BAR_EASE = 4.5f;       // 血条缓动速度
    private static final float BAR_SHAKE_DECAY = 6f;
    private static final int TRAIL_LEN = 6;
    private static final float GRAVITY = -1.4f;        // 归一化/秒^2（y 向上）

    // ---- 屏幕震动 ----
    private float trauma;
    private float shakeSeed;

    // ---- 命中顿帧 ----
    private float hitstop;

    // ---- 受击闪红（框边短促泛红，非满屏）----
    private float playerFlash;

    // ---- 灵魂受击无敌闪烁 ----
    private float soulHurt;

    // ---- 敌人闪光（命中敌人时框边描金白）----
    private float enemyFlash;

    // ---- 血条缓动 / 抖动 ----
    private float enemyBarShown = -1f;   // -1 = 未初始化
    private float playerBarShown = -1f;
    private float enemyBarShake;
    private float playerBarShake;

    // ---- HP 变化侦测 ----
    private int lastPlayerHp = Integer.MIN_VALUE;
    private int lastNpcHp = Integer.MIN_VALUE;

    // ---- 红心拖尾 ----
    private final float[] trailX = new float[TRAIL_LEN];
    private final float[] trailY = new float[TRAIL_LEN];
    private int trailCount;

    private final List<Particle> particles = new ArrayList<>();
    private final List<Popup> popups = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();
    private final GlyphLayout layout = new GlyphLayout();

    // ================= 每帧更新 =================

    /**
     * 推进所有特效计时器。使用<b>真实</b> delta（不受 hitstop 影响），
     * 这样顿帧本身也会正常倒计时。
     */
    public void update(float delta)
    {
        if (hitstop > 0f) hitstop = Math.max(0f, hitstop - delta);
        if (trauma > 0f) trauma = Math.max(0f, trauma - TRAUMA_DECAY * delta);
        if (soulHurt > 0f) soulHurt = Math.max(0f, soulHurt - delta);
        if (playerFlash > 0f) playerFlash = Math.max(0f, playerFlash - FLASH_DECAY * delta);
        if (enemyFlash > 0f) enemyFlash = Math.max(0f, enemyFlash - FLASH_DECAY * delta);
        if (enemyBarShake > 0f) enemyBarShake = Math.max(0f, enemyBarShake - BAR_SHAKE_DECAY * delta);
        if (playerBarShake > 0f) playerBarShake = Math.max(0f, playerBarShake - BAR_SHAKE_DECAY * delta);
        shakeSeed += delta * 40f;

        // 血条向目标缓动
        if (enemyBarShown >= 0f) enemyBarShown = ease(enemyBarShown, enemyBarTarget, delta);
        if (playerBarShown >= 0f) playerBarShown = ease(playerBarShown, playerBarTarget, delta);

        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle p = it.next();
            p.vy += GRAVITY * delta;
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.life -= delta;
            if (p.life <= 0f) it.remove();
        }
        for (Iterator<Popup> it = popups.iterator(); it.hasNext(); ) {
            Popup pu = it.next();
            pu.y += pu.vy * delta;
            pu.life -= delta;
            if (pu.life <= 0f) it.remove();
        }
        for (Iterator<Ring> it = rings.iterator(); it.hasNext(); ) {
            Ring r = it.next();
            r.life -= delta;
            if (r.life <= 0f) it.remove();
        }
    }

    private float enemyBarTarget = 1f;
    private float playerBarTarget = 1f;

    private static float ease(float cur, float target, float delta)
    {
        float t = Math.min(1f, BAR_EASE * delta);
        return cur + (target - cur) * t;
    }

    /**
     * 每帧观测战斗状态，自动侦测 HP 变化并触发对应特效。
     *
     * @param playerHp     玩家当前 HP
     * @param playerMaxHp  玩家最大 HP
     * @param npcHp        敌人当前 HP
     * @param npcMaxHp     敌人最大 HP
     * @param soulNx       灵魂归一化 X（用于受击粒子定位）
     * @param soulNy       灵魂归一化 Y
     * @param attackPerfect 本次敌人掉血是否来自"完美"节奏攻击
     */
    public void observe(int playerHp, int playerMaxHp, int npcHp, int npcMaxHp,
                        float soulNx, float soulNy, boolean attackPerfect)
    {
        float playerRatio = playerMaxHp > 0 ? (float) playerHp / playerMaxHp : 0f;
        float npcRatio = npcMaxHp > 0 ? (float) npcHp / npcMaxHp : 0f;
        playerBarTarget = playerRatio;
        enemyBarTarget = npcRatio;

        // 首帧 / 新战斗：初始化，不触发特效
        boolean newBattle = npcHp > lastNpcHp + 3 || lastNpcHp == Integer.MIN_VALUE;
        if (lastPlayerHp == Integer.MIN_VALUE) {
            playerBarShown = playerRatio;
        }
        if (newBattle) {
            enemyBarShown = npcRatio;
            if (playerBarShown < 0f) playerBarShown = playerRatio;
            lastPlayerHp = playerHp;
            lastNpcHp = npcHp;
            return;
        }

        if (playerHp < lastPlayerHp) onPlayerHit(soulNx, soulNy, lastPlayerHp - playerHp);
        else if (playerHp > lastPlayerHp) playerBarShake = 0.4f; // 治疗微反馈
        if (npcHp < lastNpcHp) {
            onEnemyHit(soulNx, soulNy, lastNpcHp - npcHp, attackPerfect);
            if (npcHp <= 0) onEnemyDeath();   // 击杀爆发
        }

        lastPlayerHp = playerHp;
        lastNpcHp = npcHp;
    }

    /** 在 ENEMY_TURN 每帧采样灵魂位置，形成拖尾。 */
    public void sampleSoul(float nx, float ny)
    {
        for (int i = Math.min(trailCount, TRAIL_LEN - 1); i > 0; i--) {
            trailX[i] = trailX[i - 1];
            trailY[i] = trailY[i - 1];
        }
        trailX[0] = nx;
        trailY[0] = ny;
        if (trailCount < TRAIL_LEN) trailCount++;
    }

    /** 战斗结束或离开时清空，避免残留到下一场。 */
    public void clear()
    {
        particles.clear();
        popups.clear();
        rings.clear();
        trailCount = 0;
        trauma = hitstop = playerFlash = enemyFlash = soulHurt = 0f;
        enemyBarShown = playerBarShown = -1f;
        lastPlayerHp = lastNpcHp = Integer.MIN_VALUE;
    }

    /** 离开 ENEMY_TURN（不再躲避）时清掉红心拖尾，避免残影滞留。 */
    public void clearTrail()
    {
        trailCount = 0;
    }

    /** 灵魂是否处于受击无敌闪烁中。 */
    public boolean isSoulHurt() { return soulHurt > 0f; }

    /** 受击闪烁可见性：以 ~16Hz 闪烁，受击期间隔帧不画灵魂。 */
    public boolean soulVisible()
    {
        if (soulHurt <= 0f) return true;
        return ((int) (soulHurt * 16f)) % 2 == 0;
    }

    // ================= 事件 =================

    private void onPlayerHit(float nx, float ny, int dmg)
    {
        addTrauma(0.45f);
        playerFlash = 0.8f;          // 仅框边短促泛红
        playerBarShake = 1f;
        soulHurt = 0.55f;            // 灵魂无敌闪烁
        // 少量、克制的红色碎片（贴着灵魂，速度小、寿命短）
        for (int i = 0; i < 6; i++) {
            float a = MathUtils.random(MathUtils.PI2);
            float sp = MathUtils.random(0.15f, 0.38f);
            particles.add(Particle.spark(nx, ny,
                MathUtils.cos(a) * sp, MathUtils.sin(a) * sp + 0.1f,
                MathUtils.random(0.22f, 0.36f),
                1f, 0.30f, 0.30f));
        }
        popups.add(Popup.of(nx, ny + 0.05f, "-" + dmg, 1f, 0.35f, 0.35f, 0.7f));
    }

    /** 击杀敌人时的爆发：强震动 + 金白碎片四散。 */
    private void onEnemyDeath()
    {
        addTrauma(0.85f);
        hitstop = Math.max(hitstop, 0.09f);
        enemyFlash = 1f;
        float ex = 0.25f, ey = 1.06f;
        for (int i = 0; i < 22; i++) {
            float a = MathUtils.random(MathUtils.PI2);
            float sp = MathUtils.random(0.35f, 0.95f);
            particles.add(Particle.spark(ex, ey - 0.02f,
                MathUtils.cos(a) * sp, MathUtils.sin(a) * sp + 0.2f,
                MathUtils.random(0.4f, 0.7f), 1f, 0.92f, 0.6f));
        }
        rings.add(Ring.of(ex, ey - 0.02f, 0.7f));
    }

    private void onEnemyHit(float nx, float ny, int dmg, boolean perfect)
    {
        addTrauma(perfect ? 0.6f : 0.32f);
        hitstop = Math.max(hitstop, perfect ? 0.075f : 0.035f);
        enemyFlash = 1f;
        enemyBarShake = 1f;
        // 敌人血条位于框上方左侧，伤害数字与斩击粒子在该区域
        float ex = 0.25f, ey = 1.06f;
        Color c = perfect ? new Color(1f, 0.84f, 0.28f, 1f) : new Color(1f, 0.97f, 0.85f, 1f);
        popups.add(Popup.of(ex, ey, (perfect ? "完美 " : "") + dmg,
            c.r, c.g, c.b, perfect ? 1.0f : 0.8f));
        int n = perfect ? 14 : 8;
        for (int i = 0; i < n; i++) {
            float a = MathUtils.random(-0.6f, 0.6f) + MathUtils.PI / 2f;
            float sp = MathUtils.random(0.3f, 0.8f);
            particles.add(Particle.spark(ex, ey - 0.02f,
                MathUtils.cos(a) * sp, MathUtils.sin(a) * sp,
                MathUtils.random(0.3f, 0.55f), c.r, c.g, c.b));
        }
        if (perfect) rings.add(Ring.of(ex, ey - 0.02f, 0.35f));
    }

    private void addTrauma(float amount)
    {
        trauma = Math.min(1f, trauma + amount);
    }

    // ================= 查询 =================

    public boolean isHitstop() { return hitstop > 0f; }

    /** 敌人受击闪光量 0..1（供敌人立绘闪红/抖动）。 */
    public float enemyFlashAmount() { return enemyFlash; }

    public float shakeX()
    {
        if (trauma <= 0f) return 0f;
        float t = trauma * trauma;
        return (MathUtils.sin(shakeSeed * 1.3f) + MathUtils.sin(shakeSeed * 2.7f) * 0.5f)
            * SHAKE_MAX_PX * t;
    }

    public float shakeY()
    {
        if (trauma <= 0f) return 0f;
        float t = trauma * trauma;
        return (MathUtils.cos(shakeSeed * 1.7f) + MathUtils.cos(shakeSeed * 3.1f) * 0.5f)
            * SHAKE_MAX_PX * t;
    }

    /** 敌人血条显示比例（缓动后）。未初始化时回退到 actual。 */
    public float enemyBarRatio(float actual) { return enemyBarShown >= 0f ? enemyBarShown : actual; }
    public float playerBarRatio(float actual) { return playerBarShown >= 0f ? playerBarShown : actual; }
    public float enemyBarShakeX()
    {
        return enemyBarShake > 0f ? MathUtils.sin(shakeSeed * 5f) * 3f * enemyBarShake : 0f;
    }

    public float playerBarShakeX()
    {
        return playerBarShake > 0f ? MathUtils.sin(shakeSeed * 5f) * 3f * playerBarShake : 0f;
    }

    // ================= 渲染 =================

    /**
     * 绘制所有归一化坐标系下的特效。box 矩形应为<b>已应用震动偏移</b>的坐标。
     * 内部自行管理 ShapeRenderer / SpriteBatch 的 begin/end。
     */
    public void render(ShapeRenderer shapes, SpriteBatch batch, BitmapFont font,
                       float boxX, float boxY, float boxW, float boxH)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // 受击闪红：只在框四边泛红描边（不再满屏铺红）
        if (playerFlash > 0f) {
            float a = playerFlash * 0.55f;
            float t = 10f;
            shapes.setColor(1f, 0.12f, 0.12f, a);
            shapes.rect(boxX, boxY, boxW, t);
            shapes.rect(boxX, boxY + boxH - t, boxW, t);
            shapes.rect(boxX, boxY, t, boxH);
            shapes.rect(boxX + boxW - t, boxY, t, boxH);
        }

        // 命中敌人：框四边描金白闪光
        if (enemyFlash > 0f) {
            float a = enemyFlash * 0.5f;
            float t = 6f;
            shapes.setColor(1f, 0.88f, 0.4f, a);
            shapes.rect(boxX, boxY, boxW, t);
            shapes.rect(boxX, boxY + boxH - t, boxW, t);
            shapes.rect(boxX, boxY, t, boxH);
            shapes.rect(boxX + boxW - t, boxY, t, boxH);
        }

        // 红心拖尾（像素方块残影，与像素风一致）
        for (int i = trailCount - 1; i >= 1; i--) {
            float alpha = (1f - (float) i / TRAIL_LEN) * 0.32f;
            float sx = Math.round(boxX + trailX[i] * boxW);
            float sy = Math.round(boxY + trailY[i] * boxH);
            float s = 5f - i * 0.5f;
            shapes.setColor(1f, 0.12f, 0.13f, alpha);
            shapes.rect(sx - s / 2f, sy - s / 2f, s, s);
        }

        // 粒子
        for (Particle p : particles) {
            float a = Math.min(1f, p.life / p.maxLife) * 0.95f;
            float sx = boxX + p.x * boxW;
            float sy = boxY + p.y * boxH;
            shapes.setColor(p.r, p.g, p.b, a);
            float sz = 2.2f + p.life * 4f;
            shapes.rect(sx - sz / 2f, sy - sz / 2f, sz, sz);
        }

        // 完美命中金环
        for (Ring r : rings) {
            float prog = 1f - r.life / r.maxLife;
            float rad = r.startN * boxW * 0.5f * (0.3f + prog);
            float a = (1f - prog) * 0.8f;
            shapes.setColor(1f, 0.85f, 0.3f, a);
            float cx = boxX + r.x * boxW;
            float cy = boxY + r.y * boxH;
            // 环：两个同心圆差近似（用细圈）
            shapes.circle(cx, cy, rad, 24);
            shapes.setColor(0.025f, 0.022f, 0.04f, a);
            shapes.circle(cx, cy, Math.max(0f, rad - 3f), 24);
        }
        shapes.end();

        // 浮动伤害数字
        if (!popups.isEmpty()) {
            batch.begin();
            for (Popup pu : popups) {
                float a = Math.min(1f, pu.life / pu.maxLife);
                font.setColor(pu.r, pu.g, pu.b, a);
                float fx = boxX + pu.x * boxW;
                float fy = boxY + pu.y * boxH;
                layout.setText(font, pu.text);
                font.draw(batch, pu.text, fx - layout.width / 2f, fy);
            }
            font.setColor(Color.WHITE);
            batch.end();
        }
    }

    // ================= 内部数据 =================

    private static final class Particle
    {
        float x, y, vx, vy, life, maxLife, r, g, b;

        static Particle spark(float x, float y, float vx, float vy, float life,
                              float r, float g, float b)
        {
            Particle p = new Particle();
            p.x = x; p.y = y; p.vx = vx; p.vy = vy;
            p.life = life; p.maxLife = life;
            p.r = r; p.g = g; p.b = b;
            return p;
        }
    }

    private static final class Popup
    {
        float x, y, vy, life, maxLife, r, g, b;
        String text;

        static Popup of(float x, float y, String text, float r, float g, float b, float life)
        {
            Popup pu = new Popup();
            pu.x = x; pu.y = y; pu.vy = 0.22f;
            pu.life = life; pu.maxLife = life;
            pu.r = r; pu.g = g; pu.b = b; pu.text = text;
            return pu;
        }
    }

    private static final class Ring
    {
        float x, y, startN, life, maxLife;

        static Ring of(float x, float y, float startN)
        {
            Ring r = new Ring();
            r.x = x; r.y = y; r.startN = startN;
            r.life = 0.4f; r.maxLife = 0.4f;
            return r;
        }
    }
}
