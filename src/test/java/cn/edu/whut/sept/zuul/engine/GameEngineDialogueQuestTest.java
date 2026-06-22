package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Dialogue;
import cn.edu.whut.sept.zuul.domain.Direction;
import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.infra.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEngineDialogueQuestTest
{
    private GameEngine engine;

    @BeforeEach
    void setUp()
    {
        engine = new GameEngine("测试者");
        engine.setRoomActItem("throne", 480, 400);
    }

    @Test
    void talkNpc_guardFriendlyUnlocksGate()
    {
        Dialogue greeting = engine.talkNpc("guard");
        assertTrue(engine.isInDialogue());
        assertEquals(2, greeting.getOptionTexts().size());

        Dialogue end = engine.chooseDialogueOption(0);
        assertFalse(engine.isInDialogue());
        assertTrue(end.getText().contains("久仰"));
        assertTrue(engine.isLockUnlocked("guard-gate"));
    }

    @Test
    void talkNpc_hermitIncreasesReputation()
    {
        int before = engine.getPlayer().getReputation();
        engine.talkNpc("hermit");
        engine.chooseDialogueOption(1);
        assertEquals(before + 5, engine.getPlayer().getReputation());
    }

    @Test
    void questCompletesWhenTakingGemLight()
    {
        goToVaultWithKey();
        assertTrue(engine.takeItem("gem-light"));
        assertTrue(engine.getQuestManager().isCompleted(QuestManager.QUEST_VAULT));
    }

    @Test
    void endingLightWhenHasGem()
    {
        engine.getPlayer().addItem(new Item("gem-light", "Light Gem",
            "test", 5, "light:full"));
        engine.getPlayer().addItem(new Item("light-mark", "Light Mark",
            "test", 1, null));
        engine.getPlayer().addItem(new Item("guard-medal", "Guard Medal",
            "test", 1, null));
        enterThroneHall();
        assertEquals(EndingType.LIGHT, engine.getCurrentEnding());
        assertTrue(engine.getQuestManager().isCompleted(QuestManager.QUEST_THRONE));
    }

    @Test
    void endingShadowWhenNegativeReputation()
    {
        engine.getPlayer().setReputation(-1);
        engine.getPlayer().addItem(new Item("shadow-pact", "Shadow Pact",
            "test", 1, null));
        enterThroneHall();
        assertEquals(EndingType.SHADOW, engine.getCurrentEnding());
    }

    @Test
    void endingShadowWhenGuardDefeatedWithGem()
    {
        engine.getPlayer().addItem(new Item("gem-light", "Light Gem",
            "test", 5, "light:full"));
        engine.getPlayer().addItem(new Item("shadow-pact", "Shadow Pact",
            "test", 1, null));
        engine.getPlayer().setHp(200);
        engine.startCombat("guard");
        for (int i = 0; i < 30 && engine.isInCombat(); i++) {
            engine.combatAction(CombatAction.ATTACK, null);
        }
        assertFalse(engine.isInCombat());
        assertTrue(engine.getDefeatedNpcs().contains("guard"));
        enterThroneHall();
        assertEquals(EndingType.SHADOW, engine.getCurrentEnding());
    }

    @Test
    void merchantCannotFight()
    {
        EncounterMenu menu = engine.startNpcEncounter("merchant");
        assertFalse(menu.canFight);
    }

    @Test
    void combatVictoryUnlocksGuardGate()
    {
        engine.getPlayer().setHp(200);
        engine.startCombat("guard");
        for (int i = 0; i < 30 && engine.isInCombat(); i++) {
            engine.combatAction(CombatAction.ATTACK, null);
        }
        assertTrue(engine.isLockUnlocked("guard-gate"));
        assertTrue(engine.getDefeatedNpcs().contains("guard"));
        assertEquals(CombatOutcome.VICTORY, engine.getLastCombatOutcome());
        assertNull(engine.startNpcEncounter("guard"), "死亡 NPC 不应再次创建遭遇菜单");
    }

    @Test
    void guardMercyKeepsGateLockedAndGuardAlive()
    {
        int reputationBefore = engine.getPlayer().getReputation();
        engine.getPlayer().setHp(9999);
        engine.startCombat("guard", CombatMode.UNDERTALE);
        UndertaleCombatEngine combat = (UndertaleCombatEngine) engine.getCombatSystem();
        assertNotNull(combat);

        combat.dismissBattleLine();
        combat.selectMercy();
        combat.dismissBattleLine();
        combat.updateEnemyTurn(6f);
        assertEquals(UndertaleCombatPhase.MENU, combat.getPhase());

        combat.selectMercy();
        combat.dismissBattleLine();
        engine.applyCombatOutcome();

        assertFalse(engine.isLockUnlocked("guard-gate"), "MERCY 不应开启守卫之门");
        assertFalse(engine.getDefeatedNpcs().contains("guard"), "MERCY 不算杀死守卫");
        assertTrue(engine.getSparedNpcs().contains("guard"), "守卫应保持存活的仁慈状态");
        assertEquals(reputationBefore, engine.getPlayer().getReputation(),
            "MERCY 不应应用击杀声望惩罚");

        EncounterMenu menu = engine.startNpcEncounter("guard");
        assertNotNull(menu, "MERCY 后守卫仍应留在原地并可交互");
        assertTrue(menu.canTalk);
        assertTrue(menu.canFight);
    }

    @Test
    void defeatedNpcsRoundTripInSave()
    {
        engine.getPlayer().setHp(200);
        engine.startCombat("guard");
        for (int i = 0; i < 30 && engine.isInCombat(); i++) {
            engine.combatAction(CombatAction.ATTACK, null);
        }
        assertTrue(engine.getDefeatedNpcs().contains("guard"));
        GameState state = engine.captureState();
        GameEngine loaded = new GameEngine("other");
        loaded.restoreState(state);
        assertTrue(loaded.getDefeatedNpcs().contains("guard"));
        assertFalse(loaded.isInCombat());
    }

    @Test
    void questStatesRoundTripInSave()
    {
        engine.getQuestManager().onItemTaken(
            new Item("gem-light", "Light Gem", "test", 5, null));
        GameState state = engine.captureState();
        GameEngine loaded = new GameEngine("other");
        loaded.restoreState(state);
        assertTrue(loaded.getQuestManager().isCompleted(QuestManager.QUEST_VAULT));
    }

    private void goToVaultWithKey()
    {
        engine.movePlayer(Direction.SOUTH);
        engine.takeItem("key-vault");
        engine.useItem("key-vault");
        engine.movePlayer(Direction.SOUTH);
    }

    private void enterThroneHall()
    {
        engine.movePlayer(Direction.WEST);
        engine.takeItem("key-guard");
        engine.useItem("key-guard");
        engine.movePlayer(Direction.EAST);
        engine.movePlayer(Direction.EAST);
        engine.movePlayer(Direction.EAST);
        engine.movePlayer(Direction.SOUTH);
        engine.movePlayer(Direction.SOUTH);
        assertEquals("throne-hall", engine.getCurrentRoom().getRoomId());
        assertTrue(engine.tryTriggerEnding(480, 400), "触摸王座触发结局");
    }
}
