package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.NpcCombatDef;
import cn.edu.whut.sept.zuul.domain.Player;
import cn.edu.whut.sept.zuul.engine.effect.combat.CombatActionRegistry;
import cn.edu.whut.sept.zuul.infra.CombatLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatEngineTest
{
    private NpcCombatDef guardDef;
    private NpcCombatDef hermitDef;
    private Player player;
    private CombatEngine combat;

    @BeforeEach
    void setUp() throws IOException
    {
        guardDef = CombatLoader.load("guard");
        hermitDef = CombatLoader.load("hermit");
        player = new Player("战士");
        player.setHp(200);
        combat = new CombatEngine(player, guardDef, new CombatActionRegistry(), new Random(7));
    }

    // ---- 现有测试 ----

    @Test
    void attackUntilVictory()
    {
        CombatSnapshot last = null;
        for (int i = 0; i < 30 && combat.getOutcome() == CombatOutcome.ONGOING; i++) {
            last = combat.processPlayerAction(CombatAction.ATTACK, null);
        }
        assertEquals(CombatOutcome.VICTORY, combat.getOutcome());
        assertNotNull(last);
        assertTrue(last.npcHp <= 0);
    }

    @Test
    void defendReducesDamage()
    {
        int hpBefore = player.getHp();
        combat.processPlayerAction(CombatAction.DEFEND, null);
        assertTrue(player.getHp() >= hpBefore - 20);
    }

    // ---- 新增：阶段切换 ----

    @Test
    void guardTransitionsToEnragedAtLowHp()
    {
        // 守卫 80 HP，打到 ≤ 48（60%）时进入 enraged 阶段
        while (combat.getOutcome() == CombatOutcome.ONGOING) {
            CombatSnapshot snap = combat.processPlayerAction(CombatAction.ATTACK, null);
            if (snap.npcHp <= 48 && snap.npcHp > 0) {
                String state = snap.npcStateId;
                assertTrue(state.contains("enraged") || "enraged".equals(state)
                    || state.contains("desperate") || "desperate".equals(state),
                    "HP≤60% 应进入 enraged 或更低的阶段，实际: " + state);
                break;
            }
            if (snap.npcHp <= 0) break;
        }
    }

    @Test
    void guardTransitionsToDesperateAtVeryLowHp()
    {
        while (combat.getOutcome() == CombatOutcome.ONGOING) {
            CombatSnapshot snap = combat.processPlayerAction(CombatAction.ATTACK, null);
            if (snap.npcHp <= 16 && snap.npcHp > 0) {
                assertEquals("desperate", snap.npcStateId,
                    "HP≤20% 应进入 desperate 阶段");
                break;
            }
            if (snap.npcHp <= 0) break;
        }
    }

    // ---- 新增：逃跑 ----

    @Test
    void fleeReturnsEitherFledOrOngoing()
    {
        // 验证 flee 不会抛异常，不会卡死，结果要么 FLED 要么 ONGOING
        Player p = new Player("战士");
        p.setHp(999);
        CombatEngine c = new CombatEngine(p, guardDef,
            new CombatActionRegistry(), new Random(42));
        CombatSnapshot snap = c.processPlayerAction(CombatAction.FLEE, null);
        assertTrue(snap.outcome == CombatOutcome.FLED
            || snap.outcome == CombatOutcome.ONGOING,
            "flee 结果应该是 FLED 或 ONGOING，实际: " + snap.outcome);
    }

    @Test
    void fleeFromAlreadyEndedCombatDoesNothing()
    {
        CombatEngine c = new CombatEngine(player, guardDef,
            new CombatActionRegistry(), new Random(7));
        while (c.getOutcome() == CombatOutcome.ONGOING) {
            c.processPlayerAction(CombatAction.ATTACK, null);
        }
        assertEquals(CombatOutcome.VICTORY, c.getOutcome());
        CombatSnapshot snap = c.processPlayerAction(CombatAction.FLEE, null);
        assertEquals(CombatOutcome.VICTORY, snap.outcome);
    }

    // ---- 新增：玩家死亡 ----

    @Test
    void playerDeathOnZeroHp()
    {
        player.setHp(5);
        CombatEngine c = new CombatEngine(player, guardDef,
            new CombatActionRegistry(), new Random(99));
        // 不断攻击让守卫反击，直到玩家倒下
        while (c.getOutcome() == CombatOutcome.ONGOING && player.getHp() > 0) {
            c.processPlayerAction(CombatAction.ATTACK, null);
        }
        assertEquals(0, player.getHp());
        assertEquals(CombatOutcome.DEFEAT, c.getOutcome());
    }

    // ---- 新增：锈剑加成 ----

    @Test
    void rustySwordAddsExtraDamage()
    {
        player.addItem(new Item("sword-rusty", "Rusty Sword",
            "An old sword.", 25, null));

        CombatEngine c = new CombatEngine(player, guardDef,
            new CombatActionRegistry(), new Random(42));
        CombatSnapshot snap = c.processPlayerAction(CombatAction.ATTACK, null);
        // BASE_ATTACK(10) + SWORD_BONUS(5) = 15
        int expectedHp = 80 - 15;
        assertEquals(expectedHp, snap.npcHp,
            "持有锈剑应造成15点伤害(10基础+5加成)");
    }

    // ---- 新增：NPC 防御减半 ----

    @Test
    void npcDefenseHalvesPlayerAttack()
    {
        // 用特定种子让守卫的第一次行动是 block(defense)
        // guard 的 normal 阶段有 stab 和 block 两个技能
        CombatEngine c = new CombatEngine(player, guardDef,
            new CombatActionRegistry(), new Random(2));
        CombatSnapshot snap = c.processPlayerAction(CombatAction.ATTACK, null);
        // 如果守卫用了 block，npCdefending 会在下回合生效
        // 先打一下，再打第二下时检查伤害是否减半
        if (snap.outcome == CombatOutcome.ONGOING) {
            CombatSnapshot snap2 = c.processPlayerAction(CombatAction.ATTACK, null);
            // 如果守卫放过 block，伤害应为 10/2=5
            if (snap2.npcHp > 0 && snap2.logLines.stream().anyMatch(l -> l.contains("抵消"))) {
                int damage = 80 - snap2.npcHp - (80 - snap.npcHp);
                assertTrue(damage <= 5, "守卫防御时伤害应减半(≤5)");
            }
        }
    }

    // ---- 新增：隐士战斗 ----

    @Test
    void hermitCanBeFought()
    {
        CombatEngine c = new CombatEngine(player, hermitDef,
            new CombatActionRegistry(), new Random(7));
        assertEquals("守秘隐士", c.getDef().displayName);
        assertEquals(60, c.getDef().maxHp);
        assertEquals("calm", c.getDef().defaultState);

        CombatSnapshot snap = c.processPlayerAction(CombatAction.ATTACK, null);
        assertNotNull(snap);
        assertEquals(CombatOutcome.ONGOING, snap.outcome);
    }

    @Test
    void hermitCalmPhaseHasShieldSkill()
    {
        // calm 阶段有 mind_blast 和 shield
        assertTrue(hermitDef.stateSkills.containsKey("calm"));
        assertTrue(hermitDef.stateSkills.get("calm").contains("shield"),
            "calm 阶段应有 shield 技能");
    }

    @Test
    void hermitTransitionsToWarnedAtLowHp()
    {
        CombatEngine c = new CombatEngine(player, hermitDef,
            new CombatActionRegistry(), new Random(7));
        // 隐士 60 HP，≤30（50%）进入 warned
        while (c.getOutcome() == CombatOutcome.ONGOING) {
            CombatSnapshot snap = c.processPlayerAction(CombatAction.ATTACK, null);
            if (snap.npcHp <= 30 && snap.npcHp > 0) {
                assertEquals("warned", snap.npcStateId,
                    "HP≤50% 应进入 warned 阶段");
                break;
            }
            if (snap.npcHp <= 0) break;
        }
    }

    @Test
    void defeatHermit()
    {
        CombatEngine c = new CombatEngine(player, hermitDef,
            new CombatActionRegistry(), new Random(7));
        for (int i = 0; i < 40 && c.getOutcome() == CombatOutcome.ONGOING; i++) {
            c.processPlayerAction(CombatAction.ATTACK, null);
        }
        assertEquals(CombatOutcome.VICTORY, c.getOutcome());
    }

    // ---- 新增：战斗中使用物品 ----

    @Test
    void useHealingItemDuringCombat()
    {
        player.setHp(30);
        player.addItem(new Item("healing-herb", "Healing Herb",
            "Restores vitality.", 2, "heal:20"));

        CombatEngine c = new CombatEngine(player, guardDef,
            new CombatActionRegistry(), new Random(7));
        CombatSnapshot snap = c.processPlayerAction(CombatAction.USE_ITEM, "healing-herb");
        assertEquals(50, snap.playerHp, "使用治愈草药应回复20HP");
    }

    @Test
    void undertaleNumericMenuSelectsActions()
    {
        UndertaleCombatEngine ut = new UndertaleCombatEngine(player, guardDef,
            new CombatActionRegistry());
        ut.dismissBattleLine();

        // 纯数字键菜单：初始无方向导航游标（-1 表示不高亮）
        assertEquals(-1, ut.getMenuIndex());

        // 数字键直接选择"攻击" → 进入节奏攻击条阶段
        ut.selectFight();
        assertEquals(UndertaleCombatPhase.FIGHT_BAR, ut.getPhase());
    }

    @Test
    void golemGravitySignatureRunsToCompletion() throws IOException
    {
        NpcCombatDef golemDef = CombatLoader.load("golem");
        Player p = new Player("英雄");
        p.setHp(99999);   // 不让玩家死，确保能打到胜利
        UndertaleCombatEngine ut = new UndertaleCombatEngine(p, golemDef, new CombatActionRegistry());

        int guard = 0;
        boolean sawGravity = false;
        while (ut.getOutcome() == CombatOutcome.ONGOING && guard++ < 400) {
            if (ut.isShowingBattleLine()) { ut.dismissBattleLine(); continue; }
            switch (ut.getPhase()) {
                case MENU:
                    ut.selectFight();
                    break;
                case FIGHT_BAR:
                    ut.pressFightBar();
                    break;
                case ENEMY_TURN:
                    for (int i = 0; i < 200 && ut.getPhase() == UndertaleCombatPhase.ENEMY_TURN
                        && !ut.isShowingBattleLine(); i++) {
                        ut.updateEnemyTurn(0.05f);
                        if (ut.isGravityMode()) { sawGravity = true; ut.soulJump(); }
                        ut.moveSoul(1f, 0f, 0.05f);
                    }
                    break;
                default:
                    guard = 1000;   // RESULT
                    break;
            }
        }
        assertTrue(guard <= 1000, "战斗应在有限步内推进，未死循环");
        assertEquals(CombatOutcome.VICTORY, ut.getOutcome(), "魔像最终应被击败");
    }

    @Test
    void sparNpcAutoSurrenderEndsAsSparedVictory() throws IOException
    {
        NpcCombatDef followerDef = CombatLoader.load("follower");   // markDefeated=false → 切磋认输
        Player p = new Player("英雄");
        p.setHp(99999);
        UndertaleCombatEngine ut = new UndertaleCombatEngine(p, followerDef, new CombatActionRegistry());

        int guard = 0;
        while (ut.getOutcome() == CombatOutcome.ONGOING && guard++ < 300) {
            if (ut.isShowingBattleLine()) { ut.dismissBattleLine(); continue; }
            switch (ut.getPhase()) {
                case MENU: ut.selectFight(); break;
                case FIGHT_BAR: ut.pressFightBar(); break;
                case ENEMY_TURN:
                    for (int i = 0; i < 200 && ut.getPhase() == UndertaleCombatPhase.ENEMY_TURN
                        && !ut.isShowingBattleLine(); i++) {
                        ut.updateEnemyTurn(0.05f);
                    }
                    break;
                default: guard = 1000; break;   // RESULT
            }
        }
        // 认输后应结束为胜利（修复：之前 spared 未置位导致卡在 RESULT 阶段）
        assertEquals(CombatOutcome.VICTORY, ut.getOutcome(), "切磋 NPC 认输应结束为胜利，而非卡死");
        assertTrue(ut.wasSpared(), "认输应记为仁慈化解（spared）");
    }

    @Test
    void swordBulletSurvivesOffscreenSpawn()
    {
        // 飞剑从屏幕外（x=-0.51）长距离飞入，不应被越界判定瞬间杀死
        Bullet sword = Bullet.rect(-0.51f, 0.4f, 0.45f, 0.06f, 2.4f, 0f, 6)
            .withKind(Bullet.Kind.SWORD);
        sword.update(0.05f);
        assertTrue(sword.alive, "飞剑从屏外生成时不应消失");
        // 普通子弹越界应正常消失
        Bullet normal = Bullet.circle(-0.51f, 0.4f, 0.03f, 1f, 0f, 5);
        normal.update(0.05f);
        assertFalse(normal.alive, "普通子弹越界应消失");
    }

    @Test
    void undertaleEnemyTurnEmitsWarnings()
    {
        UndertaleCombatEngine ut = new UndertaleCombatEngine(player, guardDef,
            new CombatActionRegistry());
        ut.dismissBattleLine();
        ut.selectFight();
        ut.pressFightBar();

        boolean sawWarning = false;
        for (int i = 0; i < 30; i++) {
            ut.updateEnemyTurn(0.1f);
            if (!ut.getWarnings().isEmpty()) {
                sawWarning = true;
                break;
            }
        }

        assertTrue(sawWarning, "UT 敌人回合应显示弹幕预警线或预警框");
    }
}
