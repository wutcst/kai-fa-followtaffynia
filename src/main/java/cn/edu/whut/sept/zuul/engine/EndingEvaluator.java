package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Player;

import java.util.Set;

/**
 * 根据玩家状态与 NPC 命运判定四结局。
 *
 * <h3>真结局（必须满足严格条件）</h3>
 * <pre>
 * LIGHT:   光明印记 + 光之宝石 + 守卫勋章 + 声望>=0 + 守卫未死 + 隐士未死
 * SHADOW:  暗影之契 + (守卫死 OR 声望<0)
 * NEUTRAL: 拒绝祭司 + 拒绝信徒 + 平衡之书 + 守卫勋章 + 声望>=0
 * </pre>
 *
 * <h3>假结局（兜底）</h3>
 * 未满足任何真结局条件 → FAKE —— 王座无回应，主角如蜉蝣消失。
 */
public final class EndingEvaluator
{
    public EndingType evaluate(Player player, Set<String> defeatedNpcs,
                                Set<String> playerFlags)
    {
        boolean hasLightMark = hasItem(player, "light-mark");
        boolean hasGem = hasItem(player, "gem-light");
        boolean hasGuardMedal = hasItem(player, "guard-medal");
        boolean hasShadowPact = hasItem(player, "shadow-pact");
        boolean hasBalanceBook = hasItem(player, "balance-book");

        int reputation = player.getReputation();
        boolean guardDead = defeatedNpcs != null && defeatedNpcs.contains("guard");
        boolean hermitDead = defeatedNpcs != null && defeatedNpcs.contains("hermit");

        // 光明结局：三步缺一不可
        if (hasLightMark && hasGem && hasGuardMedal
            && reputation >= 0 && !guardDead && !hermitDead) {
            return EndingType.LIGHT;
        }

        // 暗影结局：拥抱暗影，以力破门
        if (hasShadowPact && (guardDead || reputation < 0)) {
            return EndingType.SHADOW;
        }

        // 中立结局：拒绝光暗两条邀请，走第三条路
        boolean refusedPriest = playerFlags != null && playerFlags.contains("refused-priest");
        boolean refusedFollower = playerFlags != null && playerFlags.contains("refused-follower");
        if (hasBalanceBook && hasGuardMedal
            && refusedPriest && refusedFollower
            && reputation >= 0 && !guardDead && !hermitDead) {
            return EndingType.NEUTRAL;
        }

        // 不满足任何真结局 → 假结局
        return EndingType.FAKE;
    }

    private static boolean hasItem(Player player, String itemId)
    {
        return player.getInventory().stream()
            .anyMatch(i -> itemId.equals(i.getItemId()));
    }
}
