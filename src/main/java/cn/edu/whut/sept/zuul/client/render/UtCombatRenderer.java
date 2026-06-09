package cn.edu.whut.sept.zuul.client.render;

import cn.edu.whut.sept.zuul.engine.Bullet;
import cn.edu.whut.sept.zuul.engine.CombatSystem;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatPhase;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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

    public UtCombatRenderer(SpriteBatch batch, ShapeRenderer shapes,
                             BitmapFont font, BitmapFont smallFont)
    {
        this.batch = batch;
        this.shapes = shapes;
        this.font = font;
        this.smallFont = smallFont;
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

        // HP 条
        batch.begin();
        int sw = Gdx.graphics.getWidth();
        float topY = boxY + boxH + 8f;
        font.setColor(Color.WHITE);
        font.draw(batch, ut.getDef().displayName, boxX, topY);
        drawHpBar(boxX + sw * 0.28f, topY - 14f, sw * 0.18f, 10f,
            (float) ut.snapshot().npcHp / ut.getDef().maxHp, true);

        font.draw(batch, engine.getPlayer().getName(), boxX + boxW - sw * 0.22f, topY);
        drawHpBar(boxX + boxW - sw * 0.22f + sw * 0.06f, topY - 14f, sw * 0.16f, 10f,
            (float) ut.snapshot().playerHp / engine.getPlayer().getMaxHp(), false);
        batch.end();

        // 菜单 / 提示
        batch.begin();
        float bottomY = boxY - 12f;
        smallFont.setColor(Color.WHITE);
        if (phase == UndertaleCombatPhase.MENU) {
            float btnW = 110f;
            float btnH = 28f;
            String[] labels = {"1 FIGHT", "2 ACT", "3 ITEM", "4 MERCY"};
            for (int i = 0; i < 4; i++) {
                float bx = boxX + i * (btnW + 6f);
                drawUtButtonBg(bx, boxY - btnH - 6f, btnW, btnH);
            }
            for (int i = 0; i < 4; i++) {
                float bx = boxX + i * (btnW + 6f);
                smallFont.draw(batch, labels[i], bx + 8f, boxY - btnH - 6f + btnH - 8f);
            }
        } else {
            smallFont.draw(batch, ut.getPhaseMessage(), boxX, bottomY);
        }
        batch.end();
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
        shapes.setColor(0.12f, 0.12f, 0.18f, 0.9f);
        shapes.rect(x, y, w, h);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(1f, 0.55f, 0.1f, 0.65f);
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
