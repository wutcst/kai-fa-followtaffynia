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
 *   LIGHT:   光明印记 + 光之宝石 + 守卫勋章 + rep>=0 + !guardDead + !hermitDead
 *   SHADOW:  暗影之契 + (guardDead OR rep<0)
 *   NEUTRAL: 平衡之书 + 守卫勋章（不满足光明/暗影时）
 *   回退（无新物品）：旧版规则
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

    // ========== LIGHT ==========

    @Test
    void lightEnding_allConditionsMet()
    {
        giveLightItems();
        player.setReputation(10);
        assertEquals(EndingType.LIGHT, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void lightEnding_reputationZeroIsOk()
    {
        giveLightItems();
        player.setReputation(0);
        assertEquals(EndingType.LIGHT, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void lightEnding_missingLightMark()
    {
        giveGem();
        giveItem("guard-medal", "Guard Medal", 1, null);
        player.setReputation(10);
        // 有宝石+勋章但没有光明印记 → 旧版回退规则 → LIGHT
        assertEquals(EndingType.LIGHT, evaluator.evaluate(player, defeatedNpcs));
    }

    // ========== SHADOW ==========

    @Test
    void shadowEnding_guardKilled()
    {
        giveItem("shadow-pact", "Shadow Pact", 1, null);
        defeatedNpcs.add("guard");
        assertEquals(EndingType.SHADOW, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void shadowEnding_negativeRep()
    {
        giveItem("shadow-pact", "Shadow Pact", 1, null);
        player.setReputation(-5);
        assertEquals(EndingType.SHADOW, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void shadowEnding_noShadowPact()
    {
        player.setReputation(-5);
        defeatedNpcs.add("guard");
        // 无暗影之契 → 回退到旧版规则 → rep<0 → SHADOW
        assertEquals(EndingType.SHADOW, evaluator.evaluate(player, defeatedNpcs));
    }

    // ========== NEUTRAL ==========

    @Test
    void neutralEnding_balanceBook()
    {
        giveItem("balance-book", "Balance Book", 1, null);
        giveItem("guard-medal", "Guard Medal", 1, null);
        player.setReputation(10);
        assertEquals(EndingType.NEUTRAL, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void neutralEnding_missingMedal()
    {
        giveItem("balance-book", "Balance Book", 1, null);
        player.setReputation(10);
        // 有平衡书但缺守卫勋章 → 回退到旧版 → !guardDead && !hermitDead → NEUTRAL
        assertEquals(EndingType.NEUTRAL, evaluator.evaluate(player, defeatedNpcs));
    }

    // ========== 回退兼容（无新物品，旧版规则）==========

    @Test
    void legacy_lightWithGemOnly()
    {
        giveGem();
        player.setReputation(10);
        assertEquals(EndingType.LIGHT, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void legacy_shadowHermitKilled()
    {
        defeatedNpcs.add("hermit");
        assertEquals(EndingType.SHADOW, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void legacy_neutralNoGemNobodyDead()
    {
        player.setReputation(10);
        assertEquals(EndingType.NEUTRAL, evaluator.evaluate(player, defeatedNpcs));
    }

    @Test
    void nullDefeatedNpcs_doesNotCrash()
    {
        giveLightItems();
        player.setReputation(0);
        assertEquals(EndingType.LIGHT, evaluator.evaluate(player, null));
    }

    // ========== helpers ==========

    private void giveLightItems()
    {
        giveGem();
        giveItem("light-mark", "Light Mark", 1, null);
        giveItem("guard-medal", "Guard Medal", 1, null);
    }

    private void giveGem()
    {
        player.getInventory().clear();
        giveItem("gem-light", "Light Gem", 5, "light:full");
    }

    private void giveItem(String id, String name, double weight, String effect)
    {
        player.addItem(new Item(id, name, "test", weight, effect));
    }
}
