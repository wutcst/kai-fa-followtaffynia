package cn.edu.whut.sept.zuul.client.render;

import cn.edu.whut.sept.zuul.engine.Bullet;
import cn.edu.whut.sept.zuul.engine.CombatSystem;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatPhase;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * UT 战斗渲染器 —— 从 GameScreen 提取的纯渲染逻辑。
 */
public class UtCombatRenderer
{
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
            sb.append("1=FIGHT 2=ACT 3=ITEM 4=MERCY");
        } else if (phase == UndertaleCombatPhase.FIGHT_BAR) {
            sb.append("ENTER/Space=攻击! [");
            int pos = (int)(ut.getFightBarPos() * 20);
            for (int i = 0; i < 20; i++)
                sb.append(i == 10 ? "|" : i == pos ? "▌" : "·");
            sb.append("]");
        } else if (phase == UndertaleCombatPhase.ENEMY_TURN) {
            sb.append("WASD=躲避! 子弹:").append(ut.getBullets().size())
                .append(" [").append(ut.getSoulX()).append(",").append(ut.getSoulY()).append("]");
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
        UndertaleCombatPhase phase = ut.getPhase();

        // 弹幕 + 灵魂
        if (phase == UndertaleCombatPhase.ENEMY_TURN) {
            drawUtBullets(ut, boxX, boxY, boxW, boxH);
        }
        drawUtSoul(ut, boxX, boxY, boxW, boxH);

        // 节奏条
        if (phase == UndertaleCombatPhase.FIGHT_BAR) {
            drawUtFightBar(ut, boxX, boxY, boxW, boxH);
        }

        float statusY = boxY + boxH + 26f;
        float hpBarY = boxY + boxH + 8f;
        float columnGap = 24f;
        float columnW = Math.max(120f, (boxW - columnGap) / 2f);
        float enemyX = boxX;
        float playerX = boxX + columnW + columnGap;
        drawHpBar(enemyX, hpBarY, columnW, 10f,
            (float) ut.snapshot().npcHp / ut.getDef().maxHp, true);
        drawHpBar(playerX, hpBarY, columnW, 10f,
            (float) ut.snapshot().playerHp / engine.getPlayer().getMaxHp(), false);

        float btnGap = 8f;
        float btnH = 32f;
        float btnW = (boxW - btnGap * 3f) / 4f;
        float btnY = boxY - btnH - 12f;
        if (phase == UndertaleCombatPhase.MENU) {
            for (int i = 0; i < 4; i++) {
                drawUtButtonBg(boxX + i * (btnW + btnGap), btnY, btnW, btnH);
            }
        } else {
            drawUtMessageBg(boxX, boxY - 40f, boxW, 28f);
        }

        // 文本必须在所有 ShapeRenderer 绘制结束后再画，避免 GL 状态互相污染。
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, hpLabel(ut.getDef().displayName, ut.snapshot().npcHp, ut.getDef().maxHp),
            enemyX, statusY);
        font.draw(batch, hpLabel(engine.getPlayer().getName(), ut.snapshot().playerHp,
            engine.getPlayer().getMaxHp()), playerX, statusY);

        smallFont.setColor(Color.WHITE);
        if (phase == UndertaleCombatPhase.MENU) {
            String[] labels = {"1 FIGHT", "2 ACT", "3 ITEM", "4 MERCY"};
            for (int i = 0; i < 4; i++) {
                float bx = boxX + i * (btnW + btnGap);
                drawCenteredSmall(labels[i], bx, btnY, btnW, btnH);
            }
        } else {
            smallFont.draw(batch, ut.getPhaseMessage(), boxX + 12f, boxY - 20f);
        }
        batch.end();
    }

    private String hpLabel(String name, int hp, int maxHp)
    {
        return name + "  " + hp + "/" + maxHp;
    }

    private void drawCenteredSmall(String text, float x, float y, float w, float h)
    {
        layout.setText(smallFont, text);
        smallFont.draw(batch, text, x + (w - layout.width) / 2f,
            y + (h + layout.height) / 2f + 1f);
    }

    public void drawUtSoul(UndertaleCombatEngine ut,
                            float boxX, float boxY, float boxW, float boxH)
    {
        float sx = boxX + ut.getSoulX() * boxW;
        float sy = boxY + ut.getSoulY() * boxH;
        float sr = 7f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0.15f, 0.15f, 1f);
        shapes.circle(sx, sy, sr, 14);
        shapes.setColor(1f, 0.35f, 0.35f, 0.4f);
        shapes.circle(sx, sy, sr + 2f, 14);
        shapes.end();
    }

    public void drawUtBullets(UndertaleCombatEngine ut,
                               float boxX, float boxY, float boxW, float boxH)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Bullet b : ut.getBullets()) {
            if (!b.alive) continue;
            float bx = boxX + b.x * boxW;
            float by = boxY + b.y * boxH;
            if (b.shape == Bullet.Shape.CIRCLE) {
                float br = b.radius * boxW * 0.8f;
                shapes.setColor(1f, 1f, 0.85f, 1f);
                shapes.circle(bx, by, Math.max(br, 3f), 8);
            } else {
                float bw = b.width * boxW;
                float bh = b.height * boxH;
                shapes.setColor(1f, 1f, 0.85f, 1f);
                shapes.rect(bx, by, Math.max(bw, 4f), Math.max(bh, 4f));
            }
        }
        shapes.end();
    }

    public void drawUtFightBar(UndertaleCombatEngine ut,
                                float boxX, float boxY, float boxW, float boxH)
    {
        float barW = boxW * 0.5f;
        float barH = 10f;
        float barX = boxX + (boxW - barW) / 2f;
        float barY = boxY + boxH * 0.72f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.12f, 0.12f, 0.12f, 1f);
        shapes.rect(barX, barY, barW, barH);
        shapes.setColor(0.05f, 0.5f, 0.05f, 0.45f);
        shapes.rect(barX + barW * 0.4f, barY, barW * 0.2f, barH);

        float dotX = barX + ut.getFightBarPos() * barW - 3f;
        shapes.setColor(1f, 0.85f, 0.2f, 1f);
        shapes.rect(dotX, barY - 1f, 6f, barH + 2f);
        shapes.end();
    }

    public void drawUtButtonBg(float x, float y, float w, float h)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.12f, 0.12f, 0.18f, 0.94f);
        shapes.rect(x, y, w, h);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(1f, 0.62f, 0.18f, 0.82f);
        shapes.rect(x, y, w, h);
        shapes.end();
    }

    public void drawUtMessageBg(float x, float y, float w, float h)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.06f, 0.055f, 0.09f, 0.94f);
        shapes.rect(x, y, w, h);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(1f, 1f, 1f, 0.35f);
        shapes.rect(x, y, w, h);
        shapes.end();
    }

    public void drawHpBar(float x, float y, float w, float h,
                           float ratio, boolean enemy)
    {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.12f, 0.12f, 0.12f, 1f);
        shapes.rect(x, y, w, h);
        if (ratio > 0f) {
            shapes.setColor(enemy ? 0.95f : 0.15f, enemy ? 0.2f : 0.75f, enemy ? 0.1f : 0.15f, 1f);
            shapes.rect(x, y, w * Math.min(ratio, 1f), h);
        }
        shapes.end();
    }
}
