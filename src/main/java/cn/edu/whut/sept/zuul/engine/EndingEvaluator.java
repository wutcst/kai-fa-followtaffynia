package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Player;

import java.util.Set;

/**
 * 根据玩家状态与 NPC 命运判定三结局（光明 / 暗影 / 中立）。
 *
 * <h3>判定规则</h3>
 * <pre>
 * LIGHT:  光明印记 + 光明宝石 + 守卫勋章 + 声望>=0 + 守卫未死 + 隐士未死
 * SHADOW: 暗影之契 + (守卫死 OR 声望<0)
 * NEUTRAL: 平衡之书 + 守卫勋章（不满足光明/暗影时触发）
 * </pre>
 */
public final class EndingEvaluator
{
    public EndingType evaluate(Player player, Set<String> defeatedNpcs)
    {
        boolean hasLightMark = hasItem(player, "light-mark");
        boolean hasGem = hasItem(player, "gem-light");
        boolean hasGuardMedal = hasItem(player, "guard-medal");
        boolean hasShadowPact = hasItem(player, "shadow-pact");
        boolean hasBalanceBook = hasItem(player, "balance-book");

        int reputation = player.getReputation();
        boolean guardDead = defeatedNpcs != null && defeatedNpcs.contains("guard");
        boolean hermitDead = defeatedNpcs != null && defeatedNpcs.contains("hermit");

        // 光明结局：光明印记 + 光之宝石 + 守卫勋章 + 声望OK + 无人死亡
        if (hasLightMark && hasGem && hasGuardMedal
            && reputation >= 0 && !guardDead && !hermitDead) {
            return EndingType.LIGHT;
        }

        // 暗影结局：暗影之契 + (杀守卫 OR 声望为负)
        if (hasShadowPact && (guardDead || reputation < 0)) {
            return EndingType.SHADOW;
        }

        // 中立结局：平衡之书 + 守卫勋章
        if (hasBalanceBook && hasGuardMedal) {
            return EndingType.NEUTRAL;
        }

        // 回退：旧版兼容逻辑（无新物品时按旧规则判定）
        boolean hasAnyNewItem = hasLightMark || hasShadowPact || hasBalanceBook;
        if (!hasAnyNewItem) {
            if (hasGem && reputation >= 0 && !guardDead && !hermitDead) {
                return EndingType.LIGHT;
            }
            if (reputation < 0 || hermitDead || (guardDead && !hasGem)) {
                return EndingType.SHADOW;
            }
            if (!guardDead && !hermitDead) {
                return EndingType.NEUTRAL;
            }
        }
        return EndingType.NEUTRAL;
    }

    private static boolean hasItem(Player player, String itemId)
    {
        return player.getInventory().stream()
            .anyMatch(i -> itemId.equals(i.getItemId()));
    }
}
