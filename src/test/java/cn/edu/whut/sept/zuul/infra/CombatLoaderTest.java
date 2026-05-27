package cn.edu.whut.sept.zuul.infra;

import cn.edu.whut.sept.zuul.domain.NpcCombatDef;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatLoaderTest
{
    @Test
    void loadGuardJson() throws IOException
    {
        NpcCombatDef def = CombatLoader.load("guard");
        assertEquals("guard", def.npcId);
        assertEquals("王座守卫", def.displayName);
        assertEquals(80, def.maxHp);
        assertTrue(def.skills.containsKey("stab"));
        assertTrue(def.stateSkills.containsKey("enraged"));
        assertEquals(-15, def.onDefeatReputation);
        assertEquals("guard-gate", def.onDefeatUnlock);
        assertTrue(def.onDefeatMarkDefeated);
    }
}
