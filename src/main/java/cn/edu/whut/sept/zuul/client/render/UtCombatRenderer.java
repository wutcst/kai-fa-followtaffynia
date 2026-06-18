package cn.edu.whut.sept.zuul.client.render;

import cn.edu.whut.sept.zuul.engine.Bullet;
import cn.edu.whut.sept.zuul.engine.CombatSystem;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatPhase;
import cn.edu.whut.sept.zuul.engine.WarningZone;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;

/**
 * UT 战斗渲染器 —— 从 GameScreen 提取的纯渲染逻辑。
 */
public class UtCombatRenderer implements Disposable
{
    private static final String[] MENU_LABELS = {"FIGHT", "ACT", "ITEM", "MERCY"};
    private static final Color WHITE = new Color(0.98f, 0.98f, 0.92f, 1f);
    private static final Color GOLD = new Color(1f, 0.66f, 0.12f, 1f);
    private static final Color GOLD_DIM = new Color(0.64f, 0.36f, 0.08f, 1f);
    private static final Color PANEL = new Color(0.02f, 0.018f, 0.028f, 0.98f);
    private static final Color BUTTON = new Color(0.03f, 0.026f, 0.035f, 0.96f);
    private static final Color BUTTON_ACTIVE = new Color(0.13f, 0.10f, 0.035f, 0.96f);
    private static final Color DISABLED_BUTTON = new Color(0.05f, 0.05f, 0.06f, 0.85f);

    // ===== 像素风调色板（程序化像素精灵）=====
    private static final Color PX_OUTLINE = new Color(0.03f, 0.025f, 0.05f, 1f);
    private static final Color SOUL_BODY = new Color(0.95f, 0.11f, 0.13f, 1f);
    private static final Color SOUL_HI = new Color(1f, 0.52f, 0.55f, 1f);
    private static final Color BUL_WHITE = new Color(0.97f, 0.97f, 0.95f, 1f);
    private static final Color BUL_GOLD_HI = new Color(1f, 0.86f, 0.42f, 1f);
    private static final Color BUL_CYAN = new Color(0.42f, 0.72f, 1f, 1f);
    private static final Color BUL_AMBER = new Color(1f, 0.70f, 0.20f, 1f);
    private static final Color BUL_CRIMSON = new Color(0.86f, 0.16f, 0.20f, 1f);
    private static final Color BUL_ROCK = new Color(0.55f, 0.50f, 0.46f, 1f);
    private static final Color BUL_ROCK_HI = new Color(0.78f, 0.74f, 0.68f, 1f);
    private static final Color BUL_PURPLE = new Color(0.66f, 0.42f, 0.95f, 1f);
    private static final Color BUL_MAGENTA = new Color(1f, 0.20f, 0.78f, 1f);
    private static final Color STEEL = new Color(0.74f, 0.78f, 0.86f, 1f);
    private static final Color STEEL_HI = new Color(0.93f, 0.96f, 1f, 1f);
    private static final Color EARTH = new Color(0.45f, 0.32f, 0.18f, 1f);
    private static final Color EARTH_HI = new Color(0.56f, 0.42f, 0.25f, 1f);
    private static final Color EARTH_TOP = new Color(0.34f, 0.55f, 0.24f, 1f);
    private static final Color GUARD_GOLD = new Color(0.88f, 0.66f, 0.20f, 1f);
    private static final Color HANDLE_BROWN = new Color(0.40f, 0.24f, 0.12f, 1f);

    // 位图：'o'=描边 'b'=主体 'h'=高光 '.'=透明
    private static final String[] PX_HEART = {
        ".oo.oo.",
        "ohbobbo",
        "obbbbbo",
        ".obbbo.",
        "..obo..",
        "...o..."
    };
    private static final String[] PX_ORB = {
        ".ooo.",
        "ohbbo",
        "obbbo",
        "obbbo",
        ".ooo."
    };
    private static final String[] PX_DIAMOND = {
        "..o..",
        ".obo.",
        "obhbo",
        ".obo.",
        "..o.."
    };
    private static final String[] PX_STAR = {
        "..o..",
        "o.b.o",
        ".bhb.",
        "o.b.o",
        "..o.."
    };

    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final GlyphLayout layout;

    public UtCombatRenderer(SpriteBatch batch, ShapeRenderer shapes,
                             BitmapFont font, BitmapFont smallFont)
    {
        this.batch = batch;
        this.shapes = shapes;
        this.font = font;
        this.smallFont = smallFont;
        this.layout = new GlyphLayout();
    }

    public String formatUtCombat(UndertaleCombatEngine ut, GameEngine engine)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[UT] ").append(ut.getDef().displayName)
            .append(" HP:").append(ut.snapshot().npcHp).append("/").append(ut.getDef().maxHp)
            .append(" | 你 HP:").append(ut.snapshot().playerHp).append("/").append(engine.getPlayer().getMaxHp())
            .append("\n");
        sb.append(ut.getPhaseMessage()).append("\n");

        UndertaleCombatPhase phase = ut.getPhase();
        if (phase == UndertaleCombatPhase.MENU) {
            sb.append("按 1-4 选择行动：1 攻击 2 行动 3 物品 4 仁慈");
        } else if (phase == UndertaleCombatPhase.FIGHT_BAR) {
            sb.append("ENTER/Space=攻击! [");
            int pos = (int)(ut.getFightBarPos() * 20);
            for (int i = 0; i < 20; i++)
                sb.append(i == 10 ? "|" : i == pos ? "▌" : "·");
            sb.append("]");
        } else if (phase == UndertaleCombatPhase.ENEMY_TURN) {
            sb.append("WASD/方向键=躲避! 子弹:").append(ut.getBullets().size())
                .append(" 预警:").append(ut.getWarnings().size());
        }
        return sb.toString();
    }

    public UndertaleCombatEngine utEngine(GameEngine engine)
    {
        CombatSystem cs = engine.getCombatSystem();
        return (cs instanceof UndertaleCombatEngine) ? (UndertaleCombatEngine) cs : null;
    }

    public void render(UndertaleCombatEngine ut, GameEngine engine,
                       float boxX, float boxY, float boxW, float boxH)
    {
        render(ut, engine, null, boxX, boxY, boxW, boxH);
    }

    public void render(UndertaleCombatEngine ut, GameEngine engine, CombatFx fx,
                       float boxX, float boxY, float boxW, float boxH)
    {
        UndertaleCombatPhase phase = ut.getPhase();

        drawBattleScene(boxX, boxY, boxW, boxH, phase);

        if (phase == UndertaleCombatPhase.ENEMY_TURN) {
            // 重力模式：画出地面，便于判断落点
            if (ut.isGravityMode()) drawGravityGround(boxX, boxY, boxW, boxH);
            drawUtWarnings(ut, boxX, boxY, boxW, boxH);
            drawUtBullets(ut, boxX, boxY, boxW, boxH);
            // 受击无敌帧：隔帧闪烁
            if (fx == null || fx.soulVisible())
                drawUtSoul(ut, boxX, boxY, boxW, boxH);
        }

        if (phase == UndertaleCombatPhase.FIGHT_BAR) {
            drawUtFightBar(ut, boxX, boxY, boxW, boxH);
        }
        drawBattleFrame(boxX, boxY, boxW, boxH);

        float statusY = boxY + boxH + 26f;
        float hpBarY = boxY + boxH + 8f;
        float columnGap = 24f;
        float columnW = Math.max(120f, (boxW - columnGap) / 2f);
        float enemyX = boxX;
        float playerX = boxX + columnW + columnGap;
        float enemyRatio = (float) ut.snapshot().npcHp / ut.getDef().maxHp;
        float playerRatio = (float) ut.snapshot().playerHp / engine.getPlayer().getMaxHp();
        float enemyShake = 0f, playerShake = 0f;
        if (fx != null) {
            enemyRatio = fx.enemyBarRatio(enemyRatio);
            playerRatio = fx.playerBarRatio(playerRatio);
            enemyShake = fx.enemyBarShakeX();
            playerShake = fx.playerBarShakeX();
        }
        drawHpBar(enemyX + enemyShake, hpBarY, columnW, 10f, enemyRatio, true);
        drawHpBar(playerX + playerShake, hpBarY, columnW, 10f, playerRatio, false);

        float btnGap = 8f;
        float btnH = 34f;
        float btnW = (boxW - btnGap * 3f) / 4f;
        float btnY = boxY - btnH - 12f;
        if (phase == UndertaleCombatPhase.MENU) {
            for (int i = 0; i < 4; i++) {
                float bx = boxX + i * (btnW + btnGap);
                boolean selected = (i == ut.getMenuIndex());
                boolean disabled = (i == 3 && !ut.canMercy());  // MERCY 灰掉
                drawUtButtonBg(bx, btnY, btnW, btnH, selected, disabled);
            }
            // 纯数字菜单（menuIndex<0）不画游标
            if (ut.getMenuIndex() >= 0)
                drawMenuCursor(boxX + ut.getMenuIndex() * (btnW + btnGap), btnY, btnW, btnH);
        } else {
            drawUtMessageBg(boxX, boxY - 42f, boxW, 30f);
        }

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, hpLabel(ut.getDef().displayName, ut.snapshot().npcHp, ut.getDef().maxHp),
            enemyX, statusY);
        font.draw(batch, hpLabel(engine.getPlayer().getName(), ut.snapshot().playerHp,
            engine.getPlayer().getMaxHp()), playerX, statusY);

        smallFont.setColor(Color.WHITE);
        if (phase == UndertaleCombatPhase.MENU) {
            for (int i = 0; i < 4; i++) {
                float bx = boxX + i * (btnW + btnGap);
                boolean sel = (i == ut.getMenuIndex());
                boolean dis = (i == 3 && !ut.canMercy());
                drawButtonLabel(i, bx, btnY, btnW, btnH, sel, dis);
            }
        } else {
            smallFont.draw(batch, phaseHint(ut), boxX + 12f, boxY - 20f);
        }
        batch.end();
    }

    private String hpLabel(String name, int hp, int maxHp)
    {
        return name + "  " + hp + "/" + maxHp;
    }

    private String phaseHint(UndertaleCombatEngine ut)
    {
        if (ut.getPhase() == UndertaleCombatPhase.ENEMY_TURN)
            return "WASD / 方向键移动红心，避开白色弹幕。";
        if (ut.getPhase() == UndertaleCombatPhase.FIGHT_BAR)
            return "在中心区域按 Enter / Space 攻击。";
        return ut.getPhaseMessage();
    }

    private void drawButtonLabel(int index, float x, float y, float w, float h, boolean selected, boolean disabled)
    {
        String text = (index + 1) + "  " + MENU_LABELS[index];
        layout.setText(smallFont, text);
        if (disabled) {
            smallFont.setColor(0.35f, 0.35f, 0.38f, 0.6f);
        } else {
            smallFont.setColor(selected ? GOLD : WHITE);
        }
        smallFont.draw(batch, text, x + (w - layout.width) / 2f + 8f,
            y + (h + layout.height) / 2f + 1f);
    }

    public void drawUtSoul(UndertaleCombatEngine ut,
                            float boxX, float boxY, float boxW, float boxH)
    {
        float sx = boxX + ut.getSoulX() * boxW;
        float sy = boxY + ut.getSoulY() * boxH;
        float cell = Math.max(2.5f, Math.min(boxW, boxH) * 0.013f);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawPixelSprite(sx, sy, cell, PX_HEART, SOUL_BODY, SOUL_HI);
        shapes.end();
    }

    public void drawUtWarnings(UndertaleCombatEngine ut,
                               float boxX, float boxY, float boxW, float boxH)
    {
        if (ut.getWarnings().isEmpty()) return;
        boolean red = (System.nanoTime() / 90_000_000L) % 2L == 0L;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (WarningZone warning : ut.getWarnings()) {
            float alpha = warning.alpha();
            if (red) shapes.setColor(1f, 0.08f, 0.04f, 0.25f + 0.55f * alpha);
            else shapes.setColor(1f, 0.78f, 0.08f, 0.25f + 0.45f * alpha);

            if (warning.type == WarningZone.Type.BOX) {
                float x1 = boxX + Math.min(warning.x1, warning.x2) * boxW;
                float y1 = boxY + Math.min(warning.y1, warning.y2) * boxH;
                float x2 = boxX + Math.max(warning.x1, warning.x2) * boxW;
                float y2 = boxY + Math.max(warning.y1, warning.y2) * boxH;
                drawFrame(x1, y1, x2 - x1, y2 - y1, 3f);
            } else {
                drawWarningLine(warning, boxX, boxY, boxW, boxH);
            }
        }
        shapes.end();
    }

    public void drawUtBullets(UndertaleCombatEngine ut,
                               float boxX, float boxY, float boxW, float boxH)
    {
        String npcId = ut.getDef() != null ? ut.getDef().npcId : null;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Bullet b : ut.getBullets()) {
            if (!b.alive) continue;
            if (b.shape == Bullet.Shape.CIRCLE) {
                float bx = boxX + b.x * boxW;
                float by = boxY + b.y * boxH;
                float br = Math.max(5f, b.radius * Math.min(boxW, boxH) * 1.55f);
                if (b.kind == Bullet.Kind.AIMED) {
                    // 瞄准弹：醒目的品红菱晶（与各 NPC 子弹明显区分）
                    drawPixelSprite(bx, by, Math.max(2.4f, br * 0.5f), PX_DIAMOND, BUL_MAGENTA, BUL_WHITE);
                } else {
                    drawCircleBullet(bx, by, br, b.visualVariant, npcId);
                }
            } else {
                float bx = boxX + b.x * boxW;
                float by = boxY + b.y * boxH;
                float bw = Math.max(6f, b.width * boxW);
                float bh = Math.max(6f, b.height * boxH);
                if (b.kind == Bullet.Kind.SWORD) drawSword(b, bx, by, bw, bh);
                else if (b.kind == Bullet.Kind.WALL) drawWall(bx, by, bw, bh);
                else drawBoneRect(bx, by, bw, bh);
            }
        }
        shapes.end();
    }

    /**
     * 按 NPC 选择子弹"族系"：不同敌人发射不同像素子弹（形状 + 配色），
     * 增强辨识度与战斗多样性。未知 NPC 回退到按 visualVariant 轮换。
     */
    private void drawCircleBullet(float x, float y, float r, int visualVariant, String npcId)
    {
        String[] bmp;
        Color body, hi;
        float sizeMul = 1f;
        switch (npcId == null ? "" : npcId) {
            case "guard":                       // 守卫：钢蓝圆弹
                bmp = PX_ORB; body = BUL_WHITE; hi = BUL_GOLD_HI; break;
            case "priest":                      // 神父：神圣菱晶
                bmp = PX_DIAMOND; body = BUL_WHITE; hi = BUL_CYAN; break;
            case "follower":                    // 追随者：血色圆弹
                bmp = PX_ORB; body = BUL_CRIMSON; hi = BUL_WHITE; break;
            case "apprentice":                  // 学徒：魔法星火
                bmp = PX_STAR; body = BUL_PURPLE; hi = BUL_WHITE; break;
            case "golem":                       // 魔像：沉重碎石（更大）
                bmp = PX_ORB; body = BUL_ROCK; hi = BUL_ROCK_HI; sizeMul = 1.25f; break;
            default:                            // 其他：按变体轮换形状
                int s = Math.floorMod(visualVariant, 3);
                bmp = s == 0 ? PX_ORB : s == 1 ? PX_DIAMOND : PX_STAR;
                body = s == 0 ? BUL_WHITE : s == 1 ? BUL_CYAN : BUL_AMBER;
                hi = BUL_WHITE; break;
        }
        float cell = Math.max(2f, r * 0.42f * sizeMul);
        drawPixelSprite(x, y, cell, bmp, body, hi);
    }

    /**
     * 程序化像素精灵：按位图网格以小方块拼绘，带黑描边/高光，并将原点对齐到整数像素，
     * 营造刻意的像素风（避免平滑圆形那种"贴图"感）。
     * 须在 {@code shapes.begin(Filled)} 与 {@code end()} 之间调用。
     *
     * @param cx,cy 精灵中心（屏幕像素）
     * @param cell  单个"像素"的边长（屏幕像素）
     * @param rows  位图行（'o'描边 'b'主体 'h'高光 '.'透明）
     */
    private void drawPixelSprite(float cx, float cy, float cell, String[] rows,
                                 Color body, Color hi)
    {
        int rowsN = rows.length;
        int colsN = rows[0].length();
        float x0 = Math.round(cx - colsN * cell / 2f);
        float y0 = Math.round(cy - rowsN * cell / 2f);
        for (int rIdx = 0; rIdx < rowsN; rIdx++) {
            String row = rows[rIdx];
            float ry = y0 + (rowsN - 1 - rIdx) * cell;   // 行从上到下，y 向上
            for (int c = 0; c < colsN; c++) {
                char ch = row.charAt(c);
                if (ch == '.') continue;
                if (ch == 'o') shapes.setColor(PX_OUTLINE);
                else if (ch == 'h') shapes.setColor(hi);
                else shapes.setColor(body);
                shapes.rect(x0 + c * cell, ry, cell, cell);
            }
        }
    }

    private void drawBoneRect(float x, float y, float w, float h)
    {
        drawBoneShape(x - 1.5f, y - 1.5f, w + 3f, h + 3f, Color.BLACK);
        drawBoneShape(x, y, w, h, WHITE);
    }

    /** 重力模式地面：底部一条泥土带 + 草沿，给玩家落点参考。 */
    private void drawGravityGround(float boxX, float boxY, float boxW, float boxH)
    {
        float gh = Math.max(8f, boxH * 0.05f);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(EARTH);
        shapes.rect(boxX, boxY, boxW, gh);
        shapes.setColor(EARTH_TOP);
        shapes.rect(boxX, boxY + gh - Math.max(2f, gh * 0.25f), boxW, Math.max(2f, gh * 0.25f));
        shapes.end();
    }

    /**
     * 真正的剑（守卫招式）：钢色刀身 + 中央血槽高光 + 尖锐剑尖（三角）+ 金色护手十字 + 木柄。
     * <b>剑尖朝飞行方向（前端），剑柄在尾端。</b>朝向由子弹速度/嵌入位置推断。
     */
    private void drawSword(Bullet b, float x, float y, float w, float h)
    {
        boolean horizontal = w >= h;
        if (horizontal) {
            // 剑尖朝左：飞行向左，或已嵌入左框
            boolean tipLeft = b.vx < 0f || (b.vx == 0f && b.x <= 0.05f);
            float handleLen = w * 0.20f;
            float bladeLen = w - handleLen;
            float midY = y + h / 2f;
            float guardW = Math.max(3f, w * 0.025f);
            if (tipLeft) {
                // 刀身在左[x, x+bladeLen]，柄在右
                shapes.setColor(STEEL);
                shapes.rect(x, y, bladeLen, h);
                shapes.triangle(x - h * 0.9f, midY, x, y, x, y + h);            // 剑尖在最左
                shapes.setColor(STEEL_HI);
                shapes.rect(x, y + h * 0.38f, bladeLen, Math.max(1f, h * 0.24f));
                shapes.setColor(GUARD_GOLD);
                shapes.rect(x + bladeLen - guardW / 2f, y - h * 0.85f, guardW, h * 2.7f); // 护手在刀身/柄交界
                shapes.setColor(HANDLE_BROWN);
                shapes.rect(x + bladeLen + handleLen * 0.1f, y + h * 0.12f, handleLen * 0.8f, h * 0.76f); // 柄在最右
            } else {
                // 刀身在右[x+handleLen, x+w]，柄在左
                shapes.setColor(STEEL);
                shapes.rect(x + handleLen, y, bladeLen, h);
                shapes.triangle(x + w + h * 0.9f, midY, x + w, y, x + w, y + h);  // 剑尖在最右
                shapes.setColor(STEEL_HI);
                shapes.rect(x + handleLen, y + h * 0.38f, bladeLen, Math.max(1f, h * 0.24f));
                shapes.setColor(GUARD_GOLD);
                shapes.rect(x + handleLen - guardW / 2f, y - h * 0.85f, guardW, h * 2.7f);
                shapes.setColor(HANDLE_BROWN);
                shapes.rect(x + handleLen * 0.1f, y + h * 0.12f, handleLen * 0.8f, h * 0.76f); // 柄在最左
            }
        } else {
            // 剑尖朝下：飞行向下（vy<0），或已嵌入底框
            boolean tipDown = b.vy < 0f || (b.vy == 0f && b.y <= 0.05f);
            float handleLen = h * 0.20f;
            float bladeLen = h - handleLen;
            float midX = x + w / 2f;
            float guardH = Math.max(3f, h * 0.025f);
            if (tipDown) {
                // 刀身在下[y, y+bladeLen]，柄在上
                shapes.setColor(STEEL);
                shapes.rect(x, y, w, bladeLen);
                shapes.triangle(midX, y - w * 0.9f, x, y, x + w, y);            // 剑尖在最下
                shapes.setColor(STEEL_HI);
                shapes.rect(x + w * 0.38f, y, Math.max(1f, w * 0.24f), bladeLen);
                shapes.setColor(GUARD_GOLD);
                shapes.rect(x - w * 0.85f, y + bladeLen - guardH / 2f, w * 2.7f, guardH);
                shapes.setColor(HANDLE_BROWN);
                shapes.rect(x + w * 0.12f, y + bladeLen + handleLen * 0.1f, w * 0.76f, handleLen * 0.8f); // 柄在最上
            } else {
                // 刀身在上[y+handleLen, y+h]，柄在下
                shapes.setColor(STEEL);
                shapes.rect(x, y + handleLen, w, bladeLen);
                shapes.triangle(midX, y + h + w * 0.9f, x, y + h, x + w, y + h);  // 剑尖在最上
                shapes.setColor(STEEL_HI);
                shapes.rect(x + w * 0.38f, y + handleLen, Math.max(1f, w * 0.24f), bladeLen);
                shapes.setColor(GUARD_GOLD);
                shapes.rect(x - w * 0.85f, y + handleLen - guardH / 2f, w * 2.7f, guardH);
                shapes.setColor(HANDLE_BROWN);
                shapes.rect(x + w * 0.12f, y + handleLen * 0.1f, w * 0.76f, handleLen * 0.8f); // 柄在最下
            }
        }
    }

    /** 土墙（魔像招式）：泥褐砖块 + 顶部草沿，带黑描边。 */
    private void drawWall(float x, float y, float w, float h)
    {
        shapes.setColor(PX_OUTLINE);
        shapes.rect(x - 2f, y - 2f, w + 4f, h + 4f);
        shapes.setColor(EARTH);
        shapes.rect(x, y, w, h);
        // 砖块纹理：交错的高光小块
        shapes.setColor(EARTH_HI);
        float cell = Math.max(4f, w * 0.45f);
        for (float yy = y + 3f; yy < y + h - cell; yy += cell * 2f) {
            shapes.rect(x + 2f, yy, Math.max(2f, w * 0.35f), Math.max(2f, cell * 0.6f));
            shapes.rect(x + w * 0.55f, yy + cell, Math.max(2f, w * 0.35f), Math.max(2f, cell * 0.6f));
        }
        // 顶部草沿
        shapes.setColor(EARTH_TOP);
        shapes.rect(x, y + h - Math.max(3f, h * 0.12f), w, Math.max(3f, h * 0.12f));
    }

    private void drawBoneShape(float x, float y, float w, float h, Color color)
    {
        shapes.setColor(color);
        if (w >= h) {
            float cy = y + h / 2f;
            float r = Math.max(3f, Math.min(h * 0.62f, w * 0.18f));
            float bodyH = Math.max(3f, h * 0.46f);
            shapes.rect(x + r, cy - bodyH / 2f, Math.max(1f, w - r * 2f), bodyH);
            shapes.circle(x + r, cy, r, 10);
            shapes.circle(x + w - r, cy, r, 10);
        } else {
            float cx = x + w / 2f;
            float r = Math.max(3f, Math.min(w * 0.62f, h * 0.18f));
            float bodyW = Math.max(3f, w * 0.46f);
            shapes.rect(cx - bodyW / 2f, y + r, bodyW, Math.max(1f, h - r * 2f));
            shapes.circle(cx, y + r, r, 10);
            shapes.circle(cx, y + h - r, r, 10);
        }
    }

    public void drawUtFightBar(UndertaleCombatEngine ut,
                                float boxX, float boxY, float boxW, float boxH)
    {
        float panelW = boxW * 0.72f;
        float panelH = 70f;
        float panelX = boxX + (boxW - panelW) / 2f;
        float panelY = boxY + boxH * 0.50f - panelH / 2f;
        float barW = panelW - 44f;
        float barH = 24f;
        float barX = panelX + 22f;
        float barY = panelY + 22f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.96f);
        shapes.rect(panelX, panelY, panelW, panelH);
        shapes.setColor(WHITE);
        drawFrame(panelX, panelY, panelW, panelH, 3f);

        shapes.setColor(0.055f, 0.048f, 0.06f, 1f);
        shapes.rect(barX, barY, barW, barH);
        shapes.setColor(WHITE);
        drawFrame(barX, barY, barW, barH, 2f);
        shapes.setColor(0.36f, 0.04f, 0.02f, 1f);
        shapes.rect(barX + barW * 0.42f, barY + 2f, barW * 0.16f, barH - 4f);
        shapes.setColor(1f, 0.78f, 0.16f, 1f);
        shapes.rect(barX + barW * 0.47f, barY + 2f, barW * 0.06f, barH - 4f);

        float slashX = barX + ut.getFightBarPos() * barW;
        shapes.setColor(Color.BLACK);
        shapes.rect(slashX - 4f, barY - 8f, 8f, barH + 16f);
        shapes.setColor(1f, 0.12f, 0.10f, 1f);
        shapes.rect(slashX - 2f, barY - 8f, 4f, barH + 16f);
        shapes.end();
    }

    public void drawUtButtonBg(float x, float y, float w, float h)
    {
        drawUtButtonBg(x, y, w, h, false, false);
    }

    private void drawUtButtonBg(float x, float y, float w, float h, boolean active, boolean disabled)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (disabled) {
            shapes.setColor(DISABLED_BUTTON);
        } else {
            shapes.setColor(active ? BUTTON_ACTIVE : BUTTON);
        }
        shapes.rect(x, y, w, h);
        if (!disabled) {
            shapes.setColor(active ? GOLD : GOLD_DIM);
            drawFrame(x, y, w, h, active ? 3f : 2f);
        } else {
            shapes.setColor(0.2f, 0.2f, 0.22f, 1f);
            drawFrame(x, y, w, h, 1f);
        }
        if (active && !disabled) {
            shapes.setColor(1f, 0.78f, 0.16f, 0.18f);
            shapes.rect(x + 5f, y + 5f, w - 10f, h - 10f);
        }
        shapes.end();
    }

    public void drawUtMessageBg(float x, float y, float w, float h)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.018f, 0.026f, 0.96f);
        shapes.rect(x, y, w, h);
        shapes.setColor(WHITE);
        drawFrame(x, y, w, h, 2f);
        shapes.end();
    }

    public void drawHpBar(float x, float y, float w, float h,
                           float ratio, boolean enemy)
    {
        float clamped = Math.max(0f, Math.min(ratio, 1f));
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.05f, 0.045f, 0.04f, 1f);
        shapes.rect(x, y, w, h);
        shapes.setColor(0.38f, 0.24f, 0.11f, 1f);
        drawFrame(x, y, w, h, 1.5f);
        if (clamped > 0f) {
            shapes.setColor(enemy ? 0.96f : 1f, enemy ? 0.16f : 0.84f, enemy ? 0.10f : 0.10f, 1f);
            shapes.rect(x + 2f, y + 2f, (w - 4f) * clamped, h - 4f);
        }
        shapes.end();
    }

    private void drawBattleScene(float x, float y, float w, float h, UndertaleCombatPhase phase)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(PANEL);
        shapes.rect(x, y, w, h);
        shapes.setColor(0.06f, 0.055f, 0.07f,
            phase == UndertaleCombatPhase.ENEMY_TURN ? 0.52f : 0.30f);
        shapes.rect(x + 6f, y + 6f, w - 12f, h - 12f);
        shapes.end();
    }

    private void drawBattleFrame(float x, float y, float w, float h)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(WHITE);
        drawFrame(x, y, w, h, 4f);
        shapes.setColor(Color.BLACK);
        drawFrame(x + 4f, y + 4f, w - 8f, h - 8f, 1f);
        shapes.end();
    }

    private void drawMenuCursor(float x, float y, float w, float h)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawHeart(x + 18f, y + h / 2f + 1f, 6.6f, Color.BLACK);
        drawHeart(x + 18f, y + h / 2f + 1f, 5.2f, new Color(1f, 0.08f, 0.08f, 1f));
        shapes.end();
    }

    private void drawWarningLine(WarningZone warning, float boxX, float boxY, float boxW, float boxH)
    {
        float x1 = boxX + warning.x1 * boxW;
        float y1 = boxY + warning.y1 * boxH;
        float x2 = boxX + warning.x2 * boxW;
        float y2 = boxY + warning.y2 * boxH;
        if (Math.abs(y1 - y2) < 0.5f) {
            float x = Math.min(x1, x2);
            shapes.rect(x, y1 - 2f, Math.abs(x2 - x1), 4f);
        } else if (Math.abs(x1 - x2) < 0.5f) {
            float y = Math.min(y1, y2);
            shapes.rect(x1 - 2f, y, 4f, Math.abs(y2 - y1));
        } else {
            float cx = (x1 + x2) / 2f;
            float cy = (y1 + y2) / 2f;
            shapes.rect(cx - 2f, cy - 2f, 4f, 4f);
        }
    }

    private void drawHeart(float x, float y, float r, Color color)
    {
        shapes.setColor(color);
        shapes.circle(x - r * 0.38f, y + r * 0.22f, r * 0.52f, 14);
        shapes.circle(x + r * 0.38f, y + r * 0.22f, r * 0.52f, 14);
        shapes.triangle(x - r * 0.95f, y + r * 0.12f,
            x + r * 0.95f, y + r * 0.12f,
            x, y - r * 1.08f);
    }

    private void drawFrame(float x, float y, float w, float h, float t)
    {
        shapes.rect(x, y, w, t);
        shapes.rect(x, y + h - t, w, t);
        shapes.rect(x, y, t, h);
        shapes.rect(x + w - t, y, t, h);
    }

    @Override
    public void dispose()
    {
        // Procedural renderer: no texture resources to dispose.
    }
}
