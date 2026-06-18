package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.NpcCombatDef;
import cn.edu.whut.sept.zuul.domain.Player;
import cn.edu.whut.sept.zuul.engine.effect.combat.CombatActionRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Undertale 式战斗引擎。
 *
 * <h3>回合流转</h3>
 * <pre>
 *   MENU ──→ FIGHT_BAR ──→ ENEMY_TURN ──→ MENU  （循环）
 *     │                        │
 *     ├── ACT / ITEM ──────────┤ （跳过节奏攻击，直入敌人回合）
 *     └── MERCY ───────────────┤ （台词 → 敌人回合 → 菜单）
 *                                  │
 *                            MERCY×2 → RESULT（战斗结束）
 * </pre>
 *
 * <h3>战斗台词系统</h3>
 * 台词弹出时 {@link #battleLineActive} = true，屏蔽所有战斗输入。
 * 玩家按 Enter 调用 {@link #dismissBattleLine()}，根据 {@link #phaseAfterBattleLine}
 * 决定下一阶段：
 * <ul>
 *   <li>start 台词 → MENU（玩家先手）</li>
 *   <li>mercy1 / ACT / ITEM / HP阈值 → ENEMY_TURN（NPC 反击）</li>
 *   <li>mercy2 → RESULT → 调用方检测 {@link #isMercyExited()} 后切入交谈</li>
 * </ul>
 *
 * <h3>数据驱动</h3>
 * 所有 NPC 战斗参数（HP、技能、actOptions、battleLines）从
 * {@code assets/combat/<npcId>.json} 加载，运行时零硬编码。
 *
 * @see NpcCombatDef
 * @see NpcCombatDef.BattleLine
 * @see UndertaleCombatPhase
 */
public class UndertaleCombatEngine implements CombatSystem
{
    private static final int BASE_DAMAGE = 8;
    private static final int SWORD_BONUS = 3;
    private static final int SHIELD_REDUCTION = 1;
    private static final float FIGHT_BAR_SPEED = 1.55f;
    private static final float FIGHT_BAR_MAX = 1.0f;
    private static final float SOUL_SPEED = 0.78f;
    private static final float SOUL_RADIUS = 0.04f;
    private static final float ENEMY_TURN_DURATION = 5.8f;
    private static final float BULLET_SPEED_MULTIPLIER = 0.58f;
    private static final int MENU_COUNT = 4;

    // 重力/平台模式（魔像招式）参数
    private static final float GRAVITY_ACC = 5.5f;
    private static final float JUMP_V = 1.78f;
    private static final int MAX_JUMPS = 2;
    private static final float SOUL_HSPEED = 0.65f;
    private static final float INVULN_TIME = 0.6f;

    private final Player player;
    private final NpcCombatDef def;
    private final CombatActionRegistry itemRegistry;
    private final Random rng;

    private UndertaleCombatPhase phase;
    private String phaseMessage;
    private String currentBattleLine;
    private String currentBattleLineColor;

    /** 战斗台词画中画：true 时玩家只能按 Enter 继续 */
    private boolean battleLineActive;
    private String pendingBattleLineText;
    private String pendingBattleLineColor;

    /** 台词关闭后进入哪个阶段 */
    private UndertaleCombatPhase phaseAfterBattleLine;

    private int npcHp;
    private int npcMaxHp;
    private boolean lastAttackPerfect;
    private int mercyCount;
    private boolean mercyExited;
    private boolean triggerHp50Line;
    private boolean triggerHp10Line;

    private float fightBarPos;
    private boolean fightBarForward;

    private final List<Bullet> bullets;
    private final List<WarningZone> warnings;
    private BulletPattern activePattern;
    private float enemyTurnTimer;
    private float enemyTurnDuration;
    private int hitsTaken;
    private int menuIndex;

    private float soulX, soulY;
    private final List<String> log;
    private boolean spared;

    // 重力/平台模式状态
    private boolean gravityMode;
    private float soulVy;
    private int jumpsUsed;
    private float soulInvuln;   // 受击后无敌帧倒计时
    private int lastAttackId = -1;   // 上一回合招式，用于避免连续重复

    /** ACT 效果：敌人攻击力下降 */
    private boolean atkDebuff;
    /** ACT 效果：敌人防御下降（受到伤害+6） */
    private boolean defDebuff;
    /** ACT 效果：下次敌人回合弹幕速度-30% */
    private boolean bulletSlow;

    public UndertaleCombatEngine(Player player, NpcCombatDef def,
        CombatActionRegistry itemRegistry)
    {
        this.player = player;
        this.def = def;
        this.itemRegistry = itemRegistry;
        this.rng = new Random();
        this.log = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.npcHp = def.maxHp;
        this.npcMaxHp = def.maxHp;
        this.phase = UndertaleCombatPhase.MENU;
        this.soulX = 0.5f;
        this.soulY = 0.5f;
        this.mercyCount = 0;
        this.menuIndex = -1;   // 纯数字键操作，无方向导航游标
        this.triggerHp50Line = true;
        this.triggerHp10Line = true;

        showBattleLinePopup("start", UndertaleCombatPhase.MENU);
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

    @Override public NpcCombatDef getDef() { return def; }

    @Override
    public CombatSnapshot snapshot()
    {
        return new CombatSnapshot(def.npcId, def.displayName,
            player.getHp(), player.getMaxHp(), npcHp, npcMaxHp,
            "ut-" + phase.name().toLowerCase(), getOutcome(),
            new ArrayList<>(log), phase == UndertaleCombatPhase.MENU && !battleLineActive);
    }

    @Override
    public CombatSnapshot processPlayerAction(CombatAction action, String itemId)
    { log.add("UT 引擎不支持传统回合制指令。"); return snapshot(); }
    @Override public void addPlayerDefenseBuff(int turns) { log.add("防御暂时提升。"); }
    @Override public void addNpcBlindTurns(int turns) { log.add("敌人被致盲。"); }

    // ========== 公开访问器 ==========

    public UndertaleCombatPhase getPhase() { return phase; }
    public String getPhaseMessage() { return phaseMessage; }
    public boolean isShowingBattleLine() { return battleLineActive; }
    public String getBattleLineText() { return pendingBattleLineText; }
    public String getBattleLineColor() { return pendingBattleLineColor; }
    public String getCurrentBattleLine() { return currentBattleLine; }
    public String getCurrentBattleLineColor() { return currentBattleLineColor; }
    public boolean isMercyExited() { return mercyExited; }
    @Override public boolean wasSpared() { return spared || mercyExited; }
    public boolean canMercy() { return !def.noMercy; }
    public int getNpcHp() { return npcHp; }
    public int getNpcMaxHp() { return npcMaxHp; }
    /** 上一次节奏攻击是否命中"完美"区间（供客户端打击感特效判定）。 */
    public boolean wasLastAttackPerfect() { return lastAttackPerfect; }
    public float getSoulX() { return soulX; }
    public float getSoulY() { return soulY; }
    /** 是否处于重力/平台模式（魔像招式）：客户端据此切换输入与渲染。 */
    public boolean isGravityMode() { return gravityMode; }
    public float getFightBarPos() { return fightBarPos; }
    public int getMenuIndex() { return menuIndex; }
    public float getEnemyTurnProgress() { return enemyTurnDuration > 0 ? enemyTurnTimer / enemyTurnDuration : 0f; }
    public List<Bullet> getBullets() { return Collections.unmodifiableList(bullets); }
    public List<WarningZone> getWarnings() { return Collections.unmodifiableList(warnings); }

    /**
     * 关闭战斗台词画中画，根据触发来源决定下一阶段。
     *
     * <table>
     *   <tr><th>台词来源</th><th>phaseAfterBattleLine</th><th>动作</th></tr>
     *   <tr><td>start（开局战吼）</td><td>MENU</td><td>进入菜单，玩家先手</td></tr>
     *   <tr><td>mercy1 / ACT / ITEM</td><td>ENEMY_TURN</td><td>进入弹幕躲避</td></tr>
     *   <tr><td>HP50 / HP10</td><td>ENEMY_TURN</td><td>进入弹幕躲避</td></tr>
     *   <tr><td>mercy2</td><td>（无视）</td><td>mercyExited=true → RESULT</td></tr>
     * </table>
     *
     * <p>调用方在 {@code dismissBattleLine()} 之后应立即检查
     * {@link #isMercyExited()} 和 {@link #getOutcome()}：
     * <pre>{@code
     *   ut.dismissBattleLine();
     *   if (ut.isMercyExited()) {
     *       engine.talkNpcWithPrefix(ut.getDef().npcId, ut.getCurrentBattleLine());
     *       dialogueUi.startDialogue(engine.talkNpc(ut.getDef().npcId));
     *   }
     * }</pre>
     */
    public void dismissBattleLine()
    {
        if (!battleLineActive) return;
        battleLineActive = false;
        pendingBattleLineText = null;

        if (mercyExited) {
            // 二次 MERCY：战斗结束
            phase = UndertaleCombatPhase.RESULT;
            phaseMessage = currentBattleLine;
            return;
        }

        if (phaseAfterBattleLine == UndertaleCombatPhase.ENEMY_TURN) {
            // 玩家行动后 → 敌人回合
            phase = UndertaleCombatPhase.ENEMY_TURN;
            startEnemyTurn();
        } else {
            // 开局台词等 → 回到菜单
            phase = phaseAfterBattleLine;
            phaseMessage = "你的回合。";
        }
    }

    // ========== 菜单 ==========

    public void moveMenuSelection(int dir)
    {
        if (phase != UndertaleCombatPhase.MENU || battleLineActive || dir == 0) return;
        menuIndex = Math.floorMod(menuIndex + dir, MENU_COUNT);
    }

    public void confirmMenuSelection()
    {
        if (phase != UndertaleCombatPhase.MENU || battleLineActive) return;
        switch (menuIndex) {
            case 0:
                selectFight();
                break;
            case 1:
                String actId = def.actOptions.isEmpty()
                    ? null : def.actOptions.keySet().iterator().next();
                if (actId != null) selectAct(actId);
                break;
            case 2:
                if (!player.getInventory().isEmpty())
                    selectItem(player.getInventory().get(0).getItemId());
                else
                    showBattleLine("背包里没有可用物品。", "white", UndertaleCombatPhase.MENU);
                break;
            default:
                selectMercy();
                break;
        }
    }

    public void selectFight()
    {
        if (phase != UndertaleCombatPhase.MENU || battleLineActive) return;
        phase = UndertaleCombatPhase.FIGHT_BAR;
        fightBarPos = 0f;
        fightBarForward = true;
        phaseMessage = "在中心区域按键攻击！";
    }

    /**
     * ACT 行动，根据 actId 产生实际战斗效果：
     * <ul>
     *   <li>check / listen / observe → 弹幕减速（下回合-30%）</li>
     *   <li>talk / question / distract → 降攻击（敌人伤害-3）</li>
     *   <li>intimidate / threaten → 降防御（玩家造成伤害+6）</li>
     * </ul>
     */
    public void selectAct(String actId)
    {
        if (phase != UndertaleCombatPhase.MENU || battleLineActive) return;
        String response = def.actOptions.getOrDefault(actId,
            "你对 " + def.displayName + " 使用了 " + actId + "。");

        // 根据 actId 前缀施加效果
        String key = actId.toLowerCase();
        if (key.contains("check") || key.contains("listen") || key.contains("observe")) {
            bulletSlow = true;
            response += "\n（弹幕速度下降！）";
        } else if (key.contains("talk") || key.contains("question") || key.contains("distract")) {
            atkDebuff = true;
            response += "\n（敌人攻击力下降！）";
        } else if (key.contains("intimidate") || key.contains("threaten")) {
            defDebuff = true;
            response += "\n（敌人防御下降！）";
        }

        showBattleLine(response, "white", UndertaleCombatPhase.ENEMY_TURN);
        phaseMessage = response;
        log.add(response);
        if (!battleLineActive) { phase = UndertaleCombatPhase.ENEMY_TURN; startEnemyTurn(); }
    }

    public void selectItem(String itemId)
    {
        if (phase != UndertaleCombatPhase.MENU || battleLineActive) return;
        CombatActionRegistry.CombatItemResult result = itemRegistry.apply(this, player, itemId);
        showBattleLine(result.message, "white", UndertaleCombatPhase.ENEMY_TURN);
        phaseMessage = result.message;
        log.add(result.message);
        if (!battleLineActive) { phase = UndertaleCombatPhase.ENEMY_TURN; startEnemyTurn(); }
    }

    /**
     * MERCY 双连机制：
     * <ol>
     *   <li>第 1 次：弹出 {@code mercy1} 嘲讽台词，敌人回合照常</li>
     *   <li>第 2 次：弹出 {@code mercy2} 和解台词，设置 {@code mercyExited=true}</li>
     * </ol>
     * 调用方在检测到 {@code mercyExited} 后应关闭战斗并切入对话。
     */
    public void selectMercy()
    {
        if (phase != UndertaleCombatPhase.MENU || battleLineActive) return;
        mercyCount++;

        if (mercyCount >= 2) {
            mercyExited = true;
            showBattleLinePopup("mercy2", UndertaleCombatPhase.MENU /* 不执行，mercyExited 优先 */);
        } else {
            showBattleLinePopup("mercy1", UndertaleCombatPhase.ENEMY_TURN);
        }
    }

    // ========== 节奏攻击 ==========

    /**
     * 节奏攻击确认。浮动条位置越靠近中心伤害越高：
     * <ul>
     *   <li>距离 &lt; 0.1 → 完美（BASE + SWORD_BONUS）</li>
     *   <li>距离 &lt; 0.2 → 正常（BASE）</li>
     *   <li>其余 → 偏离（BASE / 2）</li>
     * </ul>
     * 装备锈剑额外 +SWORD_BONUS。攻击后检查 HP 阈值台词，
     * 若无台词则进入敌人弹幕回合。
     */
    public void pressFightBar()
    {
        if (phase != UndertaleCombatPhase.FIGHT_BAR) return;
        float dist = Math.abs(fightBarPos - 0.5f);
        int damage;
        String quality;
        if (dist < 0.1f) { damage = BASE_DAMAGE * 2; quality = "完美！"; lastAttackPerfect = true; }
        else if (dist < 0.2f) { damage = BASE_DAMAGE; quality = "不错。"; lastAttackPerfect = false; }
        else { damage = BASE_DAMAGE / 2; quality = "偏离了……"; lastAttackPerfect = false; }
        if (hasItem("sword-rusty")) damage += SWORD_BONUS;
        if (defDebuff) damage += 3;
        npcHp = Math.max(0, npcHp - damage);
        log.add(quality + " 造成 " + damage + " 点伤害。");
        phaseMessage = quality + " 造成 " + damage + " 点伤害。";

        checkHpThresholdLines();

        // 切磋模式：NPC 半血以下自动投降（算作仁慈结束，避免卡在 RESULT 阶段）
        // 切磋模式：NPC 半血以下自动投降
        if (def.spar && npcHp > 0
            && npcHp <= npcMaxHp / 2)
        {
            phase = UndertaleCombatPhase.RESULT;
            spared = true;  // 切磋不标记为击败
            phaseMessage = def.displayName + " 认输了！\"你确实很强——这枚勋章，是你应得的。\"";
            return;
        }

        if (npcHp <= 0) {
            phase = UndertaleCombatPhase.RESULT;
            phaseMessage = "你击败了 " + def.displayName + "！";
            return;
        }
        if (!battleLineActive) { phase = UndertaleCombatPhase.ENEMY_TURN; startEnemyTurn(); }
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

    // ========== 战斗台词系统 ==========

    private void showBattleLinePopup(String key, UndertaleCombatPhase after)
    {
        NpcCombatDef.BattleLine line = def.battleLines.get(key);
        if (line == null) return;

        battleLineActive = true;
        pendingBattleLineText = line.text;
        pendingBattleLineColor = line.color;
        currentBattleLine = line.text;
        currentBattleLineColor = line.color;
        phaseAfterBattleLine = after;
    }

    private void showBattleLine(String text, String color, UndertaleCombatPhase after)
    {
        if (text == null || text.isEmpty()) return;
        battleLineActive = true;
        pendingBattleLineText = text;
        pendingBattleLineColor = color;
        currentBattleLine = text;
        currentBattleLineColor = color;
        phaseAfterBattleLine = after;
    }

    private void checkHpThresholdLines()
    {
        float ratio = (float) npcHp / npcMaxHp;
        if (triggerHp50Line && ratio <= 0.5f) {
            triggerHp50Line = false;
            showBattleLinePopup("hp50", UndertaleCombatPhase.ENEMY_TURN);
        }
        if (triggerHp10Line && ratio <= 0.1f) {
            triggerHp10Line = false;
            showBattleLinePopup("hp10", UndertaleCombatPhase.ENEMY_TURN);
        }
    }

    // ========== 弹幕 ==========

    private void startEnemyTurn()
    {
        enemyTurnTimer = 0f;
        enemyTurnDuration = ENEMY_TURN_DURATION;
        hitsTaken = 0;
        bullets.clear();
        warnings.clear();
        gravityMode = false;
        soulVy = 0f;
        jumpsUsed = 0;
        soulInvuln = 0f;

        // 低血狂暴：HP ≤ 50% 时弹幕更密集、更易触发 Boss 招式
        float ratio = npcMaxHp > 0 ? (float) npcHp / npcMaxHp : 1f;
        boolean enraged = ratio <= 0.5f;
        int extra = enraged ? 4 : 0;

        // 候选招式池：6 种普通弹幕 + 该 Boss 专属招式（狂暴时招式权重更高）
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < 6; i++) pool.add(i);
        int sigId = signatureId(def.npcId);
        if (sigId >= 0) {
            int weight = enraged ? 3 : 2;
            for (int k = 0; k < weight; k++) pool.add(sigId);
        }
        // 选一个与上回合不同的招式（保证"每次躲避都是不同的招式"）
        int pick = pool.get(rng.nextInt(pool.size()));
        for (int tries = 0; tries < 8 && pick == lastAttackId; tries++) {
            pick = pool.get(rng.nextInt(pool.size()));
        }
        lastAttackId = pick;
        launchAttack(pick, enraged, extra);
    }

    /** 该 NPC 的专属招式 id（100+），无则返回 -1。 */
    private int signatureId(String npcId)
    {
        switch (npcId == null ? "" : npcId) {
            case "guard": return 100;
            case "golem": return 101;
            case "priest": return 102;
            case "follower": return 103;
            case "apprentice": return 104;
            default: return -1;
        }
    }

    /** 根据招式 id 启动对应弹幕/招式并设置提示。 */
    private void launchAttack(int id, boolean enraged, int extra)
    {
        String prefix = enraged ? "【狂暴】" : "";
        switch (id) {
            case 0: activePattern = BulletPattern.wave(enemyTurnDuration, 10 + extra, 4, rng); break;
            case 1: activePattern = BulletPattern.burst(enemyTurnDuration, 14 + extra, 4, rng); break;
            case 2: activePattern = BulletPattern.randomScatter(enemyTurnDuration, 11 + extra, 4, rng); break;
            case 3: activePattern = BulletPattern.spiral(enemyTurnDuration, 18 + extra, 4, rng); break;
            case 4: activePattern = BulletPattern.aimed(enemyTurnDuration, 8 + extra, 5, rng); break;
            case 5: activePattern = BulletPattern.pillars(enemyTurnDuration, 6 + extra / 2, 4, rng); break;
            case 100:   // 守卫：飞剑
                activePattern = BulletPattern.flyingSwords(enemyTurnDuration, 6, rng);
                phaseMessage = def.displayName + " 拔剑——「飞剑·四面斩」！看红线，躲开飞来的长剑！";
                return;
            case 101:   // 魔像：重力强袭
                gravityMode = true;
                soulX = 0.5f;
                soulY = SOUL_RADIUS;
                soulVy = 0f;
                jumpsUsed = 0;
                activePattern = BulletPattern.gravityWalls(enemyTurnDuration, 6, rng);
                phaseMessage = def.displayName + " 一拳将你砸向地面！空格跳跃(可二段)，A/D 躲避土墙！";
                return;
            case 102:   // 神父：圣十字
                activePattern = BulletPattern.crossSweep(enemyTurnDuration, 5, rng);
                phaseMessage = def.displayName + " 展开「圣十字」！穿过封锁的缺口！";
                return;
            case 103:   // 追随者：血环
                activePattern = BulletPattern.ringWaves(enemyTurnDuration, 4, rng);
                phaseMessage = def.displayName + " 释放「血环」！在环的间隙穿行！";
                return;
            case 104:   // 学徒：奥术风暴
                activePattern = BulletPattern.spiral(enemyTurnDuration, 24 + extra, 4, rng);
                phaseMessage = def.displayName + " 召唤「奥术风暴」！";
                return;
            default: activePattern = BulletPattern.wave(enemyTurnDuration, 12, 4, rng); break;
        }
        phaseMessage = prefix + "躲避 " + def.displayName + " 的攻击！";
    }

    public void updateEnemyTurn(float delta)
    {
        if (phase != UndertaleCombatPhase.ENEMY_TURN || battleLineActive) return;

        if (soulInvuln > 0f) soulInvuln = Math.max(0f, soulInvuln - delta);
        if (gravityMode) applyGravity(delta);

        if (activePattern != null) activePattern.update(delta, bullets, warnings, soulX, soulY);
        for (WarningZone warning : warnings) {
            warning.update(delta);
        }
        warnings.removeIf(w -> !w.isAlive());

        float bulletDelta = delta * (bulletSlow ? 0.5f : BULLET_SPEED_MULTIPLIER);
        for (Bullet b : bullets) {
            if (!b.alive) continue;
            b.update(bulletDelta);
            // 飞剑触及对面边框 → 吸附停住（插入边框停留）
            if (b.kind == Bullet.Kind.SWORD) {
                if (b.vx < 0f && b.x <= 0f) { b.x = 0f; b.vx = 0f; }
                else if (b.vx > 0f && b.x + b.width >= 1f) { b.x = 1f - b.width; b.vx = 0f; }
                if (b.vy < 0f && b.y <= 0f) { b.y = 0f; b.vy = 0f; }
                else if (b.vy > 0f && b.y + b.height >= 1f) { b.y = 1f - b.height; b.vy = 0f; }
            }
            // 按真实时间倒计时存活（剑停留计时不受弹幕减速影响）
            if (b.life >= 0f) {
                b.life -= delta;
                if (b.life <= 0f) { b.alive = false; continue; }
            }
            if (b.collidesWith(soulX, soulY, SOUL_RADIUS)) {
                boolean persistent = b.kind == Bullet.Kind.SWORD || b.kind == Bullet.Kind.WALL;
                if (!persistent) b.alive = false;   // 普通弹命中即消失；剑/墙是持续危险
                if (soulInvuln <= 0f) {
                    hitsTaken++;
                    int dmg = b.damage;
                    if (atkDebuff) dmg = Math.max(1, dmg - 3);
                    if (hasItem("shield-wooden")) dmg = Math.max(1, dmg - SHIELD_REDUCTION);
                    player.setHp(Math.max(0, player.getHp() - dmg));
                    soulInvuln = INVULN_TIME;
                    log.add("被击中了！-" + dmg + " HP");
                }
            }
        }
        bullets.removeIf(b -> !b.alive);
        enemyTurnTimer += delta;
        if (enemyTurnTimer >= enemyTurnDuration) {
            bulletSlow = false;
            endEnemyTurn();
        }
    }

    /** 重力模式每帧物理：重力下坠 + 落地复位跳跃次数。 */
    private void applyGravity(float delta)
    {
        soulVy -= GRAVITY_ACC * delta;
        soulY += soulVy * delta;
        float ground = SOUL_RADIUS;
        if (soulY <= ground) { soulY = ground; soulVy = 0f; jumpsUsed = 0; }
        float ceil = 1f - SOUL_RADIUS;
        if (soulY > ceil) { soulY = ceil; soulVy = 0f; }
    }

    /** 重力模式跳跃（支持二段跳）。 */
    public void soulJump()
    {
        if (!gravityMode) return;
        if (jumpsUsed < MAX_JUMPS) { soulVy = JUMP_V; jumpsUsed++; }
    }

    private void endEnemyTurn()
    {
        bullets.clear(); warnings.clear(); activePattern = null;
        // 退出重力模式，灵魂复位
        gravityMode = false;
        soulVy = 0f;
        jumpsUsed = 0;
        soulInvuln = 0f;
        soulX = 0.5f;
        soulY = 0.5f;
        if (hitsTaken == 0) log.add("完美躲避！");
        if (player.isDead()) { phase = UndertaleCombatPhase.RESULT; phaseMessage = "你倒下了……"; }
        else { phase = UndertaleCombatPhase.MENU; phaseMessage = "你的回合。"; }
    }

    public void moveSoul(float dx, float dy)
    {
        moveSoul(dx, dy, 0.016f);
    }

    public void moveSoul(float dx, float dy, float delta)
    {
        if (gravityMode) {
            // 重力模式：仅水平移动，垂直由重力/跳跃控制
            soulX = clamp(soulX + dx * SOUL_HSPEED * delta, SOUL_RADIUS, 1f - SOUL_RADIUS);
            return;
        }
        if (dx != 0f && dy != 0f) {
            float inv = 0.70710677f;
            dx *= inv;
            dy *= inv;
        }
        soulX = clamp(soulX + dx * SOUL_SPEED * delta, SOUL_RADIUS, 1f - SOUL_RADIUS);
        soulY = clamp(soulY + dy * SOUL_SPEED * delta, SOUL_RADIUS, 1f - SOUL_RADIUS);
    }

    private boolean hasItem(String itemId) { return player.getInventory().stream().anyMatch(i -> itemId.equals(i.getItemId())); }
    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
}
