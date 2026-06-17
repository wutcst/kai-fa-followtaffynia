package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模拟完整通关路径，验证走到王座前互动后正确判定结局。
 *
 * 新版结局规则：
 *   LIGHT:  光明印记 + 光之宝石 + 守卫勋章 + rep>=0 + !guardDead + !hermitDead
 *   SHADOW: 暗影之契 + (guardDead OR rep<0)
 *   NEUTRAL: 平衡之书 + 守卫勋章
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
        engine.getPlayer().setHp(500);
        engine.setRoomActItem("throne", 480, 400);
    }

    // ========== LIGHT ==========

    @Test
    void lightEnding_fullFlow()
    {
        navigateToVault();
        assertTrue(engine.takeItem("gem-light"), "拾取 gem-light");
        returnToOutside();
        giveLightItems();  // 光明印记 + 守卫勋章

        navigateToThroneHall();
        assertTrue(engine.tryTriggerEnding(480, 400), "触摸王座触发结局");
        assertEquals(EndingType.LIGHT, engine.getCurrentEnding());
    }

    // ========== SHADOW ==========

    @Test
    void shadowEnding_killGuard()
    {
        giveItem("shadow-pact");  // 暗影之契
        navigateToGuardRoom();
        killNpc("guard");
        navigateToThroneHallFromGuardRoom();
        assertTrue(engine.tryTriggerEnding(480, 400), "触摸王座触发结局");
        assertEquals(EndingType.SHADOW, engine.getCurrentEnding());
    }

    @Test
    void shadowEnding_killHermit()
    {
        giveItem("shadow-pact");
        navigateToHiddenShrine();
        killNpc("hermit");
        returnToOutsideFromShrine();
        navigateToThroneHall();
        assertTrue(engine.tryTriggerEnding(480, 400), "触摸王座触发结局");
        assertEquals(EndingType.SHADOW, engine.getCurrentEnding());
    }

    @Test
    void shadowEnding_killGuardNoGem()
    {
        giveItem("shadow-pact");
        navigateToGuardRoom();
        killNpc("guard");
        navigateToThroneHallFromGuardRoom();
        assertTrue(engine.tryTriggerEnding(480, 400), "触摸王座触发结局");
        assertEquals(EndingType.SHADOW, engine.getCurrentEnding());
    }

    // ========== NEUTRAL ==========

    @Test
    void neutralEnding_balancePath()
    {
        giveItem("balance-book");
        giveItem("guard-medal");
        engine.setFlag("refused-priest");
        engine.setFlag("refused-follower");
        navigateToThroneHall();
        assertTrue(engine.tryTriggerEnding(480, 400), "触摸王座触发结局");
        assertEquals(EndingType.NEUTRAL, engine.getCurrentEnding());
    }

    // ========== FAKE ==========

    @Test
    void fakeEnding_emptyPlayer()
    {
        navigateToThroneHall();
        assertTrue(engine.tryTriggerEnding(480, 400), "触摸王座触发结局");
        assertEquals(EndingType.FAKE, engine.getCurrentEnding(), "什么都没干 → FAKE");
    }

    @Test
    void fakeEnding_justKilledGuard()
    {
        navigateToGuardRoom();
        killNpc("guard");
        navigateToThroneHallFromGuardRoom();
        assertTrue(engine.tryTriggerEnding(480, 400), "触摸王座触发结局");
        assertEquals(EndingType.FAKE, engine.getCurrentEnding(), "杀了守卫但无暗影之契 → FAKE");
    }

    // ========== 导航工具 ==========

    private void navigateToVault()
    {
        engine.movePlayer(Direction.SOUTH);  // outside → lab
        engine.movePlayer(Direction.SOUTH);  // lab → vault
    }

    private void returnToOutside()
    {
        engine.movePlayer(Direction.NORTH);  // vault → lab
        engine.movePlayer(Direction.NORTH);  // lab → outside
    }

    private void navigateToGuardRoom()
    {
        engine.movePlayer(Direction.EAST);   // outside → pub
        engine.movePlayer(Direction.EAST);   // pub → garden
        engine.movePlayer(Direction.SOUTH);  // garden → guard-room
    }

    private void navigateToThroneHallFromGuardRoom()
    {
        engine.movePlayer(Direction.SOUTH);
    }

    private void navigateToHiddenShrine()
    {
        engine.movePlayer(Direction.NORTH);  // outside → theatre
        engine.movePlayer(Direction.EAST);   // theatre → library
        engine.movePlayer(Direction.NORTH);  // library → hidden-shrine
    }

    private void returnToOutsideFromShrine()
    {
        engine.movePlayer(Direction.SOUTH);  // shrine → library
        engine.movePlayer(Direction.WEST);   // library → theatre
        engine.movePlayer(Direction.SOUTH);  // theatre → outside
    }

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
        assertEquals(CombatOutcome.VICTORY, snap.outcome, "应能击败 " + npcId);
    }

    private void giveLightItems()
    {
        giveItem("light-mark");
        giveItem("guard-medal");
    }

    private void giveItem(String itemId)
    {
        engine.getPlayer().addItem(new Item(itemId, itemId, "test", 1, null));
    }
}
