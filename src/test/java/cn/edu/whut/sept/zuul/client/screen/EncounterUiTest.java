package cn.edu.whut.sept.zuul.client.screen;

import cn.edu.whut.sept.zuul.engine.CombatAction;
import cn.edu.whut.sept.zuul.engine.GameEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncounterUiTest
{
    @Test
    void defeatedNpcCannotOpenAnEmptyEncounter()
    {
        GameEngine engine = new GameEngine("测试者");
        engine.getPlayer().setHp(9999);
        engine.startCombat("guard");
        while (engine.isInCombat()) {
            engine.combatAction(CombatAction.ATTACK, null);
        }
        EncounterUi encounter = newUi(engine);

        assertFalse(encounter.openMenu("guard"));
        assertFalse(encounter.isMenuOpen());
    }

    @Test
    void closeMenuReallyClearsTheEncounterState()
    {
        GameEngine engine = new GameEngine("测试者");
        EncounterUi encounter = newUi(engine);

        assertTrue(encounter.openMenu("guard"));
        assertTrue(encounter.isMenuOpen());

        encounter.closeMenu();

        assertFalse(encounter.isMenuOpen());
    }

    private EncounterUi newUi(GameEngine engine)
    {
        DialogueUi dialogue = new DialogueUi(engine, null, null, null, null, null, null, null);
        return new EncounterUi(engine, null, null, null, null, dialogue);
    }
}
