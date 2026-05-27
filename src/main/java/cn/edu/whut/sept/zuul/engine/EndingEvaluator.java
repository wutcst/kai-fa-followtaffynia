package cn.edu.whut.sept.zuul.engine;

import cn.edu.whut.sept.zuul.domain.Player;

import java.util.Set;

/**
 * 根据玩家状态与 NPC 命运判定三结局（光明 / 暗影 / 中立）。
 */
public final class EndingEvaluator
{
    public EndingType evaluate(Player player, Set<String> defeatedNpcs)
    {
        boolean hasGem = player.getInventory().stream()
            .anyMatch(i -> "gem-light".equals(i.getItemId()));
        int reputation = player.getReputation();
        boolean guardDead = defeatedNpcs != null && defeatedNpcs.contains("guard");
        boolean hermitDead = defeatedNpcs != null && defeatedNpcs.contains("hermit");

        if (hasGem && reputation >= 0 && !guardDead && !hermitDead) {
            return EndingType.LIGHT;
        }
        if (reputation < 0 || hermitDead || (guardDead && !hasGem)) {
            return EndingType.SHADOW;
        }
        return EndingType.NEUTRAL;
    }
}
