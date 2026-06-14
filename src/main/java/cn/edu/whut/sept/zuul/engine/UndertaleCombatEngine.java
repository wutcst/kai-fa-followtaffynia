package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.NpcCombatDef;
import cn.edu.whut.sept.zuul.domain.Player;
import cn.edu.whut.sept.zuul.engine.effect.combat.CombatActionRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Undertale 式战斗引擎：菜单 → 玩家行动 → 弹幕躲避 → 循环。
 *
 * <p>v3: MERCY 双连退出 + 战斗台词（start/hp50/hp10/mercy1/mercy2）。
 */
public class UndertaleCombatEngine implements CombatSystem
{
    private static final int BASE_DAMAGE = 15;
    private static final int SWORD_BONUS = 8;
    private static final float FIGHT_BAR_SPEED = 3.0f;
    private static final float FIGHT_BAR_MAX = 1.0f;
    private static final float SOUL_SPEED = 2.5f;
    private static final float SOUL_RADIUS = 0.04f;
    private static final float ENEMY_TURN_DURATION = 4.0f;
    private static final int CONTACT_DAMAGE = 5;

    private final Player player;
    private final NpcCombatDef def;
    private final CombatActionRegistry itemRegistry;
    private final Random rng;

    private UndertaleCombatPhase phase;
    private String phaseMessage;
    private String currentBattleLine;
    private String currentBattleLineColor;

    private int npcHp;
    private int npcMaxHp;
    private int mercyCount;
    private boolean mercyExited;
    private boolean triggerHp50Line;
    private boolean triggerHp10Line;

    private float fightBarPos;
    private boolean fightBarForward;

    private final List<Bullet> bullets;
    private BulletPattern activePattern;
    private float enemyTurnTimer;
    private float enemyTurnDuration;
    private int hitsTaken;

    private float soulX, soulY;
    private final List<String> log;
    private boolean spared;

    public UndertaleCombatEngine(Player player, NpcCombatDef def,
        CombatActionRegistry itemRegistry)
    {
        this.player = player;
        this.def = def;
        this.itemRegistry = itemRegistry;
        this.rng = new Random();
        this.log = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.npcHp = def.maxHp;
        this.npcMaxHp = def.maxHp;
        this.phase = UndertaleCombatPhase.MENU;
        this.soulX = 0.5f;
        this.soulY = 0.5f;
        this.mercyCount = 0;
        this.triggerHp50Line = true;
        this.triggerHp10Line = true;

        // 开战台词
        NpcCombatDef.BattleLine startLine = def.battleLines.get("start");
        if (startLine != null) {
            currentBattleLine = startLine.text;
            currentBattleLineColor = startLine.color;
        }
        phaseMessage = currentBattleLine != null
            ? currentBattleLine : def.displayName + " 出现了！";
    }

    // ========== CombatSystem ==========

    @Override
    public CombatOutcome getOutcome()
    {
        if (spared || mercyExited) return CombatOutcome.VICTORY;
        if (player.isDead()) return CombatOutcome.DEFEAT;
        if (npcHp <= 0) return CombatOutcome.VICTORY;
        return CombatOutcome.ONGOING;
    }

    @Override
    public NpcCombatDef getDef() { return def; }

    @Override
    public CombatSnapshot snapshot()
    {
        return new CombatSnapshot(def.npcId, def.displayName,
            player.getHp(), player.getMaxHp(), npcHp, npcMaxHp,
            "ut-" + phase.name().toLowerCase(), getOutcome(),
            new ArrayList<>(log), phase == UndertaleCombatPhase.MENU);
    }

    @Override
    public CombatSnapshot processPlayerAction(CombatAction action, String itemId)
    {
        log.add("UT 引擎不支持传统回合制指令。");
        return snapshot();
    }

    @Override
    public void addPlayerDefenseBuff(int turns) { log.add("防御暂时提升。"); }
    @Override
    public void addNpcBlindTurns(int turns) { log.add("敌人被致盲。"); }

    // ========== 公开访问器 ==========

    public UndertaleCombatPhase getPhase() { return phase; }
    public String getPhaseMessage() { return phaseMessage; }
    public String getCurrentBattleLine() { return currentBattleLine; }
    public String getCurrentBattleLineColor() { return currentBattleLineColor; }
    public boolean isMercyExited() { return mercyExited; }
    public float getSoulX() { return soulX; }
    public float getSoulY() { return soulY; }
    public float getFightBarPos() { return fightBarPos; }
    public float getEnemyTurnProgress()
    {
        return enemyTurnDuration > 0 ? enemyTurnTimer / enemyTurnDuration : 0f;
    }
    public List<Bullet> getBullets() { return Collections.unmodifiableList(bullets); }

    // ========== 菜单 ==========

    public void selectFight()
    {
        if (phase != UndertaleCombatPhase.MENU) return;
        phase = UndertaleCombatPhase.FIGHT_BAR;
        fightBarPos = 0f;
        fightBarForward = true;
        phaseMessage = "在中心区域按键攻击！";
    }

    public void selectAct(String actId)
    {
        if (phase != UndertaleCombatPhase.MENU) return;
        String response = def.actOptions.getOrDefault(actId,
            "你对 " + def.displayName + " 使用了 " + actId + "。");
        currentBattleLine = response;
        currentBattleLineColor = "white";
        phaseMessage = response;
        log.add(response);
        phase = UndertaleCombatPhase.ENEMY_TURN;
        startEnemyTurn();
    }

    public void selectItem(String itemId)
    {
        if (phase != UndertaleCombatPhase.MENU) return;
        CombatActionRegistry.CombatItemResult result =
            itemRegistry.apply(this, player, itemId);
        phaseMessage = result.message;
        log.add(result.message);
        phase = UndertaleCombatPhase.ENEMY_TURN;
        startEnemyTurn();
    }

    public void selectMercy()
    {
        if (phase != UndertaleCombatPhase.MENU) return;
        mercyCount++;

        if (mercyCount >= 2) {
            // 第二次 MERCY → NPC 接受，退出战斗
            NpcCombatDef.BattleLine m2 = def.battleLines.get("mercy2");
            if (m2 != null) {
                currentBattleLine = m2.text;
                currentBattleLineColor = m2.color;
            } else {
                currentBattleLine = def.displayName + " 放下了武器。";
                currentBattleLineColor = "green";
            }
            mercyExited = true;
            phase = UndertaleCombatPhase.RESULT;
            phaseMessage = currentBattleLine;
            log.add(currentBattleLine);
        } else {
            // 第一次 MERCY → NPC 嘲讽
            NpcCombatDef.BattleLine m1 = def.battleLines.get("mercy1");
            if (m1 != null) {
                currentBattleLine = m1.text;
                currentBattleLineColor = m1.color;
            } else {
                currentBattleLine = "怎么？这就怕了？";
                currentBattleLineColor = "blue";
            }
            phaseMessage = currentBattleLine;
            log.add(currentBattleLine);
            phase = UndertaleCombatPhase.ENEMY_TURN;
            startEnemyTurn();
        }
    }

    // ========== 节奏攻击 ==========

    public void pressFightBar()
    {
        if (phase != UndertaleCombatPhase.FIGHT_BAR) return;
        float dist = Math.abs(fightBarPos - 0.5f);
        int damage;
        String quality;
        if (dist < 0.1f) {
            damage = BASE_DAMAGE + SWORD_BONUS;
            quality = "完美！";
        } else if (dist < 0.2f) {
            damage = BASE_DAMAGE;
            quality = "不错。";
        } else {
            damage = BASE_DAMAGE / 2;
            quality = "偏离了……";
        }
        if (hasItem("sword-rusty")) damage += SWORD_BONUS;
        npcHp = Math.max(0, npcHp - damage);
        log.add(quality + " 造成 " + damage + " 点伤害。");
        phaseMessage = quality + " 造成 " + damage + " 点伤害。";

        checkHpThresholdLines();

        if (npcHp <= 0) {
            phase = UndertaleCombatPhase.RESULT;
            phaseMessage = "你击败了 " + def.displayName + "！";
            return;
        }
        phase = UndertaleCombatPhase.ENEMY_TURN;
        startEnemyTurn();
    }

    public void updateFightBar(float delta)
    {
        if (phase != UndertaleCombatPhase.FIGHT_BAR) return;
        if (fightBarForward) {
            fightBarPos += FIGHT_BAR_SPEED * delta;
            if (fightBarPos >= FIGHT_BAR_MAX) { fightBarPos = FIGHT_BAR_MAX; fightBarForward = false; }
        } else {
            fightBarPos -= FIGHT_BAR_SPEED * delta;
            if (fightBarPos <= 0f) { fightBarPos = 0f; fightBarForward = true; }
        }
    }

    private void checkHpThresholdLines()
    {
        float ratio = (float) npcHp / npcMaxHp;
        if (triggerHp50Line && ratio <= 0.5f) {
            triggerHp50Line = false;
            NpcCombatDef.BattleLine line = def.battleLines.get("hp50");
            if (line != null) { currentBattleLine = line.text; currentBattleLineColor = line.color; }
        }
        if (triggerHp10Line && ratio <= 0.1f) {
            triggerHp10Line = false;
            NpcCombatDef.BattleLine line = def.battleLines.get("hp10");
            if (line != null) { currentBattleLine = line.text; currentBattleLineColor = line.color; }
        }
    }

    // ========== 弹幕 ==========

    private void startEnemyTurn()
    {
        enemyTurnTimer = 0f;
        enemyTurnDuration = ENEMY_TURN_DURATION;
        hitsTaken = 0;
        bullets.clear();

        int idx = rng.nextInt(4);
        switch (idx) {
            case 0: activePattern = BulletPattern.wave(enemyTurnDuration, 16, 5, rng); break;
            case 1: activePattern = BulletPattern.burst(enemyTurnDuration, 20, 5, rng); break;
            case 2: activePattern = BulletPattern.randomScatter(enemyTurnDuration, 15, 5, rng); break;
            default: activePattern = BulletPattern.spiral(enemyTurnDuration, 30, 5, rng); break;
        }
        phaseMessage = "躲避 " + def.displayName + " 的攻击！";
    }

    public void updateEnemyTurn(float delta)
    {
        if (phase != UndertaleCombatPhase.ENEMY_TURN) return;

        if (activePattern != null) activePattern.update(delta, bullets);

        for (Bullet b : bullets) {
            if (b.alive) {
                b.update(delta);
                if (b.collidesWith(soulX, soulY, SOUL_RADIUS)) {
                    b.alive = false;
                    hitsTaken++;
                    player.setHp(Math.max(0, player.getHp() - b.damage));
                    log.add("被击中了！-" + b.damage + " HP");
                }
            }
        }
        bullets.removeIf(b -> !b.alive);

        enemyTurnTimer += delta;
        if (enemyTurnTimer >= enemyTurnDuration) endEnemyTurn();
    }

    private void endEnemyTurn()
    {
        bullets.clear();
        activePattern = null;
        if (hitsTaken == 0) log.add("完美躲避！");

        if (player.isDead()) {
            phase = UndertaleCombatPhase.RESULT;
            phaseMessage = "你倒下了……";
        } else {
            phase = UndertaleCombatPhase.MENU;
            phaseMessage = "你的回合。";
        }
    }

    public void moveSoul(float dx, float dy)
    {
        soulX = clamp(soulX + dx * SOUL_SPEED * 0.016f, SOUL_RADIUS, 1f - SOUL_RADIUS);
        soulY = clamp(soulY + dy * SOUL_SPEED * 0.016f, SOUL_RADIUS, 1f - SOUL_RADIUS);
    }

    private boolean hasItem(String itemId)
    {
        return player.getInventory().stream().anyMatch(i -> itemId.equals(i.getItemId()));
    }

    private static float clamp(float v, float min, float max)
    {
        return Math.max(min, Math.min(max, v));
    }
}
