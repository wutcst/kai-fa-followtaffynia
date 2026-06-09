package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Item;
import cn.edu.whut.sept.zuul.domain.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 三结局全路径测试。
 *
 * 结局公式：
 *   LIGHT:  has gem-light AND rep >= 0 AND guard NOT dead AND hermit NOT dead
 *   SHADOW: rep < 0 OR hermit dead OR (guard dead AND no gem)
 *   NEUTRAL: 其余
 */
class EndingEvaluatorTest
{
    private EndingEvaluator evaluator;
    private Player player;
    private Set<String> defeatedNpcs;

    @BeforeEach
    void setUp()
    {
        evaluator = new EndingEvaluator();
        player = new Player("测试者");
        defeatedNpcs = new HashSet<>();
    }

    // ================================================
    // LIGHT
    // ================================================

    @Test
    void lightEnding_allConditionsMet()
    {
        giveGem();
        player.setReputation(10);
        assertEquals(EndingType.LIGHT, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void lightEnding_reputationZeroIsOk()
    {
        giveGem();
        player.setReputation(0);
        assertEquals(EndingType.LIGHT, evaluator.evaluate(player, defeatedNpcs));
    }

    // ================================================
    // SHADOW — reputation < 0
    // ================================================

    @Test
    void shadowEnding_negativeReputation()
    {
        player.setReputation(-1);
        assertEquals(EndingType.SHADOW, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void shadowEnding_negativeRepWithGem()
    {
        // 有 gem 但声望为负 → SHADOW（声望 < 0 优先触发）
        giveGem();
        player.setReputation(-5);
        assertEquals(EndingType.SHADOW, evaluator.evaluate(player, defeatedNpcs));
    }

    // ================================================
    // SHADOW — hermit dead
    // ================================================

    @Test
    void shadowEnding_hermitKilled()
    {
        defeatedNpcs.add("hermit");
        assertEquals(EndingType.SHADOW, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void shadowEnding_hermitKilledWithGem()
    {
        giveGem();
        player.setReputation(10);
        defeatedNpcs.add("hermit");
        // 即使有 gem 和好声望，杀隐士 → 暗影
        assertEquals(EndingType.SHADOW, evaluator.evaluate(player, defeatedNpcs));
    }

    // ================================================
    // SHADOW — guard dead + no gem
    // ================================================

    @Test
    void shadowEnding_guardKilledNoGem()
    {
        defeatedNpcs.add("guard");
        assertEquals(EndingType.SHADOW, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void shadowEnding_bothKilled()
    {
        defeatedNpcs.add("guard");
        defeatedNpcs.add("hermit");
        assertEquals(EndingType.SHADOW, evaluator.evaluate(player, defeatedNpcs));
    }

    // ================================================
    // NEUTRAL
    // ================================================

    @Test
    void neutralEnding_noGemNobodyDead()
    {
        player.setReputation(10);
        assertEquals(EndingType.NEUTRAL, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void neutralEnding_guardDeadButHasGem()
    {
        // 杀守卫但有 gem → rep >= 0, hasGem=true, guardDead=true
        // LIGHT: !guardDead → false
        // SHADOW: rep>=0, !hermitDead, guardDead&&!hasGem=false → false
        // → NEUTRAL
        giveGem();
        player.setReputation(0);
        defeatedNpcs.add("guard");
        assertEquals(EndingType.NEUTRAL, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void neutralEnding_repNegativeButFromCombat()
    {
        // 杀守卫(-15)但过程中拿了 gem → rep=-15, hasGem=true
        // SHADOW: rep<0 → true!
        // 等等……这就是 SHADOW，不是 NEUTRAL
        // 这个测试确认：有 gem + 杀守卫 → 声望为负 → SHADOW
        giveGem();
        player.setReputation(-15);
        defeatedNpcs.add("guard");
        assertEquals(EndingType.SHADOW, evaluator.evaluate(player, defeatedNpcs));
    }

    // ================================================
    // 边界
    // ================================================

    @Test
    void nullDefeatedNpcs_doesNotCrash()
    {
        giveGem();
        player.setReputation(0);
        assertEquals(EndingType.LIGHT, evaluator.evaluate(player, null));
    }

    @Test
    void emptyInventory_noGem()
    {
        player.setReputation(0);
        assertEquals(EndingType.NEUTRAL, evaluator.evaluate(player, defeatedNpcs));
    }

    // ================================================
    // 工具
    // ================================================

    private void giveGem()
    {
        player.getInventory().clear();
        player.addItem(new Item("gem-light", "Light Gem",
            "A radiant gem.", 5, "light:full"));
    }
}
