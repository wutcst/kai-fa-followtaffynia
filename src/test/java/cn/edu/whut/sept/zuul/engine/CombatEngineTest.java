package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.NpcCombatDef;
import cn.edu.whut.sept.zuul.domain.Player;
import cn.edu.whut.sept.zuul.engine.effect.combat.CombatActionRegistry;
import cn.edu.whut.sept.zuul.infra.CombatLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatEngineTest
{
    private NpcCombatDef guardDef;
    private Player player;
    private CombatEngine combat;

    @BeforeEach
    void setUp() throws IOException
    {
        guardDef = CombatLoader.load("guard");
        player = new Player("战士");
        player.setHp(200);
        combat = new CombatEngine(player, guardDef, new CombatActionRegistry(), new Random(7));
    }

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
}
