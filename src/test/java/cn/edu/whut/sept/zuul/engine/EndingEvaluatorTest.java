package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 四结局全路径测试。
 *
 * <pre>
 * LIGHT:   光明印记 + 光之宝石 + 守卫勋章 + rep>=0 + !guardDead + !hermitDead
 * SHADOW:  暗影之契 + (guardDead OR rep<0)
 * NEUTRAL: 拒绝光暗 + 平衡之书 + 守卫勋章 + rep>=0 + 无人死亡
 * FAKE:    其他所有情况 —— 王座无回应
 * </pre>
 */
class EndingEvaluatorTest
{
    private EndingEvaluator evaluator;
    private Player player;
    private Set<String> defeatedNpcs;
    private Set<String> playerFlags;

    @BeforeEach
    void setUp()
    {
        evaluator = new EndingEvaluator();
        player = new Player("测试者");
        defeatedNpcs = new HashSet<>();
        playerFlags = new HashSet<>();
    }

    // ========== LIGHT ==========

    @Test
    void lightEnding_allConditionsMet()
    {
        giveLightItems();
        player.setReputation(10);
        assertEquals(EndingType.LIGHT, eval());
    }

    @Test
    void lightEnding_missingLightMark()
    {
        giveGem(); giveMedal();
        player.setReputation(10);
        assertEquals(EndingType.FAKE, eval(), "缺光明印记 → FAKE");
    }

    @Test
    void lightEnding_missingGem()
    {
        giveItem("light-mark", 1); giveMedal();
        player.setReputation(10);
        assertEquals(EndingType.FAKE, eval(), "缺光之宝石 → FAKE");
    }

    // ========== SHADOW ==========

    @Test
    void shadowEnding_guardKilled()
    {
        giveItem("shadow-pact", 1);
        defeatedNpcs.add("guard");
        assertEquals(EndingType.SHADOW, eval());
    }

    @Test
    void shadowEnding_negativeRep()
    {
        giveItem("shadow-pact", 1);
        player.setReputation(-5);
        assertEquals(EndingType.SHADOW, eval());
    }

    @Test
    void shadowEnding_noShadowPact()
    {
        defeatedNpcs.add("guard");
        player.setReputation(-5);
        assertEquals(EndingType.FAKE, eval(), "无暗影之契 → FAKE");
    }

    // ========== NEUTRAL ==========

    @Test
    void neutralEnding_allConditionsMet()
    {
        giveItem("balance-book", 1); giveMedal();
        playerFlags.add("refused-priest");
        playerFlags.add("refused-follower");
        player.setReputation(10);
        assertEquals(EndingType.NEUTRAL, eval());
    }

    @Test
    void neutralEnding_missingFlag()
    {
        giveItem("balance-book", 1); giveMedal();
        player.setReputation(10);
        // 有物品但没拒绝光暗 → FAKE
        assertEquals(EndingType.FAKE, eval());
    }

    @Test
    void neutralEnding_killedGuard()
    {
        giveItem("balance-book", 1); giveMedal();
        playerFlags.add("refused-priest");
        playerFlags.add("refused-follower");
        player.setReputation(10);
        defeatedNpcs.add("guard");
        assertEquals(EndingType.FAKE, eval(), "杀守卫 → FAKE");
    }

    // ========== FAKE ==========

    @Test
    void fakeEnding_emptyInventory()
    {
        player.setReputation(0);
        assertEquals(EndingType.FAKE, eval());
    }

    @Test
    void fakeEnding_justGem()
    {
        giveGem();
        player.setReputation(10);
        assertEquals(EndingType.FAKE, eval(), "仅宝石 → FAKE");
    }

    @Test
    void fakeEnding_guardKilledNoShadowPact()
    {
        defeatedNpcs.add("guard");
        assertEquals(EndingType.FAKE, eval());
    }

    @Test
    void nullFlagsAndDefeatedNpcs_doesNotCrash()
    {
        giveLightItems();
        player.setReputation(0);
        assertEquals(EndingType.LIGHT, evaluator.evaluate(player, null, null));
    }

    // ========== canon / isCanon ==========

    @Test
    void canonEndings()
    {
        assertTrue(EndingType.LIGHT.isCanon());
        assertTrue(EndingType.SHADOW.isCanon());
        assertTrue(EndingType.NEUTRAL.isCanon());
        assertFalse(EndingType.FAKE.isCanon());
        assertFalse(EndingType.NONE.isCanon());
    }

    // ========== helpers ==========

    private EndingType eval()
    {
        return evaluator.evaluate(player, defeatedNpcs, playerFlags);
    }

    private void giveLightItems() { giveGem(); giveItem("light-mark", 1); giveMedal(); }
    private void giveMedal() { giveItem("guard-medal", 1); }
    private void giveGem() { giveItem("gem-light", 5); }
    private void giveItem(String id, double weight)
    {
        player.addItem(new Item(id, id, "test", weight, null));
    }
}
