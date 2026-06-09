package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 从游戏流程角度验证结局触发。
 * 模拟完整的通关路径，验证 throne-hall 进入时正确判定结局。
 */
class EndingFlowTest
{
    private GameEngine engine;

    @BeforeEach
    void setUp()
    {
        engine = new GameEngine("测试者");
        engine.unlockLock("guard-gate");
        engine.unlockLock("vault-door");
        // 提高玩家 HP 防止被 NPC 反杀
        engine.getPlayer().setHp(500);
    }

    // ================================================
    // LIGHT 流程：拿 gem → 不杀人 → 进王座
    // ================================================

    @Test
    void lightEnding_fullFlow()
    {
        navigateToVault();         // outside → lab → vault
        assertTrue(engine.takeItem("gem-light"), "拾取 gem-light");
        returnToOutside();         // vault → lab → outside

        navigateToThroneHall();    // 经 guard-room → throne-hall
        assertEquals(EndingType.LIGHT, engine.getCurrentEnding(),
            "光明结局：有 gem、声望 OK、不杀人");
    }

    // ================================================
    // SHADOW 流程：杀守卫 → 声望变为负
    // ================================================

    @Test
    void shadowEnding_killGuard()
    {
        navigateToGuardRoom();
        killNpc("guard");
        navigateToThroneHallFromGuardRoom();
        assertEquals(EndingType.SHADOW, engine.getCurrentEnding(),
            "暗影结局：杀守卫 → 声望 -15 < 0");
    }

    // ================================================
    // SHADOW 流程：杀隐士
    // ================================================

    @Test
    void shadowEnding_killHermit()
    {
        navigateToHiddenShrine();
        killNpc("hermit");
        returnToOutsideFromShrine();
        navigateToThroneHall();
        assertEquals(EndingType.SHADOW, engine.getCurrentEnding(),
            "暗影结局：杀隐士 → hermitDead");
    }

    // ================================================
    // NEUTRAL 流程：不拿 gem、不杀人
    // ================================================

    @Test
    void neutralEnding_noGemNoKill()
    {
        navigateToThroneHall();
        assertEquals(EndingType.NEUTRAL, engine.getCurrentEnding(),
            "中立结局：无 gem、不杀人、声望 OK");
    }

    // ================================================
    // SHADOW 流程：杀守卫 + 不拿 gem
    // ================================================

    @Test
    void shadowEnding_killGuardNoGem()
    {
        navigateToGuardRoom();
        killNpc("guard");
        navigateToThroneHallFromGuardRoom();
        assertEquals(EndingType.SHADOW, engine.getCurrentEnding(),
            "暗影结局：杀守卫 + 无 gem → guardDead && !hasGem");
    }

    // ================================================
    // 导航工具
    // ================================================

    /** outside → theatre → library → hidden-shrine */
    private void navigateToHiddenShrine()
    {
        engine.movePlayer(Direction.NORTH); // outside → theatre
        engine.movePlayer(Direction.EAST);  // theatre → library
        engine.movePlayer(Direction.NORTH); // library → hidden-shrine
    }

    /** hidden-shrine → library → theatre → outside */
    private void returnToOutsideFromShrine()
    {
        engine.moveBack(); // → library
        engine.moveBack(); // → theatre
        engine.moveBack(); // → outside
    }

    /** outside → south → lab → east → vault */
    private void navigateToVault()
    {
        engine.movePlayer(Direction.SOUTH); // outside → lab
        engine.movePlayer(Direction.EAST);  // lab → vault
    }

    /** vault → lab → outside */
    private void returnToOutside()
    {
        engine.moveBack(); // → lab
        engine.moveBack(); // → outside
    }

    /** outside → east → pub → east → garden → south → guard-room */
    private void navigateToGuardRoom()
    {
        engine.movePlayer(Direction.EAST);  // outside → pub
        engine.movePlayer(Direction.EAST);  // pub → garden
        engine.movePlayer(Direction.SOUTH); // garden → guard-room
    }

    /** guard-room → south → throne-hall */
    private void navigateToThroneHallFromGuardRoom()
    {
        engine.movePlayer(Direction.SOUTH);
    }

    /** outside → ... → throne-hall */
    private void navigateToThroneHall()
    {
        navigateToGuardRoom();
        navigateToThroneHallFromGuardRoom();
    }

    private void killNpc(String npcId)
    {
        CombatSnapshot snap = engine.startCombat(npcId);
        for (int i = 0; i < 50 && snap.outcome == CombatOutcome.ONGOING; i++) {
            snap = engine.combatAction(CombatAction.ATTACK, null);
        }
        assertEquals(CombatOutcome.VICTORY, snap.outcome,
            "应能击败 " + npcId);
    }
}
