package cn.edu.whut.sept.zuul.client.render;

import cn.edu.whut.sept.zuul.engine.Bullet;
import cn.edu.whut.sept.zuul.engine.CombatSystem;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatEngine;
import cn.edu.whut.sept.zuul.engine.UndertaleCombatPhase;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;
    private final BitmapFont smallFont;
    private final GlyphLayout layout;
    private final Texture battleBg;
    private final Texture battleFrame;
    private final Texture soulHeart;
    private final Texture bulletDot;
    private final Texture bulletRing;
    private final Texture bulletStar;
    private final Texture buttonFrame;

    public UtCombatRenderer(SpriteBatch batch, ShapeRenderer shapes,
                             BitmapFont font, BitmapFont smallFont)
    {
        this.batch = batch;
        this.shapes = shapes;
        this.font = font;
        this.smallFont = smallFont;
        this.layout = new GlyphLayout();
        this.battleBg = loadOptional("combat/ui/ut-bg.png");
        this.battleFrame = loadOptional("combat/ui/ut-frame.png");
        this.soulHeart = loadOptional("combat/ui/ut-heart.png");
        this.bulletDot = loadOptional("combat/ui/ut-bullet-dot.png");
        this.bulletRing = loadOptional("combat/ui/ut-bullet-ring.png");
        this.bulletStar = loadOptional("combat/ui/ut-bullet-star.png");
        this.buttonFrame = loadOptional("combat/ui/ut-button.png");
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

        drawBattleScene(boxX, boxY, boxW, boxH, phase);

        // 弹幕 + 灵魂。菜单和攻击条阶段不把 soul 放进框里，避免视觉上误导玩家。
        if (phase == UndertaleCombatPhase.ENEMY_TURN) {
            drawUtBullets(ut, boxX, boxY, boxW, boxH);
            drawUtSoul(ut, boxX, boxY, boxW, boxH);
        }

        // 节奏条
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
                drawUtButtonBg(boxX + i * (btnW + btnGap), btnY, btnW, btnH, true);
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

        // 状态文字
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

    private Texture loadOptional(String path)
    {
        try {
            Texture tex = new Texture(Gdx.files.internal(path));
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            return tex;
        } catch (Exception e) {
            return null;
        }
    }

    private String hpLabel(String name, int hp, int maxHp)
    {
        return name + "  " + hp + "/" + maxHp;
    }

    private Color colorFromTag(String tag)
    {
        if (tag == null) return Color.WHITE;
        switch (tag) {
            case "red":   return new Color(1f, 0.25f, 0.2f, 1f);
            case "green": return new Color(0.3f, 1f, 0.35f, 1f);
            case "blue":  return new Color(0.35f, 0.55f, 1f, 1f);
            case "pink":  return new Color(1f, 0.5f, 0.7f, 1f);
            default:      return Color.WHITE;
        }
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
        if (soulHeart != null) {
            float size = 24f;
            batch.begin();
            batch.setColor(Color.WHITE);
            batch.draw(soulHeart, sx - size / 2f, sy - size / 2f, size, size);
            batch.end();
            return;
        }

        float sr = 8f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0.15f, 0.15f, 1f);
        shapes.circle(sx - sr * 0.38f, sy + sr * 0.20f, sr * 0.52f, 12);
        shapes.circle(sx + sr * 0.38f, sy + sr * 0.20f, sr * 0.52f, 12);
        shapes.triangle(sx - sr * 0.92f, sy + sr * 0.1f,
            sx + sr * 0.92f, sy + sr * 0.1f,
            sx, sy - sr * 1.05f);
        shapes.setColor(1f, 0.38f, 0.38f, 0.35f);
        shapes.circle(sx, sy, sr + 4f, 14);
        shapes.end();
    }

    public void drawUtBullets(UndertaleCombatEngine ut,
                               float boxX, float boxY, float boxW, float boxH)
    {
        if (bulletDot != null || bulletRing != null || bulletStar != null) {
            batch.begin();
            int index = 0;
            for (Bullet b : ut.getBullets()) {
                if (!b.alive) continue;
                float bx = boxX + b.x * boxW;
                float by = boxY + b.y * boxH;
                Texture tex = bulletTexture(b, index++);
                float size = b.shape == Bullet.Shape.CIRCLE
                    ? Math.max(12f, b.radius * boxW * 2.1f)
                    : Math.max(16f, Math.max(b.width * boxW, b.height * boxH));
                batch.setColor(1f, 1f, 1f, 0.98f);
                batch.draw(tex, bx - size / 2f, by - size / 2f, size, size);
            }
            batch.setColor(Color.WHITE);
            batch.end();
            return;
        }

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

    private Texture bulletTexture(Bullet b, int index)
    {
        if (b.shape == Bullet.Shape.RECT && bulletRing != null) return bulletRing;
        if (index % 5 == 0 && bulletStar != null) return bulletStar;
        if (index % 3 == 0 && bulletRing != null) return bulletRing;
        if (bulletDot != null) return bulletDot;
        if (bulletRing != null) return bulletRing;
        return bulletStar;
    }

    public void drawUtFightBar(UndertaleCombatEngine ut,
                                float boxX, float boxY, float boxW, float boxH)
    {
        float barW = boxW * 0.5f;
        float barH = 10f;
        float barX = boxX + (boxW - barW) / 2f;
        float barY = boxY + boxH * 0.72f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.95f);
        shapes.rect(barX - 8f, barY - 10f, barW + 16f, barH + 20f);
        shapes.setColor(1f, 1f, 1f, 0.9f);
        shapes.rect(barX, barY, barW, barH);
        shapes.setColor(0.15f, 0.95f, 0.25f, 0.85f);
        shapes.rect(barX + barW * 0.44f, barY, barW * 0.12f, barH);

        float dotX = barX + ut.getFightBarPos() * barW - 3f;
        shapes.setColor(1f, 0.1f, 0.1f, 1f);
        shapes.rect(dotX, barY - 8f, 6f, barH + 16f);
        shapes.end();
    }

    public void drawUtButtonBg(float x, float y, float w, float h)
    {
        drawUtButtonBg(x, y, w, h, false);
    }

    private void drawUtButtonBg(float x, float y, float w, float h, boolean active)
    {
        if (buttonFrame != null) {
            batch.begin();
            batch.setColor(active ? Color.WHITE : new Color(1f, 1f, 1f, 0.78f));
            batch.draw(buttonFrame, x, y, w, h);
            batch.setColor(Color.WHITE);
            batch.end();
            return;
        }

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

    private void drawBattleScene(float x, float y, float w, float h, UndertaleCombatPhase phase)
    {
        if (battleBg == null) return;
        batch.begin();
        batch.setColor(1f, 1f, 1f, phase == UndertaleCombatPhase.ENEMY_TURN ? 0.86f : 0.72f);
        batch.draw(battleBg, x, y, w, h);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawBattleFrame(float x, float y, float w, float h)
    {
        if (battleFrame == null) return;
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(battleFrame, x - 2f, y - 2f, w + 4f, h + 4f);
        batch.end();
    }

    @Override
    public void dispose()
    {
        dispose(battleBg);
        dispose(battleFrame);
        dispose(soulHeart);
        dispose(bulletDot);
        dispose(bulletRing);
        dispose(bulletStar);
        dispose(buttonFrame);
    }

    private void dispose(Texture texture)
    {
        if (texture != null) texture.dispose();
    }
}
